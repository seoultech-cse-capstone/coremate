import { BlurView } from "expo-blur";
import { router, useLocalSearchParams } from "expo-router";
import { ChevronLeft, Flame, Hand, MapPin } from "lucide-react-native";
import { useRef, useState } from "react";
import { Pressable, ScrollView, StyleSheet, Text, View } from "react-native";
import { useSafeAreaInsets } from "react-native-safe-area-context";

import { FONT } from "../constants/fonts";
import { FriendAvatar } from "../src/features/friend/components/FriendAvatar";
import { FriendProgressStats } from "../src/features/friend/components/FriendProgressStats";
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
          <FriendAvatar
            backgroundColor={friend.avatarColor}
            color={friend.avatarAccentColor}
            size={90}
          />
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
          </View>
        </View>
      </ScrollView>
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
});
