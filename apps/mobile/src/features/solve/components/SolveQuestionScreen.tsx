import {
  Bookmark,
  ChevronLeft,
  ChevronRight,
  CircleHelp,
  X,
} from "lucide-react-native";
import { useEffect, useMemo, useState } from "react";
import { Pressable, ScrollView, StyleSheet, Text, View } from "react-native";
import { useSafeAreaInsets } from "react-native-safe-area-context";

import { FONT } from "../../../../constants/fonts";

export type SolveSessionType =
  | "SUBJECT_MOCK"
  | "CHAPTER_MOCK"
  | "CONCEPT_PRACTICE";

type SolveQuestion = {
  choices: string[];
  correctIndex: number;
  hint: string;
  question: string;
};

type SolveQuestionScreenProps = {
  sessionType: SolveSessionType;
  title: string;
  onBack: () => void;
  onComplete: (
    correctCount: number,
    totalCount: number,
    elapsedSeconds?: number,
  ) => void;
};

const sessionTypeLabel: Record<SolveSessionType, string> = {
  SUBJECT_MOCK: "과목 모의고사",
  CHAPTER_MOCK: "챕터 모의고사",
  CONCEPT_PRACTICE: "개념 문제집",
};

const questions: SolveQuestion[] = [
  {
    question: "배열(Array)에 대한 설명으로 가장 알맞은 것은 무엇인가요?",
    choices: [
      "원소가 메모리에 연속적으로 저장된다.",
      "중간 삽입과 삭제가 항상 O(1)이다.",
      "각 원소는 서로 다른 자료형을 가진다.",
      "인덱스로 원소에 접근할 수 없다.",
    ],
    correctIndex: 0,
    hint:
      "배열은 시작 주소와 인덱스를 이용해 원하는 원소의 위치를 계산할 수 있는 자료구조입니다.",
  },
  {
    question: "크기가 n인 배열에서 특정 인덱스의 값을 읽는 시간 복잡도는?",
    choices: ["O(log n)", "O(n)", "O(1)", "O(n log n)"],
    correctIndex: 2,
    hint: "배열은 인덱스를 통해 원하는 위치로 바로 이동할 수 있습니다.",
  },
  {
    question: "배열의 중간 위치에 원소를 삽입할 때 주로 필요한 작업은?",
    choices: [
      "앞쪽 원소를 모두 삭제한다.",
      "뒤쪽 원소를 한 칸씩 이동한다.",
      "배열의 인덱스를 정렬한다.",
      "모든 원소를 역순으로 바꾼다.",
    ],
    correctIndex: 1,
    hint:
      "새 원소를 넣을 자리를 만들기 위해 삽입 위치 뒤의 원소들이 어떻게 되는지 생각해보세요.",
  },
  {
    question: "다음 중 배열의 단점으로 가장 알맞은 것은 무엇인가요?",
    choices: [
      "임의 접근이 불가능하다.",
      "원소의 순서를 유지할 수 없다.",
      "중간 삽입과 삭제 시 이동 비용이 발생할 수 있다.",
      "같은 자료형의 원소를 저장할 수 없다.",
    ],
    correctIndex: 2,
    hint: "중간에 빈 공간을 만들거나 제거한 자리를 메우는 상황을 떠올려보세요.",
  },
  {
    question: "배열의 첫 번째 원소 인덱스로 일반적으로 사용하는 값은?",
    choices: ["-1", "0", "1", "배열 크기"],
    correctIndex: 1,
    hint: "대부분의 프로그래밍 언어에서 배열은 0부터 시작하는 인덱싱을 사용합니다.",
  },
];

