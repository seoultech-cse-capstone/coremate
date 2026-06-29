import { useEffect, useState } from "react";
import { useNavigation } from "expo-router";
import { Pressable, StyleSheet, Text, View } from "react-native";

import { FONT } from "../../constants/fonts";

type LogWeakMode = "wrongNote" | "weakness";

export default function LogWeakScreen() {
  const navigation = useNavigation();
  const [mode, setMode] = useState<LogWeakMode>("wrongNote");
  const isWrongNote = mode === "wrongNote";
  const title = isWrongNote ? "오답노트" : "약점분석";

  useEffect(() => {
    navigation.setOptions({ title });
  }, [navigation, title]);

  return (
    <View style={styles.container}>
      <View style={styles.titleArea}>
        <Text style={styles.title}>{title}</Text>
      </View>

      <View style={styles.panel}>
        <View style={styles.toggle}>
          <Pressable
            style={[styles.toggleButton, isWrongNote && styles.activeButton]}
            onPress={() => setMode("wrongNote")}
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
            onPress={() => setMode("weakness")}
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
    height: 148,
    justifyContent: "center",
    paddingHorizontal: 24,
  },
  title: {
    color: "#ffffff",
    fontFamily: FONT.pretendard,
    fontSize: 28,
    fontWeight: "800",
  },
  panel: {
    flex: 1,
    alignItems: "center",
    width: "100%",
    borderTopLeftRadius: 28,
    borderTopRightRadius: 28,
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
