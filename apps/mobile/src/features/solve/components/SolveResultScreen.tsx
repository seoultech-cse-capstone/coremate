import {
  BookOpen,
  CheckCircle2,
  ChevronLeft,
  CircleX,
  ListChecks,
  RotateCcw,
} from "lucide-react-native";
import { Pressable, ScrollView, StyleSheet, Text, View } from "react-native";
import { useSafeAreaInsets } from "react-native-safe-area-context";

import { FONT } from "../../../../constants/fonts";
import type { SolveSessionType } from "./SolveQuestionScreen";

type SolveResultScreenProps = {
  correctCount: number;
  elapsedSeconds?: number;
  sessionType: SolveSessionType;
  title: string;
  totalCount: number;
  onBackToContent: () => void;
  onReview: () => void;
  onRetry: () => void;
  isDailyQuiz?: boolean;
  onGoHome?: () => void;
};

const sessionTypeLabel: Record<SolveSessionType, string> = {
  SUBJECT_MOCK: "과목 모의고사",
  CATEGORY_LV1_MOCK: "대분류 모의고사",
  CATEGORY_LV2_MOCK: "중분류 모의고사",
  CONCEPT_PRACTICE: "개념 문제집",
  DAILY_QUIZ: "오늘의 CS 퀴즈",
};

