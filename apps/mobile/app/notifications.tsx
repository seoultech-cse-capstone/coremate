import { router } from "expo-router";
import {
  Bell,
  BookOpen,
  CalendarCheck,
  ChevronLeft,
  Hand,
  ListChecks,
  ShieldAlert,
  Trash2,
  Trophy,
  UserCheck,
  UserPlus,
  type LucideIcon,
} from "lucide-react-native";
import { useRef, type RefObject } from "react";
import {
  ActivityIndicator,
  Alert,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  View,
} from "react-native";
import ReanimatedSwipeable, {
  type SwipeableMethods,
} from "react-native-gesture-handler/ReanimatedSwipeable";
import Animated, { FadeOut, LinearTransition } from "react-native-reanimated";
import { useSafeAreaInsets } from "react-native-safe-area-context";

import { FONT } from "../constants/fonts";
import { useRequireMember } from "../src/features/auth";
import {
  useDeleteNotificationMutation,
  useMarkAllNotificationsReadMutation,
  useMarkNotificationReadMutation,
  useNotificationsQuery,
} from "../src/features/notification";
import type { AppNotification } from "../src/features/notification/types";
import { getApiErrorMessage } from "../src/shared/utils/apiError";

/** 알림 종류별로 열어 줄 화면. 대상이 없으면 목록에 머문다. */
const TARGET_ROUTES: Record<string, string | null> = {
  NONE: null,
  FRIEND: "/(tabs)/friend",
  DAILY_QUIZ: "/(tabs)/home",
  MYPAGE: "/(tabs)/mypage",
  SETTING: "/device-management",
  WEAKNESS_REPORT: "/weakness-report",
};

/** 알림 종류별 아이콘. 목록에 없는 종류가 새로 오면 종 아이콘으로 보여 준다. */
const TYPE_ICONS: Record<string, LucideIcon> = {
  DAILY_QUIZ_OPEN: ListChecks,
  ATTENDANCE_REMINDER: CalendarCheck,
  FRIEND_POKE: Hand,
  FRIEND_REQUEST_RECEIVED: UserPlus,
  FRIEND_REQUEST_ACCEPTED: UserCheck,
  LEARNING_REMINDER: BookOpen,
  LEVEL_UP: Trophy,
  NEW_DEVICE_LOGIN: ShieldAlert,
};

/** 카테고리별 아이콘 색. 같은 묶음의 알림은 같은 색으로 보여 훑어보기 쉽게 한다. */
const CATEGORY_TINTS: Record<string, { color: string; background: string }> = {
  DAILY: { color: "#6D5DFB", background: "#EEEBFF" },
  FRIEND: { color: "#EC4899", background: "#FDECF5" },
  LEARNING: { color: "#0EA5E9", background: "#E5F5FD" },
  REWARD: { color: "#F59E0B", background: "#FEF3DC" },
  SYSTEM: { color: "#64748B", background: "#EEF2F7" },
};

const DELETE_ACTION_WIDTH = 84;

export default function NotificationsScreen() {
  useRequireMember();
  const insets = useSafeAreaInsets();
  const { data: notifications = [], isLoading } = useNotificationsQuery();
  const markRead = useMarkNotificationReadMutation();
  const markAllRead = useMarkAllNotificationsReadMutation();
  const deleteNotification = useDeleteNotificationMutation();
  // 지금 삭제 버튼이 열려 있는 행. 다른 행을 밀기 시작하면 이 행을 닫는다.
  const openRowRef = useRef<RefObject<SwipeableMethods | null> | null>(null);

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

  const handleRowOpenStart = (row: RefObject<SwipeableMethods | null>) => {
    if (openRowRef.current && openRowRef.current !== row) {
      openRowRef.current.current?.close();
    }
    openRowRef.current = row;
  };

  const removeNotification = (notification: AppNotification) => {
    deleteNotification.mutate(notification.notificationId, {
      onError: (error) => Alert.alert("알림 삭제", getApiErrorMessage(error)),
    });
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
        <ScrollView
          contentContainerStyle={[styles.list, { paddingBottom: 16 + insets.bottom }]}
          showsVerticalScrollIndicator={false}
        >
          <Text style={styles.hint}>왼쪽으로 밀면 삭제할 수 있어요</Text>
          {notifications.map((notification) => (
            <Animated.View
              exiting={FadeOut.duration(150)}
              key={notification.notificationId}
              layout={LinearTransition.duration(200)}
            >
              <NotificationRow
                notification={notification}
                onDelete={() => removeNotification(notification)}
                onOpenStart={handleRowOpenStart}
                onPress={() => openNotification(notification)}
              />
            </Animated.View>
          ))}
        </ScrollView>
      )}
    </View>
  );
}

type NotificationRowProps = {
  notification: AppNotification;
  onPress: () => void;
  onDelete: () => void;
  onOpenStart: (row: RefObject<SwipeableMethods | null>) => void;
};

