import { useEffect, useState, type ReactNode } from "react";
import {
  Image,
  Pressable,
  StyleSheet,
  View,
  type GestureResponderEvent,
  type LayoutChangeEvent,
} from "react-native";

import { ROOM_GRID_COLS, ROOM_GRID_ROWS, getRoomGridLayout, type RoomGridLayout } from "../roomGrid";
import { useRoomTheme } from "../RoomThemeContext";
import { FLOOR_DOT_HEIGHT, WALL_DOT_HEIGHT, WALL_EXTEND_COLOR } from "../roomThemes";
import { useRoomFurniture, type FurniturePosition } from "../RoomFurnitureContext";
import { FURNITURE_TYPES, getFurnitureLayout, type FurnitureType } from "../roomFurniture";

const GRID_LINE_COLOR = "rgba(255, 255, 255, 0.4)";

export type RoomLayoutInfo = RoomGridLayout & { width: number };

type RoomBackdropProps = {
  height: number;
  /** 바닥 쪽으로만 추가되는 여분 높이(패널 모서리 겹침 등). RoomScene 참고. */
  bottomOverflow?: number;
  showGridLines?: boolean;
  /** 배치된 가구(침대·책상)를 같이 그릴지. 마이페이지 미리보기에서도 재사용. */
  showFurniture?: boolean;
  onFloorPress?: (event: GestureResponderEvent) => void;
  /** 폭이 측정될 때마다(및 height 변경 시) 격자 치수를 올려보낸다. */
  onLayoutChange?: (layout: RoomLayoutInfo) => void;
  /** 벽·바닥 위, 크롭 없이 그려지는 오버레이(아바타 등). 바닥과 같은 좌표계. */
  overlay?: ReactNode;
};

/**
 * 가구 한 세트에서 role(기본 "back"/"front")이 일치하는 조각만 그린다. 책상의
 * 책상 상판·침대의 이불처럼 늘 아바타보다 앞이어야 하는 조각(front)과, 의자·침대
 * 프레임처럼 늘 뒤여야 하는 조각(back)을 나눠서 — 상호작용 여부와 무관하게 항상
 * 같은 순서로 그린다. 그래야 앉거나 누울 때 가구 z가 튀어 보이지 않는다.
 */
function FurniturePieces({
  type,
  position,
  cellWidth,
  rowHeight,
  role,
}: {
  type: FurnitureType;
  position: FurniturePosition;
  cellWidth: number;
  rowHeight: number;
  role: "back" | "front";
}) {
  const { pieces } = getFurnitureLayout(type, position, cellWidth, rowHeight);

  return (
    <>
      {pieces
        .filter((piece) => piece.role === role)
        .map((piece, index) => (
          <Image
            key={index}
            resizeMode="stretch"
            source={piece.source}
            style={{
              position: "absolute",
              left: piece.left,
              top: piece.top,
              width: piece.width,
              height: piece.height,
            }}
          />
        ))}
    </>
  );
}

/**
 * 방(벽+바닥+격자선+배치된 가구)을 그린다. 아바타/터치 상호작용은 호출부(RoomScene,
 * 가구 배치 화면)가 각자 다르게 넣으므로 overlay·onFloorPress로 받는다.
 */