export function SolveResultScreen({
  correctCount,
  elapsedSeconds,
  sessionType,
  title,
  totalCount,
  onBackToContent,
  onReview,
  onRetry,
  isDailyQuiz = false,
  onGoHome,
}: SolveResultScreenProps) {
  const insets = useSafeAreaInsets();
  const wrongCount = totalCount - correctCount;
  const correctRate = Math.round((correctCount / totalCount) * 100);
  const isMockExam =
    sessionType === "SUBJECT_MOCK" ||
    sessionType === "CATEGORY_LV1_MOCK" ||
    sessionType === "CATEGORY_LV2_MOCK";
  const elapsedTimeLabel = `${Math.floor((elapsedSeconds ?? 0) / 60)
    .toString()
    .padStart(2, "0")}:${((elapsedSeconds ?? 0) % 60)
    .toString()
    .padStart(2, "0")}`;

  return (
    <View style={styles.screen}>
      <View style={[styles.header, { paddingTop: insets.top }]}>
        <Pressable
          accessibilityLabel="문제집으로 돌아가기"
          accessibilityRole="button"
          style={styles.backButton}
          onPress={onBackToContent}
        >
          <ChevronLeft color="#ffffff" size={28} strokeWidth={2.2} />
        </Pressable>
        <Text style={styles.headerTitle}>풀이 결과</Text>
        <View style={styles.headerPlaceholder} />
      </View>

      <ScrollView contentContainerStyle={styles.content} showsVerticalScrollIndicator={false}>
        <Text style={styles.sessionType}>{sessionTypeLabel[sessionType]}</Text>
        <Text numberOfLines={1} style={styles.title}>{title}</Text>

        <View style={styles.scoreCard}>
          <Text style={styles.scoreLabel}>정답률</Text>
          <Text style={styles.scoreValue}>{correctRate}%</Text>
          <Text style={styles.scoreDetail}>{correctCount} / {totalCount} 문제 정답</Text>
          {isMockExam && (
            <Text style={styles.elapsedTime}>소요 시간 {elapsedTimeLabel}</Text>
          )}
        </View>

        <View style={styles.statsRow}>
          <View style={styles.statCard}>
            <CheckCircle2 color="#16a34a" size={20} strokeWidth={2.3} />
            <Text style={styles.statValue}>{correctCount}</Text>
            <Text style={styles.statLabel}>맞은 문제</Text>
          </View>
          <View style={styles.statCard}>
            <CircleX color="#ef4444" size={20} strokeWidth={2.3} />
            <Text style={styles.statValue}>{wrongCount}</Text>
            <Text style={styles.statLabel}>틀린 문제</Text>
          </View>
        </View>

        {isDailyQuiz && (
          <View style={styles.dailyRewardCard}>
            <CheckCircle2 color="#16a34a" size={22} strokeWidth={2.3} />
            <View style={styles.dailyRewardTextArea}>
              <Text style={styles.dailyRewardTitle}>출석 완료</Text>
              <Text style={styles.dailyRewardText}>
                오늘의 퀴즈 보상이 지급되었습니다.
              </Text>
            </View>
            <Text style={styles.rewardValue}>+20 XP</Text>
          </View>
        )}

        <View style={styles.notice}>
          <Text style={styles.noticeTitle}>문제별 해설을 확인해보세요.</Text>
          <Text style={styles.noticeText}>선택한 답과 정답, 각 문제의 해설을 다시 볼 수 있어요.</Text>
        </View>

        <Pressable style={styles.reviewButton} onPress={onReview}>
          <ListChecks color="#ffffff" size={20} strokeWidth={2.2} />
          <Text style={styles.reviewButtonText}>해설 보기</Text>
        </Pressable>
        <View style={styles.secondaryActionRow}>
          <Pressable
            style={styles.retryButton}
            onPress={isDailyQuiz ? onGoHome : onRetry}
          >
            <RotateCcw color="#5b4de8" size={19} strokeWidth={2.2} />
            <Text style={styles.retryButtonText}>
              {isDailyQuiz ? "홈으로" : "다시 풀기"}
            </Text>
          </Pressable>
          <Pressable style={styles.retryButton} onPress={onBackToContent}>
            <BookOpen color="#5b4de8" size={19} strokeWidth={2.2} />
            <Text style={styles.retryButtonText}>문제집 목록으로</Text>
          </Pressable>
        </View>
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: "#F3F3F3" },
  header: { minHeight: 64, flexDirection: "row", alignItems: "center", justifyContent: "space-between", backgroundColor: "#6D5DFB", paddingHorizontal: 14 },
  backButton: { alignItems: "center", justifyContent: "center", width: 40, height: 40 },
  headerTitle: { color: "#ffffff", fontFamily: FONT.pretendard, fontSize: 17, fontWeight: "700" },
  headerPlaceholder: { width: 40 },
  content: { paddingHorizontal: 20, paddingTop: 32, paddingBottom: 44 },
  sessionType: { color: "#6D5DFB", fontFamily: FONT.pretendard, fontSize: 13, fontWeight: "700" },
  title: { color: "#1f2937", fontFamily: FONT.pretendard, fontSize: 22, fontWeight: "700", marginTop: 6 },
  scoreCard: { alignItems: "center", borderRadius: 8, backgroundColor: "#ffffff", marginTop: 22, paddingVertical: 28 },
  scoreLabel: { color: "#64748b", fontFamily: FONT.pretendard, fontSize: 14, fontWeight: "600" },
  scoreValue: { color: "#6D5DFB", fontFamily: FONT.kufam, fontSize: 44, fontWeight: "700", marginTop: 5 },
  scoreDetail: { color: "#475569", fontFamily: FONT.pretendard, fontSize: 14, fontWeight: "600", marginTop: 5 },
  elapsedTime: { color: "#6D5DFB", fontFamily: FONT.kufam, fontSize: 13, fontWeight: "700", marginTop: 8 },
  statsRow: { flexDirection: "row", gap: 12, marginTop: 12 },
  statCard: { flex: 1, alignItems: "center", borderRadius: 8, backgroundColor: "#ffffff", paddingVertical: 18 },
  statValue: { color: "#1f2937", fontFamily: FONT.kufam, fontSize: 22, fontWeight: "700", marginTop: 7 },
  statLabel: { color: "#64748b", fontFamily: FONT.pretendard, fontSize: 12, fontWeight: "600", marginTop: 2 },
  notice: { borderRadius: 8, backgroundColor: "#F1EFFF", marginTop: 22, padding: 16 },
  noticeTitle: { color: "#5144cf", fontFamily: FONT.pretendard, fontSize: 15, fontWeight: "700" },
  noticeText: { color: "#5f587d", fontFamily: FONT.pretendard, fontSize: 13, fontWeight: "500", lineHeight: 20, marginTop: 6 },
  dailyRewardCard: { flexDirection: "row", alignItems: "center", gap: 10, borderRadius: 8, backgroundColor: "#ECFDF5", marginTop: 12, padding: 16 },
  dailyRewardTextArea: { flex: 1, gap: 3 },
  dailyRewardTitle: { color: "#166534", fontFamily: FONT.pretendard, fontSize: 14, fontWeight: "700" },
  dailyRewardText: { color: "#4B7A5A", fontFamily: FONT.pretendard, fontSize: 12 },
  rewardValue: { color: "#16A34A", fontFamily: FONT.kufam, fontSize: 13, fontWeight: "700" },
  reviewButton: { minHeight: 52, flexDirection: "row", alignItems: "center", justifyContent: "center", gap: 7, borderRadius: 8, backgroundColor: "#6D5DFB", marginTop: 26 },
  reviewButtonText: { color: "#ffffff", fontFamily: FONT.pretendard, fontSize: 15, fontWeight: "700" },
  secondaryActionRow: { flexDirection: "row", gap: 10, marginTop: 10 },
  retryButton: { minHeight: 50, flex: 1, flexDirection: "row", alignItems: "center", justifyContent: "center", gap: 6, borderWidth: 1, borderColor: "#cfcafb", borderRadius: 8, backgroundColor: "#ffffff", paddingHorizontal: 10 },
  retryButtonText: { color: "#5b4de8", fontFamily: FONT.pretendard, fontSize: 13, fontWeight: "700" },
});
