import { LinearGradient } from "expo-linear-gradient";
import { StyleSheet, Text, View } from "react-native";

import { FONT } from "../../constants/fonts";

const progressItems = [
  { title: "Data Structure", percent: 85 },
  { title: "Algorithm", percent: 100 },
  { title: "Network", percent: 62 },
  { title: "Database", percent: 34 },
  { title: "Operating System", percent: 8 },
  { title: "Computer Architecture", percent: 76 },
];

function getProgressColor(percent: number) {
  if (percent <= 10) {
    return "#111827";
  }

  if (percent <= 34) {
    return "#22C55E";
  }

  if (percent <= 59) {
    return "#FACC15";
  }

  if (percent <= 79) {
    return "#F97316";
  }

  return "#EF4444";
}

export function YourProgress() {
  return (
    <>
      <Text style={styles.title}>Your Progress</Text>

      <View style={styles.card}>
        {progressItems.map((item) => (
          <View style={styles.item} key={item.title}>
            <Text style={styles.name}>{item.title}</Text>

            <View style={styles.row}>
              <View style={styles.track}>
                {item.percent === 100 ? (
                  <LinearGradient
                    colors={["#6D5DFB", "#2EE6A6"]}
                    start={{ x: 0, y: 0.5 }}
                    end={{ x: 1, y: 0.5 }}
                    style={[styles.fill, { width: `${item.percent}%` }]}
                  />
                ) : (
                  <View
                    style={[
                      styles.fill,
                      {
                        width: `${item.percent}%`,
                        backgroundColor: getProgressColor(item.percent),
                      },
                    ]}
                  />
                )}
              </View>

              <Text style={styles.percent}>{item.percent}%</Text>
            </View>
          </View>
        ))}
      </View>
    </>
  );
}

const styles = StyleSheet.create({
  title: {
    marginTop: 26,
    marginBottom: 12,
    color: "#111827",
    fontFamily: FONT.kufam,
    fontSize: 18,
    fontWeight: "500",
  },
  card: {
    borderRadius: 18,
    backgroundColor: "#ffffff",
    paddingHorizontal: 18,
    paddingVertical: 18,
  },
  item: {
    marginBottom: 16,
  },
  name: {
    color: "#1f2937",
    fontFamily: FONT.kufam,
    fontSize: 13,
    fontWeight: "500",
    marginBottom: 8,
  },
  row: {
    flexDirection: "row",
    alignItems: "center",
    gap: 10,
  },
  track: {
    flex: 1,
    height: 9,
    borderRadius: 5,
    backgroundColor: "#D9D9D9",
    overflow: "hidden",
  },
  fill: {
    height: "100%",
    borderRadius: 5,
  },
  percent: {
    width: 38,
    color: "#1f2937",
    fontFamily: FONT.kufam,
    fontSize: 12,
    fontWeight: "500",
    textAlign: "right",
  },
});
