"""약점분석 AI 엔드포인트의 HTTP 요청/응답 Pydantic 모델.

백엔드 계약과 1:1 대응(snake_case 와이어 포맷):
- 요청: apps/backend/.../weakness/dto/ai/AiWeaknessAnalysisRequest.java
- 응답: apps/backend/.../weakness/dto/ai/AiWeaknessAnalysisResponse.java
        + resources/ai/weakness-analysis-output.schema.json
"""

from enum import Enum

from pydantic import BaseModel, Field


# --------------------------------------------------------------------------- #
# 요청 (백엔드 → AI 서버): 백엔드가 선정한 약점 후보 + 압축 근거만 담는다.
# --------------------------------------------------------------------------- #
class RequestScope(BaseModel):
    subject: str


class RequestTotals(BaseModel):
    question_count: int
    wrong_count: int
    slow_count: int
    hint_used_count: int


class CandidateStats(BaseModel):
    wrong_rate: float
    slow_rate: float
    hint_rate: float


class RepeatedMisconception(BaseModel):
    code: str
    count: int


class CandidateEvidence(BaseModel):
    ref: str
    type: str
    skill_type: str | None = None
    misconception_code: str | None = None
    time_ratio: float | None = None


class WeaknessCandidate(BaseModel):
    concept_id: int
    concept_name: str
    concept_core: str | None = None
    weakness_score: int
    evidence_strength: float
    type_separation: float
    stats: CandidateStats
    repeated_misconceptions: list[RepeatedMisconception] = Field(default_factory=list)
    evidence: list[CandidateEvidence] = Field(default_factory=list)


class WeaknessAnalysisRequest(BaseModel):
    scope: RequestScope
    totals: RequestTotals
    weakness_candidates: list[WeaknessCandidate] = Field(default_factory=list)


# --------------------------------------------------------------------------- #
# 응답 (AI 서버 → 백엔드): 유형·원인·복습 방향 해석만. 수치/근거는 백엔드가 이미 확정.
# --------------------------------------------------------------------------- #
class AnalysisStatus(str, Enum):
    SUPPORTED = "SUPPORTED"
    AMBIGUOUS = "AMBIGUOUS"
    INSUFFICIENT = "INSUFFICIENT"


class PrimaryType(str, Enum):
    CONCEPT_CONFUSION = "CONCEPT_CONFUSION"
    APPLICATION_GAP = "APPLICATION_GAP"
    SPEED_FLUENCY = "SPEED_FLUENCY"
    EXECUTION_ERROR = "EXECUTION_ERROR"


class SecondarySignal(str, Enum):
    REPEATED_MISCONCEPTION = "REPEATED_MISCONCEPTION"
    CONCEPT_RELATION_CONFUSION = "CONCEPT_RELATION_CONFUSION"
    HINT_DEPENDENCY = "HINT_DEPENDENCY"
    SLOW_FLUENCY = "SLOW_FLUENCY"
    INCONSISTENT_EXECUTION = "INCONSISTENT_EXECUTION"


class CauseCode(str, Enum):
    TERM_CONFUSION = "TERM_CONFUSION"
    RELATIONSHIP_MAPPING_GAP = "RELATIONSHIP_MAPPING_GAP"
    APPLICATION_GAP = "APPLICATION_GAP"
    PROCEDURE_FLUENCY_GAP = "PROCEDURE_FLUENCY_GAP"
    REPEATED_MISCONCEPTION = "REPEATED_MISCONCEPTION"
    CARELESS_EXECUTION = "CARELESS_EXECUTION"


class ConceptAnalysis(BaseModel):
    concept_id: int
    analysis_status: AnalysisStatus
    primary_type: PrimaryType
    secondary_signals: list[SecondarySignal] = Field(default_factory=list)
    cause_code: CauseCode
    confused_relation: str
    diagnosis: str
    likely_cause: str
    concept_tip: str
    evidence_refs: list[str] = Field(default_factory=list)


class WeaknessAnalysisResponse(BaseModel):
    overview: str
    concept_analyses: list[ConceptAnalysis] = Field(default_factory=list)
