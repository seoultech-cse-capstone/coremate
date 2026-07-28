import { router, useLocalSearchParams } from "expo-router";

import { useAuth } from "../src/features/auth";
import {
  ConceptSolveScreen,
  type SolveTarget,
} from "../src/features/solve/components/ConceptSolveScreen";
import {
  SolveQuestionScreen,
  type SolveSessionType,
} from "../src/features/solve/components/SolveQuestionScreen";

const sessionTypes: SolveSessionType[] = [
  "SUBJECT_MOCK",
  "CATEGORY_LV1_MOCK",
  "CATEGORY_LV2_MOCK",
  "CONCEPT_PRACTICE",
  "DAILY_QUIZ",
];

export default function SolveScreen() {
  const { status, accessToken } = useAuth();
  const isGuest = status === "guest";
  const { conceptId, sessionType, targetId, title } = useLocalSearchParams<{
    conceptId?: string;
    sessionType?: string;
    targetId?: string;
    title?: string;
  }>();
  const resolvedSessionType = sessionTypes.includes(sessionType as SolveSessionType)
    ? (sessionType as SolveSessionType)
    : "CONCEPT_PRACTICE";
  const resolvedTitle = title ?? "Dynamic Array";
  const parsedConceptId = Number(conceptId);
  const resolvedConceptId = Number.isFinite(parsedConceptId) ? parsedConceptId : null;
  const parsedTargetId = Number(targetId);
  const resolvedTargetId = Number.isFinite(parsedTargetId) ? parsedTargetId : null;

  const target: SolveTarget | null =
    resolvedSessionType === "CONCEPT_PRACTICE" && resolvedConceptId !== null
      ? { type: "concept", conceptId: resolvedConceptId }
      : resolvedSessionType === "SUBJECT_MOCK" && resolvedTargetId !== null
        ? { type: "subjectMock", subjectId: resolvedTargetId }
        : resolvedSessionType === "CATEGORY_LV1_MOCK" && resolvedTargetId !== null
          ? { type: "categoryLv1Mock", categoryLv1Id: resolvedTargetId }
          : resolvedSessionType === "CATEGORY_LV2_MOCK" && resolvedTargetId !== null
            ? { type: "categoryLv2Mock", categoryLv2Id: resolvedTargetId }
            : null;

  // 개념 문제집/모의고사 3종은 실제 문제 API로 연결한다. 그 외(오늘의 퀴즈)는
  // 아직 백엔드가 더미 데이터라 기존 목데이터 화면을 그대로 쓴다.
  if (target !== null) {
    return (
      <ConceptSolveScreen
        target={target}
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
              conceptId: target.type === "concept" ? String(target.conceptId) : undefined,
              // "다시 풀기"가 같은 대상으로 새 세션을 다시 시작할 수 있도록
              // 모의고사 3종은 targetId도 결과 화면까지 그대로 들고 간다.
              targetId:
                target.type === "subjectMock"
                  ? String(target.subjectId)
                  : target.type === "categoryLv1Mock"
                    ? String(target.categoryLv1Id)
                    : target.type === "categoryLv2Mock"
                      ? String(target.categoryLv2Id)
                      : undefined,
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
