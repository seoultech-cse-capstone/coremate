import { QueryClientProvider } from "@tanstack/react-query";
import { Stack } from "expo-router";
import { useFonts } from "expo-font";
import { StatusBar } from "expo-status-bar";
import { SafeAreaProvider } from "react-native-safe-area-context";

import { FONT } from "../constants/fonts";
import { AuthProvider } from "../src/features/auth";
import { queryClient } from "../src/shared/api/queryClient";

export default function RootLayout() {
  const [fontsLoaded] = useFonts({
    [FONT.pretendard]: require("../assets/fonts/PretendardVariable.ttf"),
    [FONT.kufam]: require("../assets/fonts/Kufam-VariableFont_wght.ttf"),
  });

  if (!fontsLoaded) {
    return null;
  }

  return (
    <QueryClientProvider client={queryClient}>
      <AuthProvider>
        <SafeAreaProvider>
          <Stack
        screenOptions={{
          gestureEnabled: false,
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
        <Stack.Screen name="login" options={{ headerShown: false }} />
        <Stack.Screen name="register" options={{ title: "회원가입" }} />
        <Stack.Screen name="settings" options={{ headerShown: false }} />
        <Stack.Screen name="edit-profile" options={{ headerShown: false }} />
        <Stack.Screen name="edit-nickname" options={{ headerShown: false }} />
        <Stack.Screen
          name="notification-settings"
          options={{ headerShown: false }}
        />
        <Stack.Screen name="terms-policies" options={{ headerShown: false }} />
        <Stack.Screen name="privacy-policy" options={{ headerShown: false }} />
        <Stack.Screen name="delete-account" options={{ headerShown: false }} />
        <Stack.Screen
          name="favorite-problems"
          options={{ headerShown: false }}
        />
        <Stack.Screen name="avatar-shop" options={{ headerShown: false }} />
        <Stack.Screen
          name="avatar-inventory"
          options={{ headerShown: false }}
        />
        <Stack.Screen
          name="mistake-exam-review"
          options={{ headerShown: false }}
        />
        <Stack.Screen name="solve" options={{ headerShown: false }} />
        <Stack.Screen name="solve-result" options={{ headerShown: false }} />
        <Stack.Screen name="daily-quiz" options={{ headerShown: false }} />
        <Stack.Screen name="daily-quiz-result" options={{ headerShown: false }} />
        <Stack.Screen name="daily-quiz-review" options={{ headerShown: false }} />
        <Stack.Screen name="friend-profile" options={{ headerShown: false }} />
        <Stack.Screen name="weakness-report" options={{ headerShown: false }} />
        <Stack.Screen name="(tabs)" options={{ headerShown: false }} />
          </Stack>
          <StatusBar style="dark" />
        </SafeAreaProvider>
      </AuthProvider>
    </QueryClientProvider>
  );
}
