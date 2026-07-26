export type SolveSessionType =
  | "SUBJECT_MOCK"
  | "CHAPTER_MOCK"
  | "CONCEPT_PRACTICE"
  | "DAILY_QUIZ";

export type QuestionChoice = {
  key: string;
  text: string;
};

export type SolveQuestion = {
  questionId: number;
  questionText: string;
  choices: QuestionChoice[];
};

export type StartSolveSessionResponse = {
  sessionId: number | null;
  title: string;
  sessionType: SolveSessionType;
  totalQuestionCount: number;
  status: "IN_PROGRESS" | "COMPLETED" | "ABANDONED" | null;
  startedAt: string | null;
  questions: SolveQuestion[];
};

export type SubmitAnswerResponse = {
  solveLogId: number;
  questionId: number;
  selectedAnswer: string;
  correctAnswer: string;
  isCorrect: boolean;
  explanationText: string;
  nextQuestionOrder: number | null;
  isSessionAnswerCompleted: boolean;
};

export type CompleteSolveSessionResponse = {
  solveSessionId: number;
  title: string;
  sessionType: SolveSessionType;
  totalQuestionCount: number;
  correctCount: number;
  wrongCount: number;
  totalTimeSpentSec: number;
  status: string;
  completedAt: string;
  earnedXp: number;
  earnedCoin: number;
};

export type GuestAnswerResult = {
  questionId: number;
  selectedAnswer: string;
  correctAnswer: string;
  isCorrect: boolean;
  explanationText: string;
};

/** 문제 풀이 완료 후 해설 화면에서 쓸 수 있도록 문제 본문+선택지와 채점 결과를 합친 것. */
export type SolveReviewItem = {
  questionId: number;
  questionText: string;
  choices: QuestionChoice[];
  selectedAnswer: string;
  correctAnswer: string;
  isCorrect: boolean;
  explanationText: string;
};

export type ApiEnvelope<T> = {
  success: boolean;
  message: string;
  data: T;
};
