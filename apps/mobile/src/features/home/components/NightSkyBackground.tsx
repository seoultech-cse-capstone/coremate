import { StyleSheet, View } from "react-native";
import Svg, { Circle, Line } from "react-native-svg";

const SKY_COLOR = "#2A1F6B";

// 0~100 정규 좌표계(뷰박스 기준). 화면 크기와 무관하게 항상 같은 비율로 흩어진다.
const STARS = [
  { x: 8, y: 6, r: 0.9, o: 0.9 },
  { x: 18, y: 14, r: 0.5, o: 0.5 },
  { x: 27, y: 5, r: 0.7, o: 0.7 },
  { x: 34, y: 18, r: 0.9, o: 0.85 },
  { x: 41, y: 9, r: 0.5, o: 0.45 },
  { x: 12, y: 24, r: 0.6, o: 0.6 },
  { x: 22, y: 30, r: 0.4, o: 0.4 },
  { x: 5, y: 34, r: 0.7, o: 0.7 },
  { x: 48, y: 22, r: 0.5, o: 0.5 },
  { x: 55, y: 12, r: 0.8, o: 0.8 },
  { x: 62, y: 5, r: 0.5, o: 0.5 },
  { x: 68, y: 16, r: 0.9, o: 0.9 },
  { x: 74, y: 8, r: 0.4, o: 0.4 },
  { x: 80, y: 20, r: 0.6, o: 0.6 },
  { x: 87, y: 10, r: 0.9, o: 0.85 },
  { x: 93, y: 22, r: 0.5, o: 0.5 },
  { x: 96, y: 6, r: 0.6, o: 0.6 },
  { x: 60, y: 28, r: 0.4, o: 0.4 },
  { x: 72, y: 30, r: 0.5, o: 0.5 },
  { x: 84, y: 32, r: 0.4, o: 0.4 },
  { x: 15, y: 40, r: 0.5, o: 0.5 },
  { x: 30, y: 42, r: 0.6, o: 0.55 },
  { x: 45, y: 38, r: 0.4, o: 0.4 },
  { x: 90, y: 42, r: 0.5, o: 0.5 },
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
  return (
    <View style={styles.fill}>
      <Svg
        height="100%"
        preserveAspectRatio="none"
        style={StyleSheet.absoluteFill}
        viewBox="0 0 100 100"
        width="100%"
      >
        {CONSTELLATIONS.map((points, groupIndex) =>
          points.slice(1).map((point, index) => (
            <Line
              key={`${groupIndex}-${index}`}
              stroke="rgba(255, 255, 255, 0.22)"
              strokeWidth={0.15}
              x1={points[index].x}
              x2={point.x}
              y1={points[index].y}
              y2={point.y}
            />
          )),
        )}
        {STARS.map((star, index) => (
          <Circle
            cx={star.x}
            cy={star.y}
            fill="#ffffff"
            fillOpacity={star.o}
            key={index}
            r={star.r}
          />
        ))}
      </Svg>
    </View>
  );
}

const styles = StyleSheet.create({
  fill: {
    ...StyleSheet.absoluteFillObject,
    backgroundColor: SKY_COLOR,
  },
});
