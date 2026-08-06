import { StyleSheet, Text, View } from "react-native";
import Svg, { Polygon, Text as SvgText } from "react-native-svg";

import { FONT } from "../../../../constants/fonts";

const CENTER_X = 150;
const CENTER_Y = 119;
const RADIUS = 97;
/** 축 라벨을 도형 바깥에 두기 위한 여유. */
const LABEL_OFFSET = 16;

export type ProgressAxis = {
  label: string;
  /** 0~100 */
  value: number;
};

type FriendProgressStatsProps = {
  axes: ProgressAxis[];
};

/**
 * 과목별 달성률 레이더 차트.
 *
 * 축을 배열로 받아 좌표를 계산한다. 과목 수와 이름은 서버가 정하는 값이라 화면에 고정해
 * 두면 과목이 하나 늘거나 줄 때마다 그림이 깨진다.
 */
export function FriendProgressStats({ axes }: FriendProgressStatsProps) {
  const chartPoints = axes
    .map((axis, index) => axisPoint(index, axes.length, clampRatio(axis.value)).join(","))
    .join(" ");

  return (
    <View style={styles.container}>
      <Text style={styles.heading}>Learning Stats</Text>
      {/* 축이 3개 미만이면 도형이 점이나 선이 돼 버린다. 그럴 땐 아래 수치만 보여준다. */}
      {axes.length >= 3 ? (
      <Svg height={242} viewBox="0 0 300 242" width="100%">
        {[1, 0.75, 0.5, 0.25].map((scale) => (
          <Polygon
            fill="none"
            key={scale}
            points={axes
              .map((_, index) => axisPoint(index, axes.length, scale).join(","))
              .join(" ")}
            stroke="#D9E1EE"
            strokeWidth={1}
          />
        ))}
        {axes.map((axis, index) => {
          const [x, y] = axisPoint(index, axes.length, 1);
          return (
            <Polygon
              fill="none"
              key={axis.label}
              points={`${CENTER_X},${CENTER_Y} ${x},${y}`}
              stroke="#E7ECF4"
              strokeWidth={1}
            />
          );
        })}
        <Polygon
          fill="rgba(109, 93, 251, 0.24)"
          points={chartPoints}
          stroke="#6D5DFB"
          strokeWidth={2}
        />
        {axes.map((axis, index) => {
          const [x, y] = axisPoint(index, axes.length, 1, LABEL_OFFSET);
          return (
            <SvgText
              fill="#64748B"
              fontSize="12"
              key={axis.label}
              textAnchor="middle"
              x={x}
              y={y + 4}
            >
              {axis.label}
            </SvgText>
          );
        })}
      </Svg>
      ) : null}

      <View style={styles.statGrid}>
        {axes.map((axis) => (
          <View key={axis.label} style={styles.statItem}>
            <Text style={styles.statLabel}>{axis.label}</Text>
            <Text style={styles.statValue}>{Math.round(axis.value)}%</Text>
          </View>
        ))}
      </View>
    </View>
  );
}

// 12시 방향부터 시계 방향으로 축을 배치한다.
function axisPoint(index: number, count: number, scale: number, extra = 0): [number, number] {
  const angle = (Math.PI * 2 * index) / count - Math.PI / 2;
  const radius = RADIUS * scale + extra;
  return [
    Math.round(CENTER_X + radius * Math.cos(angle)),
    Math.round(CENTER_Y + radius * Math.sin(angle)),
  ];
}

function clampRatio(value: number): number {
  if (Number.isNaN(value)) {
    return 0;
  }
  return Math.min(1, Math.max(0, value / 100));
}

const styles = StyleSheet.create({
  container: {
    alignItems: "center",
  },
  heading: {
    alignSelf: "flex-start",
    color: "#1F2937",
    fontFamily: FONT.kufam,
    fontSize: 17,
    fontWeight: "600",
  },
  statGrid: {
    width: "100%",
    flexDirection: "row",
    flexWrap: "wrap",
    rowGap: 18,
    paddingHorizontal: 10,
  },
  statItem: {
    width: "33.333%",
    alignItems: "center",
    gap: 4,
  },
  statLabel: {
    color: "#94A3B8",
    fontFamily: FONT.kufam,
    fontSize: 11,
  },
  statValue: {
    color: "#334155",
    fontFamily: FONT.kufam,
    fontSize: 14,
    fontWeight: "600",
  },
});
