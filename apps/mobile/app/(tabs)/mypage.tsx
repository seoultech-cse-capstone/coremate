import { router } from "expo-router";
import { BlurView } from "expo-blur";
import { Settings } from "lucide-react-native";
import { Pressable, StyleSheet, Text, View } from "react-native";
import { useSafeAreaInsets } from "react-native-safe-area-context";

import { FONT } from "../../constants/fonts";
import { MyProfile } from "../../src/features/mypage/components/MyProfile";

export default function MyPageScreen() {
  const insets = useSafeAreaInsets();

  return (
    <View style={styles.container}>
      <BlurView
        intensity={28}
        tint="dark"
        style={[
          styles.titleArea,
          {
            minHeight: 60 + insets.top,
            paddingTop: insets.top,
          },
        ]}
      >
        <View style={styles.headerOverlay} />
        <Text style={styles.title}>My Page</Text>

        <Pressable
          accessibilityLabel="설정"
          accessibilityRole="button"
          hitSlop={10}
          style={[styles.settingsButton, { top: 9 + insets.top }]}
          onPress={() => router.navigate("/settings")}
        >
          <Settings color="#ffffff" size={25} strokeWidth={2.2} />
        </Pressable>
      </BlurView>

      <View style={styles.profileArea}>
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
  titleArea: {
    position: "relative",
    minHeight: 60,
    flexDirection: "row",
    alignItems: "center",
    overflow: "hidden",
    paddingHorizontal: 16,
    paddingTop: 0,
  },
  headerOverlay: {
    ...StyleSheet.absoluteFillObject,
    backgroundColor: "rgba(109, 93, 251, 0.85)",
  },
  title: {
    flexShrink: 1,
    color: "#ffffff",
    fontFamily: FONT.pretendard,
    fontSize: 24,
    fontWeight: "700",
  },
  profileArea: {
    justifyContent: "center",
    paddingHorizontal: 24,
    paddingTop: 12,
    paddingBottom: 28,
  },
  settingsButton: {
    position: "absolute",
    top: 24,
    right: 16,
    alignItems: "center",
    justifyContent: "center",
    width: 42,
    height: 42,
    zIndex: 10,
    elevation: 10,
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
    shadowColor: "#111827",
    shadowOffset: { width: 0, height: -6 },
    shadowOpacity: 0.12,
    shadowRadius: 14,
  },
  description: {
    color: "#64748b",
    fontFamily: FONT.pretendard,
    fontSize: 15,
  },
});
