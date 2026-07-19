import { Check, X } from "lucide-react-native";
import { Pressable, StyleSheet, Text, View } from "react-native";

import { FONT } from "../../../../constants/fonts";
import type { Friend } from "../friendData";
import { FriendAvatar } from "./FriendAvatar";

type FriendRequestCardProps = {
  friend: Friend;
  onAccept: () => void;
  onDecline: () => void;
};

export function FriendRequestCard({
  friend,
  onAccept,
  onDecline,
}: FriendRequestCardProps) {
  return (
    <View style={styles.card}>
      <FriendAvatar
        backgroundColor={friend.avatarColor}
        color={friend.avatarAccentColor}
      />
      <View style={styles.info}>
        <Text numberOfLines={1} style={styles.name}>
          {friend.nickname}
        </Text>
        <Text numberOfLines={1} style={styles.detail}>
          {friend.school} · {friend.department}
        </Text>
      </View>
      <Pressable accessibilityLabel="요청 거절" hitSlop={8} onPress={onDecline} style={styles.decline}>
        <X color="#64748B" size={18} strokeWidth={2.2} />
      </Pressable>
      <Pressable accessibilityLabel="요청 수락" onPress={onAccept} style={styles.accept}>
        <Check color="#FFFFFF" size={16} strokeWidth={2.5} />
      </Pressable>
    </View>
  );
}

const styles = StyleSheet.create({
  card: {
    minHeight: 72,
    flexDirection: "row",
    alignItems: "center",
    gap: 10,
    borderRadius: 8,
    backgroundColor: "#FFFFFF",
    paddingHorizontal: 13,
    paddingVertical: 12,
    shadowColor: "#64748B",
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.08,
    shadowRadius: 5,
    elevation: 2,
  },
  info: { flex: 1, gap: 3 },
  name: {
    color: "#1F2937",
    fontFamily: FONT.pretendard,
    fontSize: 14,
    fontWeight: "600",
  },
  detail: { color: "#94A3B8", fontFamily: FONT.pretendard, fontSize: 11 },
  decline: { width: 30, height: 30, alignItems: "center", justifyContent: "center" },
  accept: {
    width: 32,
    height: 32,
    alignItems: "center",
    justifyContent: "center",
    borderRadius: 8,
    backgroundColor: "#6D5DFB",
  },
});
