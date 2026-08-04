import { ChevronLeft, ChevronRight, CircleHelp, X } from "lucide-react-native";
import { useEffect, useMemo, useRef, useState } from "react";
import {
  ActivityIndicator,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  View,
} from "react-native";
import { useSafeAreaInsets } from "react-native-safe-area-context";

import { FONT } from "../../../../constants/fonts";
import { HttpError } from "../../../shared/api/httpClient";
import { getApiErrorMessage } from "../../../shared/utils/apiError";
import { solveApi } from "../../solve/solveApi";
import { dailyQuizApi } from "../dailyQuizApi";
import type {
  DailyQuizAnswerInput,
  DailyQuizQuestion,
  DailyQuizSubmitResult,
} from "../types";

const BRAND_PURPLE = "#6D5DFB";

type Session =
  | { status: "loading" }
  | { status: "error"; message: string }
  | { status: "alreadySolved" }
  | { status: "ready"; dailyQuizSetId: number; questions: DailyQuizQuestion[] };

type HintState =
  | { status: "idle" }
  | { status: "loading" }
  | { status: "ready"; text: string }
  | { status: "unavailable" }
  | { status: "error"; message: string };

type DailyQuizAnswerState = Omit<DailyQuizAnswerInput, "questionId">;

type DailyQuizScreenProps = {
  accessToken: string;
  onBack: () => void;
  onComplete: (result: DailyQuizSubmitResult) => void;
  onAlreadySolved: () => void;
};

function createEmptyAnswer(): DailyQuizAnswerState {
  return {
    selectedAnswer: null,
    timeSpentSec: 0,
    hintUsed: false,
    hintCount: 0,
    optionChangeCount: 0,
    isAnswerChanged: false,
  };
}

