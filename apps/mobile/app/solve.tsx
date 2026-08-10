import { router, useLocalSearchParams } from "expo-router";
import { Pressable, StyleSheet, Text, View } from "react-native";

import { useAuth } from "../src/features/auth";
import {
  ConceptSolveScreen,
  type SolveTarget,
} from "../src/features/solve/components/ConceptSolveScreen";
import type { SolveSessionType } from "../src/features/solve/types";

const sessionTypes: SolveSessionType[] = [
  "SUBJECT_MOCK",
  "CATEGORY_LV1_MOCK",
  "CATEGORY_LV2_MOCK",
  "CONCEPT_PRACTICE",
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

  if (target === null) {
    return (
      <View style={styles.stateScreen}>
        <Text style={styles.stateText}>
          잘못된 접근입니다. 문제집 목록에서 다시 시도해주세요.
        </Text>
        <Pressable style={styles.stateBackButton} onPress={() => router.back()}>
          <Text style={styles.stateBackButtonText}>돌아가기</Text>
        </Pressable>
      </View>
    );
  }

  return (
    <ConceptSolveScreen
      target={target}
      title={resolvedTitle}
      isGuest={isGuest}
      accessToken={accessToken}
      onBack={() => router.back()}
      onComplete={({ correctCount, totalCount, reviewItems, elapsedSeconds }) =>
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
            elapsedSeconds: elapsedSeconds.toString(),
          },
        })
      }
    />
  );
}

const styles = StyleSheet.create({
  stateScreen: {
    flex: 1,
    alignItems: "center",
    justifyContent: "center",
    backgroundColor: "#F3F3F3",
    paddingHorizontal: 32,
  },
  stateText: {
    color: "#334155",
    fontSize: 15,
    fontWeight: "600",
    lineHeight: 22,
    textAlign: "center",
  },
  stateBackButton: {
    marginTop: 20,
    borderRadius: 8,
    backgroundColor: "#6D5DFB",
    paddingHorizontal: 24,
    paddingVertical: 12,
  },
  stateBackButtonText: {
    color: "#ffffff",
    fontSize: 14,
    fontWeight: "700",
  },
});
