export type ApiEnvelope<T> = {
  success: boolean;
  message: string;
  data: T;
};

/** GET /api/v1/progress/concepts/summary 응답. ConceptCountSummaryResponse와 1:1 대응 */
export type ConceptCountSummary = {
  totalConceptCount: number;
  solvedConceptCount: number;
  unsolvedConceptCount: number;
};

/** GET /api/v1/progress/subjects 응답. SubjectProgressResponse와 1:1 대응 */
export type SubjectProgress = {
  userId: number;
  subjectId: number;
  subjectName: string;
  solvedQuestionCount: number;
  correctQuestionCount: number;
  wrongQuestionCount: number;
  completedConceptCount: number;
  totalConceptCount: number;
  completedCategoryMockCount: number;
  completedSubjectMockCount: number;
  statScore: number;
  achievementRate: number;
};

export type ProblemSetType =
  | "CONCEPT_PRACTICE"
  | "CATEGORY_LV1_MOCK"
  | "CATEGORY_LV2_MOCK"
  | "SUBJECT_MOCK";

export type ProblemSetProgressFilters = {
  problemSetType?: ProblemSetType;
  subjectId?: number;
  categoryLv1Id?: number;
  categoryLv2Id?: number;
};

/** GET /api/v1/progress/problem-sets 응답. ProblemSetProgressResponse와 1:1 대응 */
export type ProblemSetProgress = {
  userProblemSetProgressId: number;
  userId: number;
  problemSetType: ProblemSetType;
  targetType: string;
  targetId: number;
  subjectId: number;
  categoryLv1Id: number | null;
  categoryLv2Id: number | null;
  conceptId: number | null;
  lastSolveSessionId: number | null;
  isCompleted: boolean;
  completedCount: number;
  lastCompletedAt: string | null;
};

export type ProgressScopeType = "SUBJECT" | "CATEGORY_LV1" | "CATEGORY_LV2";

/**
 * GET /api/v1/progress/pass-rates 응답. ProblemSetPassRateResponse와 1:1 대응.
 *
 * 진행률은 그 범위의 활성 하위 개념 중 합격(PASS, 전 문제 정답)한 개념 비율이다.
 * 모의고사를 몇 번 봤는지는 들어가지 않는다 — 그건 ProblemSetProgress 쪽이다.
 */
export type ProblemSetPassRate = {
  scopeType: ProgressScopeType;
  targetId: number;
  totalConceptCount: number;
  passedConceptCount: number;
  progressRate: number;
  completed: boolean;
};