/**
 * 왼쪽으로 밀면 오른쪽에 삭제 버튼이 드러난다. 미는 것만으로는 지워지지 않고 버튼을 눌러야
 * 지워진다(overshootRight 를 꺼서 끝까지 밀어도 버튼 너비에서 멈춘다).
 * 열린 행의 카드를 누르면 알림을 여는 대신 행이 닫힌다(ReanimatedSwipeable 기본 동작).
 */
function NotificationRow({ notification, onPress, onDelete, onOpenStart }: NotificationRowProps) {
  const swipeableRef = useRef<SwipeableMethods>(null);
  const Icon = TYPE_ICONS[notification.notificationType] ?? Bell;
  const tint = CATEGORY_TINTS[notification.category] ?? CATEGORY_TINTS.SYSTEM;
  const unread = !notification.isRead;

  return (
    <ReanimatedSwipeable
      containerStyle={styles.swipeable}
      onSwipeableOpenStartDrag={() => onOpenStart(swipeableRef)}
      overshootRight={false}
      ref={swipeableRef}
      renderRightActions={() => (
        <View style={styles.deleteAction}>
          <Pressable
            accessibilityLabel="알림 삭제"
            accessibilityRole="button"
            onPress={onDelete}
            style={({ pressed }) => [styles.deleteButton, pressed && styles.deleteButtonPressed]}
          >
            <Trash2 color="#FFFFFF" size={18} />
            <Text style={styles.deleteText}>삭제</Text>
          </Pressable>
        </View>
      )}
      rightThreshold={DELETE_ACTION_WIDTH / 2}
    >
      <Pressable
        // 스크린리더는 밀기 제스처를 쓰기 어려워 같은 삭제를 동작 메뉴로도 연다.
        accessibilityActions={[{ name: "delete", label: "삭제" }]}
        onAccessibilityAction={(event) => {
          if (event.nativeEvent.actionName === "delete") {
            onDelete();
          }
        }}
        onPress={onPress}
        style={({ pressed }) => [
          styles.card,
          unread && styles.cardUnread,
          pressed && styles.cardPressed,
        ]}
      >
        <View style={[styles.iconCircle, { backgroundColor: tint.background }]}>
          <Icon color={tint.color} size={20} />
          {unread && <View style={styles.unreadDot} />}
        </View>
        <View style={styles.cardContent}>
          <View style={styles.cardHeader}>
            <Text
              numberOfLines={1}
              style={[styles.cardTitle, unread ? styles.cardTitleUnread : styles.cardTitleRead]}
            >
              {notification.title}
            </Text>
            <Text style={styles.cardTime}>{formatTime(notification.createdAt)}</Text>
          </View>
          <Text
            numberOfLines={2}
            style={[styles.cardBody, unread ? styles.cardBodyUnread : styles.cardBodyRead]}
          >
            {notification.body}
          </Text>
        </View>
      </Pressable>
    </ReanimatedSwipeable>
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
  hint: { color: "#94A3B8", fontFamily: FONT.pretendard, fontSize: 12 },
  // 열렸을 때 삭제 버튼과 카드가 같은 둥근 모서리 안에서 보이도록 바깥에서 잘라 낸다.
  swipeable: { borderRadius: 14 },
  card: {
    flexDirection: "row",
    alignItems: "flex-start",
    gap: 12,
    borderRadius: 14,
    borderWidth: 1,
    borderColor: "#EEF2F7",
    backgroundColor: "#FFFFFF",
    padding: 14,
  },
  cardUnread: { borderColor: "#E4DFFF", backgroundColor: "#F7F5FF" },
  cardPressed: { opacity: 0.7 },
  iconCircle: {
    width: 40,
    height: 40,
    alignItems: "center",
    justifyContent: "center",
    borderRadius: 20,
  },
  unreadDot: {
    position: "absolute",
    top: 0,
    right: 0,
    width: 10,
    height: 10,
    borderWidth: 2,
    borderColor: "#F7F5FF",
    borderRadius: 5,
    backgroundColor: "#6D5DFB",
  },
  cardContent: { flex: 1, gap: 4 },
  cardHeader: { flexDirection: "row", alignItems: "center", gap: 8 },
  cardTitle: { flex: 1, fontFamily: FONT.pretendard, fontSize: 15 },
  cardTitleUnread: { color: "#111827", fontWeight: "700" },
  cardTitleRead: { color: "#334155", fontWeight: "600" },
  cardBody: { fontFamily: FONT.pretendard, fontSize: 13, lineHeight: 19 },
  cardBodyUnread: { color: "#475569" },
  cardBodyRead: { color: "#64748B" },
  cardTime: { color: "#94A3B8", fontFamily: FONT.pretendard, fontSize: 12 },
  deleteAction: { width: DELETE_ACTION_WIDTH, paddingLeft: 8 },
  deleteButton: {
    flex: 1,
    alignItems: "center",
    justifyContent: "center",
    gap: 4,
    borderRadius: 14,
    backgroundColor: "#EF4444",
  },
  deleteButtonPressed: { backgroundColor: "#DC2626" },
  deleteText: { color: "#FFFFFF", fontFamily: FONT.pretendard, fontSize: 13, fontWeight: "700" },
});
