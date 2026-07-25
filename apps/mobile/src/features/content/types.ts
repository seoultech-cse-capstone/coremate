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

export type Concept = {
  conceptId: number;
  conceptCode: string;
  categoryLv2Id: number;
  conceptName: string;
  conceptNameEn: string;
  oneLineDefinition: string;
  isCompleted: boolean | null;
  sortOrder: number;
};

export type ApiEnvelope<T> = {
  success: boolean;
  message: string;
  data: T;
};
