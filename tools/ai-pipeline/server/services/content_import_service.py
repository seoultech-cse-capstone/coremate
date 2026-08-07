import json
import os
from dataclasses import dataclass
from datetime import datetime, timezone
from typing import Any

from dotenv import load_dotenv

from server.core.paths import (
    AI_PIPELINE_ROOT,
    INFRA_ENV_FILE,
    OUTPUTS_DIR,
    to_relative_path,
)
from server.models.content_import import (
    ContentImportArtifactSummary,
    ContentImportRequest,
    ContentImportResponse,
    ContentImportStatus,
    ContentImportTableSummary,
)
from server.models.pipeline import SubjectSummary
from server.services.subject_service import get_subject_config
from src.hash_utils import make_question_hash


class ContentImportError(Exception):
    pass


class ContentImportConfigError(ContentImportError):
    pass


class ContentImportDatabaseError(ContentImportError):
    pass


@dataclass(frozen=True)
class ApprovedArtifacts:
    taxonomy: dict[str, Any]
    concept_cards: list[dict[str, Any]]
    concept_relations: list[dict[str, Any]]
    common_mistakes: list[dict[str, Any]]
    questions: list[dict[str, Any]]
    summaries: list[ContentImportArtifactSummary]


@dataclass(frozen=True)
class ImportPlan:
    subject_code: str
    db_subject_code: str
    artifacts: list[ContentImportArtifactSummary]
    tables: list[ContentImportTableSummary]
    subject: dict[str, Any]
    category_lv1: list[dict[str, Any]]
    category_lv2: list[dict[str, Any]]
    concepts: list[dict[str, Any]]
    concept_relations: list[dict[str, Any]]
    common_mistakes: list[dict[str, Any]]
    questions: list[dict[str, Any]]
    warnings: list[str]


@dataclass(frozen=True)
class MySQLConnectionSettings:
    host: str
    port: int
    database: str
    user: str
    password: str
    charset: str = "utf8mb4"

    @classmethod
    def from_env(cls) -> "MySQLConnectionSettings":
        # DB 비밀번호는 infra/.env 한 곳에만 둔다(백엔드 컨테이너와 같은 값이라
        # 두 벌로 두면 한쪽만 바꿨을 때 조용히 어긋난다). load_dotenv 는 이미 있는
        # 값을 덮지 않으므로, 파이프라인 전용 .env 를 먼저 읽어 COREMATE_* 가
        # 우선하게 하고 infra/.env 의 MYSQL_PASSWORD 는 폴백으로만 쓰인다.
        load_dotenv(AI_PIPELINE_ROOT / ".env")
        load_dotenv(INFRA_ENV_FILE)

        host = _env("COREMATE_MYSQL_HOST", "MYSQL_HOST", default="127.0.0.1")
        port_raw = _env("COREMATE_MYSQL_PORT", "MYSQL_PORT", default="3306")
        database = _env("COREMATE_MYSQL_DATABASE", "MYSQL_DATABASE")
        user = _env("COREMATE_MYSQL_USER", "MYSQL_USER")
        password = _env("COREMATE_MYSQL_PASSWORD", "MYSQL_PASSWORD", default="")
        charset = _env("COREMATE_MYSQL_CHARSET", "MYSQL_CHARSET", default="utf8mb4")

        missing = []
        if not database:
            missing.append("COREMATE_MYSQL_DATABASE")
        if not user:
            missing.append("COREMATE_MYSQL_USER")
        if missing:
            raise ContentImportConfigError(
                "missing MySQL environment variables: " + ", ".join(missing)
            )

        try:
            port = int(port_raw or "3306")
        except ValueError as exc:
            raise ContentImportConfigError(
                f"COREMATE_MYSQL_PORT must be an integer: {port_raw}"
            ) from exc

        return cls(
            host=host or "127.0.0.1",
            port=port,
            database=database,
            user=user,
            password=password or "",
            charset=charset or "utf8mb4",
        )


APPROVED_ARTIFACT_FILES = {
    "taxonomy": "taxonomy.json",
    "concept_cards": "concept_cards.json",
    "concept_relations": "concept_relations.json",
    "common_mistakes": "common_mistakes.json",
    "questions": "questions.json",
}

SUBJECT_SORT_ORDER = {
    "DS": 1,
    "ALGO": 2,
    "OS": 3,
    "DB": 4,
    "CA": 5,
    "NETWORK": 6,
}


def import_approved_content(
    *,
    subject_code: str,
    request: ContentImportRequest,
) -> ContentImportResponse:
    normalized_subject_code = subject_code.upper()
    subject_summary = get_subject_config(normalized_subject_code)
    artifacts = _load_approved_artifacts(normalized_subject_code)
    plan = _build_import_plan(
        subject_code=normalized_subject_code,
        subject_summary=subject_summary,
        artifacts=artifacts,
        activate_on_import=request.activate_on_import,
    )

    if request.dry_run:
        return _build_response(
            plan=plan,
            request=request,
            status=ContentImportStatus.VALIDATED,
            imported_counts={},
        )

    imported_counts = _write_import_plan_to_mysql(plan)

    return _build_response(
        plan=plan,
        request=request,
        status=ContentImportStatus.IMPORTED,
        imported_counts=imported_counts,
    )


