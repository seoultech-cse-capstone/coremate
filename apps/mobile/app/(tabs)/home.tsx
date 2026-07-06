import { LinearGradient } from "expo-linear-gradient";
import { router, useNavigation } from "expo-router";
import { useEffect, useRef, useState } from "react";
import {
  type NativeScrollEvent,
  type NativeSyntheticEvent,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  useWindowDimensions,
  View,
} from "react-native";

import { FONT } from "../../constants/fonts";
import { YourProgress } from "../../src/features/home/components/YourProgress";

const BRAND_PURPLE = "#6D5DFB";
const PANEL_GRAY = "#F3F3F3";
const OVERSCROLL_BUFFER = 96;

type TabPressNavigation = {
  addListener: (eventName: "tabPress", callback: () => void) => () => void;
};

export default function HomeScreen() {
  const navigation = useNavigation();
  const scrollRef = useRef<ScrollView>(null);
  const { height } = useWindowDimensions();
  const [bounceBackground, setBounceBackground] = useState(BRAND_PURPLE);
  const panelTopOffset = Math.round(height * 0.46);

  useEffect(() => {
    const tabNavigation = navigation as unknown as TabPressNavigation;
    const unsubscribe = tabNavigation.addListener("tabPress", () => {
      scrollRef.current?.scrollTo({ y: 0, animated: true });
    });

    return unsubscribe;
  }, [navigation]);

  const handleScroll = (event: NativeSyntheticEvent<NativeScrollEvent>) => {
    const { contentOffset, contentSize, layoutMeasurement } = event.nativeEvent;
    const maxScrollY = Math.max(
      0,
      contentSize.height - layoutMeasurement.height,
    );
    const isNearBottom =
      maxScrollY > 0 && contentOffset.y > maxScrollY - OVERSCROLL_BUFFER;
    const nextBackground = isNearBottom ? PANEL_GRAY : BRAND_PURPLE;

    setBounceBackground((currentBackground) =>
      currentBackground === nextBackground ? currentBackground : nextBackground,
    );
  };

  return (
    <ScrollView
      bounces
      onScroll={handleScroll}
      ref={scrollRef}
      scrollEventThrottle={16}
      style={[styles.container, { backgroundColor: bounceBackground }]}
      showsVerticalScrollIndicator={false}
      contentContainerStyle={[
        styles.scrollContent,
        { paddingTop: panelTopOffset },
      ]}
    >
      <View style={[styles.bottomPanel, { minHeight: height }]}>
        <View style={styles.panelContent}>
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
                style={[styles.secondaryButtonText, styles.weaknessButtonText]}
              >
                약점 분석하기
              </Text>
            </Pressable>
          </View>

          <YourProgress />
        </View>
      </View>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  scrollContent: {
    backgroundColor: BRAND_PURPLE,
  },
  bottomPanel: {
    width: "100%",
    borderTopLeftRadius: 28,
    borderTopRightRadius: 28,
    backgroundColor: PANEL_GRAY,
    paddingHorizontal: 24,
    paddingTop: 32,
    shadowColor: "#111827",
    shadowOffset: { width: 0, height: -6 },
    shadowOpacity: 0.12,
    shadowRadius: 14,
  },
  panelContent: {
    paddingBottom: 28 + OVERSCROLL_BUFFER,
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
    backgroundColor: BRAND_PURPLE,
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
