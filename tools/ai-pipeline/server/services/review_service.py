import json
from copy import deepcopy
from dataclasses import dataclass
from datetime import datetime, timezone
from hashlib import sha256
from pathlib import Path
from typing import Any

from server.core.paths import OUTPUTS_DIR, to_relative_path
from server.models.review import (
    ApprovedArtifactExportResponse,
    BulkReviewApproveArtifactResponse,
    BulkReviewApproveRequest,
    BulkReviewApproveSubjectResponse,
    BulkSubjectReviewApproveRequest,
    ReviewArtifactSummary,
    ReviewArtifactType,
    ReviewDecisionRequest,
    ReviewItemListResponse,
    ReviewItemResponse,
    ReviewStatus,
)


class ReviewSourceNotFoundError(Exception):
    pass


class ReviewItemNotFoundError(Exception):
    pass


class ReviewDataError(Exception):
    pass


@dataclass(frozen=True)
class ReviewArtifactSpec:
    artifact_type: ReviewArtifactType
    source_file_name: str


@dataclass(frozen=True)
class SourceItem:
    item_key: str
    content: Any


REVIEW_ARTIFACT_SPECS = {
    ReviewArtifactType.TAXONOMY: ReviewArtifactSpec(
        artifact_type=ReviewArtifactType.TAXONOMY,
        source_file_name="taxonomy.json",
    ),
    ReviewArtifactType.CONCEPT_CARDS: ReviewArtifactSpec(
        artifact_type=ReviewArtifactType.CONCEPT_CARDS,
        source_file_name="concept_cards.json",
    ),
    ReviewArtifactType.CONCEPT_RELATIONS: ReviewArtifactSpec(
        artifact_type=ReviewArtifactType.CONCEPT_RELATIONS,
        source_file_name="concept_relations.json",
    ),
    ReviewArtifactType.COMMON_MISTAKES: ReviewArtifactSpec(
        artifact_type=ReviewArtifactType.COMMON_MISTAKES,
        source_file_name="common_mistakes.json",
    ),
    ReviewArtifactType.QUESTIONS: ReviewArtifactSpec(
        artifact_type=ReviewArtifactType.QUESTIONS,
        source_file_name="questions.json",
    ),
    ReviewArtifactType.QUESTION_REVIEWS: ReviewArtifactSpec(
        artifact_type=ReviewArtifactType.QUESTION_REVIEWS,
        source_file_name="question_reviews.json",
    ),
    ReviewArtifactType.CONCEPT_DIAGNOSTIC_PROFILES: ReviewArtifactSpec(
        artifact_type=ReviewArtifactType.CONCEPT_DIAGNOSTIC_PROFILES,
        source_file_name="concept_diagnostic_profiles.json",
    ),
    ReviewArtifactType.QUESTION_DIAGNOSTIC_PROFILES: ReviewArtifactSpec(
        artifact_type=ReviewArtifactType.QUESTION_DIAGNOSTIC_PROFILES,
        source_file_name="question_diagnostic_profiles.json",
    ),
}

DB_IMPORT_REVIEW_ARTIFACT_TYPES = [
    ReviewArtifactType.TAXONOMY,
    ReviewArtifactType.CONCEPT_CARDS,
    ReviewArtifactType.CONCEPT_RELATIONS,
    ReviewArtifactType.COMMON_MISTAKES,
    ReviewArtifactType.QUESTIONS,
    # 진단 프로필은 아직 생성하지 않은 과목이 많아, 없으면 건너뛴다(아래 과목 일괄 승인 참고).
    ReviewArtifactType.CONCEPT_DIAGNOSTIC_PROFILES,
    ReviewArtifactType.QUESTION_DIAGNOSTIC_PROFILES,
]


def list_review_artifacts(subject_code: str) -> list[ReviewArtifactSummary]:
    normalized_subject_code = _normalize_subject_code(subject_code)

    return [
        _build_artifact_summary(
            subject_code=normalized_subject_code,
            artifact_type=artifact_type,
        )
        for artifact_type in REVIEW_ARTIFACT_SPECS
    ]