def _load_approved_artifacts(subject_code: str) -> ApprovedArtifacts:
    approved_dir = OUTPUTS_DIR / subject_code / "reviews" / "approved"
    loaded: dict[str, Any] = {}
    summaries = []

    for artifact_type, file_name in APPROVED_ARTIFACT_FILES.items():
        path = approved_dir / file_name

        if not path.exists():
            raise ContentImportError(
                "approved artifact not found. Export approved review items first: "
                f"{to_relative_path(path)}"
            )

        try:
            data = json.loads(path.read_text(encoding="utf-8"))
        except json.JSONDecodeError as exc:
            raise ContentImportError(
                f"approved artifact is not valid JSON: {to_relative_path(path)}"
            ) from exc

        loaded[artifact_type] = data
        summaries.append(
            ContentImportArtifactSummary(
                artifact_type=artifact_type,
                path=to_relative_path(path),
                item_count=_artifact_item_count(artifact_type, data),
            )
        )

    taxonomy = _expect_dict(loaded["taxonomy"], "taxonomy")

    return ApprovedArtifacts(
        taxonomy=taxonomy,
        concept_cards=_expect_list_of_dicts(
            loaded["concept_cards"],
            "concept_cards",
        ),
        concept_relations=_expect_list_of_dicts(
            loaded["concept_relations"],
            "concept_relations",
        ),
        common_mistakes=_expect_list_of_dicts(
            loaded["common_mistakes"],
            "common_mistakes",
        ),
        questions=_expect_list_of_dicts(loaded["questions"], "questions"),
        summaries=summaries,
    )


def _build_import_plan(
    *,
    subject_code: str,
    subject_summary: SubjectSummary,
    artifacts: ApprovedArtifacts,
    activate_on_import: bool,
) -> ImportPlan:
    warnings: list[str] = []
    taxonomy_subject = _expect_dict(
        _required(artifacts.taxonomy, "subject", "taxonomy"),
        "taxonomy.subject",
    )
    category_lv1_list = _expect_list_of_dicts(
        _required(artifacts.taxonomy, "category_lv1_list", "taxonomy"),
        "taxonomy.category_lv1_list",
    )
    taxonomy_subject_code = str(
        _required(taxonomy_subject, "subject_code", "taxonomy.subject")
    )

    if taxonomy_subject_code.upper() != subject_code:
        raise ContentImportError(
            "approved taxonomy subject_code does not match request: "
            f"{taxonomy_subject_code} != {subject_code}"
        )

    db_subject_code = (
        subject_summary.db_subject_code
        or _db_subject_code_from_pipeline_code(subject_code)
        or subject_code
    )
    subject = {
        "subject_code": db_subject_code,
        "subject_name": str(
            taxonomy_subject.get("subject_name") or subject_summary.subject_name
        ),
        "subject_name_en": taxonomy_subject.get("subject_name_en")
        or subject_summary.subject_name_en,
        "sort_order": SUBJECT_SORT_ORDER.get(db_subject_code, 0),
        "is_active": True,
    }
    category_lv1, category_lv2, concept_hierarchy = _build_taxonomy_rows(
        category_lv1_list
    )
    concepts = _build_concept_rows(
        concept_cards=artifacts.concept_cards,
        concept_hierarchy=concept_hierarchy,
        activate_on_import=activate_on_import,
    )
    concept_codes = {concept["concept_code"] for concept in concepts}
    concept_relations = _build_concept_relation_rows(
        relations=artifacts.concept_relations,
        concept_codes=concept_codes,
        activate_on_import=activate_on_import,
    )
    common_mistakes, mistake_codes = _build_common_mistake_rows(
        mistake_groups=artifacts.common_mistakes,
        concept_codes=concept_codes,
        activate_on_import=activate_on_import,
    )
    questions = _build_question_rows(
        questions=artifacts.questions,
        concept_codes=concept_codes,
        mistake_codes=mistake_codes,
        activate_on_import=activate_on_import,
        warnings=warnings,
    )
    table_summaries = [
        ContentImportTableSummary(table_name="subject", planned_count=1),
        ContentImportTableSummary(
            table_name="category_lv1",
            planned_count=len(category_lv1),
        ),
        ContentImportTableSummary(
            table_name="category_lv2",
            planned_count=len(category_lv2),
        ),
        ContentImportTableSummary(table_name="concept", planned_count=len(concepts)),
        ContentImportTableSummary(
            table_name="concept_relation",
            planned_count=len(concept_relations),
        ),
        ContentImportTableSummary(
            table_name="common_mistake",
            planned_count=len(common_mistakes),
        ),
        ContentImportTableSummary(table_name="question", planned_count=len(questions)),
    ]

    return ImportPlan(
        subject_code=subject_code,
        db_subject_code=db_subject_code,
        artifacts=artifacts.summaries,
        tables=table_summaries,
        subject=subject,
        category_lv1=category_lv1,
        category_lv2=category_lv2,
        concepts=concepts,
        concept_relations=concept_relations,
        common_mistakes=common_mistakes,
        questions=questions,
        warnings=warnings,
    )


