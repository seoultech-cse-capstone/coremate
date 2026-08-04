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
  completedAt: string;
  eligible: boolean;
  warningMessage: string | null;
};

export type WeaknessEvidenceLog = {
  evidenceType: string;
  [key: string]: unknown;
};

export type ConceptLearning = Record<string, unknown> | null;

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
