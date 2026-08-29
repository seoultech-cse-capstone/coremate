import { useEffect, useRef, useState } from "react";
import {
  Animated,
  Easing,
  Image,
  Pressable,
  StyleSheet,
  View,
  type GestureResponderEvent,
  type LayoutChangeEvent,
} from "react-native";

import { AVATAR_STAGE_HEIGHT, AVATAR_STAGE_WIDTH, AvatarStage } from "./AvatarStage";
import { ROOM_GRID_COLS, ROOM_GRID_ROWS, getRoomGridLayout } from "../roomGrid";
import { useRoomTheme } from "../RoomThemeContext";

const GRID_LINE_COLOR = "rgba(255, 255, 255, 0.4)";

// AVATAR_FE.md 2절: 서기(앞) idle은 1→2→3→2 루프, 프레임별 duration이 다르다.
const IDLE_SEQUENCE: { frame: 1 | 2 | 3; duration: number }[] = [
  { frame: 1, duration: 400 },
  { frame: 2, duration: 150 },
  { frame: 3, duration: 400 },
  { frame: 2, duration: 150 },
];

// AVATAR_FE.md 6절: 이동 중 offsetY = -|sin(t)| x 진폭(2~3도트), 주기 ~250ms.
const HOP_PERIOD_MS = 250;
const HOP_AMPLITUDE_PX = 20;
const WALK_SPEED_PX_PER_SEC = 220;
const MIN_WALK_DURATION_MS = 300;

type RoomSceneProps = {
  height: number;
  /** 격자 안내선 표시 여부. 실제 바닥 에셋이 들어오면 false로 끈다 — 칸 지오메트리 자체는 roomGrid.ts에 고정돼 있으니 영향 없음. */
  showGridLines?: boolean;
};

/**
 * 방(벽+바닥) 위에 아바타를 세워 보여준다. 가만히 있을 땐 숨쉬듯 미세하게
 * 흔들리고(idle), 바닥을 탭하면 그 자리로 통통 튀며 걸어가고(위로 이동하면
 * 뒷모습, 도착하면 다시 정면), 아바타 자리를 탭하면 정면·뒷모습이 토글된다.
 */
export function RoomScene({ height, showGridLines = true }: RoomSceneProps) {
  const { theme } = useRoomTheme();
  const [width, setWidth] = useState(0);
  const [hasPositioned, setHasPositioned] = useState(false);
  const [frame, setFrame] = useState<1 | 2 | 3>(1);
  const [facing, setFacing] = useState<"front" | "back">("front");

  const posX = useRef(new Animated.Value(0)).current;
  const posY = useRef(new Animated.Value(0)).current;
  const hopPhase = useRef(new Animated.Value(0)).current;
  const currentPosRef = useRef({ x: 0, y: 0 });
  const isMovingRef = useRef(false);
  const hopLoopRef = useRef<Animated.CompositeAnimation | null>(null);

  const onLayout = (event: LayoutChangeEvent) => {
    setWidth(event.nativeEvent.layout.width);
  };

  const { cellWidth, rowHeight, gridHeight, floorHeight, wallHeight } = getRoomGridLayout(
    width,
    height,
  );
  const avatarWidth = cellWidth;
  const avatarHeight = avatarWidth * (AVATAR_STAGE_HEIGHT / AVATAR_STAGE_WIDTH);
  const avatarScale = avatarWidth / AVATAR_STAGE_WIDTH;

  // 서기(앞) idle 프레임 루프. 이동 중에도 그대로 재생한다(#AVATAR_FE.md: 걷기 전용
  // 프레임 없이 idle 스택을 그대로 통통 튀겨서 이동시킨다).
  useEffect(() => {
    let cancelled = false;
    let index = 0;
    let timeoutId: ReturnType<typeof setTimeout>;

    const tick = () => {
      if (cancelled) {
        return;
      }
      const step = IDLE_SEQUENCE[index % IDLE_SEQUENCE.length];
      setFrame(step.frame);
      index += 1;
      timeoutId = setTimeout(tick, step.duration);
    };

    tick();
    return () => {
      cancelled = true;
      clearTimeout(timeoutId);
    };
  }, []);

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

  const startWalk = (from: { x: number; y: number }, targetX: number, targetY: number) => {
    const distance = Math.hypot(targetX - from.x, targetY - from.y);
    if (distance < 6) {
      isMovingRef.current = false;
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
    });
  };

  const handleFloorPress = (event: GestureResponderEvent) => {
    if (!hasPositioned) {
      return;
    }
    const { locationX, locationY } = event.nativeEvent;
    const restPos = currentPosRef.current;

    const tappedAvatar =
      !isMovingRef.current &&
      Math.abs(locationX - restPos.x) <= avatarWidth / 2 &&
      locationY <= restPos.y &&
      locationY >= restPos.y - avatarHeight;

    if (tappedAvatar) {
      setFacing((prev) => (prev === "front" ? "back" : "front"));
      return;
    }

    const targetX = Math.min(Math.max(locationX, avatarWidth / 2), Math.max(width - avatarWidth / 2, avatarWidth / 2));
    const targetY = Math.min(Math.max(locationY, 0), floorHeight);

    if (!isMovingRef.current) {
      // 서있는 중이면 지금 위치가 곧 restPos이니 브릿지 왕복 없이 바로 출발한다.
      startWalk(restPos, targetX, targetY);
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
      startWalk({ x: resolvedX, y: resolvedY }, targetX, targetY);
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
    <View onLayout={onLayout} style={[styles.card, { height }]}>
      <Image
        resizeMode="cover"
        source={theme.wallSource}
        style={{ width: "100%", height: wallHeight }}
      />
      <Pressable
        accessibilityLabel="방 바닥"
        style={{ height: floorHeight }}
        onPress={handleFloorPress}
      >
        <Image resizeMode="cover" source={theme.floorSource} style={StyleSheet.absoluteFillObject} />
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

        {hasPositioned ? (
          <Animated.View
            pointerEvents="none"
            style={[
              styles.avatarWrap,
              {
                transform: [
                  { translateX: Animated.subtract(posX, avatarWidth / 2) },
                  { translateY: Animated.add(Animated.subtract(posY, avatarHeight), hopOffset) },
                ],
              },
            ]}
          >
            <AvatarStage facing={facing} frame={frame} scale={avatarScale} />
          </Animated.View>
        ) : null}
      </Pressable>
    </View>
  );
}

const styles = StyleSheet.create({
  card: {
    width: "100%",
  },
  avatarWrap: {
    position: "absolute",
    top: 0,
    left: 0,
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
