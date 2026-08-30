import { useEffect, useRef, useState } from "react";
import { Animated, Easing, Image, StyleSheet, View, type GestureResponderEvent } from "react-native";

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
import { useAvatarEquip } from "../AvatarEquipContext";
import { DEFAULT_LOCAL_KEY_BY_SLOT, LOCAL_CATALOG_BY_KEY } from "../localCatalog";
import { useRoomFurniture } from "../RoomFurnitureContext";
import {
  INTERACTIVE_FURNITURE_TYPES,
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

// 펫: 프레임 애니메이션 없는 정지 이미지라, 강아지 원본(18x13도트)을 기준 상자로 두고
// contain으로 맞춘다(거북이는 더 납작해서 상자 안에서 살짝 작게 보인다 — 의도된 동작).
const PET_BOX_WIDTH_DOTS = 18;
const PET_BOX_HEIGHT_DOTS = 13;
// 펫 너비 기준: 아바타(AVATAR_WIDTH_IN_CELLS) 대비 몇 배 크기로 보일지. 아바타는
// dotPx가 아니라 이 "칸 단위" 스케일로 그려서(원본 41도트를 1칸으로 축소), 펫도
// 원본 도트 수를 그대로 dotPx에 곱하면 아바타보다 커 보인다 — 같은 칸 단위 기준으로
// 맞춰야 아바타와 크기가 비교된다. 동행 동물이니 아바타보다 작게 절반으로 둔다.
const PET_WIDTH_IN_CELLS = AVATAR_WIDTH_IN_CELLS * 0.5;
// 아바타가 걷기 시작한 뒤 이만큼 늦게 출발해서 "따라오는" 느낌을 준다.
const PET_FOLLOW_DELAY_MS = 220;
// 아바타 발밑 기준 오른쪽으로 이 정도(칸 단위) 떨어진 자리를 목표로 걷는다.
const PET_FOLLOW_OFFSET_CELLS = 0.55;

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
  const { equipped } = useAvatarEquip();
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

  // 인벤토리 화면에서 고른 착용 상태(equipped)를 그대로 반영한다. 부위가 없으면
  // (한 번도 안 바꿨으면) 기본 착용으로 폴백한다 — inventory.tsx와 동일한 규칙.
  const rawPetKey = equipped.PET;
  const petKey = rawPetKey !== undefined ? rawPetKey : (DEFAULT_LOCAL_KEY_BY_SLOT.PET ?? null);
  const petItem = petKey ? LOCAL_CATALOG_BY_KEY.get(petKey) : undefined;
  const rawHairKey = equipped.HAIR;
  const hairKey = rawHairKey !== undefined ? rawHairKey : DEFAULT_LOCAL_KEY_BY_SLOT.HAIR;
  const petX = useRef(new Animated.Value(0)).current;
  const petY = useRef(new Animated.Value(0)).current;
  const petHopPhase = useRef(new Animated.Value(0)).current;
  const petCurrentPosRef = useRef({ x: 0, y: 0 });
  const petFollowTimeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const petHopLoopRef = useRef<Animated.CompositeAnimation | null>(null);

  useEffect(() => {
    return () => {
      if (petFollowTimeoutRef.current) {
        clearTimeout(petFollowTimeoutRef.current);
      }
    };
  }, []);

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
  // 아바타와 같은 "칸 단위" 스케일로 그린다(dotPx를 그대로 쓰면 아바타의 칸 축소
  // 비율과 안 맞아서 아바타보다 커 보인다).
  const petBoxWidth = layout ? PET_WIDTH_IN_CELLS * layout.cellWidth : 0;
  const petBoxHeight = petBoxWidth * (PET_BOX_HEIGHT_DOTS / PET_BOX_WIDTH_DOTS);
  const petOffsetX = layout ? PET_FOLLOW_OFFSET_CELLS * layout.cellWidth : 0;

  const bedPlacement = placements.bed;
  const bedLayout =
    bedPlacement && layout ? getFurnitureLayout("bed", bedPlacement, layout.cellWidth, layout.rowHeight) : null;
  // AVATAR_FE.md: 침대 원점 기준 수면 아바타 (4, -12)도트.
  const sleepScale = bedLayout ? bedLayout.scale / SLEEP_RAW_PX_PER_DOT : 0;
  const sleepLeft = bedLayout ? bedLayout.containerLeft + 4 * bedLayout.scale : 0;
  const sleepTop = bedLayout ? bedLayout.containerTop - 12 * bedLayout.scale : 0;

  // 바닥 크기를 알게 되면 아바타(와 펫) 시작 위치를 한 번만 잡는다.
  useEffect(() => {
    if (width > 0 && !hasPositioned) {
      const startX = width / 2;
      const startY = floorHeight * 0.65;
      posX.setValue(startX);
      posY.setValue(startY);
      currentPosRef.current = { x: startX, y: startY };
      const petStartX = Math.max(0, startX + petOffsetX);
      petX.setValue(petStartX);
      petY.setValue(startY);
      petCurrentPosRef.current = { x: petStartX, y: startY };
      setHasPositioned(true);
    }
  }, [width, floorHeight, hasPositioned, posX, posY, petX, petY, petOffsetX]);

  const startWalk = (
    from: { x: number; y: number },
    targetX: number,
    targetY: number,
    onArrive?: () => void,
  ) => {
    const distance = Math.hypot(targetX - from.x, targetY - from.y);
    if (distance < 6) {
      // 이미 목표 근처라 걷는 애니메이션 없이 바로 도착 처리한다 — 그렇다고 위치를
      // 그대로 두면(from) 가구 앵커처럼 정확히 고정돼야 하는 좌표가 매번 "거의
      // 비슷하지만 조금씩 다른" 값으로 남는다. 무조건 targetX/Y로 스냅해야 한다.
      // hopPhase도 같이 0으로 되돌려야 한다 — 이동 중 재탭으로 여기 들어온
      // 경우라면 hopLoop는 handleFloorPress에서 이미 stop()됐지만 값 자체는
      // 멈춘 그 순간(임의의 튀어오른 높이)에 남아있어서, 리셋 안 하면 앉는/눕는
      // y 위치에 그 잔여 튐 높이가 매번 다르게 얹혀 보인다.
      hopPhase.setValue(0);
      posX.setValue(targetX);
      posY.setValue(targetY);
      currentPosRef.current = { x: targetX, y: targetY };
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
      if (onArrive) {
        // 가구에 도착한 거라면 곧바로 앉기/수면 렌더링으로 바뀌어서 facing 값은
        // 더 이상 안 쓰인다. 여기서 굳이 setFacing까지 같이 부르면(둘 다 이 Animated
        // 콜백 안이라 리액트가 하나로 묶어주지 못할 수 있음) "정면으로 선 채 도착"
        // 프레임이 한 번 그려졌다가 바로 앉는 모습으로 바뀌면서 깜빡여 보인다.
        onArrive();
      } else {
        setFacing("front");
      }
    });
  };

  // 아바타보다 살짝 늦게, 아바타 옆자리(petOffsetX)를 향해 걸어온다. 사람과 같은
  // 통통 튐(HOP_PERIOD_MS/HOP_AMPLITUDE_PX)을 그대로 쓴다. 도착 지점만 맞추면
  // 되므로 아바타의 실시간 좌표를 계속 추적할 필요 없이(리스너 없이) 목표점
  // 하나만 넘기면 충분하다.
  const startPetWalk = (from: { x: number; y: number }, targetX: number, targetY: number) => {
    const distance = Math.hypot(targetX - from.x, targetY - from.y);
    if (distance < 4) {
      // startWalk의 같은 조기 리턴 경로와 같은 이유로, hopPhase 잔여값도
      // 같이 0으로 되돌린다 — 안 그러면 멈춘 순간의 임의의 튐 높이가 남는다.
      petHopPhase.setValue(0);
      petCurrentPosRef.current = { x: targetX, y: targetY };
      return;
    }
    const duration = Math.max(MIN_WALK_DURATION_MS, (distance / WALK_SPEED_PX_PER_SEC) * 1000);

    petHopPhase.setValue(0);
    const hopLoop = Animated.loop(
      Animated.timing(petHopPhase, {
        toValue: 1,
        duration: HOP_PERIOD_MS,
        easing: Easing.linear,
        useNativeDriver: true,
      }),
    );
    petHopLoopRef.current = hopLoop;
    hopLoop.start();

    Animated.parallel([
      Animated.timing(petX, { toValue: targetX, duration, easing: Easing.linear, useNativeDriver: true }),
      Animated.timing(petY, { toValue: targetY, duration, easing: Easing.linear, useNativeDriver: true }),
    ]).start(({ finished }) => {
      if (!finished) {
        return;
      }
      petHopLoopRef.current?.stop();
      petHopPhase.setValue(0);
      petCurrentPosRef.current = { x: targetX, y: targetY };
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

    const hitType = INTERACTIVE_FURNITURE_TYPES.find((type) => {
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

    // 의자/책상처럼 cellWidth÷7 같은 나눗셈에서 나온 목표 좌표는 168.428571...처럼
    // 끝없는 소수다. 가구(chair/desk)는 일반 top/left 스타일이라 RN 레이아웃 엔진이
    // 매 렌더 같은 값으로 정수 픽셀에 스냅하지만, 아바타는 useNativeDriver
    // Animated.Value라 GPU 합성 단계에서 서브픽셀 그대로 그려진다 — 같은 논리값이라도
    // 두 파이프라인의 반올림 방식이 달라서 가구와 아바타 사이 간격이 기기에 따라
    // 1px 안팎으로 어긋나 보일 수 있다. 정수로 스냅해서 이 불일치를 없앤다.
    targetX = Math.round(targetX);
    targetY = Math.round(targetY);

    if (petItem) {
      if (petFollowTimeoutRef.current) {
        clearTimeout(petFollowTimeoutRef.current);
      }
      const petTargetX = Math.min(Math.max(targetX + petOffsetX, 0), width);
      const petTargetY = targetY;
      petFollowTimeoutRef.current = setTimeout(() => {
        startPetWalk(petCurrentPosRef.current, petTargetX, petTargetY);
      }, PET_FOLLOW_DELAY_MS);
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
  // 펫도 아바타와 같은 높이·주기로 통통 튄다.
  const petHopOffset = petHopPhase.interpolate({
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
          <>
            {/* 수면 중이 아닐 때도 항상 마운트해두고 opacity만 바꾼다 — occupancy가
                바뀔 때마다 이 서브트리(AvatarSleepStage)와 아래 서기/앉기 서브트리
                (AvatarStage)를 통째로 교체하면, 그 순간 처음 마운트되는 쪽 이미지가
                디코드되며 깜빡인다. 둘 다 항상 그려두고 안 보이는 쪽만 투명하게 둔다. */}
            {bedLayout ? (
              <View
                style={{
                  position: "absolute",
                  left: sleepLeft,
                  top: sleepTop,
                  width: AVATAR_SLEEP_STAGE_WIDTH * sleepScale,
                  height: AVATAR_SLEEP_STAGE_HEIGHT * sleepScale,
                  opacity: occupancy === "bed" ? 1 : 0,
                }}
              >
                <AvatarSleepStage frame={sleepFrame} hairKey={hairKey ?? undefined} scale={sleepScale} />
              </View>
            ) : null}
            <View style={{ opacity: occupancy === "bed" ? 0 : 1 }}>
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
                  <AvatarStage activity="sit" equipped={equipped} handFrame={handFrame} scale={avatarScale} />
                ) : (
                  <AvatarStage equipped={equipped} facing={facing} frame={frame} scale={avatarScale} />
                )}
              </Animated.View>
            </View>
            {petItem ? (
              <Animated.View
                style={[
                  styles.petWrap,
                  {
                    width: petBoxWidth,
                    height: petBoxHeight,
                    transform: [
                      { translateX: Animated.subtract(petX, petBoxWidth / 2) },
                      { translateY: Animated.add(Animated.subtract(petY, petBoxHeight), petHopOffset) },
                    ],
                  },
                ]}
              >
                <Image resizeMode="contain" source={petItem.source} style={styles.petImage} />
              </Animated.View>
            ) : null}
          </>
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
  petWrap: {
    position: "absolute",
    top: 0,
    left: 0,
  },
  petImage: {
    width: "100%",
    height: "100%",
  },
});