export function RoomBackdrop({
  height,
  bottomOverflow = 0,
  showGridLines = true,
  showFurniture = true,
  onFloorPress,
  onLayoutChange,
  overlay,
}: RoomBackdropProps) {
  const { theme } = useRoomTheme();
  const { placements } = useRoomFurniture();
  const [width, setWidth] = useState(0);

  // 배치된 가구를 격자 줄(row) 기준으로 정렬 — 가구끼리는 이 순서로 뒤→앞이 겹친다.
  // 상호작용(앉기/눕기) 여부와 무관하게 항상 같은 순서다: 가구의 z는 고정, 아바타가
  // back(의자·침대 프레임)과 front(책상 상판·이불) 사이로 섞여 들어간다.
  const orderedFurnitureTypes = FURNITURE_TYPES.filter((type) => placements[type]).sort(
    (a, b) => placements[a]!.row - placements[b]!.row,
  );

  const onLayout = (event: LayoutChangeEvent) => {
    setWidth(event.nativeEvent.layout.width);
  };

  const layout = getRoomGridLayout(width, height);
  const { cellWidth, rowHeight, gridHeight, floorHeight: baseFloorHeight, wallHeight, dotPx } = layout;
  const floorHeight = baseFloorHeight + bottomOverflow;

  useEffect(() => {
    if (width > 0) {
      onLayoutChange?.({ ...layout, width });
    }
    // layout은 width/height/bottomOverflow에서 매번 새로 계산되는 순수 값이라
    // 얕은 비교 없이 그 세 값만 의존성으로 둔다.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [width, height, bottomOverflow]);

  return (
    <View onLayout={onLayout} style={[styles.card, { height: height + bottomOverflow }]}>
      {/* 벽: 벽 경계(바닥 시작선)에 아래를 맞추고, 위로 모자라면 벽 상단색으로 이어붙인다. */}
      <View style={{ height: wallHeight, backgroundColor: WALL_EXTEND_COLOR, overflow: "hidden" }}>
        <Image
          resizeMode="stretch"
          source={theme.wallSource}
          style={{
            position: "absolute",
            bottom: 0,
            left: 0,
            width: "100%",
            height: WALL_DOT_HEIGHT * dotPx,
          }}
        />
      </View>
      <Pressable
        accessibilityLabel="방 바닥"
        style={{ height: floorHeight, overflow: "hidden" }}
        onPress={onFloorPress}
      >
        {/* 바닥: 위(벽 경계)를 기준으로 맞추고, 아래로 넘치는 부분은 잘라낸다. */}
        <Image
          resizeMode="stretch"
          source={theme.floorSource}
          style={{
            position: "absolute",
            top: 0,
            left: 0,
            width: "100%",
            height: FLOOR_DOT_HEIGHT * dotPx,
          }}
        />
        {showGridLines
          ? Array.from({ length: ROOM_GRID_COLS - 1 }, (_, index) => (
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
          ? Array.from({ length: ROOM_GRID_ROWS }, (_, index) => (
              <View
                key={`row-${index}`}
                style={[styles.gridLineHorizontal, { top: (index + 1) * rowHeight }]}
              />
            ))
          : null}
      </Pressable>

      {/* 가구·아바타는 바닥 Pressable 밖의 별도 레이어다 — 바닥은 크롭(overflow hidden)이
          걸려 있어서, 안에 있으면 벽 쪽으로 살짝 넘어갈 때 잘려 보인다. */}
      {width > 0 ? (
        <View pointerEvents="none" style={[styles.overlayLayer, { top: wallHeight, height: floorHeight }]}>
          {/* back 조각(의자·침대 프레임)은 항상 아바타보다 뒤. 가구끼리는 격자 줄(row)이
              작을수록 먼저 그려서 줄이 큰 쪽이 자연스럽게 위에 겹치도록 정렬한다. */}
          {showFurniture
            ? orderedFurnitureTypes.map((type) => (
                <FurniturePieces
                  key={`${type}-back`}
                  cellWidth={cellWidth}
                  position={placements[type]!}
                  role="back"
                  rowHeight={rowHeight}
                  type={type}
                />
              ))
            : null}
          {overlay}
          {/* front 조각(책상 상판·이불)은 항상 아바타보다 앞 — 상호작용 여부와 무관하게
              고정이라, 앉거나 누울 때 가구 z가 튀지 않고 아바타가 이 사이로 섞여 들어간다. */}
          {showFurniture
            ? orderedFurnitureTypes.map((type) => (
                <FurniturePieces
                  key={`${type}-front`}
                  cellWidth={cellWidth}
                  position={placements[type]!}
                  role="front"
                  rowHeight={rowHeight}
                  type={type}
                />
              ))
            : null}
        </View>
      ) : null}
    </View>
  );
}

const styles = StyleSheet.create({
  card: {
    width: "100%",
  },
  overlayLayer: {
    position: "absolute",
    left: 0,
    right: 0,
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
