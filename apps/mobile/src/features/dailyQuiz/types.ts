export type DailyQuizChoice = {
  key: string;
  text: string;
};

export type DailyQuizQuestion = {
  questionId: number;
  subjectId: number;
  subjectName: string;
  conceptId: number;
  conceptName: string;
  sortOrder: number;
  questionFormat: string;
  questionType: string;
  questionText: string;
  choices: DailyQuizChoice[];
  difficultyLevel: number;
  expectedSolveSec: number;
  hasHint: boolean;
};

export type DailyQuizSet = {
  dailyQuizSetId: number;
  quizDate: string;
  totalQuestionCount: number;
  solvedToday: boolean;
  questions: DailyQuizQuestion[];
};

export type DailyQuizAnswerInput = {
  questionId: number;
  selectedAnswer: string | null;
  timeSpentSec: number;
  hintUsed: boolean;
  hintCount: number;
  optionChangeCount: number;
  isAnswerChanged: boolean;
};

export type DailyQuizReward = {
  xpAmount: number;
  coinAmount: number;
  randomBoxId: number | null;
  randomBoxName: string | null;
  attendanceProcessed: boolean;
  rewardClaimed: boolean;
};

export type DailyQuizSubmitResult = {
  solveSessionId: number | null;
  dailyQuizSetId: number;
  quizDate: string;
  totalQuestionCount: number;
  correctCount: number;
  wrongCount: number;
  totalTimeSpentSec: number;
  attendanceProcessed: boolean;
  reward: DailyQuizReward;
};

/**
 * 오늘의 퀴즈 문항별 정답·해설 조회 응답. 백엔드 PR #86(미머지)의
 * GET /api/v1/daily-quizzes/result 계약을 기준으로 미리 맞춰둔 타입이다.
 * 머지 후 실제 응답과 다르면 이 타입과 dailyQuizApi.getResult만 손보면 된다.
 */
export type DailyQuizResultItem = {
  questionId: number;
  questionText: string;
  choices: DailyQuizChoice[];
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
