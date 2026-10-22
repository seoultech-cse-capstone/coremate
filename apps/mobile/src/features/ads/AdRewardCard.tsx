import { useQueryClient } from "@tanstack/react-query";
import { LinearGradient } from "expo-linear-gradient";
import { Clapperboard, Coins, Sparkles } from "lucide-react-native";
import { useCallback, useEffect, useState } from "react";
import { ActivityIndicator, Pressable, StyleSheet, Text, View } from "react-native";

import { FONT } from "../../../constants/fonts";
import { useAdRewardStatus } from "./useAdRewardStatus";
import { useRewardedAd } from "./useRewardedAd";

// 시청이 끝난 뒤 코인이 들어오기를 기다리는 최대 시간.
// 구글의 콜백은 보통 몇 초 안에 오지만 늦을 수 있어 넉넉히 기다린다.
const CONFIRM_TIMEOUT_MS = 15000;

// 마지막으로 닫힌 광고. 닫힐 때마다 새 객체라 같은 횟수에서 다시 봐도 대기 타이머가 새로 돈다.
// earned 는 끝까지 본 경우이고, countBefore 는 그 직전의 오늘 지급 횟수다.
type ClosedAd = { earned: true; countBefore: number } | { earned: false };

type Props = {
  accessToken: string | null;
  userId: number | null;
};

/**
 * 상점의 「광고 보고 코인 받기」 카드(SCR-SHOP-01, FR-PAY-02).
 *
 * 광고를 끝까지 봐도 코인은 서버가 구글의 콜백을 받은 뒤에 준다. 그래서 시청 직전의 오늘 지급 횟수를 기억해 두고,
 * 서버 기록이 그보다 늘어나면 지급된 것으로 본다.
 */
