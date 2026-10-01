import { X } from "lucide-react-native";
import { Modal, Pressable, StyleSheet, Text, useWindowDimensions, View } from "react-native";

import { FONT } from "../../../../constants/fonts";
import { getPosedFurniture, getRoomHeightShowingWall, RoomSnapshot, type RoomResponse } from "../../avatar";

type FriendRoomModalProps = {
  nickname: string;
  room: RoomResponse;
  visible: boolean;
  onClose: () => void;
};

const OVERLAY_PADDING_X = 24;
const MODAL_PADDING = 20;
// 벽 그림(96도트)의 아래쪽 이만큼을 보여준다. 창문이 위에서 35~71도트 자리에 있어서, 위로 조금
// 여유를 두고 창문이 잘리지 않게 한다. 높이를 고정(290)하면 폭이 좁은 이 상자에서는 벽이 창틀까지만 보였다.
const VISIBLE_WALL_DOTS = 70;

/**
 * 포즈 경과 시간 라벨(SCR-FRIEND-02, 예: "3일 전부터 자는 중"). "3일 전부터"는 서버가 만들고
 * 무엇을 하는지는 여기서 붙인다. 접속 상태가 아니라 마지막으로 포즈를 바꾼 시각이라 "지금"처럼
 * 읽히는 말은 쓰지 않는다. 서 있거나 시각이 없으면 숨긴다.
 */
function getPoseLabel(room: RoomResponse): string | null {
  const posedFurniture = getPosedFurniture(room);
  if (!posedFurniture || !room.roomPoseElapsedText) {
    return null;
  }
  return `${room.roomPoseElapsedText} ${posedFurniture === "bed" ? "자는 중" : "공부하는 중"}`;
}

export function FriendRoomModal({ nickname, room, visible, onClose }: FriendRoomModalProps) {
  const poseLabel = getPoseLabel(room);
  const { width: windowWidth } = useWindowDimensions();
  const roomWidth = windowWidth - OVERLAY_PADDING_X * 2 - MODAL_PADDING * 2;
  const roomHeight = Math.round(getRoomHeightShowingWall(roomWidth, VISIBLE_WALL_DOTS));

  return (
    <Modal animationType="fade" onRequestClose={onClose} transparent visible={visible}>
      <View style={styles.overlay}>
        <View style={styles.modal}>
          <View style={styles.header}>
            <View>
              <Text style={styles.title}>{nickname}{"'s Room"}</Text>
              {poseLabel ? (
                <Text style={styles.poseLabel}>{poseLabel}</Text>
              ) : (
                <Text style={styles.subtitle}>Avatar & Room</Text>
              )}
            </View>
            <Pressable accessibilityLabel="Close" hitSlop={8} onPress={onClose}>
              <X color="#475569" size={22} strokeWidth={2} />
            </Pressable>
          </View>

          <View style={styles.room}>
            <RoomSnapshot height={roomHeight} room={room} />
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
    paddingHorizontal: OVERLAY_PADDING_X,
  },
  modal: {
    borderRadius: 8,
    backgroundColor: "#FFFFFF",
    padding: MODAL_PADDING,
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
  poseLabel: {
    color: "#6D5DFB",
    fontFamily: FONT.pretendard,
    fontSize: 12,
    fontWeight: "600",
    marginTop: 3,
  },
  room: {
    overflow: "hidden",
    marginTop: 18,
    borderRadius: 8,
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
