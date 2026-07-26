import {
  Bookmark as BookmarkIcon,
  ChevronLeft,
  ChevronRight,
  CircleHelp,
  X,
} from "lucide-react-native";
import { useEffect, useRef, useState } from "react";
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
import { bookmarkApi } from "../../bookmark/bookmarkApi";
import { Toast } from "../../../shared/components/Toast";
import { useToast } from "../../../shared/hooks/useToast";
import { getApiErrorMessage } from "../../../shared/utils/apiError";
import { solveApi } from "../solveApi";
import type {
  SolveQuestion,
  SolveReviewItem,
  StartSolveSessionResponse,
} from "../types";

const BRAND_PURPLE = "#6D5DFB";

// TODO: 백엔드에 실제 힌트 조회 API(CommonMistake 기반)가 생기면 문제별 실제
// 힌트로 교체할 것. 지금은 더미 문구를 고정으로 보여준다.
const DUMMY_HINT_TEXT =
  "정답을 직접 알려주지는 않지만, 문제에서 다루는 핵심 개념을 다시 짚어보면 실마리를 찾을 수 있어요.";

/** 개념 문제집은 게스트도 가능하고, 모의고사 3종은 전부 회원 전용이다. */
export type SolveTarget =
  | { type: "concept"; conceptId: number }
  | { type: "subjectMock"; subjectId: number }
  | { type: "categoryLv1Mock"; categoryLv1Id: number }
  | { type: "categoryLv2Mock"; categoryLv2Id: number };

const TARGET_SUBTITLE: Record<SolveTarget["type"], string> = {
  concept: "개념 문제집",
  subjectMock: "과목 모의고사",
  categoryLv1Mock: "대분류 모의고사",
  categoryLv2Mock: "중분류 모의고사",
};

function targetKey(target: SolveTarget): string {
  switch (target.type) {
    case "concept":
      return `concept:${target.conceptId}`;
    case "subjectMock":
      return `subjectMock:${target.subjectId}`;
    case "categoryLv1Mock":
      return `categoryLv1Mock:${target.categoryLv1Id}`;
    case "categoryLv2Mock":
      return `categoryLv2Mock:${target.categoryLv2Id}`;
  }
}

function startSession(
  target: SolveTarget,
  accessToken: string | null,
): Promise<StartSolveSessionResponse> {
  switch (target.type) {
    case "concept":
      return solveApi.startConceptSession(target.conceptId, accessToken ?? undefined);
    case "subjectMock":
      return solveApi.startSubjectMockSession(target.subjectId, accessToken as string);
    case "categoryLv1Mock":
      return solveApi.startCategoryLv1MockSession(
        target.categoryLv1Id,
        accessToken as string,
      );
    case "categoryLv2Mock":
      return solveApi.startCategoryLv2MockSession(
        target.categoryLv2Id,
        accessToken as string,
      );
  }
}

type ConceptSolveScreenProps = {
  target: SolveTarget;
  title: string;
  isGuest: boolean;
  accessToken: string | null;
  onBack: () => void;
  onComplete: (result: {
    correctCount: number;
    totalCount: number;
    reviewItems: SolveReviewItem[];
  }) => void;
};

type SessionState =
  | { status: "loading" }
  | { status: "error"; message: string }
  | { status: "ready"; sessionId: number | null; questions: SolveQuestion[] };

