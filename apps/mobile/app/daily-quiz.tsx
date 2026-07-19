import { router } from "expo-router";

import { SolveQuestionScreen } from "../src/features/solve/components/SolveQuestionScreen";

export default function DailyQuizScreen() {
  return (
    <SolveQuestionScreen
      sessionType="DAILY_QUIZ"
      title="오늘의 CS 퀴즈"
      onBack={() => router.back()}
      onComplete={(correctCount, totalCount) =>
        router.replace({
          pathname: "/daily-quiz-result",
          params: {
            correctCount: correctCount.toString(),
            totalCount: totalCount.toString(),
          },
        })
      }
    />
  );
}
