import { useEffect, useRef, useState } from "react";
import { Animated, Easing, StyleSheet, View, type GestureResponderEvent } from "react-native";

import {
  AVATAR_FOOT_X_RATIO,
  AVATAR_FOOT_Y_RATIO,
  AVATAR_SLEEP_STAGE_HEIGHT,
  AVATAR_SLEEP_STAGE_WIDTH,
  AVATAR_STAGE_HEIGHT,
  AVATAR_STAGE_WIDTH,
  AvatarSleepStage,
  AvatarStage,
} from "./AvatarStage";
import { RoomBackdrop, type RoomLayoutInfo } from "./RoomBackdrop";
import { DEFAULT_LOCAL_KEY_BY_SLOT } from "../localCatalog";
import { useRoomFurniture } from "../RoomFurnitureContext";
import {
  FURNITURE_TYPES,
  getFurnitureFootprintRect,
  getFurnitureInteractionAnchor,
  getFurnitureLayout,
  type FurnitureType,
} from "../roomFurniture";
import { useFrameSequence } from "../useFrameSequence";

// 아바타 너비 기준: 타일 몇 칸 크기로 보일지.
const AVATAR_WIDTH_IN_CELLS = 1;

// AVATAR_FE.md 2절: 서기(앞) idle은 1→2→3→2 루프, 프레임별 duration이 다르다.
const IDLE_SEQUENCE: { value: 1 | 2 | 3; duration: number }[] = [
  { value: 1, duration: 400 },
  { value: 2, duration: 150 },
  { value: 3, duration: 400 },
  { value: 2, duration: 150 },
];
// 책상 앉기: 타이핑 손 오버레이 1→2→1→3 루프, 120ms 균일.
const SIT_HAND_SEQUENCE: { value: 1 | 2 | 3; duration: number }[] = [
  { value: 1, duration: 120 },
  { value: 2, duration: 120 },
  { value: 1, duration: 120 },
  { value: 3, duration: 120 },
];
// 수면: 1→2→3→2 루프(원래 스펙은 4프레임이지만 에셋이 3장이라 idle과 같은 방식으로 재사용).
const SLEEP_SEQUENCE: { value: 1 | 2 | 3; duration: number }[] = [
  { value: 1, duration: 400 },
  { value: 2, duration: 250 },
  { value: 3, duration: 550 },
  { value: 2, duration: 250 },
];

// AVATAR_FE.md 6절: 이동 중 offsetY = -|sin(t)| x 진폭(2~3도트), 주기 ~250ms.
const HOP_PERIOD_MS = 250;
const HOP_AMPLITUDE_PX = 12;
const WALK_SPEED_PX_PER_SEC = 220;
const MIN_WALK_DURATION_MS = 300;
// 수면 캔버스(384x216, 48x27도트)는 원본이 1도트=8px라, 세트 배율(도트당 실제 px)을
// 그 8로 나눠야 AvatarSleepStage의 scale prop과 단위가 맞는다.
const SLEEP_RAW_PX_PER_DOT = 8;

function isWithinRect(
  rect: { left: number; top: number; right: number; bottom: number },
  x: number,
  y: number,
): boolean {
  return x >= rect.left && x < rect.right && y >= rect.top && y < rect.bottom;
}

type RoomSceneProps = {
  height: number;
  /**
   * 바닥 쪽으로만 추가되는 여분 높이. 홈 화면에서 방을 패널 시작선보다 더 길게
   * 그려서 패널 둥근 모서리 안쪽으로 바닥이 살짝 보이게 할 때 쓴다 — height를
   * 그냥 늘리면 늘어난 만큼 벽이 커지므로, 늘려야 하는 건 바닥 쪽이라 따로 받는다.
   */
  bottomOverflow?: number;
  /** 격자 안내선 표시 여부. 실제 바닥 에셋이 들어오면 false로 끈다 — 칸 지오메트리 자체는 roomGrid.ts에 고정돼 있으니 영향 없음. */
  showGridLines?: boolean;
};

