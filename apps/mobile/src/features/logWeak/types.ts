export type CompletedSessionType =
  | "CONCEPT_PRACTICE"
  | "CATEGORY_LV1_MOCK"
  | "CATEGORY_LV2_MOCK"
  | "SUBJECT_MOCK"
  | "DAILY_QUIZ";

export const sessionTypeLabel: Record<CompletedSessionType, string> = {
  CONCEPT_PRACTICE: "개념 문제집",
  CATEGORY_LV1_MOCK: "대분류 모의고사",
  CATEGORY_LV2_MOCK: "중분류 모의고사",
  SUBJECT_MOCK: "과목 모의고사",
  DAILY_QUIZ: "오늘의 CS 퀴즈",
};

export type CompletedProblemSetSummary = {
  solveSessionId: number;
  sessionType: CompletedSessionType;
  title: string;
  subjectId: number | null;
  subjectName: string | null;
  categoryLv1Id: number | null;
  categoryLv1Name: string | null;
  categoryLv2Id: number | null;
  categoryLv2Name: string | null;
  conceptId: number | null;
  conceptName: string | null;
  totalQuestionCount: number;
  correctCount: number;
  wrongCount: number;
  correctRate: number;
  totalTimeSpentSec: number;
  completedAt: string;
};

export type CompletedProblemSetLog = {
  solveLogId: number;
  questionId: number;
  questionOrder: number;
  questionText: string;
  choices: { key: string; text: string }[];
  selectedAnswer: string;
  correctAnswer: string;
  isCorrect: boolean;
  explanationText: string;
  timeSpentSec: number;
  expectedSolveSecSnapshot: number | null;
  hintUsed: boolean;
  hintCount: number;
  optionChangeCount: number;
  isAnswerChanged: boolean;
  isBookmarked: boolean;
};

export type CompletedProblemSetDetail = CompletedProblemSetSummary & {
  logs: CompletedProblemSetLog[];
};

export type ApiEnvelope<T> = {
  success: boolean;
  message: string;
  data: T;
};