export function DailyQuizScreen({
  accessToken,
  onBack,
  onComplete,
  onAlreadySolved,
}: DailyQuizScreenProps) {
  const insets = useSafeAreaInsets();
  const [session, setSession] = useState<Session>({ status: "loading" });
  const [currentIndex, setCurrentIndex] = useState(0);
  const [answers, setAnswers] = useState<DailyQuizAnswerState[]>([]);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isHintVisible, setIsHintVisible] = useState(false);
  const [hintState, setHintState] = useState<HintState>({ status: "idle" });
  const questionStartedAtRef = useRef(Date.now());
  const optionChangeCountRef = useRef(0);
  const hintCacheRef = useRef<Map<number, string | null>>(new Map());

  useEffect(() => {
    let cancelled = false;

    (async () => {
      try {
        const quizSet = await dailyQuizApi.getToday(accessToken);
        if (cancelled) {
          return;
        }
        if (quizSet.solvedToday) {
          setSession({ status: "alreadySolved" });
          return;
        }
        setAnswers(quizSet.questions.map(createEmptyAnswer));
        setSession({
          status: "ready",
          dailyQuizSetId: quizSet.dailyQuizSetId,
          questions: quizSet.questions,
        });
      } catch (error) {
        if (cancelled) {
          return;
        }
        console.error("[dailyQuiz] 오늘의 퀴즈 조회 실패", error);
        setSession({
          status: "error",
          message: getApiErrorMessage(error, "오늘의 퀴즈를 불러오지 못했습니다."),
        });
      }
    })();

    return () => {
      cancelled = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    questionStartedAtRef.current = Date.now();
    optionChangeCountRef.current = 0;
    setIsHintVisible(false);
    setHintState({ status: "idle" });
  }, [currentIndex]);

  const answeredCount = useMemo(
    () => answers.filter((answer) => answer.selectedAnswer !== null).length,
    [answers],
  );

  if (session.status === "loading") {
    return (
      <View style={styles.stateScreen}>
        <ActivityIndicator color={BRAND_PURPLE} size="large" />
      </View>
    );
  }

  if (session.status === "error") {
    return (
      <View style={styles.stateScreen}>
        <Text style={styles.stateText}>{session.message}</Text>
        <Pressable style={styles.stateBackButton} onPress={onBack}>
          <Text style={styles.stateBackButtonText}>돌아가기</Text>
        </Pressable>
      </View>
    );
  }

  if (session.status === "alreadySolved") {
    return (
      <View style={styles.stateScreen}>
        <Text style={styles.stateText}>오늘의 CS 퀴즈를 이미 완료했어요.{"\n"}내일 다시 도전해주세요.</Text>
        <Pressable style={styles.stateBackButton} onPress={onAlreadySolved}>
          <Text style={styles.stateBackButtonText}>해설 보러가기</Text>
        </Pressable>
      </View>
    );
  }

  const { dailyQuizSetId, questions } = session;
  const currentQuestion = questions[currentIndex];
  const currentAnswer = answers[currentIndex];
  const progress = ((currentIndex + 1) / questions.length) * 100;
  const isLastQuestion = currentIndex === questions.length - 1;
  const actionLabel = isLastQuestion ? "제출하기" : "다음 문제";
  const canContinue = currentAnswer.selectedAnswer !== null;

  const selectAnswer = (choiceKey: string) => {
    setAnswers((currentAnswers) => {
      const nextAnswers = [...currentAnswers];
      const previous = nextAnswers[currentIndex];
      if (previous.selectedAnswer !== null && previous.selectedAnswer !== choiceKey) {
        optionChangeCountRef.current += 1;
      }
      nextAnswers[currentIndex] = {
        ...previous,
        selectedAnswer: choiceKey,
        optionChangeCount: optionChangeCountRef.current,
        isAnswerChanged: optionChangeCountRef.current > 0,
      };
      return nextAnswers;
    });
  };

  const handleToggleHint = async () => {
    if (isHintVisible) {
      setIsHintVisible(false);
      return;
    }
    setIsHintVisible(true);

    const questionId = currentQuestion.questionId;
    setAnswers((currentAnswers) => {
      const nextAnswers = [...currentAnswers];
      const previous = nextAnswers[currentIndex];
      nextAnswers[currentIndex] = {
        ...previous,
        hintUsed: true,
        hintCount: previous.hintCount + 1,
      };
      return nextAnswers;
    });

    const cached = hintCacheRef.current.get(questionId);
    if (cached !== undefined) {
      setHintState(
        cached === null ? { status: "unavailable" } : { status: "ready", text: cached },
      );
      return;
    }

    setHintState({ status: "loading" });
    try {
      const hint = await solveApi.getHint(questionId);
      hintCacheRef.current.set(questionId, hint.hintText);
      setHintState({ status: "ready", text: hint.hintText });
    } catch (error) {
      if (error instanceof HttpError && error.status === 404) {
        hintCacheRef.current.set(questionId, null);
        setHintState({ status: "unavailable" });
        return;
      }
      console.error("[dailyQuiz] 힌트 조회 실패", error);
      setHintState({
        status: "error",
        message: getApiErrorMessage(error, "힌트를 불러오지 못했습니다."),
      });
    }
  };

  const recordTimeSpent = () => {
    const timeSpentSec = Math.round(
      (Date.now() - questionStartedAtRef.current) / 1000,
    );
    setAnswers((currentAnswers) => {
      const nextAnswers = [...currentAnswers];
      nextAnswers[currentIndex] = {
        ...nextAnswers[currentIndex],
        timeSpentSec: nextAnswers[currentIndex].timeSpentSec + timeSpentSec,
      };
      return nextAnswers;
    });
    return timeSpentSec;
  };

  const handleContinue = async () => {
    if (!canContinue || isSubmitting) {
      return;
    }

    recordTimeSpent();

    if (!isLastQuestion) {
      setCurrentIndex((index) => index + 1);
      return;
    }

    setIsSubmitting(true);
    try {
      const finalAnswers: DailyQuizAnswerInput[] = questions.map((question, index) => {
        const answer = answers[index];
        const extraTime =
          index === currentIndex
            ? Math.round((Date.now() - questionStartedAtRef.current) / 1000)
            : 0;
        return {
          questionId: question.questionId,
          selectedAnswer: answer.selectedAnswer,
          timeSpentSec: answer.timeSpentSec + extraTime,
          hintUsed: answer.hintUsed,
          hintCount: answer.hintCount,
          optionChangeCount: answer.optionChangeCount,
          isAnswerChanged: answer.isAnswerChanged,
        };
      });
      const totalTimeSpentSec = finalAnswers.reduce(
        (sum, answer) => sum + answer.timeSpentSec,
        0,
      );
      const result = await dailyQuizApi.submit(
        dailyQuizSetId,
        finalAnswers,
        totalTimeSpentSec,
        accessToken,
      );
      onComplete(result);
    } catch (error) {
      console.error("[dailyQuiz] 오늘의 퀴즈 제출 실패", error);
      setSession({
        status: "error",
        message: getApiErrorMessage(error, "퀴즈 제출에 실패했습니다."),
      });
    } finally {
      setIsSubmitting(false);
    }
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
            오늘의 CS 퀴즈
          </Text>
        </View>

        <View style={styles.headerButton} />
      </View>

      <ScrollView
        contentContainerStyle={styles.content}
        showsVerticalScrollIndicator={false}
      >
        <View style={styles.progressInfoRow}>
          <Text style={styles.progressText}>
            문제 {currentIndex + 1} / {questions.length}
          </Text>
          <Text style={styles.answeredText}>답변 {answeredCount}</Text>
        </View>
        <View style={styles.progressTrack}>
          <View style={[styles.progressValue, { width: `${progress}%` }]} />
        </View>

        <View style={styles.questionCard}>
          <Text style={styles.question}>{currentQuestion.questionText}</Text>

          <View style={styles.choiceList}>
            {currentQuestion.choices.map((choice) => {
              const isSelected = currentAnswer.selectedAnswer === choice.key;

              return (
                <Pressable
                  key={choice.key}
                  style={[styles.choice, isSelected && styles.selectedChoice]}
                  onPress={() => selectAnswer(choice.key)}
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
                      {choice.key}
                    </Text>
                  </View>
                  <Text style={[styles.choiceText, isSelected && styles.selectedChoiceText]}>
                    {choice.text}
                  </Text>
                </Pressable>
              );
            })}
          </View>

          {currentQuestion.hasHint ? (
            <>
              <Pressable
                accessibilityRole="button"
                style={styles.hintButton}
                onPress={handleToggleHint}
              >
                <CircleHelp color={BRAND_PURPLE} size={19} strokeWidth={2.2} />
                <Text style={styles.hintButtonText}>힌트 보기</Text>
              </Pressable>

              {isHintVisible ? (
                <View style={styles.hintPanel}>
                  <View style={styles.hintHeader}>
                    <Text style={styles.hintTitle}>힌트</Text>
                    <Pressable
                      accessibilityLabel="힌트 닫기"
                      accessibilityRole="button"
                      hitSlop={8}
                      onPress={() => setIsHintVisible(false)}
                    >
                      <X color={BRAND_PURPLE} size={18} strokeWidth={2.3} />
                    </Pressable>
                  </View>
                  {hintState.status === "loading" ? (
                    <ActivityIndicator
                      color={BRAND_PURPLE}
                      size="small"
                      style={styles.hintLoading}
                    />
                  ) : hintState.status === "ready" ? (
                    <Text style={styles.hintText}>{hintState.text}</Text>
                  ) : hintState.status === "unavailable" ? (
                    <Text style={styles.hintText}>이 문제에는 힌트가 없어요.</Text>
                  ) : hintState.status === "error" ? (
                    <Text style={styles.hintText}>{hintState.message}</Text>
                  ) : null}
                </View>
              ) : null}
            </>
          ) : null}
        </View>
      </ScrollView>

      <View style={[styles.footer, { paddingBottom: Math.max(insets.bottom, 14) }]}>
        <Pressable
          accessibilityLabel="이전 문제"
          disabled={currentIndex === 0}
          style={[styles.previousButton, currentIndex === 0 && styles.disabledButton]}
          onPress={() => setCurrentIndex((index) => index - 1)}
        >
          <ChevronLeft color={currentIndex === 0 ? "#aab2bd" : "#475569"} size={20} />
          <Text style={[styles.previousButtonText, currentIndex === 0 && styles.disabledText]}>
            이전
          </Text>
        </Pressable>

        <Pressable
          accessibilityLabel={actionLabel}
          disabled={!canContinue || isSubmitting}
          style={[
            styles.primaryButton,
            (!canContinue || isSubmitting) && styles.disabledPrimaryButton,
          ]}
          onPress={handleContinue}
        >
          <Text style={styles.primaryButtonText}>
            {isSubmitting ? "제출 중…" : actionLabel}
          </Text>
          {!isLastQuestion && !isSubmitting && <ChevronRight color="#ffffff" size={20} />}
        </Pressable>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: "#F3F3F3" },
  stateScreen: {
    flex: 1,
    alignItems: "center",
    justifyContent: "center",
    backgroundColor: "#F3F3F3",
    paddingHorizontal: 32,
  },
  stateText: {
    color: "#334155",
    fontFamily: FONT.pretendard,
    fontSize: 15,
    fontWeight: "600",
    lineHeight: 22,
    textAlign: "center",
  },
  stateBackButton: {
    marginTop: 20,
    borderRadius: 8,
    backgroundColor: BRAND_PURPLE,
    paddingHorizontal: 24,
    paddingVertical: 12,
  },
  stateBackButtonText: {
    color: "#ffffff",
    fontFamily: FONT.pretendard,
    fontSize: 14,
    fontWeight: "700",
  },
  header: {
    minHeight: 66,
    flexDirection: "row",
    alignItems: "center",
    borderBottomWidth: StyleSheet.hairlineWidth,
    borderBottomColor: "rgba(255, 255, 255, 0.18)",
    backgroundColor: BRAND_PURPLE,
    paddingHorizontal: 14,
    paddingBottom: 7,
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
  progressTrack: {
    height: 7,
    overflow: "hidden",
    borderRadius: 999,
    backgroundColor: "#dce1e8",
    marginTop: 10,
  },
  progressValue: { height: "100%", borderRadius: 999, backgroundColor: BRAND_PURPLE },
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
  selectedChoice: { borderColor: BRAND_PURPLE, backgroundColor: "#F1EFFF" },
  choiceNumber: {
    alignItems: "center",
    justifyContent: "center",
    width: 26,
    height: 26,
    borderRadius: 999,
    backgroundColor: "#eef1f5",
    marginRight: 12,
  },
  selectedChoiceNumber: { backgroundColor: BRAND_PURPLE },
  choiceNumberText: { color: "#64748b", fontFamily: FONT.kufam, fontSize: 12, fontWeight: "700" },
  selectedChoiceNumberText: { color: "#ffffff" },
  choiceText: { flex: 1, color: "#334155", fontFamily: FONT.pretendard, fontSize: 15, fontWeight: "500", lineHeight: 21 },
  selectedChoiceText: { color: "#5144cf", fontWeight: "700" },
  hintButton: { alignSelf: "flex-start", flexDirection: "row", alignItems: "center", gap: 6, marginTop: 24 },
  hintButtonText: { color: BRAND_PURPLE, fontFamily: FONT.pretendard, fontSize: 13, fontWeight: "700" },
  hintPanel: { borderRadius: 8, backgroundColor: "#F1EFFF", marginTop: 14, padding: 15 },
  hintHeader: { flexDirection: "row", alignItems: "center", justifyContent: "space-between" },
  hintTitle: { color: "#5144cf", fontFamily: FONT.pretendard, fontSize: 14, fontWeight: "700" },
  hintLoading: { marginTop: 8, alignSelf: "flex-start" },
  hintText: { color: "#494365", fontFamily: FONT.pretendard, fontSize: 14, fontWeight: "500", lineHeight: 22, marginTop: 8 },
  footer: { flexDirection: "row", gap: 10, borderTopWidth: StyleSheet.hairlineWidth, borderTopColor: "#e5e7eb", backgroundColor: "#ffffff", paddingHorizontal: 20, paddingTop: 12 },
  previousButton: { minHeight: 48, flexDirection: "row", alignItems: "center", justifyContent: "center", gap: 1, borderRadius: 8, backgroundColor: "#eef1f5", paddingHorizontal: 18 },
  previousButtonText: { color: "#475569", fontFamily: FONT.pretendard, fontSize: 14, fontWeight: "700" },
  primaryButton: { minHeight: 48, flex: 1, flexDirection: "row", alignItems: "center", justifyContent: "center", gap: 4, borderRadius: 8, backgroundColor: BRAND_PURPLE },
  primaryButtonText: { color: "#ffffff", fontFamily: FONT.pretendard, fontSize: 15, fontWeight: "700" },
  disabledButton: { backgroundColor: "#f1f3f5" },
  disabledPrimaryButton: { backgroundColor: "#b6b0f7" },
  disabledText: { color: "#aab2bd" },
});