def list_review_items(
    *,
    subject_code: str,
    artifact_type: ReviewArtifactType,
) -> ReviewItemListResponse:
    normalized_subject_code = _normalize_subject_code(subject_code)
    source_path = _source_path(normalized_subject_code, artifact_type)
    review_path = _review_path(normalized_subject_code, artifact_type)
    source_items = _load_source_items(
        subject_code=normalized_subject_code,
        artifact_type=artifact_type,
    )
    review_records = _read_review_records(review_path)
    items = [
        _to_review_item_response(
            subject_code=normalized_subject_code,
            artifact_type=artifact_type,
            source_item=source_item,
            review_record=review_records.get(source_item.item_key),
        )
        for source_item in source_items
    ]
    counts = _count_review_items(items)

    return ReviewItemListResponse(
        subject_code=normalized_subject_code,
        artifact_type=artifact_type,
        source_path=to_relative_path(source_path),
        review_path=to_relative_path(review_path),
        total_count=len(items),
        pending_count=counts[ReviewStatus.PENDING],
        approved_count=counts[ReviewStatus.APPROVED],
        rejected_count=counts[ReviewStatus.REJECTED],
        edited_count=sum(1 for item in items if item.has_edits),
        items=items,
    )


def get_review_item(
    *,
    subject_code: str,
    artifact_type: ReviewArtifactType,
    item_key: str,
) -> ReviewItemResponse:
    for item in list_review_items(
        subject_code=subject_code,
        artifact_type=artifact_type,
    ).items:
        if item.item_key == item_key:
            return item

    raise ReviewItemNotFoundError(f"review item not found: {item_key}")


def update_review_item(
    *,
    subject_code: str,
    artifact_type: ReviewArtifactType,
    item_key: str,
    request: ReviewDecisionRequest,
) -> ReviewItemResponse:
    normalized_subject_code = _normalize_subject_code(subject_code)
    review_path = _review_path(normalized_subject_code, artifact_type)
    source_items = _load_source_items(
        subject_code=normalized_subject_code,
        artifact_type=artifact_type,
    )
    source_item = next(
        (item for item in source_items if item.item_key == item_key),
        None,
    )

    if source_item is None:
        raise ReviewItemNotFoundError(f"review item not found: {item_key}")

    review_records = _read_review_records(review_path)
    review_records[item_key] = {
        "status": request.status.value,
        "reviewer": request.reviewer,
        "review_note": request.review_note,
        "edited_content": request.edited_content,
        "updated_at": datetime.now(timezone.utc).isoformat(),
    }
    _write_review_records(
        subject_code=normalized_subject_code,
        artifact_type=artifact_type,
        review_path=review_path,
        review_records=review_records,
    )

    return _to_review_item_response(
        subject_code=normalized_subject_code,
        artifact_type=artifact_type,
        source_item=source_item,
        review_record=review_records[item_key],
    )


def bulk_approve_subject_artifacts(
    *,
    subject_code: str,
    request: BulkSubjectReviewApproveRequest,
) -> BulkReviewApproveSubjectResponse:
    normalized_subject_code = _normalize_subject_code(subject_code)
    artifact_types = request.artifact_types or DB_IMPORT_REVIEW_ARTIFACT_TYPES
    seen_artifact_types: set[ReviewArtifactType] = set()
    responses = []

    for artifact_type in artifact_types:
        if artifact_type in seen_artifact_types:
            continue

        seen_artifact_types.add(artifact_type)

        try:
            responses.append(
                bulk_approve_artifact(
                    subject_code=normalized_subject_code,
                    artifact_type=artifact_type,
                    request=BulkReviewApproveRequest(
                        reviewer=request.reviewer,
                        review_note=request.review_note,
                        overwrite_existing=request.overwrite_existing,
                        export_approved=request.export_approved,
                    ),
                )
            )
        except ReviewSourceNotFoundError:
            # 과목 단위 승인은 "있는 것만 승인"한다. 아직 생성하지 않은 아티팩트
            # (예: 진단 프로필) 때문에 과목 전체 승인이 실패하지 않도록 건너뛴다.
            # 아티팩트를 콕 집어 요청하는 엔드포인트는 기존대로 404 를 낸다.
            continue

    return BulkReviewApproveSubjectResponse(
        subject_code=normalized_subject_code,
        artifacts=responses,
    )


