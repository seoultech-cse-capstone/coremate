import { BlurView } from "expo-blur";
import { router, useLocalSearchParams } from "expo-router";
import { ChevronLeft, Flame, Hand, MapPin } from "lucide-react-native";
import { useRef, useState } from "react";
import { Alert, Pressable, ScrollView, StyleSheet, Text, View } from "react-native";
import { useSafeAreaInsets } from "react-native-safe-area-context";

import { FONT } from "../constants/fonts";
import { FriendAvatar } from "../src/features/friend/components/FriendAvatar";
import { FriendProgressStats } from "../src/features/friend/components/FriendProgressStats";
import { FriendRoomModal } from "../src/features/friend/components/FriendRoomModal";
import { getFriend } from "../src/features/friend/friendData";

const BRAND_PURPLE = "#6D5DFB";
const PANEL_GRAY = "#F3F3F3";
const OVERSCROLL_BUFFER = 96;

export default function FriendProfileScreen() {
  const insets = useSafeAreaInsets();
  const scrollRef = useRef<ScrollView>(null);
  const { friendId } = useLocalSearchParams<{ friendId?: string }>();
  const friend = getFriend(friendId);
  const [isPoked, setIsPoked] = useState(false);
  const [isRoomModalVisible, setIsRoomModalVisible] = useState(false);

  return (
    <View style={styles.container}>
      <BlurView
        intensity={28}
        tint="dark"
        style={[
          styles.titleArea,
          { minHeight: 60 + insets.top, paddingTop: insets.top },
        ]}
      >
        <View style={styles.headerOverlay} />
        <Pressable
          accessibilityLabel="뒤로 가기"
          accessibilityRole="button"
          hitSlop={10}
          onPress={() => router.back()}
          style={[styles.backButton, { top: 9 + insets.top }]}
        >
          <ChevronLeft color="#FFFFFF" size={28} strokeWidth={2.2} />
        </Pressable>
        <Text numberOfLines={1} style={styles.title}>
          {friend.nickname}
        </Text>
      </BlurView>

      <ScrollView
        bounces
        contentContainerStyle={[styles.scrollContent, { paddingTop: 60 + insets.top }]}
        ref={scrollRef}
        showsVerticalScrollIndicator={false}
      >
        <View style={styles.profileArea}>
          <Pressable
            accessibilityLabel={`${friend.nickname}의 방 보기`}
            accessibilityRole="button"
            onPress={() => setIsRoomModalVisible(true)}
            style={styles.avatarButton}
          >
            <FriendAvatar
              backgroundColor={friend.avatarColor}
              color={friend.avatarAccentColor}
              size={90}
            />
            <View
              style={[
                styles.onlineIndicator,
                friend.isOnline ? styles.online : styles.offline,
              ]}
            />
          </Pressable>
          <Text style={styles.nickname}>{friend.nickname}</Text>
          <View style={styles.schoolRow}>
            <MapPin color="#E9E3FF" size={15} strokeWidth={2} />
            <Text style={styles.school}>
              {friend.school} · {friend.department}
            </Text>
          </View>
          <View style={styles.summaryRow}>
            <View style={styles.summaryItem}>
              <Text style={styles.summaryValue}>Lv.{friend.level}</Text>
              <Text style={styles.summaryLabel}>Level</Text>
            </View>
            <View style={styles.summaryDivider} />
            <View style={styles.summaryItem}>
              <View style={styles.streakValueRow}>
                <Flame color="#FDBA31" fill="#FDBA31" size={17} />
                <Text style={styles.summaryValue}>{friend.streak}</Text>
              </View>
              <Text style={styles.summaryLabel}>Day Streak</Text>
            </View>
            <View style={styles.summaryDivider} />
            <View style={styles.summaryItem}>
              <Text style={styles.summaryValue}>O</Text>
              <Text style={styles.summaryLabel}>Today</Text>
            </View>
          </View>
        </View>

        <View style={styles.panel}>
          <View style={styles.panelContent}>
            <FriendProgressStats progress={friend.progress} />
            <View style={styles.attendanceCard}>
              <Text style={styles.attendanceTitle}>Attendance</Text>
              <Text style={styles.attendanceText}>{friend.attendance}</Text>
            </View>
            <View style={styles.learningCard}>
              <Text style={styles.learningTitle}>Learning History</Text>
              <View style={styles.learningStats}>
                <LearningStat label="Study Time" value="36h 20m" />
                <View style={styles.learningDivider} />
                <LearningStat label="Questions" value="318" />
                <View style={styles.learningDivider} />
                <LearningStat label="Completed" value="27" />
              </View>
            </View>
            <Pressable
              accessibilityRole="button"
              onPress={() => setIsPoked((current) => !current)}
              style={[styles.pokeButton, isPoked && styles.pokeButtonActive]}
            >
              <Hand
                color={isPoked ? "#FFFFFF" : BRAND_PURPLE}
                fill={isPoked ? "#FFFFFF" : "transparent"}
                size={19}
                strokeWidth={2.1}
              />
              <Text style={[styles.pokeText, isPoked && styles.pokeTextActive]}>
                {isPoked ? "콕 찌름 완료" : "콕 찌르기"}
              </Text>
            </Pressable>
            <Pressable
              accessibilityRole="button"
              onPress={() => {
                Alert.alert(
                  "친구 삭제",
                  `${friend.nickname} 님을 친구 목록에서 삭제할까요?`,
                  [
                    { text: "취소", style: "cancel" },
                    {
                      text: "삭제",
                      style: "destructive",
                      onPress: () => router.back(),
                    },
                  ],
                );
              }}
              style={styles.deleteButton}
            >
              <Text style={styles.deleteButtonText}>친구 삭제</Text>
            </Pressable>
          </View>
        </View>
      </ScrollView>
      <FriendRoomModal
        friend={friend}
        onClose={() => setIsRoomModalVisible(false)}
        visible={isRoomModalVisible}
      />
    </View>
  );
}

