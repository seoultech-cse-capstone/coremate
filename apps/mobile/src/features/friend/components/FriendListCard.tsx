import { Flame, Hand, ChevronRight } from "lucide-react-native";
import { Pressable, StyleSheet, Text, View } from "react-native";

import { FONT } from "../../../../constants/fonts";
import { getAvatarColor } from "../avatarColor";
import type { Friend } from "../types";
import { FriendAvatar } from "./FriendAvatar";

type FriendListCardProps = {
  friend: Friend;
  isPoked: boolean;
  /** 오늘 콕 찌르기 한도를 다 쓴 상태. 버튼을 눌러도 서버가 막으므로 미리 비활성화한다. */
  isPokeDisabled: boolean;
  onPress: () => void;
  onPoke: () => void;
};

export function FriendListCard({
  friend,
  isPoked,
  isPokeDisabled,
  onPress,
  onPoke,
}: FriendListCardProps) {
  const avatarColor = getAvatarColor(friend.nickname);

  return (
    <Pressable
      accessibilityRole="button"
      accessibilityLabel={`${friend.nickname} 프로필 보기`}
      onPress={onPress}
      style={({ pressed }) => [styles.card, pressed && styles.cardPressed]}
    >
      <FriendAvatar
        backgroundColor={avatarColor.background}
        color={avatarColor.accent}
      />

      <View style={styles.info}>
        <Text numberOfLines={1} style={styles.nickname}>
          {friend.nickname}
        </Text>
        <Text style={styles.level}>Level {friend.level ?? 1}</Text>
      </View>

      <View style={styles.streak}>
        <Flame color="#F59E0B" fill="#F59E0B" size={14} strokeWidth={2} />
        <Text style={styles.streakText}>{friend.attendanceCount}일</Text>
      </View>

      <Pressable
        accessibilityRole="button"
        accessibilityLabel={`${friend.nickname}에게 콕 찌르기`}
        accessibilityState={{ disabled: isPokeDisabled }}
        disabled={isPokeDisabled}
        hitSlop={8}
        onPress={(event) => {
          event.stopPropagation();
          onPoke();
        }}
        style={[styles.pokeButton, isPokeDisabled && styles.pokeButtonDisabled]}
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
  pokeButtonDisabled: {
    opacity: 0.35,
  },
  pokeButton: {
    width: 28,
    height: 28,
    alignItems: "center",
    justifyContent: "center",
  },
});
