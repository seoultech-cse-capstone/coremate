export type AnalysisScopeType = "SUBJECT" | "CATEGORY_LV1" | "CATEGORY_LV2" | "CONCEPT";

export type AnalysisReportStatus =
  | "REQUESTED"
  | "GENERATING"
  | "COMPLETED"
  | "FAILED"
  | "DELETED";

export type EligibleSolveSession = {
  solveSessionId: number;
  title: string;
  sessionType: "SUBJECT_MOCK" | "CATEGORY_LV1_MOCK" | "CATEGORY_LV2_MOCK";
  subjectId: number | null;
  subjectName: string | null;
  categoryLv1Id: number | null;
  categoryLv1Name: string | null;
  categoryLv2Id: number | null;
  categoryLv2Name: string | null;
  conceptId: number | null;
  conceptName: string | null;
  totalQuestionCount: number;
  wrongCount: number;
  longTimeCount: number;
  hintUsedCount: number;
  /** 답 변경 문제 수. 분석 핵심 기준이 아니라 화면 표시용 보조 참고값이다(FR-WEAK-03). */
  answerChangedCount: number;
  completedAt: string;
  eligible: boolean;
  warningMessage: string | null;
};

export type WeaknessAnalysisCost = {
  /** 분석표 1건 생성에 필요한 코인 */
  coinCost: number;
};

export type WeaknessEvidenceLog = {
  evidenceId: number;
  solveLogId: number;
  questionId: number;
  questionText: string;
  /** WRONG, LONG_TIME, HINT_USED, ANSWER_CHANGED */
  evidenceType: string;
  timeSpentSec: number | null;
  expectedSolveSec: number | null;
  hintCount: number | null;
  selectedAnswer: string | null;
  correctAnswer: string | null;
  isCorrect: boolean | null;
  /** AI가 진단의 직접 근거로 지목한 문항인지. 서버 evidence_refs 에서 온다. */
  citedByAi: boolean | null;
};

export type ConceptLearningConfusionComparison = {
  comparedWith: string;
  difference: string;
};

export type ConceptLearning = {
  conceptCore: string | null;
  conceptExplanation: string | null;
  keyPoints: string[];
  confusionComparisons: ConceptLearningConfusionComparison[];
  prerequisiteConcepts: string[];
  learningExample: string | null;
  misconceptionNotes: string | null;
  basicReviewPoints: string[];
} | null;

export type WeaknessConcept = {
  analysisConceptId: number;
  conceptId: number;
  conceptName: string;
  weaknessScore: number;
  evidenceStrength: number;
  finalType: string;
  finalTypeSource: string;
  secondarySignals: string[];
  causeCode: string;
  analysisStatus: string;
  confusedRelation: string | null;
  diagnosis: string;
  evidenceSummary: string;
  detailedExplanationText: string | null;
  conceptTip: string | null;
  conceptLearning: ConceptLearning;
  evidenceLogs: WeaknessEvidenceLog[];
};

export type WeaknessAnalysisReportSummary = {
  analysisReportId: number;
  title: string;
  analysisScopeType: AnalysisScopeType;
  analysisScopeNameSnapshot: string;
  status: AnalysisReportStatus;
  selectedSessionCount: number;
  totalQuestionCount: number;
  totalWrongCount: number;
  coinCost: number;
  generatedAt: string | null;
  pdfAvailable: boolean;
};

export type WeaknessAnalysisReportDetail = {
  analysisReportId: number;
  userId: number;
  title: string;
  analysisScopeType: AnalysisScopeType;
  analysisScopeNameSnapshot: string;
  summaryText: string;
  analysisText: string;
  reportFormat: string;
  selectedSessionCount: number;
  totalQuestionCount: number;
  totalWrongCount: number;
  totalLongTimeCount: number;
  totalHintUsedCount: number;
  coinCost: number;
  status: AnalysisReportStatus;
  requestedAt: string;
  generatedAt: string | null;
  selectedSessions: EligibleSolveSession[];
  weaknessConcepts: WeaknessConcept[];
};

export type CreateWeaknessAnalysisReportRequest = {
  solveSessionIds: number[];
  analysisScopeType: AnalysisScopeType;
  subjectId?: number;
  categoryLv1Id?: number;
  categoryLv2Id?: number;
  conceptId?: number;
  title?: string;
  useCoin: boolean;
};

export type ApiEnvelope<T> = {
  success: boolean;
  message: string;
  data: T;
};
