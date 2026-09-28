import { useQueryClient } from "@tanstack/react-query";
import { router } from "expo-router";

import { useAuth, useRequireMember } from "../src/features/auth";
import { DailyQuizScreen } from "../src/features/dailyQuiz/components/DailyQuizScreen";

export default function DailyQuizPage() {
  useRequireMember();
  const { accessToken } = useAuth();
  const queryClient = useQueryClient();

  if (!accessToken) {
    return null;
  }

  return (
    <DailyQuizScreen
      accessToken={accessToken}
      onBack={() => router.back()}
      onAlreadySolved={() => router.replace("/daily-quiz-review")}
      onComplete={(result) => {
        // 제출 한 번으로 서버가 출석·보상까지 처리한다. 출석 화면에서 들어온 경우 뒤로 돌아가면
        // 그 화면이 제출 전 캐시("아직 출석 전")를 그대로 보여 주므로, 보상 받기와 같은 범위를 무효화한다.
        queryClient.invalidateQueries({ queryKey: ["attendance"] });
        queryClient.invalidateQueries({ queryKey: ["currency"] });
        queryClient.invalidateQueries({ queryKey: ["reward"] });
        queryClient.invalidateQueries({ queryKey: ["user", "me"] });
        router.replace({
          pathname: "/daily-quiz-result",
          params: { resultJson: JSON.stringify(result) },
        });
      }}
    />
  );
}
