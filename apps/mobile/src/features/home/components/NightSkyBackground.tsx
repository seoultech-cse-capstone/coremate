import { useState } from "react";
import { StyleSheet, View, type LayoutChangeEvent } from "react-native";
import Svg, { Circle, Line } from "react-native-svg";

const SKY_COLOR = "#2A1F6B";

// 0~100 정규 좌표계(가로·세로 비율). 실제로 그릴 땐 컨테이너 실측 크기를 곱해서 쓴다 —
// 뷰박스를 화면 비율 그대로(가로≠세로) 늘리면 원이 타원으로 찌그러지기 때문.
const STARS = [
  { x: 8, y: 6, r: 1.6, o: 0.9 },
  { x: 18, y: 14, r: 1, o: 0.5 },
  { x: 27, y: 5, r: 1.3, o: 0.7 },
  { x: 34, y: 18, r: 1.6, o: 0.85 },
  { x: 41, y: 9, r: 1, o: 0.45 },
  { x: 12, y: 24, r: 1.1, o: 0.6 },
  { x: 22, y: 30, r: 0.8, o: 0.4 },
  { x: 5, y: 34, r: 1.3, o: 0.7 },
  { x: 48, y: 22, r: 1, o: 0.5 },
  { x: 55, y: 12, r: 1.5, o: 0.8 },
  { x: 62, y: 5, r: 1, o: 0.5 },
  { x: 68, y: 16, r: 1.6, o: 0.9 },
  { x: 74, y: 8, r: 0.8, o: 0.4 },
  { x: 80, y: 20, r: 1.1, o: 0.6 },
  { x: 87, y: 10, r: 1.6, o: 0.85 },
  { x: 93, y: 22, r: 1, o: 0.5 },
  { x: 96, y: 6, r: 1.1, o: 0.6 },
  { x: 60, y: 28, r: 0.8, o: 0.4 },
  { x: 72, y: 30, r: 1, o: 0.5 },
  { x: 84, y: 32, r: 0.8, o: 0.4 },
  { x: 15, y: 40, r: 1, o: 0.5 },
  { x: 30, y: 42, r: 1.1, o: 0.55 },
  { x: 45, y: 38, r: 0.8, o: 0.4 },
  { x: 90, y: 42, r: 1, o: 0.5 },
] as const;

// 별 몇 개를 얇은 선으로 이어서 별자리 느낌을 낸다.
const CONSTELLATIONS = [
  [
    { x: 34, y: 18 },
    { x: 41, y: 9 },
    { x: 55, y: 12 },
    { x: 62, y: 5 },
  ],
  [
    { x: 68, y: 16 },
    { x: 74, y: 8 },
    { x: 87, y: 10 },
    { x: 96, y: 6 },
  ],
] as const;

/** 홈 화면 위쪽 보라 배경을 밤하늘 별자리 느낌으로 채운다. 최상단 오버스크롤 시 드러난다. */
export function NightSkyBackground() {
  const [size, setSize] = useState({ width: 0, height: 0 });

  const onLayout = (event: LayoutChangeEvent) => {
    const { width, height } = event.nativeEvent.layout;
    setSize({ width, height });
  };

  return (
    <View onLayout={onLayout} style={styles.fill}>
      {size.width > 0 ? (
        // 뷰박스를 실측 px 크기 그대로 써서 1유닛=1px로 맞춘다 — 그래야 원이 원으로 나온다.
        <Svg height={size.height} style={StyleSheet.absoluteFill} width={size.width}>
          {CONSTELLATIONS.map((points, groupIndex) =>
            points.slice(1).map((point, index) => (
              <Line
                key={`${groupIndex}-${index}`}
                stroke="rgba(255, 255, 255, 0.22)"
                strokeWidth={1}
                x1={(points[index].x / 100) * size.width}
                x2={(point.x / 100) * size.width}
                y1={(points[index].y / 100) * size.height}
                y2={(point.y / 100) * size.height}
              />
            )),
          )}
          {STARS.map((star, index) => (
            <Circle
              cx={(star.x / 100) * size.width}
              cy={(star.y / 100) * size.height}
              fill="#ffffff"
              fillOpacity={star.o}
              key={index}
              r={star.r}
            />
          ))}
        </Svg>
      ) : null}
    </View>
  );
}

const styles = StyleSheet.create({
  fill: {
    ...StyleSheet.absoluteFill,
    backgroundColor: SKY_COLOR,
  },
});
