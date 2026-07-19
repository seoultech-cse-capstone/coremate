import { router, useLocalSearchParams } from "expo-router";

import { MistakeExamReview } from "../src/features/logWeak/components/MistakeExamReview";

export default function MistakeExamReviewScreen() {
  const { category, examType, sessionType, subject } = useLocalSearchParams<{
    category?: string;
    examType?: string;
    sessionType?: "SUBJECT_MOCK" | "CHAPTER_MOCK" | "CONCEPT_PRACTICE";
    subject?: string;
  }>();

  return (
    <MistakeExamReview
      category={category ?? "Arrays"}
      examType={examType ?? "모의고사"}
      sessionType={sessionType}
      subject={subject ?? "Data Structure"}
      onBack={() => router.back()}
    />
  );
}
