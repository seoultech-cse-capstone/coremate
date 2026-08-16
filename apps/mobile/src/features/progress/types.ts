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

/** GET /api/v1/progress/concepts 응답. ConceptProgressResponse와 1:1 대응 */
export type ConceptProgress = {
  userConceptProgressId: number;
  userId: number;
  conceptId: number;
  conceptName: string;
  categoryLv2Id: number;
  isCompleted: boolean;
  completedSolveSessionId: number | null;
  completedAt: string | null;
};