def bulk_approve_artifact(
    *,
    subject_code: str,
    artifact_type: ReviewArtifactType,
    request: BulkReviewApproveRequest,
) -> BulkReviewApproveArtifactResponse:
    normalized_subject_code = _normalize_subject_code(subject_code)
    review_path = _review_path(normalized_subject_code, artifact_type)
    source_items = _load_source_items(
        subject_code=normalized_subject_code,
        artifact_type=artifact_type,
    )
    review_records = _read_review_records(review_path)
    now = datetime.now(timezone.utc).isoformat()
    changed_count = 0
    already_approved_count = 0
    skipped_count = 0

    for source_item in source_items:
        normalized_record = _normalize_review_record(
            review_records.get(source_item.item_key)
        )
        current_status = ReviewStatus(normalized_record["status"])

        if current_status == ReviewStatus.APPROVED:
            already_approved_count += 1
            if not request.overwrite_existing:
                continue
        elif current_status != ReviewStatus.PENDING and not request.overwrite_existing:
            skipped_count += 1
            continue

        review_records[source_item.item_key] = {
            "status": ReviewStatus.APPROVED.value,
            "reviewer": request.reviewer,
            "review_note": request.review_note,
            "edited_content": normalized_record.get("edited_content"),
            "updated_at": now,
        }
        changed_count += 1

    _write_review_records(
        subject_code=normalized_subject_code,
        artifact_type=artifact_type,
        review_path=review_path,
        review_records=review_records,
    )
    response = list_review_items(
        subject_code=normalized_subject_code,
        artifact_type=artifact_type,
    )
    approved_items = [
        item
        for item in response.items
        if item.status == ReviewStatus.APPROVED
    ]
    exported_path = None
    exported_count = None

    if request.export_approved:
        exported = _export_approved_items(
            subject_code=normalized_subject_code,
            artifact_type=artifact_type,
            approved_items=approved_items,
            allow_empty=len(source_items) == 0,
        )
        exported_path = exported.exported_path
        exported_count = exported.approved_count

    return BulkReviewApproveArtifactResponse(
        subject_code=normalized_subject_code,
        artifact_type=artifact_type,
        total_count=response.total_count,
        changed_count=changed_count,
        already_approved_count=already_approved_count,
        skipped_count=skipped_count,
        approved_count=response.approved_count,
        exported_path=exported_path,
        exported_count=exported_count,
    )


def export_approved_artifact(
    *,
    subject_code: str,
    artifact_type: ReviewArtifactType,
) -> ApprovedArtifactExportResponse:
    normalized_subject_code = _normalize_subject_code(subject_code)
    response = list_review_items(
        subject_code=normalized_subject_code,
        artifact_type=artifact_type,
    )
    approved_items = [
        item
        for item in response.items
        if item.status == ReviewStatus.APPROVED
    ]

    return _export_approved_items(
        subject_code=normalized_subject_code,
        artifact_type=artifact_type,
        approved_items=approved_items,
    )


def _export_approved_items(
    *,
    subject_code: str,
    artifact_type: ReviewArtifactType,
    approved_items: list[ReviewItemResponse],
    allow_empty: bool = False,
) -> ApprovedArtifactExportResponse:
    if not approved_items and not allow_empty:
        raise ReviewDataError(
            f"approved review items not found: {subject_code}/{artifact_type.value}"
        )

    if not approved_items:
        exported_content = _empty_approved_content(artifact_type)
    else:
        exported_content = _build_approved_content(
            artifact_type=artifact_type,
            approved_items=approved_items,
        )

    exported_path = _approved_export_path(subject_code, artifact_type)
    _write_json(exported_path, exported_content)

    return ApprovedArtifactExportResponse(
        subject_code=subject_code,
        artifact_type=artifact_type,
        approved_count=len(approved_items),
        exported_path=to_relative_path(exported_path),
        exported_content=exported_content,
    )


