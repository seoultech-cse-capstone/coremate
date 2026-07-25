import { useQuery } from "@tanstack/react-query";

import { contentApi } from "./contentApi";

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

export function useConcepts(categoryLv2Id: number | null) {
  return useQuery({
    queryKey: ["content", "concepts", categoryLv2Id],
    queryFn: () => contentApi.getConcepts(categoryLv2Id as number),
    enabled: categoryLv2Id !== null,
  });
}
