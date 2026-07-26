import { StyleSheet, Text, View } from "react-native";
import Svg, { Polygon, Text as SvgText } from "react-native-svg";

import { FONT } from "../../../../constants/fonts";
import type { FriendProgress } from "../friendData";

const AXES = ["DS", "Algo", "OS", "DB", "Arch", "Net"];
const POINTS = [
  [150, 22],
  [235, 70],
  [235, 168],
  [150, 216],
  [65, 168],
  [65, 70],
];

type FriendProgressStatsProps = {
  progress: FriendProgress;
};

export function FriendProgressStats({ progress }: FriendProgressStatsProps) {
  const values = [
    progress.dataStructure,
    progress.algorithm,
    progress.operatingSystem,
    progress.database,
    progress.computerArchitecture,
    progress.network,
  ];
  const chartPoints = values
    .map((value, index) => {
      const [x, y] = POINTS[index];
      return `${150 + (x - 150) * (value / 100)},${119 + (y - 119) * (value / 100)}`;
    })
    .join(" ");

  return (
    <View style={styles.container}>
      <Text style={styles.heading}>Learning Stats</Text>
      <Svg height={242} viewBox="0 0 300 242" width="100%">
        {[1, 0.75, 0.5, 0.25].map((scale) => (
          <Polygon
            fill="none"
            key={scale}
            points={POINTS.map(([x, y]) =>
              `${150 + (x - 150) * scale},${119 + (y - 119) * scale}`,
            ).join(" ")}
            stroke="#D9E1EE"
            strokeWidth={1}
          />
        ))}
        {POINTS.map(([x, y], index) => (
          <Polygon
            fill="none"
            key={AXES[index]}
            points={`150,119 ${x},${y}`}
            stroke="#E7ECF4"
            strokeWidth={1}
          />
        ))}
        <Polygon
          fill="rgba(109, 93, 251, 0.24)"
          points={chartPoints}
          stroke="#6D5DFB"
          strokeWidth={2}
        />
        <SvgText fill="#64748B" fontSize="12" x="142" y="13">
          DS
        </SvgText>
        <SvgText fill="#64748B" fontSize="12" x="246" y="66">
          Algo
        </SvgText>
        <SvgText fill="#64748B" fontSize="12" x="247" y="181">
          OS
        </SvgText>
        <SvgText fill="#64748B" fontSize="12" x="141" y="238">
          DB
        </SvgText>
        <SvgText fill="#64748B" fontSize="12" x="20" y="181">
          Arch
        </SvgText>
        <SvgText fill="#64748B" fontSize="12" x="30" y="66">
          Net
        </SvgText>
      </Svg>

      <View style={styles.statGrid}>
        {AXES.map((axis, index) => (
          <View key={axis} style={styles.statItem}>
            <Text style={styles.statLabel}>{axis}</Text>
            <Text style={styles.statValue}>{values[index]}%</Text>
          </View>
        ))}
      </View>
    </View>
  );
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
