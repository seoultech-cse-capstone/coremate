import { useMemo, useState } from "react";
import { ActivityIndicator, Pressable, StyleSheet, Text, View } from "react-native";

import { FONT } from "../constants/fonts";
import { useAuth, useRequireMember } from "../src/features/auth";
import { useCurrencyLogs, useRewardLogs, useRewardSummary, useXpLogs } from "../src/features/reward";
import {
  detailStyles,
  SettingsDetailLayout,
} from "../src/features/settings/components/SettingsDetailLayout";

type TabKey = "reward" | "xp" | "coin";

const TABS: { key: TabKey; label: string }[] = [
  { key: "reward", label: "보상" },
  { key: "xp", label: "XP" },
  { key: "coin", label: "코인" },
];

/** 서버가 주는 enum 이름을 그대로 노출하지 않고 읽을 수 있는 말로 바꾼다. */
// 서버 reward_logs.reward_type enum 전체. 빠진 값이 있으면 화면에 원시 코드가 그대로 노출된다.
const REWARD_TYPE_LABEL: Record<string, string> = {
  STARTER_PACK: "가입 축하 보상",
  CONCEPT_PRACTICE_XP: "개념 문제집",
  CATEGORY_MOCK_XP: "챕터 모의고사",
  SUBJECT_MOCK_XP: "과목 모의고사",
  CATEGORY_MOCK_FIRST_PASS_COIN: "챕터 모의고사 첫 통과",
  SUBJECT_MOCK_FIRST_PASS_COIN: "과목 모의고사 첫 통과",
  CATEGORY_MOCK_RETRY_COIN: "챕터 모의고사 재응시",
  SUBJECT_MOCK_RETRY_COIN: "과목 모의고사 재응시",
  DAILY_QUIZ_COMPLETE: "오늘의 퀴즈",
  DAILY_QUIZ_REWARD: "오늘의 퀴즈 보상",
  DAILY_QUIZ_ATTENDANCE: "오늘의 퀴즈 출석",
  ATTENDANCE_REWARD: "출석 보상",
  LEVEL_UP_5: "레벨 5 달성",
  LEVEL_UP_10: "레벨 10 달성",
  LEVEL_UP_20: "레벨 20 달성",
  LEVEL_UP_30: "레벨 30 달성",
  LEVEL_UP_40: "레벨 40 달성",
  LEVEL_UP_50: "레벨 50 달성",
  RANDOM_BOX_REWARD: "랜덤박스 보상",
  WEAKNESS_ANALYSIS_CREATE: "약점 분석표 생성",
  FEATURE_ACCESS_GRANT: "기능 이용권",
  AD_REWARD: "광고 보상",
  PAYMENT_COIN_PACKAGE: "코인 패키지 구매",
  PREMIUM_SIGNUP_BONUS: "프리미엄 가입 보너스",
  PREMIUM_DAILY_COIN: "프리미엄 일일 코인",
};

// 서버 xp_logs.reason(XpReason) enum 전체.
const XP_REASON_LABEL: Record<string, string> = {
  CONCEPT_PRACTICE_XP: "개념 문제집",
  CATEGORY_MOCK_XP: "챕터 모의고사",
  SUBJECT_MOCK_XP: "과목 모의고사",
  DAILY_QUIZ_XP: "오늘의 퀴즈",
  ATTENDANCE_XP: "출석 보상",
};

