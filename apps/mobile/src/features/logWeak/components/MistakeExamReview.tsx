import { ChevronLeft, ChevronRight, CircleCheck, CircleX } from "lucide-react-native";
import { useMemo, useRef, useState } from "react";
import { Pressable, ScrollView, StyleSheet, Text, View } from "react-native";
import { useSafeAreaInsets } from "react-native-safe-area-context";

import { FONT } from "../../../../constants/fonts";
import type { SolveReviewItem } from "../../solve/types";
import { sessionTypeLabel, type CompletedSessionType } from "../types";

type DisplayChoice = {
  key: string;
  text: string;
  isAnswer: boolean;
  isSelected: boolean;
};

type DisplayQuestion = {
  id: number;
  question: string;
  choices: DisplayChoice[];
  isCorrect: boolean;
  explanation: string;
};

type MistakeExamReviewProps = {
  category: string;
  examType: string;
  sessionType?: CompletedSessionType;
  subject: string;
  /** 실제 풀이 결과. 비어 있으면 기록을 찾지 못했다는 안내만 보여준다(#242 — 예전엔 목데이터로 채웠다). */
  results?: SolveReviewItem[];
  onBack: () => void;
};

export function MistakeExamReview({
  category,
  examType,
  sessionType,
  subject,
  results,
  onBack,
}: MistakeExamReviewProps) {
  const insets = useSafeAreaInsets();
  const [currentIndex, setCurrentIndex] = useState(0);

  const scrollRef = useRef<ScrollView>(null);

  const displayQuestions: DisplayQuestion[] = useMemo(
    () =>
      (results ?? []).map((item) => ({
        id: item.questionId,
        question: item.questionText,
        choices: item.choices.map((choice) => ({
          key: choice.key,
          text: choice.text,
          isAnswer: choice.key === item.correctAnswer,
          isSelected: choice.key === item.selectedAnswer,
        })),
        isCorrect: item.isCorrect,
        explanation: item.explanationText,
      })),
    [results],
  );

  const currentQuestion = displayQuestions[currentIndex];
  const displayedExamType = sessionType
    ? sessionTypeLabel[sessionType]
    : examType;

  // 이전/다음은 해설을 끝까지 읽은 아래쪽에서 누르는 경우가 많아, 새 문제는 맨 위부터 보여준다(#241).
  const moveTo = (index: number) => {
    setCurrentIndex(index);
    scrollRef.current?.scrollTo({ y: 0, animated: false });
  };

  return (
    <View style={styles.screen}>
      <View style={[styles.header, { paddingTop: insets.top }]}> 
        <Pressable
          accessibilityLabel="뒤로 가기"
          accessibilityRole="button"
          hitSlop={10}
          style={styles.backButton}
          onPress={onBack}
        >
          <ChevronLeft color="#1f2937" size={28} strokeWidth={2.2} />
        </Pressable>

        <Text numberOfLines={1} style={styles.headerTitle}>
          문제 해설
        </Text>

        <View style={styles.headerPlaceholder} />
      </View>

      <ScrollView
        ref={scrollRef}
        bounces
        contentContainerStyle={styles.content}
        showsVerticalScrollIndicator={false}
      >
        <View style={styles.examInfo}>
          {subject ? (
            <>
              <Text style={styles.subjectText}>{subject}</Text>
              <Text style={styles.infoDivider}>·</Text>
            </>
          ) : null}
          <Text style={styles.categoryText}>{category}</Text>
          <Text style={styles.examTypeText}>{displayedExamType}</Text>
        </View>

        {!currentQuestion ? (
          <Text style={styles.emptyText}>풀이 기록을 찾지 못했어요.</Text>
        ) : (
          <>
            <View style={styles.progressRow}>
              <Text style={styles.progressText}>
                {currentIndex + 1} / {displayQuestions.length}
              </Text>
              <View style={styles.progressTrack}>
                <View
                  style={[
                    styles.progressValue,
                    { width: `${((currentIndex + 1) / displayQuestions.length) * 100}%` },
                  ]}
                />
              </View>
            </View>

            <View style={styles.navigationRow}>
              <Pressable
                accessibilityLabel="이전 문제"
                disabled={currentIndex === 0}
                style={[
                  styles.navigationButton,
                  currentIndex === 0 && styles.disabledButton,
                ]}
                onPress={() => moveTo(currentIndex - 1)}
              >
                <ChevronLeft color={currentIndex === 0 ? "#94a3b8" : "#4b5563"} size={20} />
                <Text
                  style={[
                    styles.navigationText,
                    currentIndex === 0 && styles.disabledNavigationText,
                  ]}
                >
                  이전
                </Text>
              </Pressable>

              <Pressable
                accessibilityLabel="다음 문제"
                disabled={currentIndex === displayQuestions.length - 1}
                style={[
                  styles.navigationButton,
                  currentIndex === displayQuestions.length - 1 && styles.disabledButton,
                ]}
                onPress={() => moveTo(currentIndex + 1)}
              >
                <Text
                  style={[
                    styles.navigationText,
                    currentIndex === displayQuestions.length - 1 && styles.disabledNavigationText,
                  ]}
                >
                  다음
                </Text>
                <ChevronRight
                  color={currentIndex === displayQuestions.length - 1 ? "#94a3b8" : "#4b5563"}
                  size={20}
                />
              </Pressable>
            </View>

            <View style={styles.questionCard}>
              <View style={styles.resultHeader}>
                {currentQuestion.isCorrect ? (
                  <CircleCheck color="#16a34a" size={22} strokeWidth={2.4} />
                ) : (
                  <CircleX color="#ef4444" size={22} strokeWidth={2.4} />
                )}
                <Text style={[styles.resultText, currentQuestion.isCorrect ? styles.correct : styles.wrong]}>
                  {currentQuestion.isCorrect ? "정답이에요" : "오답이에요"}
                </Text>
              </View>

              <Text style={styles.question}>{currentQuestion.question}</Text>

              <View style={styles.choiceList}>
                {currentQuestion.choices.map((choice) => {
                  const isAnswer = choice.isAnswer;
                  const isSelected = choice.isSelected;

                  return (
                    <View
                      key={choice.key}
                      style={[
                        styles.choice,
                        isAnswer && styles.answerChoice,
                        isSelected && !isAnswer && styles.selectedWrongChoice,
                      ]}
                    >
                      <Text
                        style={[
                          styles.choiceKey,
                          isAnswer && styles.answerChoiceText,
                          isSelected && !isAnswer && styles.wrongChoiceText,
                        ]}
                      >
                        {choice.key}
                      </Text>
                      <Text
                        style={[
                          styles.choiceText,
                          isAnswer && styles.answerChoiceText,
                          isSelected && !isAnswer && styles.wrongChoiceText,
                        ]}
                      >
                        {choice.text}
                      </Text>
                      {isAnswer && (
                        <CircleCheck color="#16a34a" size={20} strokeWidth={2.5} />
                      )}
                      {isSelected && !isAnswer && (
                        <CircleX color="#ef4444" size={20} strokeWidth={2.5} />
                      )}
                    </View>
                  );
                })}
              </View>

              <View style={styles.explanation}>
                <Text style={styles.explanationTitle}>해설</Text>
                <Text style={styles.explanationText}>{currentQuestion.explanation}</Text>
              </View>
            </View>
          </>
        )}
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  screen: {
    flex: 1,
    backgroundColor: "#F3F3F3",
  },
  header: {
    minHeight: 60,
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
    borderBottomWidth: StyleSheet.hairlineWidth,
    borderBottomColor: "#e5e7eb",
    backgroundColor: "#ffffff",
    paddingHorizontal: 16,
  },
  backButton: {
    alignItems: "center",
    justifyContent: "center",
    width: 32,
    height: 40,
  },
  headerTitle: {
    color: "#1f2937",
    fontFamily: FONT.pretendard,
    fontSize: 17,
    fontWeight: "700",
  },
  headerPlaceholder: {
    width: 32,
  },
  content: {
    paddingHorizontal: 20,
    paddingTop: 24,
    paddingBottom: 44,
  },
  examInfo: {
    flexDirection: "row",
    alignItems: "center",
  },
  subjectText: {
    color: "#6D5DFB",
    fontFamily: FONT.kufam,
    fontSize: 13,
    fontWeight: "600",
  },
  infoDivider: {
    marginHorizontal: 6,
    color: "#94a3b8",
  },
  categoryText: {
    flexShrink: 1,
    color: "#475569",
    fontFamily: FONT.kufam,
    fontSize: 13,
    fontWeight: "500",
  },
  examTypeText: {
    marginLeft: "auto",
    color: "#94a3b8",
    fontFamily: FONT.pretendard,
    fontSize: 12,
    fontWeight: "600",
  },
  emptyText: {
    marginTop: 48,
    color: "#64748b",
    fontFamily: FONT.pretendard,
    fontSize: 14,
    fontWeight: "500",
    textAlign: "center",
  },
  progressRow: {
    flexDirection: "row",
    alignItems: "center",
    gap: 10,
    marginTop: 16,
  },
  progressText: {
    color: "#6D5DFB",
    fontFamily: FONT.kufam,
    fontSize: 13,
    fontWeight: "700",
  },
  progressTrack: {
    flex: 1,
    height: 6,
    overflow: "hidden",
    borderRadius: 999,
    backgroundColor: "#dfe3ea",
  },
  progressValue: {
    height: "100%",
    borderRadius: 999,
    backgroundColor: "#6D5DFB",
  },
  questionCard: {
    marginTop: 20,
    borderRadius: 8,
    backgroundColor: "#ffffff",
    padding: 20,
  },
  resultHeader: {
    flexDirection: "row",
    alignItems: "center",
    gap: 7,
  },
  resultText: {
    fontFamily: FONT.pretendard,
    fontSize: 15,
    fontWeight: "700",
  },
  correct: {
    color: "#16a34a",
  },
  wrong: {
    color: "#ef4444",
  },
  question: {
    color: "#1f2937",
    fontFamily: FONT.pretendard,
    fontSize: 18,
    fontWeight: "700",
    lineHeight: 28,
    marginTop: 16,
  },
  choiceList: {
    gap: 10,
    marginTop: 24,
  },
  choice: {
    minHeight: 54,
    flexDirection: "row",
    alignItems: "center",
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
  selectedWrongChoice: {
    borderColor: "#fca5a5",
    backgroundColor: "#fef2f2",
  },
  choiceKey: {
    width: 26,
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
  wrongChoiceText: {
    color: "#dc2626",
  },
  explanation: {
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
    color: "#45405f",
    fontFamily: FONT.pretendard,
    fontSize: 14,
    fontWeight: "500",
    lineHeight: 22,
    marginTop: 8,
  },
  navigationRow: {
    flexDirection: "row",
    justifyContent: "space-between",
    marginTop: 20,
  },
  navigationButton: {
    minWidth: 92,
    minHeight: 44,
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "center",
    gap: 2,
    borderRadius: 8,
    backgroundColor: "#ffffff",
  },
  disabledButton: {
    backgroundColor: "#edf0f3",
  },
  navigationText: {
    color: "#4b5563",
    fontFamily: FONT.pretendard,
    fontSize: 14,
    fontWeight: "700",
  },
  disabledNavigationText: {
    color: "#94a3b8",
  },
});