def _build_taxonomy_rows(
    category_lv1_list: list[dict[str, Any]],
) -> tuple[list[dict[str, Any]], list[dict[str, Any]], dict[str, dict[str, Any]]]:
    category_lv1_rows = []
    category_lv2_rows = []
    concept_hierarchy: dict[str, dict[str, Any]] = {}

    for lv1_index, lv1 in enumerate(category_lv1_list, start=1):
        lv1_context = f"taxonomy.category_lv1_list[{lv1_index - 1}]"
        lv1_code = str(_required(lv1, "category_lv1_code", lv1_context))
        category_lv1_rows.append(
            {
                "category_lv1_code": lv1_code,
                "category_lv1_name": str(
                    _required(lv1, "category_lv1_name", lv1_context)
                ),
                "category_lv1_name_en": lv1.get("category_lv1_name_en"),
                "description": lv1.get("description"),
                "sort_order": lv1_index,
                "is_active": True,
            }
        )

        lv2_list = _expect_list_of_dicts(
            _required(lv1, "category_lv2_list", lv1_context),
            f"{lv1_context}.category_lv2_list",
        )

        for lv2_index, lv2 in enumerate(lv2_list, start=1):
            lv2_context = f"{lv1_context}.category_lv2_list[{lv2_index - 1}]"
            lv2_code = str(_required(lv2, "category_lv2_code", lv2_context))
            category_lv2_rows.append(
                {
                    "category_lv1_code": lv1_code,
                    "category_lv2_code": lv2_code,
                    "category_lv2_name": str(
                        _required(lv2, "category_lv2_name", lv2_context)
                    ),
                    "category_lv2_name_en": lv2.get("category_lv2_name_en"),
                    "description": lv2.get("description"),
                    "sort_order": lv2_index,
                    "is_active": True,
                }
            )
            candidates = _expect_list_of_dicts(
                _required(lv2, "concept_candidates", lv2_context),
                f"{lv2_context}.concept_candidates",
            )

            for concept_index, candidate in enumerate(candidates, start=1):
                concept_context = (
                    f"{lv2_context}.concept_candidates[{concept_index - 1}]"
                )
                concept_code = str(
                    _required(candidate, "concept_code", concept_context)
                )

                if concept_code in concept_hierarchy:
                    raise ContentImportError(
                        f"duplicated concept_code in taxonomy: {concept_code}"
                    )

                concept_hierarchy[concept_code] = {
                    "category_lv2_code": lv2_code,
                    "sort_order": concept_index,
                    "aliases": _optional_list(candidate.get("aliases"), []),
                    "concept_name": candidate.get("concept_name"),
                    "concept_name_en": candidate.get("concept_name_en"),
                }

    return category_lv1_rows, category_lv2_rows, concept_hierarchy


def _build_concept_rows(
    *,
    concept_cards: list[dict[str, Any]],
    concept_hierarchy: dict[str, dict[str, Any]],
    activate_on_import: bool,
) -> list[dict[str, Any]]:
    rows = []
    seen_codes: set[str] = set()

    for index, card in enumerate(concept_cards):
        context = f"concept_cards[{index}]"
        concept_code = str(_required(card, "concept_code", context))

        if concept_code in seen_codes:
            raise ContentImportError(
                f"duplicated concept_code in concept_cards: {concept_code}"
            )

        hierarchy = concept_hierarchy.get(concept_code)
        if hierarchy is None:
            raise ContentImportError(
                f"concept_card is not present in approved taxonomy: {concept_code}"
            )

        seen_codes.add(concept_code)
        rows.append(
            {
                "concept_code": concept_code,
                "category_lv2_code": hierarchy["category_lv2_code"],
                "concept_name": str(
                    card.get("concept_name")
                    or hierarchy.get("concept_name")
                    or concept_code
                ),
                "concept_name_en": card.get("concept_name_en")
                or hierarchy.get("concept_name_en"),
                "aliases_json": _optional_list(
                    card.get("aliases"),
                    hierarchy.get("aliases", []),
                ),
                "one_line_definition": str(
                    _required(card, "one_line_definition", context)
                ),
                "core_summary": str(_required(card, "core_summary", context)),
                "core_keywords_json": _required_list(
                    card,
                    "core_keywords",
                    context,
                ),
                "key_properties_json": _required_list(
                    card,
                    "key_properties",
                    context,
                ),
                "source_refs": _optional_list(card.get("source_refs"), []),
                "sort_order": hierarchy["sort_order"],
                "review_status": "approved",
                "review_note": card.get("review_note"),
                "is_active": _active_value(
                    card,
                    activate_on_import=activate_on_import,
                    default=False,
                ),
                "version_no": int(card.get("version_no") or 1),
            }
        )

    return rows


