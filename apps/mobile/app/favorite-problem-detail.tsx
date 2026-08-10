import { router, useLocalSearchParams } from "expo-router";
import { ChevronLeft, CircleCheck } from "lucide-react-native";
import { useEffect, useState } from "react";
import { ActivityIndicator, Pressable, ScrollView, StyleSheet, Text, View } from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";

import { FONT } from "../constants/fonts";
import { useAuth, useRequireMember } from "../src/features/auth";
import { bookmarkApi } from "../src/features/bookmark/bookmarkApi";
import type { BookmarkedQuestionDetail } from "../src/features/bookmark/types";
import { getApiErrorMessage } from "../src/shared/utils/apiError";

type ScreenState =
  | { status: "loading" }
  | { status: "error"; message: string }
  | { status: "ready"; detail: BookmarkedQuestionDetail };

export default function FavoriteProblemDetailScreen() {
  useRequireMember();
  const { accessToken } = useAuth();
  const { questionId } = useLocalSearchParams<{ questionId: string }>();
  const [state, setState] = useState<ScreenState>({ status: "loading" });

  useEffect(() => {
    const parsedQuestionId = Number(questionId);
    if (!accessToken || !Number.isFinite(parsedQuestionId)) {
      return;
    }
    let cancelled = false;

    bookmarkApi.getBookmarkedQuestionDetail(parsedQuestionId, accessToken).then(
      (detail) => {
        if (!cancelled) {
          setState({ status: "ready", detail });
        }
      },
      (error) => {
        if (!cancelled) {
          setState({
            status: "error",
            message: getApiErrorMessage(error, "문제 정보를 불러오지 못했습니다."),
          });
        }
      },
    );

    return () => {
      cancelled = true;
    };
  }, [accessToken, questionId]);

  return (
    <View style={styles.screen}>
      <SafeAreaView edges={["top"]} style={styles.safeArea}>
        <View style={styles.headerArea}>
          <Pressable
            accessibilityLabel="뒤로가기"
            accessibilityRole="button"
            hitSlop={10}
            style={styles.backButton}
            onPress={() => router.back()}
          >
            <ChevronLeft color="#111827" size={28} strokeWidth={2.3} />
          </Pressable>
          <Text style={styles.title}>문제 해설</Text>
        </View>

        {state.status === "loading" ? (
          <View style={styles.stateArea}>
            <ActivityIndicator color="#6D5DFB" size="large" />
          </View>
        ) : state.status === "error" ? (
          <View style={styles.stateArea}>
            <Text style={styles.stateText}>{state.message}</Text>
          </View>
        ) : (
          <ScrollView contentContainerStyle={styles.content} showsVerticalScrollIndicator={false}>
            <Text style={styles.conceptText}>{state.detail.conceptName}</Text>
            <Text style={styles.question}>{state.detail.questionText}</Text>

            <View style={styles.choiceList}>
              {state.detail.choices.map((choice) => {
                const isAnswer = choice.key === state.detail.correctAnswer;
                return (
                  <View
                    key={choice.key}
                    style={[styles.choice, isAnswer && styles.answerChoice]}
                  >
                    <Text style={[styles.choiceKey, isAnswer && styles.answerChoiceText]}>
                      {choice.key}
                    </Text>
                    <Text style={[styles.choiceText, isAnswer && styles.answerChoiceText]}>
                      {choice.text}
                    </Text>
                    {isAnswer ? (
                      <CircleCheck color="#16a34a" size={20} strokeWidth={2.5} />
                    ) : null}
                  </View>
                );
              })}
            </View>

            <View style={styles.explanationBox}>
              <Text style={styles.explanationTitle}>해설</Text>
              <Text style={styles.explanationText}>{state.detail.explanationText}</Text>
            </View>

            {state.detail.memo ? (
              <View style={styles.memoBox}>
                <Text style={styles.memoTitle}>저장한 메모</Text>
                <Text style={styles.memoText}>{state.detail.memo}</Text>
              </View>
            ) : null}
          </ScrollView>
        )}
      </SafeAreaView>
    </View>
  );
}

const styles = StyleSheet.create({
  screen: {
    flex: 1,
    backgroundColor: "#F3F3F3",
  },
  safeArea: {
    flex: 1,
    backgroundColor: "transparent",
  },
  headerArea: {
    position: "relative",
    minHeight: 64,
    alignItems: "center",
    justifyContent: "center",
    backgroundColor: "#ffffff",
    paddingHorizontal: 56,
  },
  backButton: {
    position: "absolute",
    left: 12,
    top: 11,
    alignItems: "center",
    justifyContent: "center",
    width: 42,
    height: 42,
  },
  title: {
    color: "#111827",
    fontFamily: FONT.pretendard,
    fontSize: 18,
    fontWeight: "700",
    textAlign: "center",
  },
  stateArea: {
    flex: 1,
    alignItems: "center",
    justifyContent: "center",
    paddingHorizontal: 32,
  },
  stateText: {
    color: "#334155",
    fontFamily: FONT.pretendard,
    fontSize: 14,
    fontWeight: "600",
    textAlign: "center",
    lineHeight: 21,
  },
  content: {
    paddingHorizontal: 20,
    paddingTop: 20,
    paddingBottom: 44,
  },
  conceptText: {
    color: "#6D5DFB",
    fontFamily: FONT.kufam,
    fontSize: 13,
    fontWeight: "700",
  },
  question: {
    marginTop: 12,
    color: "#1f2937",
    fontFamily: FONT.pretendard,
    fontSize: 18,
    fontWeight: "700",
    lineHeight: 28,
  },
  choiceList: {
    gap: 10,
    marginTop: 24,
  },
  choice: {
    minHeight: 54,
    flexDirection: "row",
    alignItems: "center",
    gap: 10,
    borderWidth: 1,
    borderColor: "#e5e7eb",
    borderRadius: 8,
    backgroundColor: "#ffffff",
    paddingHorizontal: 14,
  },
  answerChoice: {
    borderColor: "#86efac",
    backgroundColor: "#f0fdf4",
  },
  choiceKey: {
    width: 22,
    color: "#64748b",
    fontFamily: FONT.kufam,
    fontSize: 14,
    fontWeight: "600",
  },
  choiceText: {
    flex: 1,
    color: "#334155",
    fontFamily: FONT.pretendard,
    fontSize: 14,
    fontWeight: "500",
    lineHeight: 20,
  },
  answerChoiceText: {
    color: "#15803d",
  },
  explanationBox: {
    marginTop: 24,
    borderRadius: 8,
    backgroundColor: "#F3F1FF",
    padding: 16,
  },
  explanationTitle: {
    color: "#5b4de8",
    fontFamily: FONT.pretendard,
    fontSize: 14,
    fontWeight: "700",
  },
  explanationText: {
    marginTop: 8,
    color: "#45405f",
    fontFamily: FONT.pretendard,
    fontSize: 14,
    fontWeight: "500",
    lineHeight: 22,
  },
  memoBox: {
    marginTop: 16,
    borderRadius: 8,
    backgroundColor: "#ffffff",
    borderWidth: 1,
    borderColor: "#E5E7EB",
    padding: 16,
  },
  memoTitle: {
    color: "#334155",
    fontFamily: FONT.pretendard,
    fontSize: 14,
    fontWeight: "700",
  },
  memoText: {
    marginTop: 8,
    color: "#475569",
    fontFamily: FONT.pretendard,
    fontSize: 14,
    lineHeight: 21,
  },
});
