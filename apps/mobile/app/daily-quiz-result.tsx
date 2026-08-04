import { router, useLocalSearchParams } from "expo-router";

import { useRequireMember } from "../src/features/auth";
import type { DailyQuizSubmitResult } from "../src/features/dailyQuiz";
import { SolveResultScreen } from "../src/features/solve/components/SolveResultScreen";

export default function DailyQuizResultScreen() {
  useRequireMember();

  const { resultJson } = useLocalSearchParams<{ resultJson?: string }>();
  const result: DailyQuizSubmitResult | null = resultJson
    ? JSON.parse(resultJson)
    : null;

  if (!result) {
    router.replace("/(tabs)/home");
    return null;
  }

  return (
    <SolveResultScreen
      correctCount={result.correctCount}
      totalCount={result.totalQuestionCount}
      dailyReward={{
        xpAmount: result.reward.xpAmount,
        coinAmount: result.reward.coinAmount,
      }}
      isDailyQuiz
      onBackToContent={() => router.replace("/(tabs)/content")}
      onGoHome={() => router.replace("/(tabs)/home")}
      onRetry={() => router.replace("/daily-quiz")}
      onReview={() => router.push("/daily-quiz-review")}
      sessionType="DAILY_QUIZ"
      title="오늘의 CS 퀴즈"
    />
  );
}