function LearningStat({ label, value }: { label: string; value: string }) {
  return (
    <View style={styles.learningStat}>
      <Text style={styles.learningValue}>{value}</Text>
      <Text style={styles.learningLabel}>{label}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: BRAND_PURPLE },
  titleArea: {
    position: "absolute",
    top: 0,
    right: 0,
    left: 0,
    zIndex: 10,
    minHeight: 60,
    alignItems: "center",
    justifyContent: "center",
    overflow: "hidden",
  },
  headerOverlay: {
    ...StyleSheet.absoluteFillObject,
    backgroundColor: "rgba(109, 93, 251, 0.85)",
  },
  backButton: {
    position: "absolute",
    left: 10,
    width: 42,
    height: 42,
    alignItems: "center",
    justifyContent: "center",
    zIndex: 1,
  },
  title: {
    color: "#FFFFFF",
    fontFamily: FONT.pretendard,
    fontSize: 19,
    fontWeight: "700",
  },
  scrollContent: { backgroundColor: BRAND_PURPLE },
  profileArea: {
    alignItems: "center",
    gap: 8,
    paddingHorizontal: 24,
    paddingTop: 18,
    paddingBottom: 30,
  },
  avatarButton: {
    position: "relative",
  },
  onlineIndicator: {
    position: "absolute",
    right: 1,
    bottom: 1,
    width: 18,
    height: 18,
    borderWidth: 3,
    borderColor: BRAND_PURPLE,
    borderRadius: 9,
  },
  online: { backgroundColor: "#22C55E" },
  offline: { backgroundColor: "#94A3B8" },
  nickname: {
    marginTop: 4,
    color: "#FFFFFF",
    fontFamily: FONT.pretendard,
    fontSize: 22,
    fontWeight: "700",
  },
  schoolRow: { flexDirection: "row", alignItems: "center", gap: 4 },
  school: {
    color: "#F1EEFF",
    fontFamily: FONT.pretendard,
    fontSize: 13,
  },
  summaryRow: {
    width: "100%",
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-evenly",
    marginTop: 12,
  },
  summaryItem: { flex: 1, alignItems: "center", gap: 4 },
  summaryDivider: { width: 1, height: 30, backgroundColor: "rgba(255,255,255,0.32)" },
  summaryValue: {
    color: "#FFFFFF",
    fontFamily: FONT.kufam,
    fontSize: 15,
    fontWeight: "600",
  },
  streakValueRow: { flexDirection: "row", alignItems: "center", gap: 3 },
  summaryLabel: {
    color: "#E9E3FF",
    fontFamily: FONT.kufam,
    fontSize: 10,
  },
  panel: {
    width: "100%",
    borderTopLeftRadius: 28,
    borderTopRightRadius: 28,
    backgroundColor: PANEL_GRAY,
    overflow: "hidden",
  },
  panelContent: {
    gap: 22,
    backgroundColor: PANEL_GRAY,
    paddingHorizontal: 24,
    paddingTop: 32,
    paddingBottom: 28 + OVERSCROLL_BUFFER,
  },
  attendanceCard: {
    borderRadius: 8,
    backgroundColor: "#FFFFFF",
    paddingHorizontal: 18,
    paddingVertical: 16,
    gap: 5,
  },
  attendanceTitle: {
    color: "#1F2937",
    fontFamily: FONT.kufam,
    fontSize: 14,
    fontWeight: "600",
  },
  attendanceText: {
    color: "#64748B",
    fontFamily: FONT.pretendard,
    fontSize: 13,
  },
  learningCard: {
    borderRadius: 8,
    backgroundColor: "#FFFFFF",
    paddingHorizontal: 18,
    paddingVertical: 16,
    gap: 15,
  },
  learningTitle: {
    color: "#1F2937",
    fontFamily: FONT.kufam,
    fontSize: 14,
    fontWeight: "600",
  },
  learningStats: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
  },
  learningStat: {
    flex: 1,
    alignItems: "center",
    gap: 5,
  },
  learningValue: {
    color: BRAND_PURPLE,
    fontFamily: FONT.kufam,
    fontSize: 14,
    fontWeight: "600",
  },
  learningLabel: {
    color: "#94A3B8",
    fontFamily: FONT.kufam,
    fontSize: 10,
  },
  learningDivider: {
    width: 1,
    height: 28,
    backgroundColor: "#E2E8F0",
  },
  pokeButton: {
    height: 50,
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "center",
    gap: 8,
    borderWidth: 1,
    borderColor: BRAND_PURPLE,
    borderRadius: 8,
    backgroundColor: "#FFFFFF",
  },
  pokeButtonActive: { backgroundColor: BRAND_PURPLE },
  pokeText: {
    color: BRAND_PURPLE,
    fontFamily: FONT.pretendard,
    fontSize: 15,
    fontWeight: "600",
  },
  pokeTextActive: { color: "#FFFFFF" },
  deleteButton: {
    height: 48,
    alignItems: "center",
    justifyContent: "center",
    borderWidth: 1,
    borderColor: "#FCA5A5",
    borderRadius: 8,
    backgroundColor: "#FFF7F7",
  },
  deleteButtonText: {
    color: "#DC2626",
    fontFamily: FONT.pretendard,
    fontSize: 14,
    fontWeight: "600",
  },
});
