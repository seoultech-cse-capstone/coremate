import { useRef, useState } from "react";
import { Animated, Pressable, StyleSheet, View, type LayoutChangeEvent } from "react-native";

import { AVATAR_STAGE_WIDTH, AvatarStage } from "./AvatarStage";
import { ROOM_GRID_COLS, ROOM_GRID_ROWS, getRoomGridLayout } from "../roomGrid";

// 실제 벽지·바닥 에셋(#165)이 아직 없어서, 방이라는 걸 알아볼 수 있는 최소한의
// 자리만 잡아둔다. 이미지가 정해지면 이 두 영역을 이미지로 바꾸면 된다.
const WALL_COLOR = "#DCD6FB";
const FLOOR_COLOR = "#C9BFE8";
const GRID_LINE_COLOR = "rgba(255, 255, 255, 0.4)";

type RoomSceneProps = {
  height: number;
  /** 격자 안내선 표시 여부. 실제 바닥 에셋이 들어오면 false로 끈다 — 칸 지오메트리 자체는 roomGrid.ts에 고정돼 있으니 영향 없음. */
  showGridLines?: boolean;
  /** 화면에서 미리보기용으로만 다른 칸수를 보고 싶을 때. 생략하면 확정된 10x7(roomGrid.ts) 사용. */
  gridCols?: number;
  gridRows?: number;
};

/** 방(벽+바닥) 위에 아바타를 세워 보여준다. 아바타를 탭하면 살짝 튀어오른다. */
export function RoomScene({
  height,
  showGridLines = true,
  gridCols = ROOM_GRID_COLS,
  gridRows = ROOM_GRID_ROWS,
}: RoomSceneProps) {
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

  const { cellWidth, rowHeight, gridHeight, floorHeight, wallHeight } = getRoomGridLayout(
    width,
    height,
    gridCols,
    gridRows,
  );
  const avatarScale = cellWidth / AVATAR_STAGE_WIDTH;
  const avatarWidth = cellWidth;

  return (
    <View onLayout={onLayout} style={[styles.card, { height }]}>
      <View style={{ height: wallHeight, backgroundColor: WALL_COLOR }} />
      <View style={{ height: floorHeight, backgroundColor: FLOOR_COLOR }}>
        {showGridLines
          ? Array.from({ length: gridCols - 1 }, (_, index) => (
              <View
                key={`col-${index}`}
                style={[
                  styles.gridLineVertical,
                  { left: (index + 1) * cellWidth, height: gridHeight },
                ]}
              />
            ))
          : null}
        {showGridLines
          ? Array.from({ length: gridRows }, (_, index) => (
              <View
                key={`row-${index}`}
                style={[styles.gridLineHorizontal, { top: (index + 1) * rowHeight }]}
              />
            ))
          : null}
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