/**
 * 방(벽+바닥+배치된 가구) 위에 아바타를 세워 보여준다. 가만히 있을 땐 숨쉬듯
 * 미세하게 흔들리고(idle), 바닥을 탭하면 그 자리로 통통 튀며 걸어가고(위로
 * 이동하면 뒷모습, 도착하면 다시 정면), 아바타 자리를 탭하면 정면·뒷모습이 토글된다.
 * 배치된 책상·침대를 탭하면 거기로 걸어가서 앉거나 눕고, 다시 탭하면 일어난다.
 */
export function RoomScene({ height, bottomOverflow = 0, showGridLines = true }: RoomSceneProps) {
  const { placements } = useRoomFurniture();
  const [layout, setLayout] = useState<RoomLayoutInfo | null>(null);
  const [hasPositioned, setHasPositioned] = useState(false);
  const [facing, setFacing] = useState<"front" | "back">("front");
  const [occupancy, setOccupancy] = useState<FurnitureType | null>(null);

  const posX = useRef(new Animated.Value(0)).current;
  const posY = useRef(new Animated.Value(0)).current;
  const hopPhase = useRef(new Animated.Value(0)).current;
  const currentPosRef = useRef({ x: 0, y: 0 });
  const isMovingRef = useRef(false);
  const hopLoopRef = useRef<Animated.CompositeAnimation | null>(null);

  const frame = useFrameSequence(IDLE_SEQUENCE, occupancy !== "bed");
  const handFrame = useFrameSequence(SIT_HAND_SEQUENCE, occupancy === "desk");
  const sleepFrame = useFrameSequence(SLEEP_SEQUENCE, occupancy === "bed");

  const width = layout?.width ?? 0;
  const floorHeight = (layout?.floorHeight ?? 0) + bottomOverflow;
  // 아바타 너비를 타일 2칸 크기로 고정한다(dotPx 그대로 쓰면 약 1.8칸이라 요청대로 조정).
  const avatarScale = layout ? (AVATAR_WIDTH_IN_CELLS * layout.cellWidth) / AVATAR_STAGE_WIDTH : 0;
  const avatarRenderWidth = AVATAR_STAGE_WIDTH * avatarScale;
  const avatarRenderHeight = AVATAR_STAGE_HEIGHT * avatarScale;
  const feetOffsetX = avatarRenderWidth * AVATAR_FOOT_X_RATIO;
  const feetOffsetY = avatarRenderHeight * AVATAR_FOOT_Y_RATIO;

  const bedPlacement = placements.bed;
  const bedLayout =
    bedPlacement && layout ? getFurnitureLayout("bed", bedPlacement, layout.cellWidth, layout.rowHeight) : null;
  // AVATAR_FE.md: 침대 원점 기준 수면 아바타 (4, -12)도트.
  const sleepScale = bedLayout ? bedLayout.scale / SLEEP_RAW_PX_PER_DOT : 0;
  const sleepLeft = bedLayout ? bedLayout.containerLeft + 4 * bedLayout.scale : 0;
  const sleepTop = bedLayout ? bedLayout.containerTop - 12 * bedLayout.scale : 0;

  // 바닥 크기를 알게 되면 아바타 시작 위치를 한 번만 잡는다.
  useEffect(() => {
    if (width > 0 && !hasPositioned) {
      const startX = width / 2;
      const startY = floorHeight * 0.65;
      posX.setValue(startX);
      posY.setValue(startY);
      currentPosRef.current = { x: startX, y: startY };
      setHasPositioned(true);
    }
  }, [width, floorHeight, hasPositioned, posX, posY]);

  const startWalk = (
    from: { x: number; y: number },
    targetX: number,
    targetY: number,
    onArrive?: () => void,
  ) => {
    const distance = Math.hypot(targetX - from.x, targetY - from.y);
    if (distance < 6) {
      isMovingRef.current = false;
      onArrive?.();
      return;
    }

    const movingUp = targetY < from.y - 4;
    setFacing(movingUp ? "back" : "front");
    isMovingRef.current = true;

    const duration = Math.max(MIN_WALK_DURATION_MS, (distance / WALK_SPEED_PX_PER_SEC) * 1000);

    hopPhase.setValue(0);
    const hopLoop = Animated.loop(
      Animated.timing(hopPhase, {
        toValue: 1,
        duration: HOP_PERIOD_MS,
        easing: Easing.linear,
        useNativeDriver: true,
      }),
    );
    hopLoopRef.current = hopLoop;
    hopLoop.start();

    Animated.parallel([
      Animated.timing(posX, { toValue: targetX, duration, easing: Easing.linear, useNativeDriver: true }),
      Animated.timing(posY, { toValue: targetY, duration, easing: Easing.linear, useNativeDriver: true }),
    ]).start(({ finished }) => {
      // 이동 도중 다시 탭해서 끊긴 애니메이션(finished=false)의 콜백은 여기서 멈춘다.
      // 그대로 두면 이미 시작된 "다음" 이동의 hopLoop를 멈추고 위치·상태를 옛 목적지로
      // 되돌려버려서 이상하게 움직이는 것처럼 보인다.
      if (!finished) {
        return;
      }
      hopLoopRef.current?.stop();
      hopPhase.setValue(0);
      currentPosRef.current = { x: targetX, y: targetY };
      isMovingRef.current = false;
      setFacing("front");
      onArrive?.();
    });
  };

  const handleFloorPress = (event: GestureResponderEvent) => {
    if (!hasPositioned || !layout) {
      return;
    }
    const { locationX, locationY } = event.nativeEvent;
    const { cellWidth, rowHeight } = layout;

    // 앉아/누워 있으면 먼저 일어난다. 같은 가구를 다시 탭한 거면 그걸로 끝.
    let justStoodUp = false;
    if (occupancy) {
      const currentPlacement = placements[occupancy];
      const withinCurrent = currentPlacement
        ? isWithinRect(
            getFurnitureFootprintRect(occupancy, currentPlacement, cellWidth, rowHeight),
            locationX,
            locationY,
          )
        : false;
      setOccupancy(null);
      justStoodUp = true;
      if (withinCurrent) {
        return;
      }
    }

    const restPos = currentPosRef.current;

    const tappedAvatar =
      !isMovingRef.current &&
      !justStoodUp &&
      locationX >= restPos.x - feetOffsetX &&
      locationX <= restPos.x + (avatarRenderWidth - feetOffsetX) &&
      locationY >= restPos.y - feetOffsetY &&
      locationY <= restPos.y + (avatarRenderHeight - feetOffsetY);

    if (tappedAvatar) {
      setFacing((prev) => (prev === "front" ? "back" : "front"));
      return;
    }

    const hitType = FURNITURE_TYPES.find((type) => {
      const position = placements[type];
      if (!position) {
        return false;
      }
      return isWithinRect(getFurnitureFootprintRect(type, position, cellWidth, rowHeight), locationX, locationY);
    });

    let targetX: number;
    let targetY: number;
    let onArrive: (() => void) | undefined;

    if (hitType) {
      const position = placements[hitType];
      if (!position) {
        return;
      }
      const anchor = getFurnitureInteractionAnchor(hitType, position, cellWidth, rowHeight);
      targetX = anchor.x;
      targetY = anchor.y;
      onArrive = () => setOccupancy(hitType);
    } else {
      targetX = Math.min(
        Math.max(locationX, feetOffsetX),
        Math.max(width - (avatarRenderWidth - feetOffsetX), feetOffsetX),
      );
      targetY = Math.min(Math.max(locationY, 0), floorHeight);
    }

    if (!isMovingRef.current) {
      // 서있는 중이면 지금 위치가 곧 restPos이니 브릿지 왕복 없이 바로 출발한다.
      startWalk(restPos, targetX, targetY, onArrive);
      return;
    }

    // 이동 중 재탭: 진행 중인 애니메이션을 멈추고, 멈춘 바로 그 위치에서 새로 시작한다.
    // stopAnimation은 네이티브→JS 브릿지 왕복이 있어서, X·Y를 순서대로(중첩) 기다리면
    // 그만큼 두 배로 느려진다 — 동시에 요청하고 둘 다 도착했을 때만 시작한다.
    hopLoopRef.current?.stop();
    let resolvedX: number | null = null;
    let resolvedY: number | null = null;
    const tryStart = () => {
      if (resolvedX === null || resolvedY === null) {
        return;
      }
      startWalk({ x: resolvedX, y: resolvedY }, targetX, targetY, onArrive);
    };
    posX.stopAnimation((value) => {
      resolvedX = value;
      tryStart();
    });
    posY.stopAnimation((value) => {
      resolvedY = value;
      tryStart();
    });
  };

  const hopOffset = hopPhase.interpolate({
    inputRange: [0, 0.25, 0.5, 0.75, 1],
    outputRange: [0, -HOP_AMPLITUDE_PX * 0.71, -HOP_AMPLITUDE_PX, -HOP_AMPLITUDE_PX * 0.71, 0],
  });

  return (
    <RoomBackdrop
      bottomOverflow={bottomOverflow}
      height={height}
      occupiedFurniture={occupancy}
      onFloorPress={handleFloorPress}
      onLayoutChange={setLayout}
      overlay={
        hasPositioned ? (
          occupancy === "bed" ? (
            bedLayout ? (
              <View
                style={{
                  position: "absolute",
                  left: sleepLeft,
                  top: sleepTop,
                  width: AVATAR_SLEEP_STAGE_WIDTH * sleepScale,
                  height: AVATAR_SLEEP_STAGE_HEIGHT * sleepScale,
                }}
              >
                <AvatarSleepStage
                  frame={sleepFrame}
                  hairKey={DEFAULT_LOCAL_KEY_BY_SLOT.HAIR}
                  scale={sleepScale}
                />
              </View>
            ) : null
          ) : (
            <>
              {/* 그림자는 발밑 접지점(posX, posY)에 고정 — hopOffset을 안 더해서
                  아바타가 튀어올라도 그림자는 바닥에 그대로 남는다. */}
              <Animated.View
                style={[
                  styles.shadow,
                  {
                    width: avatarRenderWidth * 0.6,
                    height: avatarRenderWidth * 0.6 * 0.35,
                    transform: [
                      { translateX: Animated.subtract(posX, (avatarRenderWidth * 0.6) / 2) },
                      { translateY: Animated.subtract(posY, (avatarRenderWidth * 0.6 * 0.35) / 2) },
                    ],
                  },
                ]}
              />
              <Animated.View
                style={[
                  styles.avatarWrap,
                  {
                    transform: [
                      { translateX: Animated.subtract(posX, feetOffsetX) },
                      { translateY: Animated.add(Animated.subtract(posY, feetOffsetY), hopOffset) },
                    ],
                  },
                ]}
              >
                {occupancy === "desk" ? (
                  <AvatarStage activity="sit" handFrame={handFrame} scale={avatarScale} />
                ) : (
                  <AvatarStage facing={facing} frame={frame} scale={avatarScale} />
                )}
              </Animated.View>
            </>
          )
        ) : null
      }
      showGridLines={showGridLines}
    />
  );
}

const styles = StyleSheet.create({
  shadow: {
    position: "absolute",
    top: 0,
    left: 0,
    borderRadius: 999,
    backgroundColor: "rgba(0, 0, 0, 0.25)",
  },
  avatarWrap: {
    position: "absolute",
    top: 0,
    left: 0,
  },
});
