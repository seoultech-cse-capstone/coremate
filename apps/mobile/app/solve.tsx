import { router, useLocalSearchParams } from "expo-router";

import {
  SolveQuestionScreen,
  type SolveSessionType,
} from "../src/features/solve/components/SolveQuestionScreen";

const sessionTypes: SolveSessionType[] = [
  "SUBJECT_MOCK",
  "CHAPTER_MOCK",
  "CONCEPT_PRACTICE",
  "DAILY_QUIZ",
];

export default function SolveScreen() {
  const { sessionType, title } = useLocalSearchParams<{
    sessionType?: string;
    title?: string;
  }>();
  const resolvedSessionType = sessionTypes.includes(sessionType as SolveSessionType)
    ? (sessionType as SolveSessionType)
    : "CONCEPT_PRACTICE";
  const resolvedTitle = title ?? "Dynamic Array";

  return (
    <SolveQuestionScreen
      sessionType={resolvedSessionType}
      title={resolvedTitle}
      onBack={() => router.back()}
      onComplete={(correctCount, totalCount, elapsedSeconds) =>
        router.replace({
          pathname: "/solve-result",
          params: {
            title: resolvedTitle,
            sessionType: resolvedSessionType,
            correctCount: correctCount.toString(),
            totalCount: totalCount.toString(),
            elapsedSeconds: elapsedSeconds?.toString(),
          },
        })
      }
    />
  );
}