def _build_concept_relation_rows(
    *,
    relations: list[dict[str, Any]],
    concept_codes: set[str],
    activate_on_import: bool,
) -> list[dict[str, Any]]:
    rows = []
    seen_keys: set[tuple[str, str, str]] = set()

    for index, relation in enumerate(relations):
        context = f"concept_relations[{index}]"
        source_code = str(_required(relation, "source_concept_code", context))
        target_code = str(_required(relation, "target_concept_code", context))
        relation_type = str(_required(relation, "relation_type", context))

        if source_code not in concept_codes:
            raise ContentImportError(
                f"relation source_concept_code is not approved: {source_code}"
            )
        if target_code not in concept_codes:
            raise ContentImportError(
                f"relation target_concept_code is not approved: {target_code}"
            )
        if source_code == target_code:
            raise ContentImportError(
                f"relation cannot point to the same concept: {source_code}"
            )

        key = (source_code, target_code, relation_type)
        if key in seen_keys:
            continue
        seen_keys.add(key)

        rows.append(
            {
                "source_concept_code": source_code,
                "target_concept_code": target_code,
                "relation_type": relation_type,
                "relation_note": relation.get("relation_note"),
                "is_active": _active_value(
                    relation,
                    activate_on_import=activate_on_import,
                    default=True,
                ),
            }
        )

    return rows


def _build_common_mistake_rows(
    *,
    mistake_groups: list[dict[str, Any]],
    concept_codes: set[str],
    activate_on_import: bool,
) -> tuple[list[dict[str, Any]], set[str]]:
    rows = []
    mistake_codes = set()

    for group_index, group in enumerate(mistake_groups):
        group_context = f"common_mistakes[{group_index}]"
        concept_code = str(_required(group, "concept_code", group_context))

        if concept_code not in concept_codes:
            raise ContentImportError(
                f"common_mistake concept_code is not approved: {concept_code}"
            )

        mistakes = _expect_list_of_dicts(
            _required(group, "mistakes", group_context),
            f"{group_context}.mistakes",
        )

        for mistake_index, mistake in enumerate(mistakes, start=1):
            context = f"{group_context}.mistakes[{mistake_index - 1}]"
            _require_approved_status(mistake, context)
            mistake_code = str(
                mistake.get("mistake_code")
                or f"{concept_code}_M{mistake_index:02d}"
            )

            if mistake_code in mistake_codes:
                raise ContentImportError(
                    f"duplicated mistake_code in approved common_mistakes: {mistake_code}"
                )

            mistake_codes.add(mistake_code)
            rows.append(
                {
                    "mistake_code": mistake_code,
                    "concept_code": concept_code,
                    "mistake_title": str(
                        _required(mistake, "mistake_title", context)
                    ),
                    "mistake_text": str(_required(mistake, "mistake_text", context)),
                    "correction_text": str(
                        _required(mistake, "correction_text", context)
                    ),
                    "review_status": "approved",
                    "review_note": mistake.get("review_note"),
                    "is_active": _active_value(
                        mistake,
                        activate_on_import=activate_on_import,
                        default=False,
                    ),
                }
            )

    return rows, mistake_codes


