import { router } from "expo-router";

import { useAuth, useRequireMember } from "../src/features/auth";
import { DailyQuizScreen } from "../src/features/dailyQuiz/components/DailyQuizScreen";

export default function DailyQuizPage() {
  useRequireMember();
  const { accessToken } = useAuth();

  if (!accessToken) {
    return null;
  }

  return (
    <DailyQuizScreen
      accessToken={accessToken}
      onBack={() => router.back()}
      onAlreadySolved={() => router.replace("/daily-quiz-review")}
      onComplete={(result) =>
        router.replace({
          pathname: "/daily-quiz-result",
          params: { resultJson: JSON.stringify(result) },
        })
      }
    />
  );
}
