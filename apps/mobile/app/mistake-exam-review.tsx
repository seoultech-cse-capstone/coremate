import { router, useLocalSearchParams } from "expo-router";
import { useEffect } from "react";

import { useAuth } from "../src/features/auth";
import { MistakeExamReview } from "../src/features/logWeak/components/MistakeExamReview";

export default function MistakeExamReviewScreen() {
  const { status } = useAuth();
  const { category, examType, sessionType, subject } = useLocalSearchParams<{
    category?: string;
    examType?: string;
    sessionType?:
      | "SUBJECT_MOCK"
      | "CHAPTER_MOCK"
      | "CONCEPT_PRACTICE"
      | "DAILY_QUIZ";
    subject?: string;
  }>();

  // 게스트는 개념 문제집 해설만 볼 수 있다. 그 외(모의고사 등) 기록 화면은 회원 전용.
  const isBlockedForGuest =
    status === "guest" && sessionType !== "CONCEPT_PRACTICE";

  useEffect(() => {
    if (isBlockedForGuest) {
      router.replace("/login-required");
    }
  }, [isBlockedForGuest]);

  if (isBlockedForGuest) {
    return null;
  }

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