def _build_question_rows(
    *,
    questions: list[dict[str, Any]],
    concept_codes: set[str],
    mistake_codes: set[str],
    activate_on_import: bool,
    warnings: list[str],
) -> list[dict[str, Any]]:
    rows = []
    seen_hashes: set[str] = set()

    for index, question in enumerate(questions):
        context = f"questions[{index}]"
        _require_approved_status(question, context)
        concept_code = str(_required(question, "concept_code", context))

        if concept_code not in concept_codes:
            raise ContentImportError(
                f"question concept_code is not approved: {concept_code}"
            )

        hint_mistake_code = question.get("hint_mistake_code")
        if hint_mistake_code is not None:
            hint_mistake_code = str(hint_mistake_code)
            if hint_mistake_code not in mistake_codes:
                raise ContentImportError(
                    "question hint_mistake_code is not approved in common_mistakes: "
                    f"{hint_mistake_code}"
                )

        validation_errors = question.get("validation_errors") or []
        if validation_errors:
            warnings.append(
                f"{context} has validation_errors but is approved: {validation_errors}"
            )

        content_hash = str(question.get("content_hash") or make_question_hash(question))
        if content_hash in seen_hashes:
            raise ContentImportError(
                f"duplicated question content_hash in approved questions: {content_hash}"
            )
        seen_hashes.add(content_hash)

        rows.append(
            {
                "concept_code": concept_code,
                "hint_mistake_code": hint_mistake_code,
                "question_format": str(
                    question.get("question_format") or "MCQ_4"
                ),
                "question_type": str(_required(question, "question_type", context)),
                "question_text": str(_required(question, "question_text", context)),
                "choices_json": _optional_list(question.get("choices"), []),
                "correct_answer": str(
                    _required(question, "correct_answer", context)
                ),
                "explanation_text": str(
                    _required(question, "explanation_text", context)
                ),
                "difficulty_level": int(question.get("difficulty_level") or 3),
                "difficulty_source": str(
                    question.get("difficulty_source") or "ai_initial"
                ),
                "expected_solve_sec": question.get("expected_solve_sec"),
                "content_hash": content_hash,
                "review_status": "approved",
                "review_note": question.get("review_note"),
                "source_type": str(question.get("source_type") or "ai_generated"),
                "source_refs": _optional_list(question.get("source_refs"), []),
                "is_daily_quiz_eligible": bool(
                    question.get("is_daily_quiz_eligible", True)
                ),
                "is_mock_eligible": bool(question.get("is_mock_eligible", True)),
                "is_concept_practice_eligible": bool(
                    question.get("is_concept_practice_eligible", True)
                ),
                "is_active": _active_value(
                    question,
                    activate_on_import=activate_on_import,
                    default=False,
                ),
                "version_no": int(question.get("version_no") or 1),
            }
        )

    return rows


def _write_import_plan_to_mysql(plan: ImportPlan) -> dict[str, int]:
    settings = MySQLConnectionSettings.from_env()
    connection = _connect_mysql(settings)

    try:
        cursor = connection.cursor(dictionary=True)
        try:
            imported_counts = _execute_import(cursor, plan)
            connection.commit()
            return imported_counts
        except Exception:
            connection.rollback()
            raise
        finally:
            cursor.close()
    except ContentImportError:
        raise
    except Exception as exc:
        raise ContentImportDatabaseError(f"MySQL import failed: {exc}") from exc
    finally:
        connection.close()


def _connect_mysql(settings: MySQLConnectionSettings) -> Any:
    try:
        import mysql.connector
    except ModuleNotFoundError as exc:
        raise ContentImportConfigError(
            "mysql-connector-python is not installed. Install tools/ai-pipeline requirements."
        ) from exc

    try:
        return mysql.connector.connect(
            host=settings.host,
            port=settings.port,
            user=settings.user,
            password=settings.password,
            database=settings.database,
            autocommit=False,
            charset=settings.charset,
        )
    except Exception as exc:
        raise ContentImportDatabaseError(
            f"could not connect to MySQL at {settings.host}:{settings.port}/{settings.database}: {exc}"
        ) from exc


def _execute_import(cursor: Any, plan: ImportPlan) -> dict[str, int]:
    approved_at = datetime.now(timezone.utc).replace(tzinfo=None)
    imported_counts: dict[str, int] = {}

    subject_id = _upsert_subject(cursor, plan.subject)
    imported_counts["subject"] = 1

    category_lv1_ids = _upsert_category_lv1(cursor, subject_id, plan.category_lv1)
    imported_counts["category_lv1"] = len(plan.category_lv1)

    category_lv2_ids = _upsert_category_lv2(
        cursor,
        category_lv1_ids,
        plan.category_lv2,
    )
    imported_counts["category_lv2"] = len(plan.category_lv2)

    concept_ids = _upsert_concepts(
        cursor,
        category_lv2_ids,
        plan.concepts,
        approved_at,
    )
    imported_counts["concept"] = len(plan.concepts)

    _upsert_concept_relations(cursor, concept_ids, plan.concept_relations)
    imported_counts["concept_relation"] = len(plan.concept_relations)

    mistake_ids = _upsert_common_mistakes(
        cursor,
        concept_ids,
        plan.common_mistakes,
    )
    imported_counts["common_mistake"] = len(plan.common_mistakes)

    _upsert_questions(
        cursor,
        concept_ids,
        mistake_ids,
        plan.questions,
        approved_at,
    )
    imported_counts["question"] = len(plan.questions)

    return imported_counts


def _upsert_subject(cursor: Any, subject: dict[str, Any]) -> int:
    cursor.execute(
        """
        INSERT INTO subject (
            subject_code,
            subject_name,
            subject_name_en,
            sort_order,
            is_active
        )
        VALUES (%s, %s, %s, %s, %s)
        ON DUPLICATE KEY UPDATE
            subject_name = VALUES(subject_name),
            subject_name_en = VALUES(subject_name_en),
            sort_order = VALUES(sort_order),
            is_active = VALUES(is_active),
            updated_at = CURRENT_TIMESTAMP
        """,
        (
            subject["subject_code"],
            subject["subject_name"],
            subject["subject_name_en"],
            subject["sort_order"],
            subject["is_active"],
        ),
    )
    return _fetch_required_id(
        cursor,
        "SELECT subject_id FROM subject WHERE subject_code = %s",
        (subject["subject_code"],),
        "subject",
    )


