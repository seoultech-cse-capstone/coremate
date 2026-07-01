from fastapi import APIRouter, HTTPException, status

from server.models.review import (
    ApprovedArtifactExportResponse,
    ReviewArtifactSummary,
    ReviewArtifactType,
    ReviewDecisionRequest,
    ReviewItemListResponse,
    ReviewItemResponse,
)
from server.services.review_service import (
    ReviewDataError,
    ReviewItemNotFoundError,
    ReviewSourceNotFoundError,
    export_approved_artifact,
    get_review_item,
    list_review_artifacts,
    list_review_items,
    update_review_item,
)
from server.services.subject_service import (
    SubjectNotFoundError,
    get_subject_config,
)

router = APIRouter()


@router.get(
    "/subjects/{subject_code}/artifacts",
    response_model=list[ReviewArtifactSummary],
)
def list_subject_review_artifacts(subject_code: str) -> list[ReviewArtifactSummary]:
    _ensure_subject_exists(subject_code)
    return list_review_artifacts(subject_code)


@router.get(
    "/subjects/{subject_code}/artifacts/{artifact_type}/items",
    response_model=ReviewItemListResponse,
)
def list_subject_review_items(
    subject_code: str,
    artifact_type: ReviewArtifactType,
) -> ReviewItemListResponse:
    _ensure_subject_exists(subject_code)

    try:
        return list_review_items(
            subject_code=subject_code,
            artifact_type=artifact_type,
        )
    except ReviewSourceNotFoundError as exc:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=str(exc),
        ) from exc
    except ReviewDataError as exc:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=str(exc),
        ) from exc


@router.get(
    "/subjects/{subject_code}/artifacts/{artifact_type}/items/{item_key:path}",
    response_model=ReviewItemResponse,
)
def get_subject_review_item(
    subject_code: str,
    artifact_type: ReviewArtifactType,
    item_key: str,
) -> ReviewItemResponse:
    _ensure_subject_exists(subject_code)

    try:
        return get_review_item(
            subject_code=subject_code,
            artifact_type=artifact_type,
            item_key=item_key,
        )
    except ReviewSourceNotFoundError as exc:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=str(exc),
        ) from exc
    except ReviewItemNotFoundError as exc:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=str(exc),
        ) from exc
    except ReviewDataError as exc:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=str(exc),
        ) from exc


@router.put(
    "/subjects/{subject_code}/artifacts/{artifact_type}/items/{item_key:path}",
    response_model=ReviewItemResponse,
)
def update_subject_review_item(
    subject_code: str,
    artifact_type: ReviewArtifactType,
    item_key: str,
    request: ReviewDecisionRequest,
) -> ReviewItemResponse:
    _ensure_subject_exists(subject_code)

    try:
        return update_review_item(
            subject_code=subject_code,
            artifact_type=artifact_type,
            item_key=item_key,
            request=request,
        )
    except ReviewSourceNotFoundError as exc:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=str(exc),
        ) from exc
    except ReviewItemNotFoundError as exc:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=str(exc),
        ) from exc
    except ReviewDataError as exc:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=str(exc),
        ) from exc


@router.post(
    "/subjects/{subject_code}/artifacts/{artifact_type}/export-approved",
    response_model=ApprovedArtifactExportResponse,
)
def export_subject_approved_artifact(
    subject_code: str,
    artifact_type: ReviewArtifactType,
) -> ApprovedArtifactExportResponse:
    _ensure_subject_exists(subject_code)

    try:
        return export_approved_artifact(
            subject_code=subject_code,
            artifact_type=artifact_type,
        )
    except ReviewSourceNotFoundError as exc:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=str(exc),
        ) from exc
    except ReviewDataError as exc:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=str(exc),
        ) from exc


def _ensure_subject_exists(subject_code: str) -> None:
    try:
        get_subject_config(subject_code)
    except SubjectNotFoundError as exc:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=str(exc),
        ) from exc