// 서버 currency_logs.reason(CurrencyReason) enum 전체. 코인 탭은 reward_type 이 아니라 이 값을 준다.
// 두 enum 은 이름이 비슷하지만 다르다(예: CATEGORY_MOCK_FIRST_PASS_COIN ↔ CATEGORY_MOCK_FIRST_PASS_REWARD).
const CURRENCY_REASON_LABEL: Record<string, string> = {
  DAILY_QUIZ_REWARD: "오늘의 퀴즈 보상",
  ATTENDANCE_REWARD: "출석 보상",
  CATEGORY_MOCK_FIRST_PASS_REWARD: "챕터 모의고사 첫 통과",
  SUBJECT_MOCK_FIRST_PASS_REWARD: "과목 모의고사 첫 통과",
  CATEGORY_MOCK_RETRY_REWARD: "챕터 모의고사 재응시",
  SUBJECT_MOCK_RETRY_REWARD: "과목 모의고사 재응시",
  LEVEL_UP_REWARD: "레벨 달성 보상",
  STARTER_PACK: "가입 축하 보상",
  RANDOM_BOX_PURCHASE: "랜덤박스 구매",
  ROOM_THEME_PURCHASE: "방 테마 구매",
  NICKNAME_CHANGE: "닉네임 변경",
  GENDER_CHANGE: "성별 변경",
  WEAKNESS_ANALYSIS_CREATE: "약점 분석표 생성",
  AD_REWARD: "광고 보상",
  PREMIUM_DAILY_COIN: "프리미엄 일일 코인",
  PREMIUM_SIGNUP_BONUS: "프리미엄 가입 보너스",
  COIN_PACKAGE_PURCHASE: "코인 패키지 구매",
  SHOP_ITEM_PURCHASE: "상점 구매",
  RANDOM_BOX_REWARD: "랜덤박스 보상",
  ADMIN_GRANT: "운영자 지급",
  REFUND: "환불",
  DUPLICATE_ITEM_REFUND: "중복 아이템 환급",
};

// 서버 reward_logs.skip_reason enum 전체.
const SKIP_REASON_LABEL: Record<string, string> = {
  DAILY_LIMIT_EXCEEDED: "일일 한도 초과",
  ALREADY_REWARDED: "이미 받음",
  ALREADY_REWARDED_TODAY: "오늘 같은 범위에서 이미 받음",
  LOW_CORRECT_RATE: "정답률 미달",
  INSUFFICIENT_COIN: "코인 부족",
  INVALID_REWARD_AMOUNT: "지급 금액 오류",
  POLICY_NOT_FOUND: "보상 정책 없음",
};

function label(map: Record<string, string>, key: string | null) {
  if (key === null) {
    return "";
  }
  return map[key] ?? key;
}

function formatDate(value: string) {
  return value.slice(0, 10).replace(/-/g, ".") + " " + value.slice(11, 16);
}

function signed(amount: number) {
  return amount > 0 ? `+${amount}` : String(amount);
}

export default function RewardHistoryScreen() {
  useRequireMember();
  const { accessToken } = useAuth();
  const [tab, setTab] = useState<TabKey>("reward");

  const summaryQuery = useRewardSummary(accessToken);
  const rewardLogsQuery = useRewardLogs(accessToken);
  const xpLogsQuery = useXpLogs(accessToken);
  const currencyLogsQuery = useCurrencyLogs(accessToken);

  const activeQuery =
    tab === "reward" ? rewardLogsQuery : tab === "xp" ? xpLogsQuery : currencyLogsQuery;

  const rows = useMemo(() => {
    if (tab === "reward") {
      return (rewardLogsQuery.data ?? []).map((log) => {
        const granted = log.rewardStatus === "GRANTED";
        const amounts = [
          log.xpAmount ? `XP ${signed(log.xpAmount)}` : null,
          log.coinAmount ? `코인 ${signed(log.coinAmount)}` : null,
        ].filter(Boolean);
        return {
          id: `reward-${log.rewardLogId}`,
          title: label(REWARD_TYPE_LABEL, log.rewardType),
          // 지급되지 않은 건도 내려오므로 사유를 보여준다. 안 보여주면 "왜 0인지" 알 수 없다.
          subtitle: granted
            ? formatDate(log.createdAt)
            : `${formatDate(log.createdAt)} · ${label(SKIP_REASON_LABEL, log.skipReason) || "미지급"}`,
          value: granted && amounts.length > 0 ? amounts.join(" · ") : "미지급",
          muted: !granted,
        };
      });
    }

    if (tab === "xp") {
      return (xpLogsQuery.data ?? []).map((log) => ({
        id: `xp-${log.xpLogId}`,
        title: label(XP_REASON_LABEL, log.reason),
        subtitle: `${formatDate(log.createdAt)} · 누적 ${log.totalXpAfter}`,
        value: `${signed(log.amount)} XP`,
        muted: false,
      }));
    }

    return (currencyLogsQuery.data ?? []).map((log) => ({
      id: `coin-${log.currencyLogId}`,
      title: label(CURRENCY_REASON_LABEL, log.reason),
      subtitle: `${formatDate(log.createdAt)} · 잔액 ${log.balanceAfter}`,
      value: `${signed(log.amount)} 코인`,
      muted: log.amount < 0,
    }));
  }, [tab, rewardLogsQuery.data, xpLogsQuery.data, currencyLogsQuery.data]);

  return (
    <SettingsDetailLayout title="보상 내역">
      <Text style={detailStyles.sectionTitle}>현재 상태</Text>
      <View style={detailStyles.card}>
        {summaryQuery.isLoading ? (
          <ActivityIndicator color="#6D5DFB" />
        ) : summaryQuery.isError ? (
          <Text style={styles.errorText}>요약을 불러오지 못했어요.</Text>
        ) : (
          <View style={styles.summaryRow}>
            <Summary label="레벨" value={`Lv.${summaryQuery.data?.level ?? 1}`} />
            <Summary label="누적 XP" value={`${summaryQuery.data?.totalXp ?? 0}`} />
            <Summary label="코인" value={`${summaryQuery.data?.coin ?? 0}`} />
          </View>
        )}
      </View>

      <View style={styles.tabRow}>
        {TABS.map((item) => (
          <Pressable
            key={item.key}
            style={[styles.tab, tab === item.key && styles.tabActive]}
            onPress={() => setTab(item.key)}
          >
            <Text style={[styles.tabText, tab === item.key && styles.tabTextActive]}>
              {item.label}
            </Text>
          </Pressable>
        ))}
      </View>

      <View style={detailStyles.card}>
        {activeQuery.isLoading ? (
          <ActivityIndicator color="#6D5DFB" />
        ) : activeQuery.isError ? (
          <Text style={styles.errorText}>내역을 불러오지 못했어요.</Text>
        ) : rows.length === 0 ? (
          <Text style={styles.emptyText}>아직 내역이 없어요.</Text>
        ) : (
          rows.map((row) => (
            <View key={row.id} style={styles.logRow}>
              <View style={styles.logTextArea}>
                <Text style={styles.logTitle}>{row.title}</Text>
                <Text style={styles.logSubtitle}>{row.subtitle}</Text>
              </View>
              <Text style={[styles.logValue, row.muted && styles.logValueMuted]}>{row.value}</Text>
            </View>
          ))
        )}
      </View>
    </SettingsDetailLayout>
  );
}