def _upsert_category_lv1(
    cursor: Any,
    subject_id: int,
    rows: list[dict[str, Any]],
) -> dict[str, int]:
    ids = {}

    for row in rows:
        cursor.execute(
            """
            INSERT INTO category_lv1 (
                subject_id,
                category_lv1_code,
                category_lv1_name,
                category_lv1_name_en,
                description,
                sort_order,
                is_active
            )
            VALUES (%s, %s, %s, %s, %s, %s, %s)
            ON DUPLICATE KEY UPDATE
                category_lv1_name = VALUES(category_lv1_name),
                category_lv1_name_en = VALUES(category_lv1_name_en),
                description = VALUES(description),
                sort_order = VALUES(sort_order),
                is_active = VALUES(is_active),
                updated_at = CURRENT_TIMESTAMP
            """,
            (
                subject_id,
                row["category_lv1_code"],
                row["category_lv1_name"],
                row["category_lv1_name_en"],
                row["description"],
                row["sort_order"],
                row["is_active"],
            ),
        )
        ids[row["category_lv1_code"]] = _fetch_required_id(
            cursor,
            """
            SELECT category_lv1_id
            FROM category_lv1
            WHERE subject_id = %s AND category_lv1_code = %s
            """,
            (subject_id, row["category_lv1_code"]),
            "category_lv1",
        )

    return ids


def _upsert_category_lv2(
    cursor: Any,
    category_lv1_ids: dict[str, int],
    rows: list[dict[str, Any]],
) -> dict[str, int]:
    ids = {}

    for row in rows:
        category_lv1_id = category_lv1_ids[row["category_lv1_code"]]
        cursor.execute(
            """
            INSERT INTO category_lv2 (
                category_lv1_id,
                category_lv2_code,
                category_lv2_name,
                category_lv2_name_en,
                description,
                sort_order,
                is_active
            )
            VALUES (%s, %s, %s, %s, %s, %s, %s)
            ON DUPLICATE KEY UPDATE
                category_lv2_name = VALUES(category_lv2_name),
                category_lv2_name_en = VALUES(category_lv2_name_en),
                description = VALUES(description),
                sort_order = VALUES(sort_order),
                is_active = VALUES(is_active),
                updated_at = CURRENT_TIMESTAMP
            """,
            (
                category_lv1_id,
                row["category_lv2_code"],
                row["category_lv2_name"],
                row["category_lv2_name_en"],
                row["description"],
                row["sort_order"],
                row["is_active"],
            ),
        )
        ids[row["category_lv2_code"]] = _fetch_required_id(
            cursor,
            """
            SELECT category_lv2_id
            FROM category_lv2
            WHERE category_lv1_id = %s AND category_lv2_code = %s
            """,
            (category_lv1_id, row["category_lv2_code"]),
            "category_lv2",
        )

    return ids


def _upsert_concepts(
    cursor: Any,
    category_lv2_ids: dict[str, int],
    rows: list[dict[str, Any]],
    approved_at: datetime,
) -> dict[str, int]:
    ids = {}

    for row in rows:
        cursor.execute(
            """
            INSERT INTO concept (
                concept_code,
                category_lv2_id,
                concept_name,
                concept_name_en,
                aliases_json,
                one_line_definition,
                core_summary,
                core_keywords_json,
                key_properties_json,
                source_refs,
                sort_order,
                review_status,
                review_note,
                is_active,
                version_no,
                approved_at
            )
            VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
            ON DUPLICATE KEY UPDATE
                category_lv2_id = VALUES(category_lv2_id),
                concept_name = VALUES(concept_name),
                concept_name_en = VALUES(concept_name_en),
                aliases_json = VALUES(aliases_json),
                one_line_definition = VALUES(one_line_definition),
                core_summary = VALUES(core_summary),
                core_keywords_json = VALUES(core_keywords_json),
                key_properties_json = VALUES(key_properties_json),
                source_refs = VALUES(source_refs),
                sort_order = VALUES(sort_order),
                review_status = VALUES(review_status),
                review_note = VALUES(review_note),
                is_active = VALUES(is_active),
                version_no = VALUES(version_no),
                approved_at = VALUES(approved_at),
                updated_at = CURRENT_TIMESTAMP
            """,
            (
                row["concept_code"],
                category_lv2_ids[row["category_lv2_code"]],
                row["concept_name"],
                row["concept_name_en"],
                _json_param(row["aliases_json"]),
                row["one_line_definition"],
                row["core_summary"],
                _json_param(row["core_keywords_json"]),
                _json_param(row["key_properties_json"]),
                _json_param(row["source_refs"]),
                row["sort_order"],
                row["review_status"],
                row["review_note"],
                row["is_active"],
                row["version_no"],
                approved_at,
            ),
        )
        ids[row["concept_code"]] = _fetch_required_id(
            cursor,
            "SELECT concept_id FROM concept WHERE concept_code = %s",
            (row["concept_code"],),
            "concept",
        )

    return ids


