import { router } from "expo-router";
import { useMemo, useState } from "react";
import { ActivityIndicator, Pressable, StyleSheet, Text, View } from "react-native";

import { FONT } from "../constants/fonts";
import {
  useAttendanceCalendar,
  useClaimAttendanceReward,
  useTodayAttendance,
} from "../src/features/attendance";
import { useAuth, useRequireMember } from "../src/features/auth";
import {
  detailStyles,
  SettingsDetailLayout,
} from "../src/features/settings/components/SettingsDetailLayout";

const WEEKDAYS = ["일", "월", "화", "수", "목", "금", "토"];

function toMonthKey(date: Date) {
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}`;
}

function addMonths(monthKey: string, delta: number) {
  const [year, month] = monthKey.split("-").map(Number);
  const next = new Date(year, month - 1 + delta, 1);
  return toMonthKey(next);
}

/** 달력 격자. 1일이 시작하는 요일만큼 앞을 비운다. */
function buildCells(monthKey: string) {
  const [year, month] = monthKey.split("-").map(Number);
  const firstWeekday = new Date(year, month - 1, 1).getDay();
  const lastDay = new Date(year, month, 0).getDate();
  const cells: (number | null)[] = Array.from({ length: firstWeekday }, () => null);
  for (let day = 1; day <= lastDay; day += 1) {
    cells.push(day);
  }
  return cells;
}

export default function AttendanceScreen() {
  useRequireMember();
  const { accessToken } = useAuth();
  const [monthKey, setMonthKey] = useState(() => toMonthKey(new Date()));

  const todayQuery = useTodayAttendance(accessToken);
  const calendarQuery = useAttendanceCalendar(accessToken, monthKey);
  const claimReward = useClaimAttendanceReward(accessToken);

  // 오늘 출석 전이면 백엔드가 ATTENDANCE_NOT_FOUND 를 준다. 오류가 아니라 "아직 출석 전"이다.
  const todayAttendance = todayQuery.data ?? null;
  const hasAttendedToday = todayAttendance !== null;
  const needsRewardClaim = todayAttendance !== null && !todayAttendance.rewardClaimed;

  const attendedDays = useMemo(() => {
    const days = new Set<number>();
    for (const date of calendarQuery.data?.attendedDates ?? []) {
      days.add(Number(date.slice(8, 10)));
    }
    return days;
  }, [calendarQuery.data?.attendedDates]);

  const cells = useMemo(() => buildCells(monthKey), [monthKey]);
  const isCurrentMonth = monthKey === toMonthKey(new Date());

  return (
    <SettingsDetailLayout title="출석 현황">
      <Text style={detailStyles.sectionTitle}>오늘</Text>
      <View style={detailStyles.card}>
        {todayQuery.isLoading ? (
          <ActivityIndicator color="#6D5DFB" />
        ) : hasAttendedToday ? (
          <>
            <Text style={styles.todayTitle}>오늘 출석했어요</Text>
            <Text style={styles.todayHint}>
              {todayAttendance.rewardClaimed
                ? "출석 보상까지 받았어요."
                : "출석 보상이 아직 지급되지 않았어요."}
            </Text>
            {needsRewardClaim ? (
              <Pressable
                disabled={claimReward.isPending}
                style={[detailStyles.primaryButton, claimReward.isPending && styles.disabled]}
                onPress={() => claimReward.mutate(todayAttendance.attendanceId)}
              >
                <Text style={detailStyles.primaryButtonText}>
                  {claimReward.isPending ? "받는 중…" : "출석 보상 받기"}
                </Text>
              </Pressable>
            ) : null}
            {claimReward.isError ? (
              <Text style={styles.errorText}>보상을 받지 못했어요. 잠시 후 다시 시도해주세요.</Text>
            ) : null}
          </>
        ) : (
          <>
            <Text style={styles.todayTitle}>아직 오늘 출석 전이에요</Text>
            {/* 출석 기록은 오늘의 CS 퀴즈를 제출할 때 백엔드가 자동으로 만든다. 별도 체크인 동작이 없다. */}
            <Text style={styles.todayHint}>오늘의 CS 퀴즈를 풀면 출석과 보상이 함께 처리돼요.</Text>
            <Pressable style={detailStyles.primaryButton} onPress={() => router.push("/daily-quiz")}>
              <Text style={detailStyles.primaryButtonText}>오늘의 퀴즈 풀러 가기</Text>
            </Pressable>
          </>
        )}
      </View>

      <Text style={detailStyles.sectionTitle}>출석 달력</Text>
      <View style={detailStyles.card}>
        <View style={styles.monthRow}>
          <Pressable hitSlop={10} onPress={() => setMonthKey(addMonths(monthKey, -1))}>
            <Text style={styles.monthArrow}>‹</Text>
          </Pressable>
          <Text style={styles.monthLabel}>{monthKey.replace("-", ".")}</Text>
          <Pressable
            disabled={isCurrentMonth}
            hitSlop={10}
            onPress={() => setMonthKey(addMonths(monthKey, 1))}
          >
            <Text style={[styles.monthArrow, isCurrentMonth && styles.monthArrowDisabled]}>›</Text>
          </Pressable>
        </View>

        {calendarQuery.isLoading ? (
          <ActivityIndicator color="#6D5DFB" style={styles.calendarSpacing} />
        ) : calendarQuery.isError ? (
          <Text style={[styles.errorText, styles.calendarSpacing]}>
            출석 달력을 불러오지 못했어요.
          </Text>
        ) : (
          <>
            <View style={styles.weekRow}>
              {WEEKDAYS.map((weekday) => (
                <Text key={weekday} style={styles.weekday}>
                  {weekday}
                </Text>
              ))}
            </View>

            <View style={styles.grid}>
              {cells.map((day, index) => (
                <View key={day === null ? `blank-${index}` : `day-${day}`} style={styles.cell}>
                  {day === null ? null : (
                    <View style={[styles.dayCircle, attendedDays.has(day) && styles.dayAttended]}>
                      <Text style={[styles.dayText, attendedDays.has(day) && styles.dayTextAttended]}>
                        {day}
                      </Text>
                    </View>
                  )}
                </View>
              ))}
            </View>

            <Text style={styles.totalText}>
              이번 달 출석 {calendarQuery.data?.totalCount ?? 0}일
            </Text>
          </>
        )}
      </View>
    </SettingsDetailLayout>
  );
}

const styles = StyleSheet.create({
  todayTitle: {
    color: "#111827",
    fontFamily: FONT.pretendard,
    fontSize: 16,
    fontWeight: "700",
  },
  todayHint: {
    marginTop: 6,
    marginBottom: 12,
    color: "#6B7280",
    fontFamily: FONT.pretendard,
    fontSize: 13,
  },
  disabled: {
    opacity: 0.5,
  },
  errorText: {
    marginTop: 10,
    color: "#dc2626",
    fontFamily: FONT.pretendard,
    fontSize: 13,
  },
  monthRow: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "center",
    gap: 24,
  },
  monthArrow: {
    color: "#111827",
    fontSize: 24,
    fontWeight: "700",
  },
  monthArrowDisabled: {
    color: "#D1D5DB",
  },
  monthLabel: {
    minWidth: 88,
    textAlign: "center",
    color: "#111827",
    fontFamily: FONT.pretendard,
    fontSize: 16,
    fontWeight: "700",
  },
  calendarSpacing: {
    marginTop: 16,
  },
  weekRow: {
    flexDirection: "row",
    marginTop: 14,
  },
  weekday: {
    flex: 1,
    textAlign: "center",
    color: "#9CA3AF",
    fontFamily: FONT.pretendard,
    fontSize: 12,
    fontWeight: "600",
  },
  grid: {
    flexDirection: "row",
    flexWrap: "wrap",
    marginTop: 6,
  },
  cell: {
    width: `${100 / 7}%`,
    alignItems: "center",
    paddingVertical: 4,
  },
  dayCircle: {
    width: 32,
    height: 32,
    borderRadius: 16,
    alignItems: "center",
    justifyContent: "center",
  },
  dayAttended: {
    backgroundColor: "#6D5DFB",
  },
  dayText: {
    color: "#374151",
    fontFamily: FONT.pretendard,
    fontSize: 13,
  },
  dayTextAttended: {
    color: "#ffffff",
    fontWeight: "700",
  },
  totalText: {
    marginTop: 12,
    textAlign: "center",
    color: "#6B7280",
    fontFamily: FONT.pretendard,
    fontSize: 13,
  },
});
