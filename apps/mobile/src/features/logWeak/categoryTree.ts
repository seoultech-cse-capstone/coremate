// 오답노트 필터 트리(subject → chapter1 → chapter2). concept 단계는 필터로 노출하지 않는다.
// TODO: 실제 콘텐츠 카테고리 데이터로 교체 예정. 지금은 Data Structure만 예시로 채워두고
// 나머지 과목은 빈 chapter1 배열로 자리만 잡아둔 상태.

export type FilterNode = { id: string; label: string };
export type Chapter1Filter = FilterNode & { chapter2: FilterNode[] };
export type SubjectFilter = FilterNode & { chapter1: Chapter1Filter[] };

export const categoryTree: SubjectFilter[] = [
  {
    id: "data-structure",
    label: "Data Structure",
    chapter1: [
      {
        id: "linear-structure",
        label: "Linear Structure",
        chapter2: [
          { id: "array", label: "Array" },
          { id: "linked-list", label: "Linked List" },
          { id: "stack", label: "Stack" },
          { id: "queue", label: "Queue" },
        ],
      },
    ],
  },
  { id: "algorithm", label: "Algorithm", chapter1: [] },
  { id: "database", label: "Database", chapter1: [] },
  { id: "network", label: "Network", chapter1: [] },
  { id: "operating-system", label: "Operating System", chapter1: [] },
  { id: "computer-architecture", label: "Computer Architecture", chapter1: [] },
];
