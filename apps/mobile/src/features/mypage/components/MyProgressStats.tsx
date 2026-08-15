import { router } from "expo-router";
import { Bookmark, PackageOpen, ShoppingBag } from "lucide-react-native";
import { Pressable, StyleSheet, Text, View } from "react-native";
import Svg, { Circle, Line, Polygon, Text as SvgText } from "react-native-svg";

import { FONT } from "../../../../constants/fonts";
import { useAuth } from "../../auth";
import { useSubjects } from "../../content";
import { useMySubjectStats } from "../../user";

const CHART_SIZE = 220;
const CENTER = CHART_SIZE / 2;
const RADIUS = 76;
const GRID_LEVELS = [0.25, 0.5, 0.75, 1];

const radarOrder = ["DS", "Algo", "OS", "DB", "Arch", "Net"];

// 백엔드 subjectCode(과목 코드) → 레이더 차트에 쓰는 짧은 라벨.
const SHORT_TITLE_BY_SUBJECT_CODE: Record<string, string> = {
  DS: "DS",
  ALGO: "Algo",
  OS: "OS",
  DB: "DB",
  CA: "Arch",
  NETWORK: "Net",
};

const actionItems = [
  {
    label: "즐겨찾기 문제 목록",
    Icon: Bookmark,
    route: "/favorite-problems",
  },
  {
    label: "아바타 샵",
    Icon: ShoppingBag,
    route: "/avatar-shop",
  },
  {
    label: "아바타 인벤토리",
    Icon: PackageOpen,
    route: "/avatar-inventory",
  },
] as const;

function getAngle(index: number) {
  return -Math.PI / 2 + (Math.PI * 2 * index) / radarOrder.length;
}

function getPoint(index: number, radius: number) {
  const angle = getAngle(index);

  return {
    x: CENTER + Math.cos(angle) * radius,
    y: CENTER + Math.sin(angle) * radius,
  };
}

function getPolygonPoints(radius: number) {
  return radarOrder
    .map((_, index) => {
      const point = getPoint(index, radius);

      return `${point.x},${point.y}`;
    })
    .join(" ");
}

function getValuePoints(percentByShortTitle: Record<string, number>) {
  return radarOrder
    .map((shortTitle, index) => {
      const percent = percentByShortTitle[shortTitle] ?? 0;
      const point = getPoint(index, RADIUS * (percent / 100));

      return `${point.x},${point.y}`;
    })
    .join(" ");
}

export function MyProgressStats() {
  const { accessToken } = useAuth();
  const { data: subjects = [] } = useSubjects();
  const { data: subjectStats = [] } = useMySubjectStats(accessToken);

  const shortTitleBySubjectId = new Map<number, string>();
  for (const subject of subjects) {
    const shortTitle = SHORT_TITLE_BY_SUBJECT_CODE[subject.subjectCode];
    if (shortTitle) {
      shortTitleBySubjectId.set(subject.subjectId, shortTitle);
    }
  }

  const percentByShortTitle: Record<string, number> = {};
  for (const stat of subjectStats) {
    const shortTitle = shortTitleBySubjectId.get(stat.subjectId);
    if (shortTitle) {
      percentByShortTitle[shortTitle] = Math.round(parseFloat(stat.achievementRate) * 100);
    }
  }

  return (
    <View style={styles.container}>
      <Text style={styles.sectionTitle}>Progress Stats</Text>

      <View style={styles.card}>
        <Svg width={CHART_SIZE} height={CHART_SIZE}>
          {GRID_LEVELS.map((level) => (
            <Polygon
              fill="none"
              key={level}
              points={getPolygonPoints(RADIUS * level)}
              stroke="#E5E7EB"
              strokeWidth={1}
            />
          ))}

          {radarOrder.map((label, index) => {
            const endPoint = getPoint(index, RADIUS);

            return (
              <Line
                key={label}
                stroke="#E5E7EB"
                strokeWidth={1}
                x1={CENTER}
                x2={endPoint.x}
                y1={CENTER}
                y2={endPoint.y}
              />
            );
          })}

          <Polygon
            fill="rgba(109, 93, 251, 0.32)"
            points={getValuePoints(percentByShortTitle)}
            stroke="#6D5DFB"
            strokeLinejoin="round"
            strokeWidth={2}
          />

          <Circle cx={CENTER} cy={CENTER} fill="#6D5DFB" r={2.5} />

          {radarOrder.map((label, index) => {
            const point = getPoint(index, RADIUS + 20);

            return (
              <SvgText
                fill="#64748b"
                fontSize={12}
                fontWeight="600"
                key={label}
                textAnchor="middle"
                x={point.x}
                y={point.y + 4}
              >
                {label}
              </SvgText>
            );
          })}
        </Svg>

        <View style={styles.statGrid}>
          {radarOrder.map((shortTitle) => (
            <View style={styles.statItem} key={shortTitle}>
              <Text style={styles.statLabel}>{shortTitle}</Text>
              <Text style={styles.statValue}>{percentByShortTitle[shortTitle] ?? 0}%</Text>
            </View>
          ))}
        </View>
      </View>

      <View style={styles.actionList}>
        {actionItems.map(({ Icon, label, route }) => (
          <Pressable
            key={label}
            style={styles.actionButton}
            onPress={() => router.push(route)}
          >
            <View style={styles.actionIconBox}>
              <Icon color="#6D5DFB" size={22} strokeWidth={2.2} />
            </View>
            <Text style={styles.actionText}>{label}</Text>
          </Pressable>
        ))}
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    gap: 14,
  },
  sectionTitle: {
    color: "#111827",
    fontFamily: FONT.kufam,
    fontSize: 18,
    fontWeight: "600",
  },
  card: {
    alignItems: "center",
    borderRadius: 18,
    backgroundColor: "#ffffff",
    paddingHorizontal: 18,
    paddingTop: 18,
    paddingBottom: 20,
  },
  statGrid: {
    width: "100%",
    flexDirection: "row",
    flexWrap: "wrap",
    rowGap: 16,
  },
  statItem: {
    width: "33.333%",
    alignItems: "center",
  },
  statLabel: {
    color: "#64748b",
    fontFamily: FONT.kufam,
    fontSize: 12,
    fontWeight: "500",
  },
  statValue: {
    marginTop: 4,
    color: "#1f2937",
    fontFamily: FONT.kufam,
    fontSize: 15,
    fontWeight: "700",
  },
  actionList: {
    gap: 12,
  },
  actionButton: {
    minHeight: 62,
    flexDirection: "row",
    alignItems: "center",
    borderRadius: 18,
    backgroundColor: "#ffffff",
    paddingHorizontal: 16,
  },
  actionIconBox: {
    alignItems: "center",
    justifyContent: "center",
    width: 40,
    height: 40,
    borderRadius: 14,
    backgroundColor: "rgba(109, 93, 251, 0.12)",
  },
  actionText: {
    marginLeft: 14,
    color: "#111827",
    fontFamily: FONT.pretendard,
    fontSize: 16,
    fontWeight: "600",
  },
});