export function AdRewardCard({ accessToken, userId }: Props) {
  const queryClient = useQueryClient();
  const [closedAd, setClosedAd] = useState<ClosedAd | null>(null);
  const [timedOut, setTimedOut] = useState(false);

  const earnedAd = closedAd?.earned ? closedAd : null;
  const status = useAdRewardStatus(
    accessToken,
    earnedAd !== null && !timedOut ? earnedAd.countBefore : null,
  );
  const rewarded = earnedAd !== null && status.usedCount > earnedAd.countBefore;
  const confirming = earnedAd !== null && !rewarded && !timedOut;

  const handleClosed = useCallback(
    (earned: boolean) => {
      setTimedOut(false);
      setClosedAd(earned ? { earned: true, countBefore: status.usedCount } : { earned: false });
    },
    [status.usedCount],
  );

  const ad = useRewardedAd(userId, status.isReady && status.remainingCount > 0, handleClosed);

  useEffect(() => {
    if (!closedAd?.earned) {
      return;
    }
    const timer = setTimeout(() => setTimedOut(true), CONFIRM_TIMEOUT_MS);
    return () => clearTimeout(timer);
  }, [closedAd]);

  // 상점 머리의 코인 배지와 홈·마이페이지의 코인 표시를 새로 불러온다.
  useEffect(() => {
    if (!rewarded) {
      return;
    }
    void queryClient.invalidateQueries({ queryKey: ["currency", "me"] });
    void queryClient.invalidateQueries({ queryKey: ["reward", "summary"] });
  }, [rewarded, queryClient]);

  if (!status.isReady) {
    return null;
  }

  const notice = rewarded
    ? { tone: "success" as const, text: `${status.coinAmount}코인을 받았어요!` }
    : earnedAd !== null && timedOut
      ? { tone: "warning" as const, text: "코인이 아직 들어오지 않았어요. 잠시 뒤 다시 확인해 주세요." }
      : closedAd !== null && !closedAd.earned
        ? // EX-REWARD-005
          { tone: "warning" as const, text: "광고를 끝까지 보지 않아 코인을 받지 못했어요." }
        : null;

  const exhausted = status.remainingCount === 0;
  const busy = confirming || ad.state === "loading" || ad.state === "showing";
  const pressable = !exhausted && !confirming && (ad.state === "ready" || ad.state === "error");

  const ctaText = exhausted
    ? "오늘은 다 받았어요"
    : confirming
      ? "코인 확인 중"
      : ad.state === "ready"
        ? `광고 보고 ${status.coinAmount}코인 받기`
        : ad.state === "error"
          ? "광고를 불러오지 못했어요 · 다시 시도"
          : ad.state === "unavailable"
            ? "이 빌드에서는 광고를 볼 수 없어요"
            : "광고 불러오는 중";

  return (
    <Pressable
      accessibilityRole="button"
      accessibilityLabel={ctaText}
      disabled={!pressable}
      style={styles.wrap}
      onPress={ad.showOrRetry}
    >
      <LinearGradient
        colors={["#10B981", "#059669", "#047857"]}
        start={{ x: 0, y: 0 }}
        end={{ x: 1, y: 1 }}
        style={styles.card}
      >
        <View style={styles.badge}>
          <Sparkles color="#FDE68A" size={12} strokeWidth={2.6} />
          <Text style={styles.badgeText}>광고 보상</Text>
        </View>

        <View style={styles.countPill}>
          <Text style={styles.countText}>
            오늘 {status.remainingCount}/{status.dailyLimit}회 남음
          </Text>
        </View>

        <View style={styles.iconCircle}>
          <Clapperboard color="#ffffff" size={46} strokeWidth={2} />
        </View>

        <Text style={styles.name}>광고 보고 코인 받기</Text>
        <Text style={styles.desc}>광고를 끝까지 보면 {status.coinAmount}코인을 드려요</Text>

        <View style={[styles.cta, !pressable && !busy && styles.ctaDisabled]}>
          {busy ? (
            <ActivityIndicator color="#7C2D12" size="small" />
          ) : (
            <Coins color="#7C2D12" size={16} strokeWidth={2.6} />
          )}
          <Text style={styles.ctaText}>{ctaText}</Text>
        </View>

        {notice ? (
          <Text
            style={[
              styles.notice,
              notice.tone === "success" ? styles.noticeSuccess : styles.noticeWarning,
            ]}
          >
            {notice.text}
          </Text>
        ) : null}
      </LinearGradient>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  wrap: {
    borderRadius: 22,
    overflow: "hidden",
    shadowColor: "#059669",
    shadowOffset: { width: 0, height: 8 },
    shadowOpacity: 0.35,
    shadowRadius: 16,
    elevation: 8,
  },
  card: {
    alignItems: "center",
    paddingHorizontal: 20,
    paddingVertical: 22,
  },
  badge: {
    flexDirection: "row",
    alignItems: "center",
    gap: 4,
    borderRadius: 999,
    backgroundColor: "rgba(0,0,0,0.22)",
    paddingHorizontal: 10,
    paddingVertical: 4,
  },
  badgeText: {
    color: "#FDE68A",
    fontFamily: FONT.pretendard,
    fontSize: 11,
    fontWeight: "800",
    letterSpacing: 0.5,
  },
  countPill: {
    position: "absolute",
    top: 14,
    right: 14,
    borderRadius: 999,
    backgroundColor: "rgba(0,0,0,0.28)",
    paddingHorizontal: 10,
    paddingVertical: 5,
  },
  countText: {
    color: "#ffffff",
    fontFamily: FONT.pretendard,
    fontSize: 11,
    fontWeight: "800",
    letterSpacing: 0.3,
  },
  iconCircle: {
    alignItems: "center",
    justifyContent: "center",
    width: 96,
    height: 96,
    marginVertical: 18,
    borderRadius: 48,
    backgroundColor: "rgba(255,255,255,0.16)",
  },
  name: {
    color: "#ffffff",
    fontFamily: FONT.pretendard,
    fontSize: 18,
    fontWeight: "800",
  },
  desc: {
    marginTop: 4,
    color: "rgba(255,255,255,0.8)",
    fontFamily: FONT.pretendard,
    fontSize: 13,
    fontWeight: "500",
  },
  cta: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "center",
    gap: 6,
    marginTop: 16,
    borderRadius: 12,
    backgroundColor: "#FDE68A",
    paddingVertical: 12,
    paddingHorizontal: 20,
    alignSelf: "stretch",
  },
  ctaDisabled: {
    opacity: 0.6,
  },
  ctaText: {
    color: "#7C2D12",
    fontFamily: FONT.pretendard,
    fontSize: 15,
    fontWeight: "800",
  },
  notice: {
    marginTop: 10,
    fontFamily: FONT.pretendard,
    fontSize: 13,
    fontWeight: "700",
    textAlign: "center",
  },
  noticeSuccess: {
    color: "#ECFDF5",
  },
  noticeWarning: {
    color: "#FEF3C7",
  },
});
