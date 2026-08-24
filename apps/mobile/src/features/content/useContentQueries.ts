import { useQueries, useQuery } from "@tanstack/react-query";

import { contentApi } from "./contentApi";

export type ChildCountKind = "category-lv1" | "category-lv2" | "concepts" | null;

const CHILD_COUNT_FETCHERS: Record<
  Exclude<ChildCountKind, null>,
  (id: number) => Promise<number>
> = {
  "category-lv1": (id) => contentApi.getCategoryLv1List(id).then((list) => list.length),
  "category-lv2": (id) => contentApi.getCategoryLv2List(id).then((list) => list.length),
  concepts: (id) => contentApi.getConcepts(id).then((list) => list.length),
};

/**
 * 목록 카드에 "N chapter/concept" 식으로 보여줄, 바로 하위 단계 개수를
 * 항목별로 병렬 조회한다. 상세 데이터가 아니라 개수 하나만 필요하므로
 * 응답 배열의 length만 사용한다.
 */
export function useChildCounts(
  kind: ChildCountKind,
  ids: number[],
): Map<number, number> {
  const queries = useQueries({
    queries: ids.map((id) => ({
      queryKey: ["content", "child-count", kind, id],
      queryFn: () => CHILD_COUNT_FETCHERS[kind as Exclude<ChildCountKind, null>](id),
      enabled: kind !== null,
    })),
  });

  const countsById = new Map<number, number>();
  ids.forEach((id, index) => {
    const count = queries[index]?.data;
    if (typeof count === "number") {
      countsById.set(id, count);
    }
  });
  return countsById;
}

export function useSubjects() {
  return useQuery({
    queryKey: ["content", "subjects"],
    queryFn: contentApi.getSubjects,
  });
}

export function useCategoryLv1List(subjectId: number | null) {
  return useQuery({
    queryKey: ["content", "category-lv1", subjectId],
    queryFn: () => contentApi.getCategoryLv1List(subjectId as number),
    enabled: subjectId !== null,
  });
}

export function useCategoryLv2List(categoryLv1Id: number | null) {
  return useQuery({
    queryKey: ["content", "category-lv2", categoryLv1Id],
    queryFn: () => contentApi.getCategoryLv2List(categoryLv1Id as number),
    enabled: categoryLv1Id !== null,
  });
}

export function useConcepts(categoryLv2Id: number | null, accessToken?: string | null) {
  return useQuery({
    // accessToken 유무를 키에 포함해야, 로그인/로그아웃으로 값이 바뀔 때
    // 이전 로그인 상태로 캐싱된 완료 상태를 그대로 보여주지 않는다.
    queryKey: ["content", "concepts", categoryLv2Id, accessToken ?? null],
    queryFn: () => contentApi.getConcepts(categoryLv2Id as number, accessToken),
    enabled: categoryLv2Id !== null,
  });
}

export function useConceptDetail(conceptId: number | null) {
  return useQuery({
    queryKey: ["content", "concept-detail", conceptId],
    queryFn: () => contentApi.getConceptDetail(conceptId as number),
    enabled: conceptId !== null,
  });
}
