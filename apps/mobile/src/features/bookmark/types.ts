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
