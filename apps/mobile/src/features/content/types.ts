export type Subject = {
  subjectId: number;
  subjectCode: string;
  subjectName: string;
  subjectNameEn: string;
  sortOrder: number;
  isActive: boolean;
};

export type CategoryLv1 = {
  categoryLv1Id: number;
  subjectId: number;
  categoryLv1Code: string;
  categoryLv1Name: string;
  categoryLv1NameEn: string;
  description: string;
  sortOrder: number;
  isActive: boolean;
};

export type CategoryLv2 = {
  categoryLv2Id: number;
  categoryLv1Id: number;
  categoryLv2Code: string;
  categoryLv2Name: string;
  categoryLv2NameEn: string;
  description: string;
  sortOrder: number;
  isActive: boolean;
};

export type ConceptSolveStatus = "NOT_STARTED" | "IN_PROGRESS" | "COMPLETED";
export type PassStatus = "PASS" | "FAIL";

export type Concept = {
  conceptId: number;
  conceptCode: string;
  categoryLv2Id: number;
  conceptName: string;
  conceptNameEn: string;
  oneLineDefinition: string;
  status: ConceptSolveStatus;
  /** 최고 점수 완료 회차의 총 문제 수. 미완료면 null */
  totalQuestionCount: number | null;
  /** 최고 점수 완료 회차의 정답 수. 미완료면 null */
  correctCount: number | null;
  /** 합격 여부(전 문제 정답 시 PASS). 미완료면 null */
  passStatus: PassStatus | null;
  completedCount: number;
  lastCompletedAt: string | null;
  sortOrder: number;
};

export type SourceRef = {
  chunkId: string | null;
  sourceFile: string | null;
  page: number | null;
};

export type ConceptDetail = {
  conceptId: number;
  conceptCode: string;
  subjectId: number;
  subjectName: string;
  categoryLv1Id: number;
  categoryLv1Name: string;
  categoryLv2Id: number;
  categoryLv2Name: string;
  conceptName: string;
  conceptNameEn: string;
  aliases: string[];
  oneLineDefinition: string;
  coreSummary: string;
  coreKeywords: string[];
  keyProperties: string[];
  sourceRefs: SourceRef[];
  reviewStatus: string;
  isActive: boolean;
};

export type ApiEnvelope<T> = {
  success: boolean;
  message: string;
  data: T;
};
