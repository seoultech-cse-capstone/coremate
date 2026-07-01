import { Tabs } from "expo-router";

import { FONT } from "../../constants/fonts";

export default function TabsLayout() {
  return (
    <Tabs
      screenOptions={{
        headerShown: false,
        tabBarActiveTintColor: "#6D5DFB",
        tabBarInactiveTintColor: "#94a3b8",
        tabBarStyle: {
          height: 68,
          paddingTop: 6,
          paddingBottom: 10,
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
  );
}
