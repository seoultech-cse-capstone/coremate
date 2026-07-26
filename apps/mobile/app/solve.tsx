import { router, useLocalSearchParams } from "expo-router";

import { useAuth } from "../src/features/auth";
import { ConceptSolveScreen } from "../src/features/solve/components/ConceptSolveScreen";
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
  const { status, accessToken } = useAuth();
  const isGuest = status === "guest";
  const { conceptId, sessionType, title } = useLocalSearchParams<{
    conceptId?: string;
    sessionType?: string;
    title?: string;
  }>();
  const resolvedSessionType = sessionTypes.includes(sessionType as SolveSessionType)
    ? (sessionType as SolveSessionType)
    : "CONCEPT_PRACTICE";
  const resolvedTitle = title ?? "Dynamic Array";
  const parsedConceptId = Number(conceptId);
  const resolvedConceptId = Number.isFinite(parsedConceptId) ? parsedConceptId : null;

  // 개념 문제집은 실제 문제 API로 연결한다. 그 외(모의고사/오늘의 퀴즈)는
  // 아직 백엔드가 더미 데이터라 기존 목데이터 화면을 그대로 쓴다.
  if (resolvedSessionType === "CONCEPT_PRACTICE" && resolvedConceptId !== null) {
    return (
      <ConceptSolveScreen
        conceptId={resolvedConceptId}
        title={resolvedTitle}
        isGuest={isGuest}
        accessToken={accessToken}
        onBack={() => router.back()}
        onComplete={({ correctCount, totalCount, reviewItems }) =>
          router.replace({
            pathname: "/solve-result",
            params: {
              title: resolvedTitle,
              sessionType: resolvedSessionType,
              conceptId: String(resolvedConceptId),
              correctCount: correctCount.toString(),
              totalCount: totalCount.toString(),
              reviewItemsJson: JSON.stringify(reviewItems),
            },
          })
        }
      />
    );
  }

  return (
    <SolveQuestionScreen
      sessionType={resolvedSessionType}
      title={resolvedTitle}
      hideBookmark={isGuest}
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
