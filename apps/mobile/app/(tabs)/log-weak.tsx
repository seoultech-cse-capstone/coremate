import { router, useLocalSearchParams, useNavigation } from "expo-router";
import { useCallback, useEffect, useState } from "react";
import { Pressable, StyleSheet, Text, View } from "react-native";

import { FONT } from "../../constants/fonts";

type LogWeakMode = "wrongNote" | "weakness";

type TabPressNavigation = {
  addListener: (eventName: "tabPress", callback: () => void) => () => void;
};

export default function LogWeakScreen() {
  const navigation = useNavigation();
  const { mode: modeParam } = useLocalSearchParams<{ mode?: string }>();
  const [mode, setMode] = useState<LogWeakMode>("wrongNote");
  const isWrongNote = mode === "wrongNote";
  const title = isWrongNote ? "Mistake Notes" : "Weakness Analysis";

  const updateMode = (nextMode: LogWeakMode) => {
    setMode(nextMode);
    router.setParams({ mode: nextMode });
  };

  const resetLogWeak = useCallback(() => {
    setMode("wrongNote");
    router.setParams({ mode: "wrongNote" });
  }, []);

  useEffect(() => {
    if (modeParam === "weakness") {
      setMode("weakness");
    }

    if (modeParam === "wrongNote") {
      setMode("wrongNote");
    }
  }, [modeParam]);

  useEffect(() => {
    navigation.setOptions({ title, tabBarLabel: title });
  }, [navigation, title]);

  useEffect(() => {
    const tabNavigation = navigation as unknown as TabPressNavigation;
    const unsubscribe = tabNavigation.addListener("tabPress", resetLogWeak);

    return unsubscribe;
  }, [navigation, resetLogWeak]);

  return (
    <View style={styles.container}>
      <View style={styles.titleArea}>
        <Text style={styles.title}>{title}</Text>
      </View>

      <View style={styles.panel}>
        <View style={styles.toggle}>
          <Pressable
            style={[styles.toggleButton, isWrongNote && styles.activeButton]}
            onPress={() => updateMode("wrongNote")}
          >
            <Text
              style={[
                styles.toggleText,
                isWrongNote && styles.activeToggleText,
              ]}
            >
              오답노트
            </Text>
          </Pressable>

          <Pressable
            style={[styles.toggleButton, !isWrongNote && styles.activeButton]}
            onPress={() => updateMode("weakness")}
          >
            <Text
              style={[
                styles.toggleText,
                !isWrongNote && styles.activeToggleText,
              ]}
            >
              약점분석
            </Text>
          </Pressable>
        </View>

        <Text style={styles.description}>
          {isWrongNote
            ? "오답노트 화면을 준비 중입니다."
            : "약점분석 화면을 준비 중입니다."}
        </Text>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: "#6D5DFB",
  },
  titleArea: {
    minHeight: 82,
    flexDirection: "row",
    alignItems: "center",
    backgroundColor: "rgba(109, 93, 251, 0.8)",
    paddingHorizontal: 16,
    paddingTop: 8,
  },
  title: {
    flexShrink: 1,
    color: "#ffffff",
    fontFamily: FONT.pretendard,
    fontSize: 24,
    fontWeight: "700",
  },
  panel: {
    flex: 1,
    alignItems: "center",
    width: "100%",
    borderTopLeftRadius: 18,
    borderTopRightRadius: 18,
    backgroundColor: "#F3F3F3",
    paddingHorizontal: 24,
    paddingTop: 32,
  },
  toggle: {
    flexDirection: "row",
    width: "100%",
    maxWidth: 320,
    padding: 4,
    borderRadius: 14,
    backgroundColor: "#D9D9D9",
    marginBottom: 28,
  },
  toggleButton: {
    flex: 1,
    alignItems: "center",
    borderRadius: 10,
    paddingVertical: 10,
  },
  activeButton: {
    backgroundColor: "#ffffff",
  },
  toggleText: {
    color: "#64748b",
    fontFamily: FONT.pretendard,
    fontSize: 14,
    fontWeight: "700",
  },
  activeToggleText: {
    color: "#6D5DFB",
  },
  description: {
    color: "#64748b",
    fontFamily: FONT.pretendard,
    fontSize: 15,
  },
});
