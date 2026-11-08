import { useEffect, useState } from "react";
import { Animated, Easing, Pressable, StyleSheet, Text, View, type LayoutChangeEvent } from "react-native";
import Svg, { Path } from "react-native-svg";

import { FONT } from "../../../constants/fonts";

export type SpotlightRect = { x: number; y: number; width: number; height: number };

const HOLE_PADDING = 8;
const HOLE_RADIUS = 16;
const TOOLTIP_GAP = 14;
const DIM_COLOR = "rgba(8, 6, 20, 0.74)";

type SpotlightOverlayProps = {
  /** 밝게 뚫을 영역(오버레이 기준 좌표). null 이면 구멍 없이 가운데 안내 카드만 띄운다. */
  target: SpotlightRect | null;
  /** 아직 강조할 버튼 위치를 재는 중이면 true. 그동안은 어둡게만 덮고 카드는 숨긴다. */
  measuring: boolean;
  stepLabel: string;
  title: string;
  body: string;
  /** 구멍(강조된 버튼)을 눌렀을 때 */
  onTargetPress: () => void;
  /** target 이 없는 안내 카드의 버튼 */
  centerActionLabel?: string;
  onSkip: () => void;
};

/**
 * 화면 전체를 어둡게 덮고 target 자리만 둥글게 뚫어 보여준다.
 * 뚫린 자리만 눌리고 나머지 터치는 모두 여기서 막는다(아래 더미 화면은 눌리지 않는다).
 */
export function SpotlightOverlay({
  target,
  measuring,
  stepLabel,
  title,
  body,
  onTargetPress,
  centerActionLabel = "다음",
  onSkip,
}: SpotlightOverlayProps) {
  const [size, setSize] = useState({ width: 0, height: 0 });
  const [pulse] = useState(() => new Animated.Value(0));

  useEffect(() => {
    const loop = Animated.loop(
      Animated.timing(pulse, {
        toValue: 1,
        duration: 1400,
        easing: Easing.out(Easing.ease),
        useNativeDriver: true,
      }),
    );
    loop.start();
    return () => loop.stop();
  }, [pulse]);

  const handleLayout = (event: LayoutChangeEvent) => {
    const { width, height } = event.nativeEvent.layout;
    setSize({ width, height });
  };

  const hole = target
    ? {
        x: target.x - HOLE_PADDING,
        y: target.y - HOLE_PADDING,
        width: target.width + HOLE_PADDING * 2,
        height: target.height + HOLE_PADDING * 2,
      }
    : null;

  // 바깥 사각형 + 둥근 구멍을 evenodd 로 칠하면 구멍만 비어 보인다.
  const outer = `M0 0H${size.width}V${size.height}H0Z`;
  const holePath = hole ? roundedRectPath(hole.x, hole.y, hole.width, hole.height, HOLE_RADIUS) : "";

  // 구멍이 화면 위쪽이면 카드를 아래에, 아래쪽이면 위에 띄운다.
  const placeBelow = hole ? hole.y + hole.height / 2 < size.height / 2 : false;
  const tooltipPosition = hole
    ? placeBelow
      ? { top: hole.y + hole.height + TOOLTIP_GAP }
      : { bottom: size.height - hole.y + TOOLTIP_GAP }
    : null;

  return (
    <View
      style={StyleSheet.absoluteFill}
      onLayout={handleLayout}
      // 구멍 바깥 터치를 전부 여기서 받아 버린다.
      onStartShouldSetResponder={() => true}
    >
      {size.width > 0 ? (
        <Svg width={size.width} height={size.height} style={StyleSheet.absoluteFill} pointerEvents="none">
          <Path d={measuring ? outer : outer + holePath} fill={DIM_COLOR} fillRule="evenodd" />
        </Svg>
      ) : null}

      {hole && !measuring ? (
        <>
          <Animated.View
            pointerEvents="none"
            style={[
              styles.pulseRing,
              {
                left: hole.x,
                top: hole.y,
                width: hole.width,
                height: hole.height,
                opacity: pulse.interpolate({ inputRange: [0, 1], outputRange: [0.9, 0] }),
                transform: [{ scale: pulse.interpolate({ inputRange: [0, 1], outputRange: [1, 1.08] }) }],
              },
            ]}
          />
          <Pressable
            accessibilityRole="button"
            accessibilityLabel={title}
            style={[styles.hole, { left: hole.x, top: hole.y, width: hole.width, height: hole.height }]}
            onPress={onTargetPress}
          />
        </>
      ) : null}

      {!measuring ? (
        <View
          style={[styles.card, tooltipPosition ?? styles.centerCard]}
          pointerEvents="box-none"
        >
          <View style={styles.cardHeader}>
            <Text style={styles.stepLabel}>{stepLabel}</Text>
            <Pressable accessibilityRole="button" hitSlop={10} onPress={onSkip}>
              <Text style={styles.skipText}>건너뛰기</Text>
            </Pressable>
          </View>
          <Text style={styles.title}>{title}</Text>
          <Text style={styles.body}>{body}</Text>
          {hole ? (
            <Text style={styles.hint}>반짝이는 곳을 눌러보세요</Text>
          ) : (
            <Pressable accessibilityRole="button" style={styles.centerButton} onPress={onTargetPress}>
              <Text style={styles.centerButtonText}>{centerActionLabel}</Text>
            </Pressable>
          )}
        </View>
      ) : null}
    </View>
  );
}

