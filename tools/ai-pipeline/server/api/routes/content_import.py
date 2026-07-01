from fastapi import APIRouter, HTTPException, status

from server.models.content_import import (
    ContentImportRequest,
    ContentImportResponse,
)
from server.services.content_import_service import (
    ContentImportConfigError,
    ContentImportDatabaseError,
    ContentImportError,
    import_approved_content,
)
from server.services.subject_service import SubjectNotFoundError

router = APIRouter()


@router.post(
    "/subjects/{subject_code}",
    response_model=ContentImportResponse,
)
def import_subject_approved_content(
    subject_code: str,
    request: ContentImportRequest = ContentImportRequest(),
) -> ContentImportResponse:
    try:
        return import_approved_content(
            subject_code=subject_code,
            request=request,
        )
    except SubjectNotFoundError as exc:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=str(exc),
        ) from exc
    except ContentImportConfigError as exc:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=str(exc),
        ) from exc
    except ContentImportDatabaseError as exc:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=str(exc),
        ) from exc
    except ContentImportError as exc:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=str(exc),
        ) from exc
