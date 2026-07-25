import { router } from "expo-router";

import { useRequireMember } from "../src/features/auth";
import { MistakeExamReview } from "../src/features/logWeak/components/MistakeExamReview";

export default function DailyQuizReviewScreen() {
  useRequireMember();

  return (
    <MistakeExamReview
      category="오늘의 퀴즈"
      examType="오늘의 CS 퀴즈"
      sessionType="DAILY_QUIZ"
      subject="Data Structure"
      onBack={() => router.back()}
    />
  );
}
