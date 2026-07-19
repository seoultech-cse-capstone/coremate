import { ChevronLeft, ChevronRight, CircleCheck, CircleX } from "lucide-react-native";
import { useMemo, useState } from "react";
import { Pressable, ScrollView, StyleSheet, Text, View } from "react-native";
import { useSafeAreaInsets } from "react-native-safe-area-context";

import { FONT } from "../../../../constants/fonts";

type ReviewQuestion = {
  id: number;
  question: string;
  choices: string[];
  answerIndex: number;
  selectedIndex: number;
  explanation: string;
};

type MistakeExamReviewProps = {
  category: string;
  examType: string;
  sessionType?:
    | "SUBJECT_MOCK"
    | "CHAPTER_MOCK"
    | "CONCEPT_PRACTICE"
    | "DAILY_QUIZ";
  subject: string;
  onBack: () => void;
};

const sessionTypeLabel = {
  SUBJECT_MOCK: "과목 모의고사",
  CHAPTER_MOCK: "챕터 모의고사",
  CONCEPT_PRACTICE: "개념 문제집",
  DAILY_QUIZ: "오늘의 CS 퀴즈",
} as const;

const questions: ReviewQuestion[] = [
  {
    id: 1,
    question: "배열(Array)에 대한 설명으로 가장 알맞은 것은 무엇인가요?",
    choices: [
      "원소가 메모리에 연속적으로 저장된다.",
      "원소 삽입과 삭제가 항상 O(1)이다.",
      "각 원소는 서로 다른 자료형을 가진다.",
      "인덱스로 원소에 접근할 수 없다.",
    ],
    answerIndex: 0,
    selectedIndex: 1,
    explanation:
      "배열은 같은 자료형의 원소를 연속된 메모리 공간에 저장합니다. 따라서 인덱스를 이용한 접근은 빠르지만, 중간 삽입과 삭제에는 원소 이동이 필요할 수 있습니다.",
  },
  {
    id: 2,
    question: "크기가 n인 배열에서 특정 인덱스의 값을 읽는 시간 복잡도는?",
    choices: ["O(log n)", "O(n)", "O(1)", "O(n log n)"],
    answerIndex: 2,
    selectedIndex: 2,
    explanation:
      "배열은 시작 주소와 인덱스를 이용해 원하는 위치를 바로 계산할 수 있으므로, 특정 원소를 읽는 데 O(1)이 걸립니다.",
  },
  {
    id: 3,
    question: "배열의 중간 위치에 새 원소를 삽입할 때 주로 발생하는 작업은?",
    choices: [
      "앞쪽 원소를 모두 삭제한다.",
      "뒤쪽 원소를 한 칸씩 이동한다.",
      "배열의 인덱스를 정렬한다.",
      "모든 원소를 역순으로 바꾼다.",
    ],
    answerIndex: 1,
    selectedIndex: 3,
    explanation:
      "중간 삽입을 위해서는 삽입 위치 뒤의 원소들을 한 칸씩 뒤로 이동해야 합니다. 이 때문에 최악의 경우 O(n)의 시간이 걸립니다.",
  },
];

export function MistakeExamReview({
  category,
  examType,
  sessionType,
  subject,
  onBack,
}: MistakeExamReviewProps) {
  const insets = useSafeAreaInsets();
  const [currentIndex, setCurrentIndex] = useState(0);
  const currentQuestion = questions[currentIndex];
  const isCorrect = currentQuestion.answerIndex === currentQuestion.selectedIndex;
  const resultText = useMemo(
    () => (isCorrect ? "정답이에요" : "오답이에요"),
    [isCorrect],
  );
  const displayedExamType = sessionType
    ? sessionTypeLabel[sessionType]
    : examType;

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
        bounces
        contentContainerStyle={styles.content}
        showsVerticalScrollIndicator={false}
      >
        <View style={styles.examInfo}>
          <Text style={styles.subjectText}>{subject}</Text>
          <Text style={styles.infoDivider}>·</Text>
          <Text style={styles.categoryText}>{category}</Text>
          <Text style={styles.examTypeText}>{displayedExamType}</Text>
        </View>

        <View style={styles.progressRow}>
          <Text style={styles.progressText}>
            {currentIndex + 1} / {questions.length}
          </Text>
          <View style={styles.progressTrack}>
            <View
              style={[
                styles.progressValue,
                { width: `${((currentIndex + 1) / questions.length) * 100}%` },
              ]}
            />
          </View>
        </View>

        <View style={styles.questionCard}>
          <View style={styles.resultHeader}>
            {isCorrect ? (
              <CircleCheck color="#16a34a" size={22} strokeWidth={2.4} />
            ) : (
              <CircleX color="#ef4444" size={22} strokeWidth={2.4} />
            )}
            <Text style={[styles.resultText, isCorrect ? styles.correct : styles.wrong]}>
              {resultText}
            </Text>
          </View>

          <Text style={styles.question}>{currentQuestion.question}</Text>

          <View style={styles.choiceList}>
            {currentQuestion.choices.map((choice, index) => {
              const isAnswer = index === currentQuestion.answerIndex;
              const isSelected = index === currentQuestion.selectedIndex;

              return (
                <View
                  key={choice}
                  style={[
                    styles.choice,
                    isAnswer && styles.answerChoice,
                    isSelected && !isAnswer && styles.selectedWrongChoice,
                  ]}
                >
                  <Text
                    style={[
                      styles.choiceNumber,
                      isAnswer && styles.answerChoiceText,
                      isSelected && !isAnswer && styles.wrongChoiceText,
                    ]}
                  >
                    {index + 1}
                  </Text>
                  <Text
                    style={[
                      styles.choiceText,
                      isAnswer && styles.answerChoiceText,
                      isSelected && !isAnswer && styles.wrongChoiceText,
                    ]}
                  >
                    {choice}
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

        <View style={styles.navigationRow}>
          <Pressable
            accessibilityLabel="이전 문제"
            disabled={currentIndex === 0}
            style={[
              styles.navigationButton,
              currentIndex === 0 && styles.disabledButton,
            ]}
            onPress={() => setCurrentIndex((index) => index - 1)}
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
            disabled={currentIndex === questions.length - 1}
            style={[
              styles.navigationButton,
              currentIndex === questions.length - 1 && styles.disabledButton,
            ]}
            onPress={() => setCurrentIndex((index) => index + 1)}
          >
            <Text
              style={[
                styles.navigationText,
                currentIndex === questions.length - 1 && styles.disabledNavigationText,
              ]}
            >
              다음
            </Text>
            <ChevronRight
              color={currentIndex === questions.length - 1 ? "#94a3b8" : "#4b5563"}
              size={20}
            />
          </Pressable>
        </View>
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
  choiceNumber: {
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
