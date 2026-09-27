import { httpClient } from "../../shared/api/httpClient";
import type {
  ApiEnvelope,
  CategoryLv1,
  CategoryLv2,
  Concept,
  ConceptDetail,
  Subject,
} from "./types";

/**
 * content 도메인(과목/대분류/중분류/개념 탐색 트리) REST 호출.
 * 전부 permitAll이라 게스트도 그대로 호출할 수 있다.
 */
export const contentApi = {
  getSubjects(): Promise<Subject[]> {
    return httpClient
      .get<ApiEnvelope<Subject[]>>("/api/v1/content/subjects")
      .then((res) => res.data);
  },

  getCategoryLv1List(subjectId: number): Promise<CategoryLv1[]> {
    return httpClient
      .get<ApiEnvelope<CategoryLv1[]>>(`/api/v1/content/subjects/${subjectId}/category-lv1`)
      .then((res) => res.data);
  },

  getCategoryLv2List(categoryLv1Id: number): Promise<CategoryLv2[]> {
    return httpClient
      .get<ApiEnvelope<CategoryLv2[]>>(`/api/v1/content/category-lv1/${categoryLv1Id}/category-lv2`)
      .then((res) => res.data);
  },

  // 회원이면 토큰을 실어서 본인 풀이 상태(완료 여부/문제 수/정답 수)가 반영된 응답을
  // 받는다. 이 엔드포인트 자체는 permitAll이라 토큰 없이도 200은 오지만, 그 경우
  // 서버가 전부 미풀이로 반환한다(로그인 사용자에게도 잘못 그렇게 보이던 버그였음).
  getConcepts(categoryLv2Id: number, accessToken?: string | null): Promise<Concept[]> {
    return httpClient
      .get<ApiEnvelope<Concept[]>>(
        `/api/v1/content/category-lv2/${categoryLv2Id}/concepts`,
        accessToken ? { accessToken } : undefined,
      )
      .then((res) => res.data);
  },

  getConceptDetail(conceptId: number): Promise<ConceptDetail> {
    return httpClient
      .get<ApiEnvelope<ConceptDetail>>(`/api/v1/content/concepts/${conceptId}`)
      .then((res) => res.data);
  },
};