def _build_artifact_summary(
    *,
    subject_code: str,
    artifact_type: ReviewArtifactType,
) -> ReviewArtifactSummary:
    source_path = _source_path(subject_code, artifact_type)
    review_path = _review_path(subject_code, artifact_type)
    approved_export_path = _approved_export_path(subject_code, artifact_type)
    error_message = None
    source_items: list[SourceItem] = []

    try:
        source_items = _load_source_items(
            subject_code=subject_code,
            artifact_type=artifact_type,
        )
    except ReviewSourceNotFoundError:
        source_items = []
    except ReviewDataError as exc:
        error_message = str(exc)

    try:
        review_records = _read_review_records(review_path)
    except ReviewDataError as exc:
        review_records = {}
        error_message = str(exc)

    source_item_keys = {item.item_key for item in source_items}
    review_items = [
        _to_review_item_response(
            subject_code=subject_code,
            artifact_type=artifact_type,
            source_item=item,
            review_record=review_records.get(item.item_key),
        )
        for item in source_items
    ]
    counts = _count_review_items(review_items)

    return ReviewArtifactSummary(
        subject_code=subject_code,
        artifact_type=artifact_type,
        source_path=to_relative_path(source_path),
        source_exists=source_path.exists(),
        review_path=to_relative_path(review_path),
        review_exists=review_path.exists(),
        approved_export_path=to_relative_path(approved_export_path),
        approved_export_exists=approved_export_path.exists(),
        total_count=len(source_items),
        pending_count=counts[ReviewStatus.PENDING],
        approved_count=counts[ReviewStatus.APPROVED],
        rejected_count=counts[ReviewStatus.REJECTED],
        edited_count=sum(1 for item in review_items if item.has_edits),
        stale_review_count=len(set(review_records) - source_item_keys),
        error_message=error_message,
    )


def _load_source_items(
    *,
    subject_code: str,
    artifact_type: ReviewArtifactType,
) -> list[SourceItem]:
    source_path = _source_path(subject_code, artifact_type)
    source_data = _read_source_json(source_path)
    return _extract_source_items(artifact_type, source_data)


def _extract_source_items(
    artifact_type: ReviewArtifactType,
    source_data: Any,
) -> list[SourceItem]:
    if artifact_type == ReviewArtifactType.TAXONOMY:
        if not isinstance(source_data, dict):
            raise ReviewDataError("taxonomy.json must be a JSON object")
        return [SourceItem(item_key="taxonomy", content=source_data)]

    if artifact_type == ReviewArtifactType.CONCEPT_CARDS:
        return _extract_list_items(
            artifact_type=artifact_type,
            source_data=source_data,
            key_fields=("concept_code",),
        )

    if artifact_type == ReviewArtifactType.CONCEPT_RELATIONS:
        return _extract_relation_items(source_data)

    if artifact_type == ReviewArtifactType.COMMON_MISTAKES:
        return _extract_common_mistake_items(source_data)

    if artifact_type == ReviewArtifactType.QUESTIONS:
        return _extract_list_items(
            artifact_type=artifact_type,
            source_data=source_data,
            key_fields=("content_hash", "question_code"),
        )

    if artifact_type == ReviewArtifactType.QUESTION_REVIEWS:
        return _extract_question_review_items(source_data)

    if artifact_type == ReviewArtifactType.CONCEPT_DIAGNOSTIC_PROFILES:
        return _extract_list_items(
            artifact_type=artifact_type,
            source_data=source_data,
            key_fields=("concept_code",),
        )

    if artifact_type == ReviewArtifactType.QUESTION_DIAGNOSTIC_PROFILES:
        # 문항 프로필은 문항과 동일하게 content_hash 로 식별한다(09 임포터와 같은 키).
        return _extract_list_items(
            artifact_type=artifact_type,
            source_data=source_data,
            key_fields=("content_hash",),
        )

    raise ReviewDataError(f"unsupported review artifact type: {artifact_type.value}")


def _extract_list_items(
    *,
    artifact_type: ReviewArtifactType,
    source_data: Any,
    key_fields: tuple[str, ...],
) -> list[SourceItem]:
    if not isinstance(source_data, list):
        raise ReviewDataError(f"{artifact_type.value}.json must be a JSON array")

    items = []
    seen_keys: dict[str, int] = {}

    for index, content in enumerate(source_data):
        if not isinstance(content, dict):
            raise ReviewDataError(
                f"{artifact_type.value}.json item[{index}] must be a JSON object"
            )

        base_key = _first_non_empty(content, key_fields) or _content_hash(content)
        items.append(
            SourceItem(
                item_key=_unique_item_key(str(base_key), seen_keys),
                content=content,
            )
        )

    return items


def _extract_relation_items(source_data: Any) -> list[SourceItem]:
    if not isinstance(source_data, list):
        raise ReviewDataError("concept_relations.json must be a JSON array")

    items = []
    seen_keys: dict[str, int] = {}

    for index, relation in enumerate(source_data):
        if not isinstance(relation, dict):
            raise ReviewDataError(
                f"concept_relations.json item[{index}] must be a JSON object"
            )

        base_key = "__".join(
            [
                str(relation.get("source_concept_code") or "unknown_source"),
                str(relation.get("relation_type") or "unknown_relation"),
                str(relation.get("target_concept_code") or "unknown_target"),
            ]
        )
        items.append(
            SourceItem(
                item_key=_unique_item_key(base_key, seen_keys),
                content=relation,
            )
        )

    return items


