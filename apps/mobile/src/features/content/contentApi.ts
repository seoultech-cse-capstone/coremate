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

  getConcepts(categoryLv2Id: number): Promise<Concept[]> {
    return httpClient
      .get<ApiEnvelope<Concept[]>>(`/api/v1/content/category-lv2/${categoryLv2Id}/concepts`)
      .then((res) => res.data);
  },

  getConceptDetail(conceptId: number): Promise<ConceptDetail> {
    return httpClient
      .get<ApiEnvelope<ConceptDetail>>(`/api/v1/content/concepts/${conceptId}`)
      .then((res) => res.data);
  },
};
