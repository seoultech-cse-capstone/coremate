from datetime import datetime, timezone

from fastapi import APIRouter

from server.models.health import HealthResponse

router = APIRouter()


@router.get("/health", response_model=HealthResponse)
def health_check() -> HealthResponse:
    return HealthResponse(
        status="ok",
        service="coremate-ai-pipeline-server",
        checked_at=datetime.now(timezone.utc),
    )