def _extract_common_mistake_items(source_data: Any) -> list[SourceItem]:
    if not isinstance(source_data, list):
        raise ReviewDataError("common_mistakes.json must be a JSON array")

    items = []
    seen_keys: dict[str, int] = {}

    for group_index, group in enumerate(source_data):
        if not isinstance(group, dict):
            raise ReviewDataError(
                f"common_mistakes.json group[{group_index}] must be a JSON object"
            )

        concept_code = group.get("concept_code")
        concept_name = group.get("concept_name")
        mistakes = group.get("mistakes", [])

        if not isinstance(mistakes, list):
            raise ReviewDataError(
                f"common_mistakes.json group[{group_index}].mistakes must be a JSON array"
            )

        for mistake_index, mistake in enumerate(mistakes):
            if not isinstance(mistake, dict):
                raise ReviewDataError(
                    "common_mistakes.json "
                    f"group[{group_index}].mistakes[{mistake_index}] must be a JSON object"
                )

            content = {
                "concept_code": concept_code,
                "concept_name": concept_name,
                **mistake,
            }
            base_key = mistake.get("mistake_code") or f"{concept_code}_M{mistake_index + 1:02d}"
            items.append(
                SourceItem(
                    item_key=_unique_item_key(str(base_key), seen_keys),
                    content=content,
                )
            )

    return items


def _extract_question_review_items(source_data: Any) -> list[SourceItem]:
    if not isinstance(source_data, list):
        raise ReviewDataError("question_reviews.json must be a JSON array")

    items = []
    seen_keys: dict[str, int] = {}

    for index, review in enumerate(source_data):
        if not isinstance(review, dict):
            raise ReviewDataError(
                f"question_reviews.json item[{index}] must be a JSON object"
            )

        concept_code = review.get("concept_code") or "unknown_concept"
        base_key = f"{concept_code}__{_content_hash(review)}"
        items.append(
            SourceItem(
                item_key=_unique_item_key(base_key, seen_keys),
                content=review,
            )
        )

    return items


def _to_review_item_response(
    *,
    subject_code: str,
    artifact_type: ReviewArtifactType,
    source_item: SourceItem,
    review_record: dict | None,
) -> ReviewItemResponse:
    normalized_record = _normalize_review_record(review_record)
    edited_content = normalized_record.get("edited_content")
    has_edits = edited_content is not None

    return ReviewItemResponse(
        subject_code=subject_code,
        artifact_type=artifact_type,
        item_key=source_item.item_key,
        status=ReviewStatus(normalized_record["status"]),
        reviewer=normalized_record.get("reviewer"),
        review_note=normalized_record.get("review_note"),
        has_edits=has_edits,
        updated_at=normalized_record.get("updated_at"),
        original_content=source_item.content,
        effective_content=edited_content if has_edits else source_item.content,
    )


def _normalize_review_record(review_record: dict | None) -> dict:
    if not review_record:
        return {
            "status": ReviewStatus.PENDING.value,
            "reviewer": None,
            "review_note": None,
            "edited_content": None,
            "updated_at": None,
        }

    status = review_record.get("status", ReviewStatus.PENDING.value)

    if status not in {item.value for item in ReviewStatus}:
        status = ReviewStatus.PENDING.value

    return {
        "status": status,
        "reviewer": review_record.get("reviewer"),
        "review_note": review_record.get("review_note"),
        "edited_content": review_record.get("edited_content"),
        "updated_at": review_record.get("updated_at"),
    }


def _count_review_items(items: list[ReviewItemResponse]) -> dict[ReviewStatus, int]:
    counts = {
        ReviewStatus.PENDING: 0,
        ReviewStatus.APPROVED: 0,
        ReviewStatus.REJECTED: 0,
    }

    for item in items:
        counts[item.status] += 1

    return counts


def _build_approved_content(
    *,
    artifact_type: ReviewArtifactType,
    approved_items: list[ReviewItemResponse],
) -> Any:
    if artifact_type == ReviewArtifactType.TAXONOMY:
        return deepcopy(approved_items[0].effective_content)

    if artifact_type == ReviewArtifactType.COMMON_MISTAKES:
        return _build_approved_common_mistakes(approved_items)

    return [
        _mark_approved(deepcopy(item.effective_content))
        for item in approved_items
    ]


