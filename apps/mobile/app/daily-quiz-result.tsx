import { router, useLocalSearchParams } from "expo-router";

import { SolveResultScreen } from "../src/features/solve/components/SolveResultScreen";

export default function DailyQuizResultScreen() {
  const { correctCount, totalCount } = useLocalSearchParams<{
    correctCount?: string;
    totalCount?: string;
  }>();
  const resolvedTotalCount = Number(totalCount) || 5;
  const resolvedCorrectCount = Math.min(
    Number(correctCount) || 0,
    resolvedTotalCount,
  );

  return (
    <SolveResultScreen
      correctCount={resolvedCorrectCount}
      isDailyQuiz
      onBackToContent={() => router.replace("/(tabs)/content")}
      onGoHome={() => router.replace("/(tabs)/home")}
      onRetry={() => router.replace("/daily-quiz")}
      onReview={() =>
        router.push("/daily-quiz-review")
      }
      sessionType="DAILY_QUIZ"
      title="오늘의 CS 퀴즈"
      totalCount={resolvedTotalCount}
    />
  );
}
