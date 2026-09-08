"""런타임 약점분석 서비스.

백엔드가 보낸 압축 근거를 OpenAI(Structured Outputs)로 해석해 유형·원인·복습 방향을
반환한다. AI 는 DB 를 보지 않으며, 백엔드가 확정한 약점 후보/점수/근거를 바꾸지 않는다.
(사용자 약점 분석표 수정안 §1.4, §2.2, §2.4)
"""

import json
import sys

from pydantic import ValidationError

from schemas.weakness_analysis_schema import WEAKNESS_ANALYSIS_SCHEMA
from server.core.paths import AI_PIPELINE_ROOT
from server.models.weakness import (
    CallMeta,
    WeaknessAnalysisRequest,
    WeaknessAnalysisResponse,
)

# 서버 프로세스에서 src.ai_client 를 import 할 수 있도록 ai-pipeline 루트를 경로에 추가.
if str(AI_PIPELINE_ROOT) not in sys.path:
    sys.path.insert(0, str(AI_PIPELINE_ROOT))

from src.ai_client import call_ai_json_with_meta  # noqa: E402

SCHEMA_NAME = "weakness_analysis"
REASONING_EFFORT = "medium"
MAX_EVIDENCE_REFS = 2
MAX_ATTEMPTS = 2

SYSTEM_PROMPT = (
    "당신은 CoreMate의 사용자 약점 분석 해석기다. 백엔드가 이미 약점 개념 후보와 약점 점수,"
    " 근거 충분도, 대표 근거를 확정했다. 당신의 역할은 각 후보의 압축 근거를 읽고 약점 유형과"
    " 예상 원인, 복습 방향을 독립적으로 해석하는 것뿐이다.\n\n"
    "지켜야 할 원칙:\n"
    "- 입력에 포함된 약점 후보와 대표 근거만 사용한다. 입력에 없는 문제·개념·오개념·풀이 기록을"
    " 만들지 않는다.\n"
    "- 약점 개념 후보, 약점 점수, 우선순위, 대표 근거를 변경하지 않는다.\n"
    "- primary_type 은 허용된 값 중 가장 설명력이 높은 하나만 고른다: CONCEPT_CONFUSION(개념 혼동),"
    " APPLICATION_GAP(적용 부족), SPEED_FLUENCY(속도·숙련 부족), EXECUTION_ERROR(풀이 실행 오류).\n"
    "- 주 유형만으로 설명되지 않는 패턴은 secondary_signals(최대 2개)로 분리한다:"
    " REPEATED_MISCONCEPTION, CONCEPT_RELATION_CONFUSION, HINT_DEPENDENCY, SLOW_FLUENCY,"
    " INCONSISTENT_EXECUTION.\n"
    "- cause_code 는 다음 중 하나: TERM_CONFUSION, RELATIONSHIP_MAPPING_GAP, APPLICATION_GAP,"
    " PROCEDURE_FLUENCY_GAP, REPEATED_MISCONCEPTION, CARELESS_EXECUTION.\n"
    "- 근거가 서로 충돌하거나 부족하면 SUPPORTED 를 억지로 쓰지 말고 analysis_status 를"
    " AMBIGUOUS 또는 INSUFFICIENT 로 둔다.\n"
    "\n입력 신호 읽는 법(콘텐츠 등록 단계에서 만든 진단 프로필에서 온다):\n"
    "- evidence.skill_type 은 그 문항이 요구한 사고 수준이다. RECALL(정의 회상),"
    " COMPREHENSION(개념 이해), APPLICATION(적용·계산), ANALYSIS(비교·추론).\n"
    "  회상·이해 단계에서 틀렸다면 개념 자체를 잘못 알고 있을 가능성(CONCEPT_CONFUSION),"
    " 적용·분석 단계에서 틀렸다면 알긴 아는데 쓰지 못할 가능성(APPLICATION_GAP)이 크다.\n"
    "- evidence.misconception_code 는 학습자가 고른 그 오답이 드러낸 오개념의 식별자다."
    " 같은 코드가 여러 근거에 나오면 같은 오개념을 반복한 것이다.\n"
    "- repeated_misconceptions 는 그렇게 2회 이상 반복된 오개념 코드와 횟수다."
    " 비어 있지 않으면 secondary_signals 에 REPEATED_MISCONCEPTION 을 우선 검토하고,"
    " cause_code 로 REPEATED_MISCONCEPTION 이 더 적절하지 않은지 따져 본다.\n"
    "  반복이 확인되면 confused_relation·concept_tip 을 그 오개념을 교정하는 방향으로 쓴다.\n"
    "- concept_core 는 그 개념의 핵심 요약이다. 진단과 복습 팁은 이 내용과 어긋나지 않아야 한다.\n"
    "- 값이 null 이거나 비어 있으면 그 정보가 없다는 뜻이다. 없는 정보를 추측해 채우지 않는다.\n"
    "- diagnosis 와 likely_cause 는 반드시 입력의 evidence 를 근거로 삼고, 그 중 직접 근거가 된 것을"
    " evidence_refs 에 담는다. 입력에 존재하는 ref 만, 최대 2개.\n"
    "  ⚠️ ref 문자열(e1, e2 …)은 내부 식별자다. diagnosis·likely_cause·confused_relation·"
    "concept_tip 본문에는 절대 쓰지 않는다. 사용자 화면에는 ref 가 표시되지 않아 'e1에서'처럼"
    " 쓰면 무엇을 가리키는지 알 수 없다. 연결은 evidence_refs 필드로만 표현한다.\n"
    "- 예상 원인은 단정하지 말고 '가능성이 있습니다', '것으로 보입니다'처럼 추정 표현으로 쓴다."
    " 사용자를 평가하거나 비난하지 않는다.\n"
    "- concept_analyses 의 concept_id 는 입력 weakness_candidates 의 concept_id 만 사용한다.\n"
    "- 길이 제한(반드시 준수): overview 120자 이하, diagnosis 100자 이하, likely_cause 120자 이하,"
    " confused_relation 100자 이하, concept_tip 180자 이하. 문장은 짧고 구체적으로.\n"
    "- 반드시 지정된 JSON Schema 형식으로만 응답한다."
)