def _empty_approved_content(artifact_type: ReviewArtifactType) -> Any:
    if artifact_type == ReviewArtifactType.TAXONOMY:
        raise ReviewDataError("approved taxonomy item is required")

    return []


def _build_approved_common_mistakes(
    approved_items: list[ReviewItemResponse],
) -> list[dict]:
    groups: dict[str, dict] = {}

    for item in approved_items:
        content = deepcopy(item.effective_content)

        if not isinstance(content, dict):
            raise ReviewDataError(
                f"approved common mistake item must be a JSON object: {item.item_key}"
            )

        concept_code = content.pop("concept_code", None)
        concept_name = content.pop("concept_name", None)

        if not concept_code:
            raise ReviewDataError(
                f"approved common mistake item has no concept_code: {item.item_key}"
            )

        group = groups.setdefault(
            str(concept_code),
            {
                "concept_code": concept_code,
                "concept_name": concept_name,
                "mistakes": [],
            },
        )
        group["mistakes"].append(_mark_approved(content))

    return list(groups.values())


def _mark_approved(value: Any) -> Any:
    if isinstance(value, dict) and "review_status" in value:
        value["review_status"] = "approved"

    return value


def _read_source_json(path: Path) -> Any:
    if not path.exists():
        raise ReviewSourceNotFoundError(f"source artifact not found: {path}")

    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except json.JSONDecodeError as exc:
        raise ReviewDataError(f"source artifact is not valid JSON: {path}") from exc


def _read_review_records(path: Path) -> dict[str, dict]:
    if not path.exists():
        return {}

    try:
        data = json.loads(path.read_text(encoding="utf-8"))
    except json.JSONDecodeError as exc:
        raise ReviewDataError(f"review state is not valid JSON: {path}") from exc

    items = data.get("items", {})

    if not isinstance(items, dict):
        raise ReviewDataError(f"review state items must be a JSON object: {path}")

    return {
        str(item_key): item
        for item_key, item in items.items()
        if isinstance(item, dict)
    }


def _write_review_records(
    *,
    subject_code: str,
    artifact_type: ReviewArtifactType,
    review_path: Path,
    review_records: dict[str, dict],
) -> None:
    payload = {
        "subject_code": subject_code,
        "artifact_type": artifact_type.value,
        "source_file": to_relative_path(_source_path(subject_code, artifact_type)),
        "updated_at": datetime.now(timezone.utc).isoformat(),
        "items": review_records,
    }
    _write_json(review_path, payload)


def _write_json(path: Path, content: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    temp_path = path.with_suffix(f"{path.suffix}.tmp")

    with open(temp_path, "w", encoding="utf-8") as file:
        json.dump(content, file, ensure_ascii=False, indent=2)
        file.write("\n")

    temp_path.replace(path)


def _source_path(subject_code: str, artifact_type: ReviewArtifactType) -> Path:
    spec = REVIEW_ARTIFACT_SPECS[artifact_type]
    return OUTPUTS_DIR / subject_code / spec.source_file_name


def _review_path(subject_code: str, artifact_type: ReviewArtifactType) -> Path:
    return OUTPUTS_DIR / subject_code / "reviews" / "state" / f"{artifact_type.value}.json"


def _approved_export_path(subject_code: str, artifact_type: ReviewArtifactType) -> Path:
    return OUTPUTS_DIR / subject_code / "reviews" / "approved" / f"{artifact_type.value}.json"


def _normalize_subject_code(subject_code: str) -> str:
    return subject_code.upper()


def _first_non_empty(content: dict, fields: tuple[str, ...]) -> Any | None:
    for field in fields:
        value = content.get(field)

        if value is not None and str(value).strip():
            return value

    return None


def _content_hash(content: Any) -> str:
    serialized = json.dumps(
        content,
        ensure_ascii=False,
        sort_keys=True,
        separators=(",", ":"),
    )
    return sha256(serialized.encode("utf-8")).hexdigest()[:16]


def _unique_item_key(base_key: str, seen_keys: dict[str, int]) -> str:
    if base_key not in seen_keys:
        seen_keys[base_key] = 1
        return base_key

    seen_keys[base_key] += 1
    return f"{base_key}__dup_{seen_keys[base_key]}"
