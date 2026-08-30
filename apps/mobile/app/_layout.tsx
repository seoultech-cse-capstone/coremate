import { QueryClientProvider, focusManager } from "@tanstack/react-query";
import { Stack } from "expo-router";
import { useFonts } from "expo-font";
import { StatusBar } from "expo-status-bar";
import { useEffect } from "react";
import { AppState } from "react-native";
import { SafeAreaProvider } from "react-native-safe-area-context";

import { FONT } from "../constants/fonts";
import { AuthProvider, SessionRevokedModal } from "../src/features/auth";
import { RoomFurnitureProvider, RoomThemeProvider } from "../src/features/avatar";
import { usePushRegistration } from "../src/features/notification";
import { queryClient } from "../src/shared/api/queryClient";
import { LanguageProvider } from "../src/shared/language/LanguageContext";

// 기기 토큰 등록·해제와 알림 탭 딥링크(FR-NOTI-02).
//
// RootLayout 이 아니라 별도 컴포넌트로 둔 이유는, RootLayout 이 AuthProvider 를 "렌더링하는"
// 쪽이라 그 안에서 useAuth 를 부르면 아직 Provider 바깥이라 앱이 통째로 뜨지 않는다.
// 화면에 그릴 것이 없어 null 을 반환한다.
function PushRegistration() {
  usePushRegistration();
  return null;
}

export default function RootLayout() {
  const [fontsLoaded] = useFonts({
    [FONT.pretendard]: require("../assets/fonts/PretendardVariable.ttf"),
    [FONT.kufam]: require("../assets/fonts/Kufam-VariableFont_wght.ttf"),
  });

  // react-query 의 포커스 재조회는 웹의 window focus 이벤트를 전제한다.
  // RN 에는 그 이벤트가 없어 직접 연결해 주지 않으면, 앱을 내렸다 올려도
  // 그 사이 서버에서 바뀐 내용이 화면에 반영되지 않는다.
  useEffect(() => {
    const subscription = AppState.addEventListener("change", (nextState) => {
      focusManager.setFocused(nextState === "active");
    });

    return () => subscription.remove();
  }, []);

  if (!fontsLoaded) {
    return null;
  }

  return (
    <QueryClientProvider client={queryClient}>
      <AuthProvider>
        <PushRegistration />
        <RoomFurnitureProvider>
        <RoomThemeProvider>
        <LanguageProvider>
        <SafeAreaProvider>
          <Stack
        screenOptions={{
          gestureEnabled: false,
          animation: "fade",
          animationDuration: 120,
          headerStyle: { backgroundColor: "#ffffff" },
          headerTitleStyle: {
            fontFamily: FONT.pretendard,
            fontWeight: "700",
          },
          headerTintColor: "#1f2937",
          contentStyle: { backgroundColor: "#f8fafc" },
        }}
      >
        <Stack.Screen name="index" options={{ headerShown: false }} />
        <Stack.Screen name="email-login" options={{ headerShown: false }} />
        <Stack.Screen name="register" options={{ headerShown: false }} />
        <Stack.Screen name="find-account" options={{ headerShown: false }} />
        <Stack.Screen
          name="login-required"
          options={{ headerShown: false }}
        />
        <Stack.Screen name="settings" options={{ headerShown: false }} />
        <Stack.Screen name="edit-profile" options={{ headerShown: false }} />
        <Stack.Screen name="edit-nickname" options={{ headerShown: false }} />
        <Stack.Screen
          name="change-password"
          options={{ headerShown: false }}
        />
        <Stack.Screen
          name="notification-settings"
          options={{ headerShown: false }}
        />
        <Stack.Screen
          name="language-settings"
          options={{ headerShown: false }}
        />
        <Stack.Screen name="terms-policies" options={{ headerShown: false }} />
        <Stack.Screen name="privacy-policy" options={{ headerShown: false }} />
        <Stack.Screen
          name="marketing-consent"
          options={{ headerShown: false }}
        />
        <Stack.Screen name="delete-account" options={{ headerShown: false }} />
        <Stack.Screen
          name="favorite-problems"
          options={{ headerShown: false }}
        />
        <Stack.Screen
          name="favorite-problem-detail"
          options={{ headerShown: false }}
        />
        <Stack.Screen name="avatar-shop" options={{ headerShown: false }} />
        <Stack.Screen
          name="avatar-inventory"
          options={{ headerShown: false }}
        />
        <Stack.Screen name="random-box" options={{ headerShown: false }} />
        <Stack.Screen name="room-inventory" options={{ headerShown: false }} />
        <Stack.Screen
          name="mistake-exam-review"
          options={{ headerShown: false }}
        />
        <Stack.Screen
          name="concept-detail"
          options={{ headerShown: false }}
        />
        <Stack.Screen name="solve" options={{ headerShown: false }} />
        <Stack.Screen name="solve-result" options={{ headerShown: false }} />
        <Stack.Screen name="daily-quiz" options={{ headerShown: false }} />
        <Stack.Screen name="daily-quiz-result" options={{ headerShown: false }} />
        <Stack.Screen name="daily-quiz-review" options={{ headerShown: false }} />
        <Stack.Screen name="friend-profile" options={{ headerShown: false }} />
        <Stack.Screen name="weakness-confirm" options={{ headerShown: false }} />
        <Stack.Screen
          name="weakness-generating"
          options={{ headerShown: false }}
        />
        <Stack.Screen name="weakness-report" options={{ headerShown: false }} />
        <Stack.Screen name="attendance" options={{ headerShown: false }} />
        <Stack.Screen name="reward-history" options={{ headerShown: false }} />
        <Stack.Screen name="device-management" options={{ headerShown: false }} />
        <Stack.Screen name="notifications" options={{ headerShown: false }} />
        <Stack.Screen name="(tabs)" options={{ headerShown: false }} />
          </Stack>
          {/* Stack 바깥에 둔다. 세션이 끊기면 (tabs) 네비게이터가 재마운트되면서
              로그인 화면으로 리다이렉트되는데, 화면 안에 있으면 모달이 뜨기 전에
              언마운트된다. */}
          <SessionRevokedModal />
          <StatusBar style="dark" />
        </SafeAreaProvider>
        </LanguageProvider>
        </RoomThemeProvider>
        </RoomFurnitureProvider>
      </AuthProvider>
    </QueryClientProvider>
  );
}
