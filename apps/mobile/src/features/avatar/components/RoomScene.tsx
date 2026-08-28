import { useRef, useState } from "react";
import { Animated, Pressable, StyleSheet, View, type LayoutChangeEvent } from "react-native";

import { AVATAR_STAGE_WIDTH, AvatarStage } from "./AvatarStage";

// 실제 벽지·바닥 에셋(#165)이 아직 없어서, 방이라는 걸 알아볼 수 있는 최소한의
// 자리만 잡아둔다. 이미지가 정해지면 이 두 영역을 이미지로 바꾸면 된다.
const WALL_COLOR = "#DCD6FB";
const FLOOR_COLOR = "#C9BFE8";
const GRID_LINE_COLOR = "rgba(255, 255, 255, 0.4)";

// 확정된 방 격자: 가로 10 × 세로 7(#165). 한 칸 크기를 아바타 폭 기준으로 잡아서,
// 바닥 가로에 아바타가 대략 10개, 세로에 7개 들어가는 크기로 맞춘다.
const GRID_COLS = 10;
const GRID_ROWS = 7;

type RoomSceneProps = {
  height: number;
};

/** 방(벽+바닥) 위에 아바타를 세워 보여준다. 아바타를 탭하면 살짝 튀어오른다. */
export function RoomScene({ height }: RoomSceneProps) {
  const [width, setWidth] = useState(0);
  const bounce = useRef(new Animated.Value(0)).current;

  const onLayout = (event: LayoutChangeEvent) => {
    setWidth(event.nativeEvent.layout.width);
  };

  const handlePressAvatar = () => {
    Animated.sequence([
      Animated.timing(bounce, { toValue: -16, duration: 120, useNativeDriver: true }),
      Animated.spring(bounce, { toValue: 0, useNativeDriver: true, friction: 4, tension: 80 }),
    ]).start();
  };

  const cellSize = width / GRID_COLS;
  const floorHeight = Math.min(height, cellSize * GRID_ROWS);
  const wallHeight = height - floorHeight;
  const avatarScale = cellSize / AVATAR_STAGE_WIDTH;
  const avatarWidth = cellSize;

  return (
    <View onLayout={onLayout} style={[styles.card, { height }]}>
      <View style={{ height: wallHeight, backgroundColor: WALL_COLOR }} />
      <View style={{ height: floorHeight, backgroundColor: FLOOR_COLOR }}>
        {Array.from({ length: GRID_COLS - 1 }, (_, index) => (
          <View
            key={`col-${index}`}
            style={[styles.gridLineVertical, { left: (index + 1) * cellSize }]}
          />
        ))}
        {Array.from({ length: GRID_ROWS - 1 }, (_, index) => (
          <View
            key={`row-${index}`}
            style={[styles.gridLineHorizontal, { top: (index + 1) * cellSize }]}
          />
        ))}
      </View>

      {width > 0 ? (
        <Pressable
          accessibilityLabel="내 아바타"
          accessibilityRole="button"
          onPress={handlePressAvatar}
          style={[
            styles.avatarWrap,
            { bottom: floorHeight * 0.35, transform: [{ translateX: -avatarWidth / 2 }] },
          ]}
        >
          <Animated.View style={{ transform: [{ translateY: bounce }] }}>
            <AvatarStage scale={avatarScale} />
          </Animated.View>
        </Pressable>
      ) : null}
    </View>
  );
}

const styles = StyleSheet.create({
  card: {
    width: "100%",
  },
  avatarWrap: {
    position: "absolute",
    left: "50%",
  },
  gridLineVertical: {
    position: "absolute",
    top: 0,
    bottom: 0,
    width: 1,
    backgroundColor: GRID_LINE_COLOR,
  },
  gridLineHorizontal: {
    position: "absolute",
    left: 0,
    right: 0,
    height: 1,
    backgroundColor: GRID_LINE_COLOR,
  },
});
