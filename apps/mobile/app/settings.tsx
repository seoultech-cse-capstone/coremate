import { router } from "expo-router";
import { Pressable, StyleSheet, Text, View } from "react-native";

import { FONT } from "../constants/fonts";

export default function SettingsScreen() {
  return (
    <View style={styles.container}>
      <View style={styles.headerArea}>
        <Pressable style={styles.backButton} onPress={() => router.back()}>
          <Text style={styles.backButtonText}>‹</Text>
        </Pressable>
        <Text style={styles.title}>설정</Text>
      </View>

      <View style={styles.panel}>
        <Text style={styles.description}>설정 화면을 준비 중입니다.</Text>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: "#6D5DFB",
  },
  headerArea: {
    height: 148,
    justifyContent: "center",
    paddingHorizontal: 24,
  },
  backButton: {
    position: "absolute",
    left: 20,
    top: 34,
    alignItems: "center",
    justifyContent: "center",
    width: 42,
    height: 42,
    borderRadius: 21,
    backgroundColor: "rgba(255, 255, 255, 0.16)",
  },
  backButtonText: {
    color: "#ffffff",
    fontFamily: FONT.pretendard,
    fontSize: 34,
    fontWeight: "500",
    lineHeight: 36,
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
    justifyContent: "center",
    width: "100%",
    borderTopLeftRadius: 28,
    borderTopRightRadius: 28,
    backgroundColor: "#F3F3F3",
    paddingHorizontal: 24,
  },
  description: {
    color: "#64748b",
    fontFamily: FONT.pretendard,
    fontSize: 15,
  },
});