def _upsert_concept_relations(
    cursor: Any,
    concept_ids: dict[str, int],
    rows: list[dict[str, Any]],
) -> None:
    for row in rows:
        cursor.execute(
            """
            INSERT INTO concept_relation (
                source_concept_id,
                target_concept_id,
                relation_type,
                relation_note,
                is_active
            )
            VALUES (%s, %s, %s, %s, %s)
            ON DUPLICATE KEY UPDATE
                relation_note = VALUES(relation_note),
                is_active = VALUES(is_active),
                updated_at = CURRENT_TIMESTAMP
            """,
            (
                concept_ids[row["source_concept_code"]],
                concept_ids[row["target_concept_code"]],
                row["relation_type"],
                row["relation_note"],
                row["is_active"],
            ),
        )


def _upsert_common_mistakes(
    cursor: Any,
    concept_ids: dict[str, int],
    rows: list[dict[str, Any]],
) -> dict[str, int]:
    ids = {}

    for row in rows:
        concept_id = concept_ids[row["concept_code"]]
        existing_id = _fetch_optional_id(
            cursor,
            """
            SELECT common_mistake_id
            FROM common_mistake
            WHERE concept_id = %s AND mistake_title = %s
            ORDER BY common_mistake_id
            LIMIT 1
            """,
            (concept_id, row["mistake_title"]),
        )

        if existing_id is None:
            cursor.execute(
                """
                INSERT INTO common_mistake (
                    concept_id,
                    mistake_title,
                    mistake_text,
                    correction_text,
                    review_status,
                    review_note,
                    is_active
                )
                VALUES (%s, %s, %s, %s, %s, %s, %s)
                """,
                (
                    concept_id,
                    row["mistake_title"],
                    row["mistake_text"],
                    row["correction_text"],
                    row["review_status"],
                    row["review_note"],
                    row["is_active"],
                ),
            )
            ids[row["mistake_code"]] = int(cursor.lastrowid)
            continue

        cursor.execute(
            """
            UPDATE common_mistake
            SET
                mistake_text = %s,
                correction_text = %s,
                review_status = %s,
                review_note = %s,
                is_active = %s,
                updated_at = CURRENT_TIMESTAMP
            WHERE common_mistake_id = %s
            """,
            (
                row["mistake_text"],
                row["correction_text"],
                row["review_status"],
                row["review_note"],
                row["is_active"],
                existing_id,
            ),
        )
        ids[row["mistake_code"]] = existing_id

    return ids


def _upsert_questions(
    cursor: Any,
    concept_ids: dict[str, int],
    mistake_ids: dict[str, int],
    rows: list[dict[str, Any]],
    approved_at: datetime,
) -> None:
    for row in rows:
        hint_common_mistake_id = None
        if row["hint_mistake_code"] is not None:
            hint_common_mistake_id = mistake_ids[row["hint_mistake_code"]]

        cursor.execute(
            """
            INSERT INTO question (
                concept_id,
                hint_common_mistake_id,
                question_format,
                question_type,
                question_text,
                choices_json,
                correct_answer,
                explanation_text,
                difficulty_level,
                difficulty_source,
                expected_solve_sec,
                content_hash,
                review_status,
                review_note,
                source_type,
                source_refs,
                is_daily_quiz_eligible,
                is_mock_eligible,
                is_concept_practice_eligible,
                is_active,
                version_no,
                approved_at
            )
            VALUES (
                %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s,
                %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s
            )
            ON DUPLICATE KEY UPDATE
                concept_id = VALUES(concept_id),
                hint_common_mistake_id = VALUES(hint_common_mistake_id),
                question_format = VALUES(question_format),
                question_type = VALUES(question_type),
                question_text = VALUES(question_text),
                choices_json = VALUES(choices_json),
                correct_answer = VALUES(correct_answer),
                explanation_text = VALUES(explanation_text),
                difficulty_level = VALUES(difficulty_level),
                difficulty_source = VALUES(difficulty_source),
                expected_solve_sec = VALUES(expected_solve_sec),
                review_status = VALUES(review_status),
                review_note = VALUES(review_note),
                source_type = VALUES(source_type),
                source_refs = VALUES(source_refs),
                is_daily_quiz_eligible = VALUES(is_daily_quiz_eligible),
                is_mock_eligible = VALUES(is_mock_eligible),
                is_concept_practice_eligible = VALUES(is_concept_practice_eligible),
                is_active = VALUES(is_active),
                version_no = VALUES(version_no),
                approved_at = VALUES(approved_at),
                updated_at = CURRENT_TIMESTAMP
            """,
            (
                concept_ids[row["concept_code"]],
                hint_common_mistake_id,
                row["question_format"],
                row["question_type"],
                row["question_text"],
                _json_param(row["choices_json"]),
                row["correct_answer"],
                row["explanation_text"],
                row["difficulty_level"],
                row["difficulty_source"],
                row["expected_solve_sec"],
                row["content_hash"],
                row["review_status"],
                row["review_note"],
                row["source_type"],
                _json_param(row["source_refs"]),
                row["is_daily_quiz_eligible"],
                row["is_mock_eligible"],
                row["is_concept_practice_eligible"],
                row["is_active"],
                row["version_no"],
                approved_at,
            ),
        )


