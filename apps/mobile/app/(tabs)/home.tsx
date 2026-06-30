import { LinearGradient } from "expo-linear-gradient";
import { router } from "expo-router";
import {
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  useWindowDimensions,
  View,
} from "react-native";

import { FONT } from "../../constants/fonts";
import { YourProgress } from "../../src/features/home/components/YourProgress";

export default function HomeScreen() {
  const { height } = useWindowDimensions();
  const panelTopOffset = Math.round(height * 0.42);

  return (
    <View style={styles.screen}>
      <View style={{ height: panelTopOffset }} />

      <View style={styles.bottomPanel}>
        <ScrollView
          style={styles.panelScroll}
          showsVerticalScrollIndicator={false}
          contentContainerStyle={[
            styles.panelContent,
            { paddingBottom: Math.round(height * 0.45) },
          ]}
        >
          <View style={styles.levelBarTrack}>
            <LinearGradient
              colors={["#6D5DFB", "#2EE6A6"]}
              start={{ x: 0, y: 0.5 }}
              end={{ x: 1, y: 0.5 }}
              style={styles.levelBarFill}
            >
              <Text style={styles.levelText}>Lv.1</Text>
            </LinearGradient>
          </View>

          <Pressable style={styles.primaryButton}>
            <Text style={styles.primaryButtonText}>오늘의 퀴즈</Text>
          </Pressable>

          <View style={styles.secondaryRow}>
            <Pressable
              style={styles.secondaryButton}
              onPress={() => router.push("/(tabs)/content")}
            >
              <Text style={styles.secondaryButtonText}>연습문제 풀이</Text>
            </Pressable>

            <Pressable
              style={[styles.secondaryButton, styles.weaknessButton]}
              onPress={() =>
                router.push({
                  pathname: "/(tabs)/log-weak",
                  params: { mode: "weakness", entry: Date.now().toString() },
                })
              }
            >
              <Text
                style={[
                  styles.secondaryButtonText,
                  styles.weaknessButtonText,
                ]}
              >
                약점 분석하기
              </Text>
            </Pressable>
          </View>

          <YourProgress />
        </ScrollView>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  screen: {
    flex: 1,
    backgroundColor: "#6D5DFB",
  },
  bottomPanel: {
    flex: 1,
    width: "100%",
    borderTopLeftRadius: 28,
    borderTopRightRadius: 28,
    backgroundColor: "#F3F3F3",
    overflow: "hidden",
  },
  panelScroll: {
    flex: 1,
    backgroundColor: "#F3F3F3",
  },
  panelContent: {
    paddingHorizontal: 24,
    paddingTop: 32,
  },
  levelBarTrack: {
    width: "100%",
    height: 24,
    borderRadius: 12,
    backgroundColor: "#D9D9D9",
    overflow: "hidden",
    marginBottom: 18,
  },
  levelBarFill: {
    width: "38%",
    height: "100%",
    justifyContent: "center",
    borderRadius: 12,
    paddingLeft: 12,
  },
  levelText: {
    color: "#ffffff",
    fontFamily: FONT.kufam,
    fontSize: 12,
    fontWeight: "700",
  },
  primaryButton: {
    alignItems: "center",
    justifyContent: "center",
    minHeight: 64,
    borderRadius: 18,
    backgroundColor: "#6D5DFB",
  },
  primaryButtonText: {
    color: "#ffffff",
    fontFamily: FONT.pretendard,
    fontSize: 18,
    fontWeight: "500",
  },
  secondaryRow: {
    flexDirection: "row",
    gap: 12,
    marginTop: 14,
  },
  secondaryButton: {
    flex: 1,
    alignItems: "center",
    justifyContent: "center",
    minHeight: 72,
    borderRadius: 18,
    backgroundColor: "#ffffff",
    paddingHorizontal: 12,
  },
  secondaryButtonText: {
    color: "#1f2937",
    fontFamily: FONT.pretendard,
    fontSize: 15,
    fontWeight: "500",
    textAlign: "center",
  },
  weaknessButton: {
    backgroundColor: "#111827",
  },
  weaknessButtonText: {
    color: "#ffffff",
  },
});
