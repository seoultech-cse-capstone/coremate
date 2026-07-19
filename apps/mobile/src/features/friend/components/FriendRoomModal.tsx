import { Armchair, BookOpen, Lamp, Sparkles, X } from "lucide-react-native";
import { Modal, Pressable, StyleSheet, Text, View } from "react-native";

import { FONT } from "../../../../constants/fonts";
import type { Friend } from "../friendData";
import { FriendAvatar } from "./FriendAvatar";

type FriendRoomModalProps = {
  friend: Friend;
  visible: boolean;
  onClose: () => void;
};

export function FriendRoomModal({ friend, visible, onClose }: FriendRoomModalProps) {
  return (
    <Modal animationType="fade" onRequestClose={onClose} transparent visible={visible}>
      <View style={styles.overlay}>
        <View style={styles.modal}>
          <View style={styles.header}>
            <View>
              <Text style={styles.title}>{friend.nickname}{"'s Room"}</Text>
              <Text style={styles.subtitle}>Avatar & Room</Text>
            </View>
            <Pressable accessibilityLabel="Close" hitSlop={8} onPress={onClose}>
              <X color="#475569" size={22} strokeWidth={2} />
            </Pressable>
          </View>

          <View style={styles.room}>
            <View style={styles.window}>
              <View style={styles.windowDividerVertical} />
              <View style={styles.windowDividerHorizontal} />
            </View>
            <View style={styles.poster}>
              <Sparkles color="#6D5DFB" size={22} strokeWidth={1.9} />
            </View>
            <View style={styles.lamp}>
              <Lamp color="#F59E0B" size={31} strokeWidth={1.8} />
            </View>
            <View style={styles.bookShelf}>
              <BookOpen color="#FFFFFF" size={22} strokeWidth={1.9} />
            </View>
            <View style={styles.chair}>
              <Armchair color="#6D5DFB" size={34} strokeWidth={1.8} />
            </View>
            <View style={styles.avatarSpot}>
              <FriendAvatar
                backgroundColor={friend.avatarColor}
                color={friend.avatarAccentColor}
                size={74}
              />
              <Text style={styles.avatarName}>{friend.nickname}</Text>
            </View>
            <View style={styles.floorLine} />
          </View>

          <View style={styles.note}>
            <Text style={styles.noteText}>친구가 꾸민 학습 공간입니다.</Text>
          </View>
        </View>
      </View>
    </Modal>
  );
}

const styles = StyleSheet.create({
  overlay: {
    flex: 1,
    justifyContent: "center",
    backgroundColor: "rgba(15, 23, 42, 0.48)",
    paddingHorizontal: 24,
  },
  modal: {
    borderRadius: 8,
    backgroundColor: "#FFFFFF",
    padding: 20,
  },
  header: {
    flexDirection: "row",
    alignItems: "flex-start",
    justifyContent: "space-between",
  },
  title: {
    color: "#1F2937",
    fontFamily: FONT.kufam,
    fontSize: 18,
    fontWeight: "600",
  },
  subtitle: {
    color: "#94A3B8",
    fontFamily: FONT.kufam,
    fontSize: 11,
    marginTop: 3,
  },
  room: {
    height: 290,
    position: "relative",
    overflow: "hidden",
    marginTop: 18,
    borderRadius: 8,
    backgroundColor: "#EDEAFF",
  },
  window: {
    position: "absolute",
    top: 24,
    left: 26,
    width: 70,
    height: 76,
    borderWidth: 5,
    borderColor: "#FFFFFF",
    borderRadius: 3,
    backgroundColor: "#BCE2FF",
  },
  windowDividerVertical: {
    position: "absolute",
    top: -5,
    bottom: -5,
    left: 30,
    width: 5,
    backgroundColor: "#FFFFFF",
  },
  windowDividerHorizontal: {
    position: "absolute",
    right: -5,
    bottom: 31,
    left: -5,
    height: 5,
    backgroundColor: "#FFFFFF",
  },
  poster: {
    position: "absolute",
    top: 36,
    right: 31,
    width: 52,
    height: 64,
    alignItems: "center",
    justifyContent: "center",
    borderRadius: 3,
    backgroundColor: "#FFE5E5",
  },
  lamp: { position: "absolute", bottom: 70, left: 22 },
  bookShelf: {
    position: "absolute",
    right: 25,
    bottom: 68,
    width: 55,
    height: 48,
    alignItems: "center",
    justifyContent: "center",
    borderRadius: 3,
    backgroundColor: "#8D7FDC",
  },
  chair: { position: "absolute", bottom: 34, left: 88 },
  avatarSpot: {
    position: "absolute",
    bottom: 31,
    right: 83,
    alignItems: "center",
    gap: 5,
  },
  avatarName: {
    color: "#475569",
    fontFamily: FONT.pretendard,
    fontSize: 11,
    fontWeight: "600",
  },
  floorLine: {
    position: "absolute",
    right: 0,
    bottom: 22,
    left: 0,
    height: 10,
    backgroundColor: "#D6D0EE",
  },
  note: {
    alignItems: "center",
    paddingTop: 14,
  },
  noteText: {
    color: "#64748B",
    fontFamily: FONT.pretendard,
    fontSize: 12,
  },
});