def _fetch_required_id(
    cursor: Any,
    query: str,
    params: tuple[Any, ...],
    label: str,
) -> int:
    value = _fetch_optional_id(cursor, query, params)
    if value is None:
        raise ContentImportDatabaseError(f"could not resolve imported {label} id")

    return value


def _fetch_optional_id(
    cursor: Any,
    query: str,
    params: tuple[Any, ...],
) -> int | None:
    cursor.execute(query, params)
    row = cursor.fetchone()

    if row is None:
        return None

    first_value = next(iter(row.values()))
    return int(first_value)


def _build_response(
    *,
    plan: ImportPlan,
    request: ContentImportRequest,
    status: ContentImportStatus,
    imported_counts: dict[str, int],
) -> ContentImportResponse:
    return ContentImportResponse(
        subject_code=plan.subject_code,
        db_subject_code=plan.db_subject_code,
        status=status,
        dry_run=request.dry_run,
        activate_on_import=request.activate_on_import,
        artifacts=plan.artifacts,
        tables=[
            ContentImportTableSummary(
                table_name=summary.table_name,
                planned_count=summary.planned_count,
                imported_count=imported_counts.get(summary.table_name, 0),
            )
            for summary in plan.tables
        ],
        warnings=plan.warnings,
    )


def _artifact_item_count(artifact_type: str, data: Any) -> int:
    if artifact_type == "taxonomy":
        return 1 if isinstance(data, dict) else 0

    if artifact_type == "common_mistakes" and isinstance(data, list):
        return sum(
            len(item.get("mistakes", []))
            for item in data
            if isinstance(item, dict)
        )

    if isinstance(data, list):
        return len(data)

    return 0


def _expect_dict(value: Any, context: str) -> dict[str, Any]:
    if not isinstance(value, dict):
        raise ContentImportError(f"{context} must be a JSON object")

    return value


def _expect_list_of_dicts(value: Any, context: str) -> list[dict[str, Any]]:
    if not isinstance(value, list):
        raise ContentImportError(f"{context} must be a JSON array")

    for index, item in enumerate(value):
        if not isinstance(item, dict):
            raise ContentImportError(f"{context}[{index}] must be a JSON object")

    return value


def _required(data: dict[str, Any], key: str, context: str) -> Any:
    value = data.get(key)

    if value is None or (isinstance(value, str) and not value.strip()):
        raise ContentImportError(f"{context}.{key} is required")

    return value


def _required_list(data: dict[str, Any], key: str, context: str) -> list[Any]:
    value = _required(data, key, context)
    if not isinstance(value, list):
        raise ContentImportError(f"{context}.{key} must be a JSON array")

    return value


def _optional_list(value: Any, default: list[Any]) -> list[Any]:
    if value is None:
        return list(default)

    if not isinstance(value, list):
        return list(default)

    return value


def _require_approved_status(data: dict[str, Any], context: str) -> None:
    review_status = data.get("review_status")

    if review_status is not None and review_status != "approved":
        raise ContentImportError(
            f"{context}.review_status must be approved, got {review_status}"
        )


def _active_value(
    data: dict[str, Any],
    *,
    activate_on_import: bool,
    default: bool,
) -> bool:
    if activate_on_import:
        return True

    return bool(data.get("is_active", default))


def _json_param(value: Any) -> str | None:
    if value is None:
        return None

    return json.dumps(value, ensure_ascii=False)


def _env(*names: str, default: str | None = None) -> str | None:
    for name in names:
        value = os.getenv(name)
        if value is not None and value != "":
            return value

    return default


def _db_subject_code_from_pipeline_code(subject_code: str) -> str | None:
    mapping = {
        "DATA_STRUCTURE": "DS",
        "ALGORITHM": "ALGO",
        "OPERATING_SYSTEM": "OS",
        "DATABASE": "DB",
        "COMPUTER_ARCHITECTURE": "CA",
        "NETWORK": "NETWORK",
    }
    return mapping.get(subject_code)