function Summary({ label: text, value }: { label: string; value: string }) {
  return (
    <View style={styles.summaryItem}>
      <Text style={styles.summaryValue}>{value}</Text>
      <Text style={styles.summaryLabel}>{text}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  summaryRow: {
    flexDirection: "row",
  },
  summaryItem: {
    flex: 1,
    alignItems: "center",
  },
  summaryValue: {
    color: "#111827",
    fontFamily: FONT.pretendard,
    fontSize: 18,
    fontWeight: "800",
  },
  summaryLabel: {
    marginTop: 4,
    color: "#9CA3AF",
    fontFamily: FONT.pretendard,
    fontSize: 12,
  },
  tabRow: {
    flexDirection: "row",
    gap: 8,
    marginTop: 20,
    marginBottom: 12,
  },
  tab: {
    flex: 1,
    alignItems: "center",
    paddingVertical: 9,
    borderRadius: 999,
    backgroundColor: "#F3F4F6",
  },
  tabActive: {
    backgroundColor: "#6D5DFB",
  },
  tabText: {
    color: "#6B7280",
    fontFamily: FONT.pretendard,
    fontSize: 14,
    fontWeight: "600",
  },
  tabTextActive: {
    color: "#ffffff",
  },
  logRow: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
    paddingVertical: 10,
  },
  logTextArea: {
    flex: 1,
    paddingRight: 12,
  },
  logTitle: {
    color: "#111827",
    fontFamily: FONT.pretendard,
    fontSize: 14,
    fontWeight: "600",
  },
  logSubtitle: {
    marginTop: 3,
    color: "#9CA3AF",
    fontFamily: FONT.pretendard,
    fontSize: 12,
  },
  logValue: {
    color: "#111827",
    fontFamily: FONT.pretendard,
    fontSize: 14,
    fontWeight: "700",
  },
  logValueMuted: {
    color: "#9CA3AF",
  },
  emptyText: {
    color: "#9CA3AF",
    fontFamily: FONT.pretendard,
    fontSize: 13,
    textAlign: "center",
    paddingVertical: 12,
  },
  errorText: {
    color: "#dc2626",
    fontFamily: FONT.pretendard,
    fontSize: 13,
  },
});
