import { router } from "expo-router";
import { Bell, ChevronLeft } from "lucide-react-native";
import {
  ActivityIndicator,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  View,
} from "react-native";
import { useSafeAreaInsets } from "react-native-safe-area-context";

import { FONT } from "../constants/fonts";
import { useRequireMember } from "../src/features/auth";
import {
  useMarkAllNotificationsReadMutation,
  useMarkNotificationReadMutation,
  useNotificationsQuery,
} from "../src/features/notification";
import type { AppNotification } from "../src/features/notification/types";

/** 알림 종류별로 열어 줄 화면. 대상이 없으면 목록에 머문다. */
const TARGET_ROUTES: Record<string, string | null> = {
  NONE: null,
  FRIEND: "/(tabs)/friend",
  DAILY_QUIZ: "/(tabs)/home",
  MYPAGE: "/(tabs)/mypage",
  SETTING: "/device-management",
  WEAKNESS_REPORT: "/weakness-report",
};

export default function NotificationsScreen() {
  useRequireMember();
  const insets = useSafeAreaInsets();
  const { data: notifications = [], isLoading } = useNotificationsQuery();
  const markRead = useMarkNotificationReadMutation();
  const markAllRead = useMarkAllNotificationsReadMutation();

  const hasUnread = notifications.some((notification) => !notification.isRead);

  const openNotification = (notification: AppNotification) => {
    if (!notification.isRead) {
      markRead.mutate(notification.notificationId);
    }
    // 약점분석 완료 알림은 target_id(리포트 ID)로 해당 분석표 상세를 바로 연다.
    if (notification.targetType === "WEAKNESS_REPORT") {
      if (notification.targetId !== null) {
        router.push({
          pathname: "/weakness-report",
          params: { reportId: String(notification.targetId) },
        } as never);
      }
      return;
    }
    const route = TARGET_ROUTES[notification.targetType];
    if (route) {
      router.push(route as never);
    }
  };

  return (
    <View style={[styles.container, { paddingTop: insets.top }]}>
      <View style={styles.header}>
        <Pressable accessibilityLabel="뒤로 가기" hitSlop={8} onPress={() => router.back()}>
          <ChevronLeft color="#1F2937" size={24} />
        </Pressable>
        <Text style={styles.title}>알림</Text>
        <Pressable
          accessibilityLabel="전체 읽음 처리"
          disabled={!hasUnread}
          hitSlop={8}
          onPress={() => markAllRead.mutate()}
        >
          <Text style={[styles.markAll, !hasUnread && styles.markAllDisabled]}>모두 읽음</Text>
        </Pressable>
      </View>

      {isLoading ? (
        <View style={styles.center}>
          <ActivityIndicator color="#6D5DFB" />
        </View>
      ) : notifications.length === 0 ? (
        <View style={styles.center}>
          <Bell color="#CBD5E1" size={40} />
          <Text style={styles.emptyText}>아직 받은 알림이 없어요</Text>
        </View>
      ) : (
        <ScrollView contentContainerStyle={styles.list} showsVerticalScrollIndicator={false}>
          {notifications.map((notification) => (
            <Pressable
              key={notification.notificationId}
              onPress={() => openNotification(notification)}
              style={({ pressed }) => [
                styles.card,
                !notification.isRead && styles.cardUnread,
                pressed && styles.cardPressed,
              ]}
            >
              <View style={styles.cardHeader}>
                {!notification.isRead && <View style={styles.unreadDot} />}
                <Text numberOfLines={1} style={styles.cardTitle}>
                  {notification.title}
                </Text>
              </View>
              <Text numberOfLines={2} style={styles.cardBody}>
                {notification.body}
              </Text>
              <Text style={styles.cardTime}>{formatTime(notification.createdAt)}</Text>
            </Pressable>
          ))}
        </ScrollView>
      )}
    </View>
  );
}

/** 방금·N분 전·N시간 전·날짜. 목록에서 시각 자체보다 "얼마나 됐는지"가 중요하다. */
function formatTime(isoString: string): string {
  const created = new Date(isoString);
  const diffMinutes = Math.floor((Date.now() - created.getTime()) / 60000);

  if (diffMinutes < 1) {
    return "방금";
  }
  if (diffMinutes < 60) {
    return `${diffMinutes}분 전`;
  }
  if (diffMinutes < 60 * 24) {
    return `${Math.floor(diffMinutes / 60)}시간 전`;
  }
  return `${created.getMonth() + 1}월 ${created.getDate()}일`;
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: "#F8FAFC" },
  header: {
    height: 56,
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
    backgroundColor: "#FFFFFF",
    paddingHorizontal: 16,
  },
  title: { color: "#1F2937", fontFamily: FONT.pretendard, fontSize: 17, fontWeight: "700" },
  markAll: { color: "#6D5DFB", fontFamily: FONT.pretendard, fontSize: 13, fontWeight: "600" },
  markAllDisabled: { color: "#CBD5E1" },
  center: { flex: 1, alignItems: "center", justifyContent: "center", gap: 12 },
  emptyText: { color: "#94A3B8", fontFamily: FONT.pretendard, fontSize: 14 },
  list: { gap: 10, padding: 16 },
  card: {
    gap: 6,
    borderRadius: 10,
    backgroundColor: "#FFFFFF",
    padding: 14,
  },
  cardUnread: { backgroundColor: "#F5F3FF" },
  cardPressed: { opacity: 0.7 },
  cardHeader: { flexDirection: "row", alignItems: "center", gap: 6 },
  unreadDot: { width: 6, height: 6, borderRadius: 3, backgroundColor: "#6D5DFB" },
  cardTitle: {
    flex: 1,
    color: "#1F2937",
    fontFamily: FONT.pretendard,
    fontSize: 14,
    fontWeight: "600",
  },
  cardBody: { color: "#475569", fontFamily: FONT.pretendard, fontSize: 13, lineHeight: 18 },
  cardTime: { color: "#94A3B8", fontFamily: FONT.pretendard, fontSize: 11 },
});