export function ConceptSolveScreen({
  target,
  title,
  isGuest,
  accessToken,
  onBack,
  onComplete,
}: ConceptSolveScreenProps) {
  const insets = useSafeAreaInsets();
  const [session, setSession] = useState<SessionState>({ status: "loading" });
  const [currentIndex, setCurrentIndex] = useState(0);
  const [selectedAnswer, setSelectedAnswer] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isHintVisible, setIsHintVisible] = useState(false);
  const [bookmarkMap, setBookmarkMap] = useState<Map<number, number>>(new Map());
  const [isBookmarkPending, setIsBookmarkPending] = useState(false);
  const { message: toastMessage, opacity: toastOpacity, showToast } = useToast();
  const reviewItemsRef = useRef<SolveReviewItem[]>([]);
  const answersRef = useRef<{ questionId: number; selectedAnswer: string }[]>([]);
  const startedAtRef = useRef<number>(0);
  // 문제별 풀이 시간 계산용. "이전"으로 갔다가 같은 문제로 다시 돌아와도
  // 각 방문 시간이 모두 합산되도록, 문제 인덱스별 누적 시간(과거 방문분)과
  // 지금 보고 있는 방문이 시작된 시각을 따로 추적한다.
  const questionTimeAccumulatorRef = useRef<Map<number, number>>(new Map());
  const questionVisitStartedAtRef = useRef<number>(0);
  const previousIndexRef = useRef(currentIndex);
  // 문제당 답을 바꾼 횟수. 다음 문제로 넘어가면 리셋된다("이전"으로 돌아간
  // 문제는 이미 제출됐으므로 다시 세지 않는다).
  const optionChangeCountRef = useRef(0);

  useEffect(() => {
    if (session.status !== "ready") {
      return;
    }
    const now = Date.now();
    if (questionVisitStartedAtRef.current !== 0) {
      const prevIndex = previousIndexRef.current;
      const elapsed = now - questionVisitStartedAtRef.current;
      questionTimeAccumulatorRef.current.set(
        prevIndex,
        (questionTimeAccumulatorRef.current.get(prevIndex) ?? 0) + elapsed,
      );
    }
    questionVisitStartedAtRef.current = now;
    previousIndexRef.current = currentIndex;
    optionChangeCountRef.current = 0;
  }, [currentIndex, session.status]);

  useEffect(() => {
    let cancelled = false;

    // "다시 풀기"처럼 같은 화면이 재사용되며 target만 바뀌는 경우, 이전
    // 세션에서 남은 진행 상태/타이머가 새 세션에 섞이지 않도록 전부
    // 초기화한다.
    setSession({ status: "loading" });
    setCurrentIndex(0);
    setSelectedAnswer(null);
    setIsHintVisible(false);
    reviewItemsRef.current = [];
    answersRef.current = [];
    questionTimeAccumulatorRef.current = new Map();
    questionVisitStartedAtRef.current = 0;
    previousIndexRef.current = 0;
    optionChangeCountRef.current = 0;

    (async () => {
      try {
        const response = await startSession(target, accessToken);
        if (cancelled) {
          return;
        }
        startedAtRef.current = Date.now();
        setSession({
          status: "ready",
          sessionId: response.sessionId,
          questions: response.questions,
        });
      } catch (error) {
        if (cancelled) {
          return;
        }
        console.error("[solve] 세션 시작 실패", error);
        setSession({
          status: "error",
          message: getApiErrorMessage(error, "문제를 불러오지 못했습니다."),
        });
      }
    })();

    return () => {
      cancelled = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [targetKey(target)]);

  // 게스트는 북마크 기능이 없다. 회원이면 이 세션의 문제들 중 이미
  // 북마크된 것이 있는지 한 번에 조회해둔다(문제별 개별 조회 API가 없음).
  useEffect(() => {
    if (isGuest || !accessToken || session.status !== "ready") {
      return;
    }

    let cancelled = false;

    bookmarkApi.getBookmarkedQuestions(accessToken).then(
      (bookmarkedQuestions) => {
        if (cancelled) {
          return;
        }
        const nextMap = new Map<number, number>();
        for (const item of bookmarkedQuestions) {
          nextMap.set(item.questionId, item.bookmarkId);
        }
        setBookmarkMap(nextMap);
      },
      (error) => {
        // 북마크 조회 실패는 풀이 자체를 막을 이유가 없으니 화면은 그대로 두되,
        // 원인 파악을 위해 로그는 남긴다.
        console.error("[bookmark] 북마크 목록 조회 실패", error);
      },
    );

    return () => {
      cancelled = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isGuest, accessToken, session.status]);

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

  const { sessionId, questions } = session;
  const currentQuestion = questions[currentIndex];
  const isLastQuestion = currentIndex === questions.length - 1;
  const progress = ((currentIndex + 1) / questions.length) * 100;

  const handleContinue = async () => {
    if (!selectedAnswer || isSubmitting) {
      return;
    }

    if (sessionId !== null && !accessToken) {
      setSession({
        status: "error",
        message: "로그인이 만료되어 이어서 풀이할 수 없어요. 다시 로그인한 뒤 처음부터 풀어주세요.",
      });
      return;
    }

    // "이전"으로 돌아갔다가 다시 고른 경우, 이미 제출된 문제라면 재제출하지 않고
    // 다음으로만 이동한다(이미 서버/누적 목록에 답안이 반영돼 있음).
    const answeredCount =
      sessionId !== null && accessToken
        ? reviewItemsRef.current.length
        : answersRef.current.length;
    if (currentIndex < answeredCount) {
      setCurrentIndex((index) => index + 1);
      setSelectedAnswer(null);
      setIsHintVisible(false);
      return;
    }

    setIsSubmitting(true);
    try {
      const currentVisitElapsedMs = Date.now() - questionVisitStartedAtRef.current;
      const accumulatedMs =
        (questionTimeAccumulatorRef.current.get(currentIndex) ?? 0) +
        currentVisitElapsedMs;
      const timeSpentSec = Math.round(accumulatedMs / 1000);
      const optionChangeCount = optionChangeCountRef.current;
      const isAnswerChanged = optionChangeCount > 0;

      if (sessionId !== null && accessToken) {
        const result = await solveApi.submitAnswer(
          sessionId,
          {
            questionId: currentQuestion.questionId,
            selectedAnswer,
            timeSpentSec,
            optionChangeCount,
            isAnswerChanged,
          },
          accessToken,
        );
        reviewItemsRef.current.push({
          questionId: currentQuestion.questionId,
          questionText: currentQuestion.questionText,
          choices: currentQuestion.choices,
          selectedAnswer: result.selectedAnswer,
          correctAnswer: result.correctAnswer,
          isCorrect: result.isCorrect,
          explanationText: result.explanationText,
        });
      } else {
        answersRef.current.push({
          questionId: currentQuestion.questionId,
          selectedAnswer,
        });
      }

      if (!isLastQuestion) {
        setCurrentIndex((index) => index + 1);
        setSelectedAnswer(null);
        setIsHintVisible(false);
        return;
      }

      if (sessionId !== null && accessToken) {
        const totalTimeSpentSec = Math.round(
          (Date.now() - startedAtRef.current) / 1000,
        );
        const completeResult = await solveApi.completeSession(
          sessionId,
          totalTimeSpentSec,
          accessToken,
        );
        onComplete({
          correctCount: completeResult.correctCount,
          totalCount: completeResult.totalQuestionCount,
          reviewItems: reviewItemsRef.current,
        });
      } else {
        // 모의고사 3종은 전부 회원 전용이라 이 분기(게스트 완료)는 개념
        // 문제집에서만 실제로 도달한다.
        if (target.type !== "concept") {
          throw new Error("게스트 풀이는 개념 문제집에서만 가능합니다.");
        }
        const guestResults = await solveApi.completeGuestSession(
          target.conceptId,
          answersRef.current,
        );
        const reviewItems: SolveReviewItem[] = guestResults.map((result) => {
          const question = questions.find(
            (item) => item.questionId === result.questionId,
          );
          return {
            questionId: result.questionId,
            questionText: question?.questionText ?? "",
            choices: question?.choices ?? [],
            selectedAnswer: result.selectedAnswer,
            correctAnswer: result.correctAnswer,
            isCorrect: result.isCorrect,
            explanationText: result.explanationText,
          };
        });
        const correctCount = guestResults.filter((item) => item.isCorrect).length;
        onComplete({
          correctCount,
          totalCount: guestResults.length,
          reviewItems,
        });
      }
    } catch (error) {
      console.error("[solve] 답안 제출/완료 실패", error);
      setSession({
        status: "error",
        message: getApiErrorMessage(error, "제출에 실패했습니다. 다시 시도해주세요."),
      });
    } finally {
      setIsSubmitting(false);
    }
  };

  const toggleBookmark = async () => {
    if (!accessToken || isBookmarkPending) {
      return;
    }

    const existingBookmarkId = bookmarkMap.get(currentQuestion.questionId);
    setIsBookmarkPending(true);
    try {
      if (existingBookmarkId !== undefined) {
        await bookmarkApi.remove(existingBookmarkId, accessToken);
        setBookmarkMap((current) => {
          const next = new Map(current);
          next.delete(currentQuestion.questionId);
          return next;
        });
        showToast("즐겨찾기에서 제거했어요");
      } else {
        const created = await bookmarkApi.create(
          currentQuestion.questionId,
          accessToken,
        );
        setBookmarkMap((current) => {
          const next = new Map(current);
          next.set(currentQuestion.questionId, created.bookmarkId);
          return next;
        });
        showToast("즐겨찾기에 추가했어요");
      }
    } catch (error) {
      // 북마크 토글 실패는 풀이 흐름을 막지 않지만, 원인 파악을 위해 로그는 남긴다.
      console.error("[bookmark] 북마크 토글 실패", error);
    } finally {
      setIsBookmarkPending(false);
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
            {title}
          </Text>
          <Text style={styles.headerSubtitle}>{TARGET_SUBTITLE[target.type]}</Text>
        </View>

        {!accessToken ? (
          <View style={styles.headerButton} />
        ) : (
          <Pressable
            accessibilityLabel={
              bookmarkMap.has(currentQuestion.questionId)
                ? "즐겨찾기 해제"
                : "즐겨찾기 등록"
            }
            accessibilityRole="button"
            disabled={isBookmarkPending}
            style={styles.headerButton}
            onPress={toggleBookmark}
          >
            <BookmarkIcon
              color="#ffffff"
              fill={bookmarkMap.has(currentQuestion.questionId) ? "#ffffff" : "transparent"}
              size={22}
              strokeWidth={2.1}
            />
          </Pressable>
        )}
      </View>

      <ScrollView
        contentContainerStyle={styles.content}
        showsVerticalScrollIndicator={false}
      >
        <View style={styles.progressInfoRow}>
          <Text style={styles.progressText}>
            문제 {currentIndex + 1} / {questions.length}
          </Text>
          {isGuest && <Text style={styles.guestBadge}>게스트 체험</Text>}
        </View>
        <View style={styles.progressTrack}>
          <View style={[styles.progressValue, { width: `${progress}%` }]} />
        </View>

        <View style={styles.questionCard}>
          <Text style={styles.question}>{currentQuestion.questionText}</Text>

          <View style={styles.choiceList}>
            {currentQuestion.choices.map((choice) => {
              const isSelected = selectedAnswer === choice.key;

              return (
                <Pressable
                  key={choice.key}
                  style={[styles.choice, isSelected && styles.selectedChoice]}
                  onPress={() => {
                    if (selectedAnswer !== null && selectedAnswer !== choice.key) {
                      optionChangeCountRef.current += 1;
                    }
                    setSelectedAnswer(choice.key);
                  }}
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

          <Pressable
            accessibilityRole="button"
            style={styles.hintButton}
            onPress={() => setIsHintVisible((currentValue) => !currentValue)}
          >
            <CircleHelp color={BRAND_PURPLE} size={19} strokeWidth={2.2} />
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
                  <X color={BRAND_PURPLE} size={18} strokeWidth={2.3} />
                </Pressable>
              </View>
              <Text style={styles.hintText}>{DUMMY_HINT_TEXT}</Text>
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
            setSelectedAnswer(null);
            setIsHintVisible(false);
          }}
        >
          <ChevronLeft color={currentIndex === 0 ? "#aab2bd" : "#475569"} size={20} />
          <Text style={[styles.previousButtonText, currentIndex === 0 && styles.disabledText]}>
            이전
          </Text>
        </Pressable>

        <Pressable
          accessibilityLabel={isLastQuestion ? "제출하기" : "다음 문제"}
          disabled={!selectedAnswer || isSubmitting}
          style={[
            styles.primaryButton,
            (!selectedAnswer || isSubmitting) && styles.disabledPrimaryButton,
          ]}
          onPress={handleContinue}
        >
          <Text style={styles.primaryButtonText}>
            {isSubmitting ? "처리 중…" : isLastQuestion ? "제출하기" : "다음 문제"}
          </Text>
          {!isLastQuestion && !isSubmitting && (
            <ChevronRight color="#ffffff" size={20} />
          )}
        </Pressable>
      </View>

      <Toast
        message={toastMessage}
        opacity={toastOpacity}
        bottom={insets.bottom + 84}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: "#F3F3F3" },
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
    backgroundColor: BRAND_PURPLE,
    paddingHorizontal: 20,
    paddingVertical: 10,
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
  headerSubtitle: {
    marginTop: 2,
    color: "#E2DFFF",
    fontFamily: FONT.pretendard,
    fontSize: 11,
    fontWeight: "600",
  },
  content: { flexGrow: 1, paddingHorizontal: 20, paddingTop: 24, paddingBottom: 30 },
  progressInfoRow: { flexDirection: "row", justifyContent: "space-between", alignItems: "center" },
  progressText: {
    color: "#334155",
    fontFamily: FONT.pretendard,
    fontSize: 14,
    fontWeight: "700",
  },
  guestBadge: {
    color: BRAND_PURPLE,
    fontFamily: FONT.pretendard,
    fontSize: 12,
    fontWeight: "700",
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
