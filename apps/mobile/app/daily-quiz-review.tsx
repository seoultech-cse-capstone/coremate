import { router } from "expo-router";

import { MistakeExamReview } from "../src/features/logWeak/components/MistakeExamReview";

export default function DailyQuizReviewScreen() {
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
