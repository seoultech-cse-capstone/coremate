from fastapi import APIRouter, HTTPException, status

from server.models.weakness import WeaknessAnalysisRequest, WeaknessAnalysisResponse
from server.services.weakness_service import WeaknessAnalysisError, analyze

router = APIRouter()


@router.post("/analyze", response_model=WeaknessAnalysisResponse)
def analyze_weakness(request: WeaknessAnalysisRequest) -> WeaknessAnalysisResponse:
    """백엔드가 보낸 압축 근거를 OpenAI 로 해석해 약점 유형·원인·복습 방향을 반환한다."""
    try:
        return analyze(request)
    except WeaknessAnalysisError as exc:
        raise HTTPException(
            status_code=status.HTTP_502_BAD_GATEWAY,
            detail=str(exc),
        ) from exc
