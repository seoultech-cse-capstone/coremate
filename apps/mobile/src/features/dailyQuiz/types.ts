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

export type DailyQuizResultItem = {
  questionId: number;
  sortOrder: number;
  subjectName: string;
  conceptName: string;
  questionText: string;
  choices: DailyQuizChoice[];
  selectedAnswer: string;
  correctAnswer: string;
  isCorrect: boolean;
  explanationText: string;
};

/** GET /api/v1/daily-quizzes/result 응답. DailyQuizResultResponse와 1:1 대응 */
export type DailyQuizResult = {
  quizDate: string;
  questions: DailyQuizResultItem[];
};

export type ApiEnvelope<T> = {
  success: boolean;
  message: string;
  data: T;
};