function roundedRectPath(x: number, y: number, w: number, h: number, r: number) {
  const radius = Math.min(r, w / 2, h / 2);
  return (
    `M${x + radius} ${y}` +
    `H${x + w - radius}A${radius} ${radius} 0 0 1 ${x + w} ${y + radius}` +
    `V${y + h - radius}A${radius} ${radius} 0 0 1 ${x + w - radius} ${y + h}` +
    `H${x + radius}A${radius} ${radius} 0 0 1 ${x} ${y + h - radius}` +
    `V${y + radius}A${radius} ${radius} 0 0 1 ${x + radius} ${y}Z`
  );
}

const styles = StyleSheet.create({
  hole: {
    position: "absolute",
    borderRadius: HOLE_RADIUS,
  },
  pulseRing: {
    position: "absolute",
    borderRadius: HOLE_RADIUS,
    borderWidth: 2,
    borderColor: "#C4B5FD",
  },
  card: {
    position: "absolute",
    left: 20,
    right: 20,
    borderRadius: 20,
    backgroundColor: "#FFFFFF",
    paddingHorizontal: 20,
    paddingTop: 16,
    paddingBottom: 18,
  },
  centerCard: {
    top: "34%",
  },
  cardHeader: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
    marginBottom: 8,
  },
  stepLabel: {
    color: "#6D5DFB",
    fontFamily: FONT.pretendard,
    fontSize: 12,
    fontWeight: "800",
  },
  skipText: {
    color: "#94A3B8",
    fontFamily: FONT.pretendard,
    fontSize: 13,
    fontWeight: "600",
  },
  title: {
    color: "#111827",
    fontFamily: FONT.pretendard,
    fontSize: 18,
    fontWeight: "800",
  },
  body: {
    marginTop: 6,
    color: "#475569",
    fontFamily: FONT.pretendard,
    fontSize: 14,
    fontWeight: "500",
    lineHeight: 21,
  },
  hint: {
    marginTop: 12,
    color: "#6D5DFB",
    fontFamily: FONT.pretendard,
    fontSize: 12,
    fontWeight: "700",
  },
  centerButton: {
    marginTop: 16,
    alignItems: "center",
    justifyContent: "center",
    minHeight: 50,
    borderRadius: 14,
    backgroundColor: "#6D5DFB",
  },
  centerButtonText: {
    color: "#FFFFFF",
    fontFamily: FONT.pretendard,
    fontSize: 16,
    fontWeight: "700",
  },
});
