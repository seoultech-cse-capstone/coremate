import { Stack } from "expo-router";
import { useFonts } from "expo-font";
import { StatusBar } from "expo-status-bar";
import { StyleSheet } from "react-native";
import { SafeAreaProvider, SafeAreaView } from "react-native-safe-area-context";

import { FONT } from "../constants/fonts";

export default function RootLayout() {
  const [fontsLoaded] = useFonts({
    [FONT.pretendard]: require("../assets/fonts/PretendardVariable.ttf"),
    [FONT.kufam]: require("../assets/fonts/Kufam-VariableFont_wght.ttf"),
  });

  if (!fontsLoaded) {
    return null;
  }

  return (
    <SafeAreaProvider>
      <SafeAreaView style={styles.safeArea} edges={["top"]}>
        <Stack
          screenOptions={{
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
          <Stack.Screen name="(tabs)" options={{ headerShown: false }} />
        </Stack>
        <StatusBar style="dark" />
      </SafeAreaView>
    </SafeAreaProvider>
  );
}

const styles = StyleSheet.create({
  safeArea: {
    flex: 1,
    backgroundColor: "#6D5DFB",
  },
});