export function SolveQuestionScreen({
  sessionType,
  title,
  onBack,
  onComplete,
}: SolveQuestionScreenProps) {
  const insets = useSafeAreaInsets();
  const [answers, setAnswers] = useState<(number | null)[]>(
    questions.map(() => null),
  );
  const [currentIndex, setCurrentIndex] = useState(0);
  const [isBookmarked, setIsBookmarked] = useState(false);
  const [isHintVisible, setIsHintVisible] = useState(false);
  const [elapsedSeconds, setElapsedSeconds] = useState(0);
  const currentQuestion = questions[currentIndex];
  const currentAnswer = answers[currentIndex];
  const progress = ((currentIndex + 1) / questions.length) * 100;
  const isLastQuestion = currentIndex === questions.length - 1;
  const actionLabel = isLastQuestion ? "제출하기" : "다음 문제";
  const sessionLabel = sessionTypeLabel[sessionType];
  const canContinue = currentAnswer !== null;
  const isMockExam = sessionType !== "CONCEPT_PRACTICE";
  const elapsedTimeLabel = `${Math.floor(elapsedSeconds / 60)
    .toString()
    .padStart(2, "0")}:${(elapsedSeconds % 60).toString().padStart(2, "0")}`;
  const answeredCount = useMemo(
    () => answers.filter((answer) => answer !== null).length,
    [answers],
  );

  useEffect(() => {
    if (!isMockExam) {
      return;
    }

    const intervalId = setInterval(() => {
      setElapsedSeconds((seconds) => seconds + 1);
    }, 1000);

    return () => clearInterval(intervalId);
  }, [isMockExam]);

  const selectAnswer = (answerIndex: number) => {
    setAnswers((currentAnswers) => {
      const nextAnswers = [...currentAnswers];
      nextAnswers[currentIndex] = answerIndex;
      return nextAnswers;
    });
  };

  const handleContinue = () => {
    if (!canContinue) {
      return;
    }

    if (!isLastQuestion) {
      setCurrentIndex((index) => index + 1);
      setIsHintVisible(false);
      return;
    }

    const correctCount = answers.reduce<number>(
      (count, answer, index) =>
        answer === questions[index].correctIndex ? count + 1 : count,
      0,
    );
    onComplete(
      correctCount,
      questions.length,
      isMockExam ? elapsedSeconds : undefined,
    );
  };

  return (
    <View style={styles.screen}>
      <View style={[styles.header, { paddingTop: insets.top }]}> 
        <Pressable
          accessibilityLabel="문제 풀이 나가기"
          accessibilityRole="button"
          hitSlop={10}
          style={styles.headerButton}
          onPress={onBack}
        >
          <ChevronLeft color="#ffffff" size={28} strokeWidth={2.2} />
        </Pressable>

        <View style={styles.headerTitleArea}>
          <Text numberOfLines={1} style={styles.headerTitle}>
            {title}
          </Text>
          <Text style={styles.headerSubtitle}>{sessionLabel}</Text>
        </View>

        <Pressable
          accessibilityLabel={isBookmarked ? "즐겨찾기 해제" : "즐겨찾기 등록"}
          accessibilityRole="button"
          style={styles.headerButton}
          onPress={() => setIsBookmarked((currentValue) => !currentValue)}
        >
          <Bookmark
            color="#ffffff"
            fill={isBookmarked ? "#ffffff" : "transparent"}
            size={22}
            strokeWidth={2.1}
          />
        </Pressable>
      </View>

      <ScrollView
        contentContainerStyle={styles.content}
        showsVerticalScrollIndicator={false}
      >
        <View style={styles.progressInfoRow}>
          <Text style={styles.progressText}>
            문제 {currentIndex + 1} / {questions.length}
          </Text>
          <View style={styles.progressMeta}>
            <Text style={styles.answeredText}>답변 {answeredCount}</Text>
            {isMockExam && <Text style={styles.timerText}>{elapsedTimeLabel}</Text>}
          </View>
        </View>
        <View style={styles.progressTrack}>
          <View style={[styles.progressValue, { width: `${progress}%` }]} />
        </View>

        <View style={styles.questionCard}>
          <Text style={styles.question}>{currentQuestion.question}</Text>

          <View style={styles.choiceList}>
            {currentQuestion.choices.map((choice, index) => {
              const isSelected = currentAnswer === index;

              return (
                <Pressable
                  key={choice}
                  style={[styles.choice, isSelected && styles.selectedChoice]}
                  onPress={() => selectAnswer(index)}
                >
                  <View
                    style={[
                      styles.choiceNumber,
                      isSelected && styles.selectedChoiceNumber,
                    ]}
                  >
                    <Text
                      style={[
                        styles.choiceNumberText,
                        isSelected && styles.selectedChoiceNumberText,
                      ]}
                    >
                      {index + 1}
                    </Text>
                  </View>
                  <Text style={[styles.choiceText, isSelected && styles.selectedChoiceText]}>
                    {choice}
                  </Text>
                </Pressable>
              );
            })}
          </View>

          <Pressable
            accessibilityRole="button"
            style={styles.hintButton}
            onPress={() => setIsHintVisible((currentValue) => !currentValue)}
          >
            <CircleHelp color="#6D5DFB" size={19} strokeWidth={2.2} />
            <Text style={styles.hintButtonText}>힌트 보기</Text>
          </Pressable>

          {isHintVisible && (
            <View style={styles.hintPanel}>
              <View style={styles.hintHeader}>
                <Text style={styles.hintTitle}>힌트</Text>
                <Pressable
                  accessibilityLabel="힌트 닫기"
                  accessibilityRole="button"
                  hitSlop={8}
                  onPress={() => setIsHintVisible(false)}
                >
                  <X color="#6D5DFB" size={18} strokeWidth={2.3} />
                </Pressable>
              </View>
              <Text style={styles.hintText}>{currentQuestion.hint}</Text>
            </View>
          )}
        </View>
      </ScrollView>

      <View style={[styles.footer, { paddingBottom: Math.max(insets.bottom, 14) }]}>
        <Pressable
          accessibilityLabel="이전 문제"
          disabled={currentIndex === 0}
          style={[styles.previousButton, currentIndex === 0 && styles.disabledButton]}
          onPress={() => {
            setCurrentIndex((index) => index - 1);
            setIsHintVisible(false);
          }}
        >
          <ChevronLeft color={currentIndex === 0 ? "#aab2bd" : "#475569"} size={20} />
          <Text style={[styles.previousButtonText, currentIndex === 0 && styles.disabledText]}>
            이전
          </Text>
        </Pressable>

        <Pressable
          accessibilityLabel={actionLabel}
          disabled={!canContinue}
          style={[styles.primaryButton, !canContinue && styles.disabledPrimaryButton]}
          onPress={handleContinue}
        >
          <Text style={styles.primaryButtonText}>{actionLabel}</Text>
          {!isLastQuestion && <ChevronRight color="#ffffff" size={20} />}
        </Pressable>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: "#F3F3F3" },
  header: {
    minHeight: 66,
    flexDirection: "row",
    alignItems: "center",
    borderBottomWidth: StyleSheet.hairlineWidth,
    borderBottomColor: "rgba(255, 255, 255, 0.18)",
    backgroundColor: "#6D5DFB",
    paddingHorizontal: 14,
  },
  headerButton: {
    alignItems: "center",
    justifyContent: "center",
    width: 40,
    height: 40,
  },
  headerTitleArea: { flex: 1, alignItems: "center", paddingHorizontal: 8 },
  headerTitle: {
    color: "#ffffff",
    fontFamily: FONT.pretendard,
    fontSize: 16,
    fontWeight: "700",
  },
  headerSubtitle: {
    marginTop: 2,
    color: "#E2DFFF",
    fontFamily: FONT.pretendard,
    fontSize: 11,
    fontWeight: "600",
  },
  content: { flexGrow: 1, paddingHorizontal: 20, paddingTop: 24, paddingBottom: 30 },
  progressInfoRow: { flexDirection: "row", justifyContent: "space-between" },
  progressText: {
    color: "#334155",
    fontFamily: FONT.pretendard,
    fontSize: 14,
    fontWeight: "700",
  },
  answeredText: {
    color: "#64748b",
    fontFamily: FONT.pretendard,
    fontSize: 13,
    fontWeight: "500",
  },
  progressMeta: { flexDirection: "row", alignItems: "center", gap: 10 },
  timerText: {
    color: "#5144cf",
    fontFamily: FONT.kufam,
    fontSize: 13,
    fontWeight: "700",
  },
  progressTrack: {
    height: 7,
    overflow: "hidden",
    borderRadius: 999,
    backgroundColor: "#dce1e8",
    marginTop: 10,
  },
  progressValue: { height: "100%", borderRadius: 999, backgroundColor: "#6D5DFB" },
  questionCard: { borderRadius: 8, backgroundColor: "#ffffff", marginTop: 22, padding: 20 },
  question: {
    color: "#1f2937",
    fontFamily: FONT.pretendard,
    fontSize: 19,
    fontWeight: "700",
    lineHeight: 29,
  },
  choiceList: { gap: 11, marginTop: 28 },
  choice: {
    minHeight: 58,
    flexDirection: "row",
    alignItems: "center",
    borderWidth: 1,
    borderColor: "#e2e8f0",
    borderRadius: 8,
    backgroundColor: "#ffffff",
    paddingHorizontal: 14,
  },
  selectedChoice: { borderColor: "#6D5DFB", backgroundColor: "#F1EFFF" },
  choiceNumber: {
    alignItems: "center",
    justifyContent: "center",
    width: 26,
    height: 26,
    borderRadius: 999,
    backgroundColor: "#eef1f5",
    marginRight: 12,
  },
  selectedChoiceNumber: { backgroundColor: "#6D5DFB" },
  choiceNumberText: { color: "#64748b", fontFamily: FONT.kufam, fontSize: 12, fontWeight: "700" },
  selectedChoiceNumberText: { color: "#ffffff" },
  choiceText: { flex: 1, color: "#334155", fontFamily: FONT.pretendard, fontSize: 15, fontWeight: "500", lineHeight: 21 },
  selectedChoiceText: { color: "#5144cf", fontWeight: "700" },
  hintButton: { alignSelf: "flex-start", flexDirection: "row", alignItems: "center", gap: 6, marginTop: 24 },
  hintButtonText: { color: "#6D5DFB", fontFamily: FONT.pretendard, fontSize: 13, fontWeight: "700" },
  hintPanel: { borderRadius: 8, backgroundColor: "#F1EFFF", marginTop: 14, padding: 15 },
  hintHeader: { flexDirection: "row", alignItems: "center", justifyContent: "space-between" },
  hintTitle: { color: "#5144cf", fontFamily: FONT.pretendard, fontSize: 14, fontWeight: "700" },
  hintText: { color: "#494365", fontFamily: FONT.pretendard, fontSize: 14, fontWeight: "500", lineHeight: 22, marginTop: 8 },
  footer: { flexDirection: "row", gap: 10, borderTopWidth: StyleSheet.hairlineWidth, borderTopColor: "#e5e7eb", backgroundColor: "#ffffff", paddingHorizontal: 20, paddingTop: 12 },
  previousButton: { minHeight: 48, flexDirection: "row", alignItems: "center", justifyContent: "center", gap: 1, borderRadius: 8, backgroundColor: "#eef1f5", paddingHorizontal: 18 },
  previousButtonText: { color: "#475569", fontFamily: FONT.pretendard, fontSize: 14, fontWeight: "700" },
  primaryButton: { minHeight: 48, flex: 1, flexDirection: "row", alignItems: "center", justifyContent: "center", gap: 4, borderRadius: 8, backgroundColor: "#6D5DFB" },
  primaryButtonText: { color: "#ffffff", fontFamily: FONT.pretendard, fontSize: 15, fontWeight: "700" },
  disabledButton: { backgroundColor: "#f1f3f5" },
  disabledPrimaryButton: { backgroundColor: "#b6b0f7" },
  disabledText: { color: "#aab2bd" },
});