class WeaknessAnalysisError(RuntimeError):
    """AI 약점 분석 해석 실패(형식/스키마 검증 실패 등)."""


def analyze(request: WeaknessAnalysisRequest) -> WeaknessAnalysisResponse:
    user_prompt = _build_user_prompt(request)
    allowed_concept_ids = {c.concept_id for c in request.weakness_candidates}
    allowed_refs = {e.ref for c in request.weakness_candidates for e in c.evidence}

    last_error: Exception | None = None
    for attempt in range(MAX_ATTEMPTS):
        try:
            raw, call_meta = call_ai_json_with_meta(
                system_prompt=SYSTEM_PROMPT,
                user_prompt=user_prompt,
                schema_name=SCHEMA_NAME,
                schema=WEAKNESS_ANALYSIS_SCHEMA,
                reasoning_effort=REASONING_EFFORT,
            )
            response = WeaknessAnalysisResponse.model_validate(raw)
            # 모델이 스키마를 어겨 재시도한 횟수까지 담아야 로그로 품질을 따질 수 있다.
            response.meta = CallMeta(**call_meta, retry_count=attempt)
            return _sanitize(response, allowed_concept_ids, allowed_refs)
        except (ValidationError, json.JSONDecodeError, ValueError) as exc:
            last_error = exc  # 형식/스키마 오류만 제한적으로 재시도

    raise WeaknessAnalysisError(f"AI 약점 분석 응답 검증에 실패했습니다: {last_error}")


def _build_user_prompt(request: WeaknessAnalysisRequest) -> str:
    payload = request.model_dump()
    return (
        "다음은 백엔드가 선정한 약점 후보와 압축 근거다. 이 데이터만 사용해 해석하라.\n"
        + json.dumps(payload, ensure_ascii=False)
    )


def _sanitize(
    response: WeaknessAnalysisResponse,
    allowed_concept_ids: set[int],
    allowed_refs: set[str],
) -> WeaknessAnalysisResponse:
    """AI 가 입력에 없는 concept_id/근거 ref 를 만들어냈으면 걸러낸다(§2.4 방어)."""
    cleaned = []
    for analysis in response.concept_analyses:
        if analysis.concept_id not in allowed_concept_ids:
            continue
        refs = [ref for ref in analysis.evidence_refs if ref in allowed_refs][:MAX_EVIDENCE_REFS]
        cleaned.append(analysis.model_copy(update={"evidence_refs": refs}))
    return WeaknessAnalysisResponse(
        overview=response.overview, concept_analyses=cleaned, meta=response.meta
    )
