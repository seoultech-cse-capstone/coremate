import { LinearGradient } from "expo-linear-gradient";
import { StyleSheet, View } from "react-native";

type ProgressLineProps = {
  progress: number;
};

function getProgressColor(progress: number) {
  if (progress <= 10) {
    return "#111827";
  }

  if (progress <= 34) {
    return "#22C55E";
  }

  if (progress <= 59) {
    return "#FACC15";
  }

  if (progress <= 79) {
    return "#F97316";
  }

  return "#EF4444";
}

export function ProgressLine({ progress }: ProgressLineProps) {
  const safeProgress = Math.max(0, Math.min(progress, 100));

  return (
    <View style={styles.track}>
      {safeProgress === 100 ? (
        <LinearGradient
          colors={["#6D5DFB", "#2EE6A6"]}
          start={{ x: 0, y: 0.5 }}
          end={{ x: 1, y: 0.5 }}
          style={[styles.fill, { width: `${safeProgress}%` }]}
        />
      ) : (
        <View
          style={[
            styles.fill,
            {
              width: `${safeProgress}%`,
              backgroundColor: getProgressColor(safeProgress),
            },
          ]}
        />
      )}
      <View style={[styles.progressDot, { left: `${safeProgress}%` }]} />
    </View>
  );
}

const styles = StyleSheet.create({
  track: {
    position: "relative",
    height: 4,
    borderRadius: 999,
    backgroundColor: "#D6DCE0",
    overflow: "hidden",
  },
  fill: {
    height: "100%",
    borderRadius: 999,
  },
  progressDot: {
    position: "absolute",
    top: 0,
    width: 4,
    height: 4,
    marginLeft: -2,
    borderRadius: 2,
    backgroundColor: "#3B82F6",
  },
});
