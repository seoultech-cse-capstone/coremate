import { router, useLocalSearchParams } from "expo-router";
import { useEffect, useMemo } from "react";
import { ActivityIndicator, Pressable, StyleSheet, Text, View } from "react-native";

import { FONT } from "../constants/fonts";
import { useAuth } from "../src/features/auth";
import { MistakeExamReview } from "../src/features/logWeak/components/MistakeExamReview";
import type { CompletedSessionType } from "../src/features/logWeak/types";
import { sessionTypeLabel } from "../src/features/logWeak/types";
import { useCompletedProblemSetDetail } from "../src/features/logWeak/useCompletedProblemSets";
import type { SolveReviewItem } from "../src/features/solve/types";
import { getApiErrorMessage } from "../src/shared/utils/apiError";

export default function MistakeExamReviewScreen() {
  const { status, accessToken } = useAuth();
  const { category, examType, reviewItemsJson, sessionType, subject, solveSessionId } =
    useLocalSearchParams<{
      category?: string;
      examType?: string;
      reviewItemsJson?: string;
      sessionType?: CompletedSessionType;
      subject?: string;
      solveSessionId?: string;
    }>();

  const parsedSolveSessionId = solveSessionId ? Number(solveSessionId) : NaN;
  const resolvedSolveSessionId = Number.isFinite(parsedSolveSessionId)
    ? parsedSolveSessionId
    : null;

  // 방금 개념 문제집을 풀고 결과 화면에서 바로 넘어온 경우, 이미 메모리에 있는
  // 결과를 그대로 쓴다(별도 API 조회 불필요).
  const reviewItemsFromParams = useMemo<SolveReviewItem[] | undefined>(() => {
    if (!reviewItemsJson) {
      return undefined;
    }
    try {
      return JSON.parse(reviewItemsJson) as SolveReviewItem[];
    } catch (error) {
      console.error("[mistake-exam-review] reviewItemsJson 파싱 실패", error);
      return undefined;
    }
  }, [reviewItemsJson]);

  // 복습 탭에서 지난 기록을 다시 볼 때는 solveSessionId로 상세를 조회한다.
  const shouldFetchDetail = !reviewItemsFromParams && resolvedSolveSessionId !== null;
  const detailQuery = useCompletedProblemSetDetail(
    shouldFetchDetail ? resolvedSolveSessionId : null,
    accessToken,
  );

  useEffect(() => {
    if (detailQuery.isError) {
      console.error("[mistake-exam-review] 기록 상세 조회 실패", detailQuery.error);
    }
  }, [detailQuery.isError, detailQuery.error]);

  const detail = detailQuery.data;
  const reviewItemsFromDetail = useMemo<SolveReviewItem[] | undefined>(() => {
    if (!detail) {
      return undefined;
    }
    return detail.logs.map((log) => ({
      questionId: log.questionId,
      questionText: log.questionText,
      choices: log.choices,
      selectedAnswer: log.selectedAnswer,
      correctAnswer: log.correctAnswer,
      isCorrect: log.isCorrect,
      explanationText: log.explanationText,
    }));
  }, [detail]);

  const results = reviewItemsFromParams ?? reviewItemsFromDetail;
  const resolvedSessionType = detail?.sessionType ?? sessionType;

  // 게스트는 개념 문제집 해설만 볼 수 있다. 그 외(모의고사 등) 기록 화면은 회원 전용.
  const isBlockedForGuest =
    status === "guest" && resolvedSessionType !== "CONCEPT_PRACTICE";

  useEffect(() => {
    if (isBlockedForGuest) {
      router.replace("/login-required");
    }
  }, [isBlockedForGuest]);

  if (isBlockedForGuest) {
    return null;
  }

  if (shouldFetchDetail && detailQuery.isLoading) {
    return (
      <View style={styles.stateScreen}>
        <ActivityIndicator color="#6D5DFB" size="large" />
      </View>
    );
  }

  if (shouldFetchDetail && detailQuery.isError) {
    return (
      <View style={styles.stateScreen}>
        <Text style={styles.stateText}>
          {getApiErrorMessage(detailQuery.error, "기록을 불러오지 못했습니다.")}
        </Text>
        <Pressable style={styles.stateBackButton} onPress={() => router.back()}>
          <Text style={styles.stateBackButtonText}>돌아가기</Text>
        </Pressable>
      </View>
    );
  }

  return (
    <MistakeExamReview
      category={
        category ??
        detail?.categoryLv2Name ??
        detail?.categoryLv1Name ??
        detail?.subjectName ??
        ""
      }
      examType={
        examType ?? (resolvedSessionType ? sessionTypeLabel[resolvedSessionType] : "")
      }
      sessionType={resolvedSessionType}
      subject={subject ?? detail?.subjectName ?? ""}
      results={results}
      onBack={() => router.back()}
    />
  );
}

const styles = StyleSheet.create({
  stateScreen: {
    flex: 1,
    alignItems: "center",
    justifyContent: "center",
    gap: 16,
    backgroundColor: "#ffffff",
    paddingHorizontal: 24,
  },
  stateText: {
    color: "#64748b",
    fontFamily: FONT.pretendard,
    fontSize: 14,
    fontWeight: "500",
    textAlign: "center",
  },
  stateBackButton: {
    borderRadius: 999,
    backgroundColor: "#6D5DFB",
    paddingHorizontal: 20,
    paddingVertical: 10,
  },
  stateBackButtonText: {
    color: "#ffffff",
    fontFamily: FONT.pretendard,
    fontSize: 14,
    fontWeight: "700",
  },
});
