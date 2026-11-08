import { router, Tabs } from "expo-router";
import type { BottomTabBarButtonProps } from "expo-router/build/react-navigation/bottom-tabs/types";
import {
  BookOpenCheck,
  ChartNoAxesCombined,
  Home,
  UserRound,
  Users,
} from "lucide-react-native";
import { Pressable, View } from "react-native";
import { useSafeAreaInsets } from "react-native-safe-area-context";

import { FONT } from "../../constants/fonts";
import { useAuth } from "../../src/features/auth";
import {
  LogWeakSelectionProvider,
  useLogWeakSelection,
} from "../../src/features/logWeak/LogWeakSelectionContext";
import { spotlightRef } from "../../src/features/onboarding/spotlightRegistry";

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

  // 회원이 아니면(게스트든 로그아웃 상태든) 회원 전용 탭(친구/복습/마이페이지)을 눌렀을 때
  // 탭을 바꾸지 않고 바로 로그인 화면으로 보낸다. 막아 두기만 하면 왜 안 눌리는지 알 수 없다(#239).
  const createTabBarButton = (isMemberOnly: boolean, tabName: string) => {
    return (props: BottomTabBarButtonProps) => {
      const { ref: _ref, onPress, ...buttonProps } = props;
      const locked = isMemberLocked && isMemberOnly;

      return (
        <Pressable
          {...buttonProps}
          ref={spotlightRef(`tab-${tabName}`)}
          disabled={isLogWeakSelecting}
          onPress={locked ? () => router.replace("/") : onPress}
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
            tabBarButton: createTabBarButton(false, "home"),
            tabBarIcon: ({ color }) => (
              <Home color={color} size={22} strokeWidth={2.1} />
            ),
          }}
        />
        <Tabs.Screen
          name="friend"
          options={{
            title: "친구",
            tabBarButton: createTabBarButton(true, "friend"),
            tabBarIcon: ({ color }) => (
              <Users color={color} size={22} strokeWidth={2.1} />
            ),
          }}
        />
        <Tabs.Screen
          name="content"
          options={{
            title: "문제집",
            tabBarButton: createTabBarButton(false, "content"),
            tabBarIcon: ({ color }) => (
              <BookOpenCheck color={color} size={22} strokeWidth={2.1} />
            ),
          }}
        />
        <Tabs.Screen
          name="log-weak"
          options={{
            title: "복습",
            tabBarButton: createTabBarButton(true, "log-weak"),
            tabBarIcon: ({ color }) => (
              <ChartNoAxesCombined color={color} size={22} strokeWidth={2.1} />
            ),
          }}
        />
        <Tabs.Screen
          name="mypage"
          options={{
            title: "마이페이지",
            tabBarButton: createTabBarButton(true, "mypage"),
            tabBarIcon: ({ color }) => (
              <UserRound color={color} size={22} strokeWidth={2.1} />
            ),
          }}
        />
      </Tabs>
    </View>
  );
}
