import { useEffect, useState } from "react";
import { ActivityIndicator, Pressable, StyleSheet, Text, View } from "react-native";

import { router } from "expo-router";
import { useAuth, useRequireMember } from "../src/features/auth";
import { dailyQuizApi } from "../src/features/dailyQuiz";
import { MistakeExamReview } from "../src/features/logWeak/components/MistakeExamReview";
import type { SolveReviewItem } from "../src/features/solve/types";
import { getApiErrorMessage } from "../src/shared/utils/apiError";

type ResultState =
  | { status: "loading" }
  | { status: "error"; message: string }
  | { status: "ready"; items: SolveReviewItem[] };

export default function DailyQuizReviewScreen() {
  useRequireMember();
  const { accessToken } = useAuth();
  const [state, setState] = useState<ResultState>({ status: "loading" });

  useEffect(() => {
    if (!accessToken) {
      return;
    }
    let cancelled = false;

    dailyQuizApi.getResult(accessToken).then(
      (result) => {
        if (!cancelled) {
          setState({ status: "ready", items: result.questions });
        }
      },
      (error) => {
        if (!cancelled) {
          setState({
            status: "error",
            message: getApiErrorMessage(error, "오늘의 퀴즈 해설을 불러오지 못했습니다."),
          });
        }
      },
    );

    return () => {
      cancelled = true;
    };
  }, [accessToken]);

  if (state.status === "loading") {
    return (
      <View style={styles.stateScreen}>
        <ActivityIndicator color="#6D5DFB" size="large" />
      </View>
    );
  }

  if (state.status === "error") {
    return (
      <View style={styles.stateScreen}>
        <Text style={styles.stateText}>{state.message}</Text>
        <Pressable
          style={styles.stateBackButton}
          onPress={() => router.replace("/(tabs)/home")}
        >
          <Text style={styles.stateBackButtonText}>홈으로 돌아가기</Text>
        </Pressable>
      </View>
    );
  }

  return (
    <MistakeExamReview
      category="오늘의 퀴즈"
      examType="오늘의 CS 퀴즈"
      sessionType="DAILY_QUIZ"
      subject="Data Structure"
      results={state.items}
      onBack={() => router.back()}
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
