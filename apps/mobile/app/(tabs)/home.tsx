import { LinearGradient } from "expo-linear-gradient";
import { router, useNavigation } from "expo-router";
import { Bell, Coins, ListChecks } from "lucide-react-native";
import { useEffect, useRef, useState } from "react";
import {
  type NativeScrollEvent,
  type NativeSyntheticEvent,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  useWindowDimensions,
  View,
} from "react-native";

import { FONT } from "../../constants/fonts";
import { useAuth } from "../../src/features/auth";
import { dailyQuizApi } from "../../src/features/dailyQuiz";
import { useMyCurrency } from "../../src/features/currency";
import { useUnreadCountQuery } from "../../src/features/notification";
import { getLevelProgress, useMyProfile } from "../../src/features/user";
import { YourProgress } from "../../src/features/home/components/YourProgress";
import { refetchActiveQueries } from "../../src/shared/api/queryClient";

const BRAND_PURPLE = "#6D5DFB";
const PANEL_GRAY = "#F3F3F3";
const OVERSCROLL_BUFFER = 96;

type TabPressNavigation = {
  addListener: (eventName: "tabPress", callback: () => void) => () => void;
};

export default function HomeScreen() {
  const navigation = useNavigation();
  const scrollRef = useRef<ScrollView>(null);
  const { height } = useWindowDimensions();
  const [bounceBackground, setBounceBackground] = useState(BRAND_PURPLE);
  const { status, accessToken } = useAuth();
  const isGuest = status === "guest";
  const panelTopOffset = Math.round(height * 0.46);
  const [dailyQuizSolved, setDailyQuizSolved] = useState(false);
  const { data: profile } = useMyProfile(accessToken);
  const { data: currency } = useMyCurrency(accessToken);
  const level = profile?.level ?? 1;
  const levelProgress = getLevelProgress(level, profile?.totalXp ?? 0);

  useEffect(() => {
    const tabNavigation = navigation as unknown as TabPressNavigation;
    const unsubscribe = tabNavigation.addListener("tabPress", () => {
      scrollRef.current?.scrollTo({ y: 0, animated: true });
      refetchActiveQueries();
    });

    return unsubscribe;
  }, [navigation]);

  useEffect(() => {
    if (isGuest || !accessToken) {
      return;
    }
    let cancelled = false;
    dailyQuizApi.getToday(accessToken).then(
      (quizSet) => {
        if (!cancelled) {
          setDailyQuizSolved(quizSet.solvedToday);
        }
      },
      (error) => {
        // 홈 진입 자체를 막을 이유는 없으니 조회 실패는 조용히 미완료로 둔다.
        console.error("[home] 오늘의 퀴즈 상태 조회 실패", error);
      },
    );
    return () => {
      cancelled = true;
    };
  }, [isGuest, accessToken]);

  // FR-GUEST-02: 게스트가 회원 전용 기능을 선택하면 로그인 필요 안내로 보낸다.
  const { data: unreadCount = 0 } = useUnreadCountQuery();

  const goToMemberFeature = (memberHref: Parameters<typeof router.push>[0]) => {
    if (isGuest) {
      router.push("/login-required");
      return;
    }
    router.push(memberHref);
  };

  const handleScroll = (event: NativeSyntheticEvent<NativeScrollEvent>) => {
    const { contentOffset, contentSize, layoutMeasurement } = event.nativeEvent;
    const maxScrollY = Math.max(
      0,
      contentSize.height - layoutMeasurement.height,
    );
    const isNearBottom =
      maxScrollY > 0 && contentOffset.y > maxScrollY - OVERSCROLL_BUFFER;
    const nextBackground = isNearBottom ? PANEL_GRAY : BRAND_PURPLE;

    setBounceBackground((currentBackground) =>
      currentBackground === nextBackground ? currentBackground : nextBackground,
    );
  };

  return (
    <ScrollView
      bounces
      onScroll={handleScroll}
      ref={scrollRef}
      scrollEventThrottle={16}
      style={[styles.container, { backgroundColor: bounceBackground }]}
      showsVerticalScrollIndicator={false}
      contentContainerStyle={[
        styles.scrollContent,
        { paddingTop: panelTopOffset },
      ]}
    >
      <View style={[styles.bottomPanel, { minHeight: height }]}>
        <View style={styles.panelContent}>
          <View style={styles.notificationRow}>
            <View style={styles.identityGroup}>
              {!isGuest ? (
                <>
                  <Text numberOfLines={1} style={styles.nicknameText}>
                    {profile?.nickname ?? ""}
                  </Text>
                  <View style={styles.coinPill}>
                    <Coins color="#B45309" size={13} strokeWidth={2.2} />
                    <Text style={styles.coinText}>{currency?.coin ?? 0}</Text>
                  </View>
                </>
              ) : null}
            </View>

            <Pressable
              accessibilityRole="button"
              accessibilityLabel={
                unreadCount > 0 ? `알림 ${unreadCount}건` : "알림"
              }
              hitSlop={8}
              onPress={() => goToMemberFeature("/notifications")}
              style={styles.notificationButton}
            >
              <Bell color="#1F2937" size={22} strokeWidth={2} />
              {unreadCount > 0 && (
                <View style={styles.badge}>
                  <Text style={styles.badgeText}>
                    {unreadCount > 99 ? "99+" : unreadCount}
                  </Text>
                </View>
              )}
            </Pressable>
          </View>
          <View style={styles.levelBarTrack}>
            <LinearGradient
              colors={["#6D5DFB", "#2EE6A6"]}
              start={{ x: 0, y: 0.5 }}
              end={{ x: 1, y: 0.5 }}
              style={[styles.levelBarFill, { width: `${levelProgress.percent}%` }]}
            >
              <Text style={styles.levelText}>Lv.{level}</Text>
            </LinearGradient>
          </View>

          <View style={styles.dailyQuizRow}>
          <Pressable
            accessibilityRole="button"
            onPress={() => goToMemberFeature("/daily-quiz")}
            style={[
              styles.primaryButton,
              styles.dailyQuizButton,
              dailyQuizSolved && styles.dailyQuizCompletedButton,
            ]}
          >
            <View style={styles.dailyQuizButtonContent}>
              <Text style={styles.primaryButtonText}>오늘의 퀴즈</Text>
              {dailyQuizSolved ? (
                <Text style={styles.dailyQuizStatus}>출석 완료</Text>
              ) : null}
            </View>
          </Pressable>
          {dailyQuizSolved ? (
            <Pressable
              accessibilityLabel="오늘의 퀴즈 다시보기"
              accessibilityRole="button"
              onPress={() => goToMemberFeature("/daily-quiz-review")}
              style={styles.dailyQuizReviewButton}
            >
              <ListChecks color="#475569" size={22} strokeWidth={2.1} />
            </Pressable>
          ) : null}
          </View>

          <View style={styles.secondaryRow}>
            <Pressable
              style={styles.secondaryButton}
              onPress={() => router.push("/(tabs)/content")}
            >
              <Text style={styles.secondaryButtonText}>연습문제 풀이</Text>
            </Pressable>

            <Pressable
              style={[styles.secondaryButton, styles.weaknessButton]}
              onPress={() =>
                goToMemberFeature({
                  pathname: "/(tabs)/log-weak",
                  params: { mode: "weakness", entry: Date.now().toString() },
                })
              }
            >
              <Text
                style={[styles.secondaryButtonText, styles.weaknessButtonText]}
              >
                약점 분석하기
              </Text>
            </Pressable>
          </View>

          <YourProgress />
        </View>
      </View>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  notificationRow: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
    paddingBottom: 8,
  },
  identityGroup: {
    flex: 1,
    flexDirection: "row",
    alignItems: "center",
    gap: 8,
  },
  nicknameText: {
    flexShrink: 1,
    color: "#1F2937",
    fontFamily: FONT.pretendard,
    fontSize: 16,
    fontWeight: "700",
  },
  coinPill: {
    flexDirection: "row",
    alignItems: "center",
    gap: 4,
    borderRadius: 999,
    backgroundColor: "#FEF3C7",
    paddingHorizontal: 8,
    paddingVertical: 4,
  },
  coinText: {
    color: "#B45309",
    fontFamily: FONT.kufam,
    fontSize: 12,
    fontWeight: "700",
  },
  notificationButton: {
    width: 36,
    height: 36,
    alignItems: "center",
    justifyContent: "center",
  },
  badge: {
    position: "absolute",
    top: 2,
    right: 2,
    minWidth: 16,
    height: 16,
    alignItems: "center",
    justifyContent: "center",
    borderRadius: 8,
    backgroundColor: "#EF4444",
    paddingHorizontal: 4,
  },
  badgeText: {
    color: "#FFFFFF",
    fontFamily: FONT.pretendard,
    fontSize: 10,
    fontWeight: "700",
  },
  container: {
    flex: 1,
  },
  scrollContent: {
    backgroundColor: BRAND_PURPLE,
  },
  bottomPanel: {
    width: "100%",
    borderTopLeftRadius: 28,
    borderTopRightRadius: 28,
    backgroundColor: PANEL_GRAY,
    paddingHorizontal: 24,
    paddingTop: 32,
  },
  panelContent: {
    paddingBottom: 28 + OVERSCROLL_BUFFER,
  },
  levelBarTrack: {
    width: "100%",
    height: 24,
    borderRadius: 12,
    backgroundColor: "#D9D9D9",
    overflow: "hidden",
    marginBottom: 18,
  },
  levelBarFill: {
    height: "100%",
    justifyContent: "center",
    borderRadius: 12,
    paddingLeft: 12,
  },
  levelText: {
    color: "#ffffff",
    fontFamily: FONT.kufam,
    fontSize: 12,
    fontWeight: "700",
  },
  primaryButton: {
    alignItems: "center",
    justifyContent: "center",
    minHeight: 64,
    borderRadius: 18,
    backgroundColor: BRAND_PURPLE,
  },
  dailyQuizRow: {
    flexDirection: "row",
    gap: 10,
  },
  dailyQuizButton: {
    flex: 1,
  },
  dailyQuizCompletedButton: {
    backgroundColor: "#94A3B8",
  },
  dailyQuizReviewButton: {
    width: 64,
    alignItems: "center",
    justifyContent: "center",
    borderRadius: 18,
    backgroundColor: "#FFFFFF",
  },
  primaryButtonText: {
    color: "#ffffff",
    fontFamily: FONT.pretendard,
    fontSize: 18,
    fontWeight: "500",
  },
  dailyQuizButtonContent: {
    alignItems: "center",
    gap: 3,
  },
  dailyQuizStatus: {
    color: "rgba(255, 255, 255, 0.76)",
    fontFamily: FONT.pretendard,
    fontSize: 11,
    fontWeight: "500",
  },
  secondaryRow: {
    flexDirection: "row",
    gap: 12,
    marginTop: 14,
    marginBottom: 28,
  },
  secondaryButton: {
    flex: 1,
    alignItems: "center",
    justifyContent: "center",
    minHeight: 72,
    borderRadius: 18,
    backgroundColor: "#ffffff",
    paddingHorizontal: 12,
  },
  secondaryButtonText: {
    color: "#1f2937",
    fontFamily: FONT.pretendard,
    fontSize: 15,
    fontWeight: "500",
    textAlign: "center",
  },
  weaknessButton: {
    backgroundColor: "#111827",
  },
  weaknessButtonText: {
    color: "#ffffff",
  },
});
