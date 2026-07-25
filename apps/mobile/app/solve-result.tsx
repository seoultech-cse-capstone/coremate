import { router, useLocalSearchParams } from "expo-router";

import {
  SolveResultScreen,
} from "../src/features/solve/components/SolveResultScreen";
import type { SolveSessionType } from "../src/features/solve/components/SolveQuestionScreen";

const sessionTypes: SolveSessionType[] = [
  "SUBJECT_MOCK",
  "CHAPTER_MOCK",
  "CONCEPT_PRACTICE",
  "DAILY_QUIZ",
];

export default function SolveResultRoute() {
  const { correctCount, elapsedSeconds, sessionType, title, totalCount } = useLocalSearchParams<{
    correctCount?: string;
    elapsedSeconds?: string;
    sessionType?: string;
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

  return (
    <SolveResultScreen
      correctCount={resolvedCorrectCount}
      elapsedSeconds={Number(elapsedSeconds) || 0}
      sessionType={resolvedSessionType}
      title={resolvedTitle}
      totalCount={resolvedTotalCount}
      onBackToContent={() => router.back()}
      onRetry={() =>
        router.replace({
          pathname: "/solve",
          params: { title: resolvedTitle, sessionType: resolvedSessionType },
        })
      }
      onReview={() => {
        router.push({
          pathname: "/mistake-exam-review",
          params: {
            subject: "Data Structure",
            category: resolvedTitle,
            sessionType: resolvedSessionType,
          },
        });
      }}
    />
  );
}
