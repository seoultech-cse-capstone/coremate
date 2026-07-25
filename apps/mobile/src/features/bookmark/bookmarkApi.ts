import { httpClient } from "../../shared/api/httpClient";
import type { Bookmark, BookmarkedQuestion } from "./types";

/**
 * 북마크(즐겨찾기) 도메인 REST 호출. 회원 전용(인증 필수), ApiResponse 래핑 없이
 * DTO를 그대로 응답한다.
 */
export const bookmarkApi = {
  getBookmarkedQuestions(accessToken: string): Promise<BookmarkedQuestion[]> {
    return httpClient.get<BookmarkedQuestion[]>("/api/bookmarks/questions", {
      accessToken,
    });
  },

  create(questionId: number, accessToken: string): Promise<Bookmark> {
    return httpClient.post<Bookmark>(
      "/api/bookmarks",
      { questionId },
      { accessToken },
    );
  },

  remove(bookmarkId: number, accessToken: string): Promise<void> {
    return httpClient.delete<void>(`/api/bookmarks/${bookmarkId}`, {
      accessToken,
    });
  },

  updateMemo(bookmarkId: number, memo: string, accessToken: string): Promise<Bookmark> {
    return httpClient.put<Bookmark>(
      `/api/bookmarks/${bookmarkId}/memo`,
      { memo },
      { accessToken },
    );
  },
};
