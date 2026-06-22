import { Tabs } from "expo-router";

export default function TabsLayout() {
  return (
    <Tabs
      screenOptions={{
        headerShown: false,
        tabBarActiveTintColor: "#6D5DFB",
      }}
    >
      <Tabs.Screen name="home" options={{ title: "Home" }} />
    </Tabs>
  );
}
