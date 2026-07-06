import { router } from "expo-router";
import { Pressable, StyleSheet, Text, View } from "react-native";

import { FONT } from "../../constants/fonts";
import { MyProfile } from "../../src/features/mypage/components/MyProfile";

export default function MyPageScreen() {
  return (
    <View style={styles.container}>
      <View style={styles.headerArea}>
        <Pressable
          accessibilityLabel="설정"
          accessibilityRole="button"
          hitSlop={10}
          style={styles.settingsButton}
          onPress={() => router.navigate("../settings")}
        >
          <Text style={styles.settingsIcon}>⚙</Text>
        </Pressable>

        <Text style={styles.title}>마이페이지</Text>
        <MyProfile />
      </View>

      <View style={styles.panel}>
        <Text style={styles.description}>마이페이지 화면을 준비 중입니다.</Text>
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
    position: "relative",
    minHeight: 304,
    justifyContent: "center",
    paddingHorizontal: 24,
    paddingTop: 20,
    paddingBottom: 28,
  },
  title: {
    color: "#ffffff",
    fontFamily: FONT.pretendard,
    fontSize: 28,
    fontWeight: "800",
    marginBottom: 24,
  },
  settingsButton: {
    position: "absolute",
    top: 24,
    right: 24,
    alignItems: "center",
    justifyContent: "center",
    width: 42,
    height: 42,
    borderRadius: 21,
    backgroundColor: "rgba(255, 255, 255, 0.16)",
    borderWidth: 1,
    borderColor: "rgba(255, 255, 255, 0.22)",
    zIndex: 10,
    elevation: 10,
  },
  settingsIcon: {
    color: "#ffffff",
    fontFamily: FONT.pretendard,
    fontSize: 22,
    fontWeight: "700",
    lineHeight: 24,
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
