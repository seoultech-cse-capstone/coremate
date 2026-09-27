import { router, useLocalSearchParams } from "expo-router";

import { useAuth } from "../src/features/auth";
import {
  SolveResultScreen,
} from "../src/features/solve/components/SolveResultScreen";
import type { SolveSessionType } from "../src/features/solve/types";

const sessionTypes: SolveSessionType[] = [
  "SUBJECT_MOCK",
  "CATEGORY_LV1_MOCK",
  "CATEGORY_LV2_MOCK",
  "CONCEPT_PRACTICE",
];

export default function SolveResultRoute() {
  const { isAuthenticated } = useAuth();
  const {
    conceptId,
    correctCount,
    earnedCoin,
    earnedXp,
    elapsedSeconds,
    reviewItemsJson,
    sessionType,
    targetId,
    title,
    totalCount,
  } = useLocalSearchParams<{
    conceptId?: string;
    correctCount?: string;
    earnedCoin?: string;
    earnedXp?: string;
    elapsedSeconds?: string;
    reviewItemsJson?: string;
    sessionType?: string;
    targetId?: string;
    title?: string;
    totalCount?: string;
  }>();
  const resolvedSessionType = sessionTypes.includes(sessionType as SolveSessionType)
    ? (sessionType as SolveSessionType)
    : "CONCEPT_PRACTICE";
  const resolvedTitle = title ?? "Dynamic Array";
  const resolvedTotalCount = Number(totalCount) || 5;
  const resolvedCorrectCount = Math.min(
    Number(correctCount) || 0,
    resolvedTotalCount,
  );
  // earnedXp/earnedCoin은 회원 완료 시에만 전달된다(게스트 풀이는 보상이 없다).
  // 둘 다 항상 숫자로 내려오므로(백엔드 SolveRewardResult가 int 타입, 미지급이면 0)
  // 존재 여부가 아니라 실제로 지급된 값이 있는지로 카드 표시 여부를 판단해야 한다.
  const resolvedXpAmount = Number(earnedXp) || 0;
  const resolvedCoinAmount = Number(earnedCoin) || 0;
  const reward =
    earnedXp !== undefined &&
    earnedCoin !== undefined &&
    (resolvedXpAmount > 0 || resolvedCoinAmount > 0)
      ? { xpAmount: resolvedXpAmount, coinAmount: resolvedCoinAmount }
      : undefined;

  return (
    <SolveResultScreen
      correctCount={resolvedCorrectCount}
      elapsedSeconds={Number(elapsedSeconds) || 0}
      reward={reward}
      sessionType={resolvedSessionType}
      title={resolvedTitle}
      totalCount={resolvedTotalCount}
      onBackToContent={() => router.back()}
      onRetry={() =>
        router.replace({
          pathname: "/solve",
          params: {
            title: resolvedTitle,
            sessionType: resolvedSessionType,
            ...(conceptId ? { conceptId } : {}),
            ...(targetId ? { targetId } : {}),
          },
        })
      }
      onWeaknessAnalysis={() => {
        // FR-GUEST-02: 약점 분석은 회원 전용이라 게스트는 로그인 안내로 보낸다.
        if (!isAuthenticated) {
          router.push("/login-required");
          return;
        }
        router.push({ pathname: "/(tabs)/log-weak", params: { mode: "weakness" } });
      }}
      onReview={() => {
        router.push({
          pathname: "/mistake-exam-review",
          params: {
            subject: "Data Structure",
            category: resolvedTitle,
            sessionType: resolvedSessionType,
            ...(reviewItemsJson ? { reviewItemsJson } : {}),
          },
        });
      }}
    />
  );
}
