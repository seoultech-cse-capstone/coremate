import { Flame, Hand, ChevronRight } from "lucide-react-native";
import { Pressable, StyleSheet, Text, View } from "react-native";

import { FONT } from "../../../../constants/fonts";
import type { Friend } from "../friendData";
import { FriendAvatar } from "./FriendAvatar";

type FriendListCardProps = {
  friend: Friend;
  isPoked: boolean;
  onPress: () => void;
  onPoke: () => void;
};

export function FriendListCard({
  friend,
  isPoked,
  onPress,
  onPoke,
}: FriendListCardProps) {
  return (
    <Pressable
      accessibilityRole="button"
      accessibilityLabel={`${friend.nickname} 프로필 보기`}
      onPress={onPress}
      style={({ pressed }) => [styles.card, pressed && styles.cardPressed]}
    >
      <FriendAvatar
        backgroundColor={friend.avatarColor}
        color={friend.avatarAccentColor}
      />

      <View style={styles.info}>
        <Text numberOfLines={1} style={styles.nickname}>
          {friend.nickname}
        </Text>
        <Text style={styles.level}>Level {friend.level}</Text>
      </View>

      <View style={styles.streak}>
        <Flame color="#F59E0B" fill="#F59E0B" size={14} strokeWidth={2} />
        <Text style={styles.streakText}>{friend.streak} days</Text>
      </View>

      <Pressable
        accessibilityRole="button"
        accessibilityLabel={`${friend.nickname}에게 콕 찌르기`}
        hitSlop={8}
        onPress={(event) => {
          event.stopPropagation();
          onPoke();
        }}
        style={styles.pokeButton}
      >
        <Hand
          color={isPoked ? "#6D5DFB" : "#94A3B8"}
          fill={isPoked ? "#E9E3FF" : "transparent"}
          size={17}
          strokeWidth={2}
        />
      </Pressable>

      <ChevronRight color="#94A3B8" size={18} strokeWidth={2.1} />
    </Pressable>
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
  cardPressed: {
    opacity: 0.78,
  },
  info: {
    flex: 1,
    gap: 2,
  },
  nickname: {
    color: "#1F2937",
    fontFamily: FONT.pretendard,
    fontSize: 15,
    fontWeight: "600",
  },
  level: {
    color: "#94A3B8",
    fontFamily: FONT.kufam,
    fontSize: 10,
  },
  streak: {
    flexDirection: "row",
    alignItems: "center",
    gap: 3,
  },
  streakText: {
    color: "#64748B",
    fontFamily: FONT.kufam,
    fontSize: 10,
  },
  pokeButton: {
    width: 28,
    height: 28,
    alignItems: "center",
    justifyContent: "center",
  },
});
