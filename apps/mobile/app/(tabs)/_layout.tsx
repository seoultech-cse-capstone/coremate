import { Tabs } from "expo-router";
import type { BottomTabBarButtonProps } from "@react-navigation/bottom-tabs";
import {
  BookOpenCheck,
  ChartNoAxesCombined,
  Home,
  UserRound,
  Users,
} from "lucide-react-native";
import { Pressable, StyleSheet, View } from "react-native";
import { useSafeAreaInsets } from "react-native-safe-area-context";

import { FONT } from "../../constants/fonts";
import { useAuth } from "../../src/features/auth";
import {
  LogWeakSelectionProvider,
  useLogWeakSelection,
} from "../../src/features/logWeak/LogWeakSelectionContext";

const BRAND_PURPLE = "#6D5DFB";
const TAB_GRAY = "#94a3b8";

export default function TabsLayout() {
  const { status } = useAuth();
  return (
    <LogWeakSelectionProvider>
      {/* status가 바뀔 때마다 재마운트해 로그아웃 전 마지막 탭(예: 마이페이지) 기억을 초기화한다 */}
      <TabsNavigator key={status} />
    </LogWeakSelectionProvider>
  );
}

function TabsNavigator() {
  const insets = useSafeAreaInsets();
  const { isLogWeakSelecting } = useLogWeakSelection();
  const { status } = useAuth();
  const isMemberLocked = status !== "authenticated";

  // 회원이 아니면(게스트든 로그아웃 상태든) 잠긴 탭(친구/오답노트/마이페이지)을 흐리게 표시하고 비활성화한다.
  const createTabBarButton = (isMemberOnly: boolean) => {
    return (props: BottomTabBarButtonProps) => {
      const { ref: _ref, style, ...buttonProps } = props;
      const locked = isMemberLocked && isMemberOnly;

      return (
        <Pressable
          {...buttonProps}
          disabled={isLogWeakSelecting || locked}
          style={[style, locked && styles.lockedTabButton]}
        />
      );
    };
  };

  return (
    <View style={{ flex: 1, backgroundColor: BRAND_PURPLE }}>
      <Tabs
        screenOptions={{
          headerShown: false,
          tabBarActiveTintColor: BRAND_PURPLE,
          tabBarInactiveTintColor: TAB_GRAY,
          tabBarStyle: {
            height: 58 + insets.bottom,
            paddingTop: 6,
            paddingBottom: 0,
            backgroundColor: "#ffffff",
          },
          tabBarIconStyle: {
            backgroundColor: "transparent",
          },
          tabBarLabelStyle: {
            fontFamily: FONT.pretendard,
            fontSize: 11,
            fontWeight: "600",
            lineHeight: 14,
          },
        }}
      >
        <Tabs.Screen
          name="home"
          options={{
            title: "홈",
            tabBarButton: createTabBarButton(false),
            tabBarIcon: ({ color }) => (
              <Home color={color} size={22} strokeWidth={2.1} />
            ),
          }}
        />
        <Tabs.Screen
          name="friend"
          options={{
            title: "친구",
            tabBarButton: createTabBarButton(true),
            tabBarIcon: ({ color }) => (
              <Users color={color} size={22} strokeWidth={2.1} />
            ),
          }}
        />
        <Tabs.Screen
          name="content"
          options={{
            title: "문제집",
            tabBarButton: createTabBarButton(false),
            tabBarIcon: ({ color }) => (
              <BookOpenCheck color={color} size={22} strokeWidth={2.1} />
            ),
          }}
        />
        <Tabs.Screen
          name="log-weak"
          options={{
            title: "복습",
            tabBarButton: createTabBarButton(true),
            tabBarIcon: ({ color }) => (
              <ChartNoAxesCombined color={color} size={22} strokeWidth={2.1} />
            ),
          }}
        />
        <Tabs.Screen
          name="mypage"
          options={{
            title: "마이페이지",
            tabBarButton: createTabBarButton(true),
            tabBarIcon: ({ color }) => (
              <UserRound color={color} size={22} strokeWidth={2.1} />
            ),
          }}
        />
      </Tabs>
    </View>
  );
}

const styles = StyleSheet.create({
  lockedTabButton: {
    opacity: 0.35,
  },
});
