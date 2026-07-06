import { Tabs } from "expo-router";
import { SafeAreaView, useSafeAreaInsets } from "react-native-safe-area-context";

import { FONT } from "../../constants/fonts";

export default function TabsLayout() {
  const insets = useSafeAreaInsets();
  const bottomInset = Math.max(insets.bottom, 10);

  return (
    <SafeAreaView edges={["top"]} style={{ flex: 1, backgroundColor: "#6D5DFB" }}>
      <Tabs
      screenOptions={{
        headerShown: false,
        tabBarActiveTintColor: "#6D5DFB",
        tabBarInactiveTintColor: "#94a3b8",
        tabBarStyle: {
          height: 58 + bottomInset,
          paddingTop: 6,
          paddingBottom: bottomInset,
        },
        tabBarLabelStyle: {
          fontFamily: FONT.pretendard,
          fontSize: 11,
          fontWeight: "600",
          lineHeight: 14,
        },
      }}
    >
      <Tabs.Screen name="home" options={{ title: "홈" }} />
      <Tabs.Screen name="friend" options={{ title: "친구" }} />
      <Tabs.Screen name="content" options={{ title: "연습노트" }} />
      <Tabs.Screen name="log-weak" options={{ title: "오답노트" }} />
      <Tabs.Screen name="mypage" options={{ title: "마이페이지" }} />
      </Tabs>
    </SafeAreaView>
  );
}
