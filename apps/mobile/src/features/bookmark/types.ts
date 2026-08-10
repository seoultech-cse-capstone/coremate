export type Bookmark = {
  bookmarkId: number;
  userId: number;
  questionId: number;
  sourceSolveSessionId: number | null;
  memo: string | null;
  createdAt: string;
  updatedAt: string;
};

export type BookmarkedQuestion = {
  bookmarkId: number;
  questionId: number;
  conceptId: number;
  questionFormat: string;
  questionType: string;
  difficultyLevel: number;
  memo: string | null;
  bookmarkedAt: string;
};

export type BookmarkedQuestionDetail = {
  bookmarkId: number;
  questionId: number;
  conceptId: number;
  conceptName: string;
  questionFormat: string;
  questionType: string;
  difficultyLevel: number;
  questionText: string;
  choices: { key: string; text: string }[];
  correctAnswer: string;
  explanationText: string;
  expectedSolveSec: number;
  memo: string | null;
};
