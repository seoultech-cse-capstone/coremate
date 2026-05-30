import { Tabs } from 'expo-router';

export default function TabsLayout() {
  return (
    <Tabs
      screenOptions={{
        headerStyle: { backgroundColor: '#ffffff' },
        headerTitleStyle: { fontWeight: '700' },
        headerTintColor: '#1f2937',
        tabBarActiveTintColor: '#2563eb',
        tabBarInactiveTintColor: '#64748b',
        tabBarStyle: {
          borderTopColor: '#e5e7eb',
          backgroundColor: '#ffffff',
        },
      }}>
      <Tabs.Screen name="home" options={{ title: '홈' }} />
      <Tabs.Screen name="friends" options={{ title: '친구' }} />
      <Tabs.Screen name="mypage" options={{ title: '마이페이지' }} />
      <Tabs.Screen name="problemset" options={{ title: '문제집' }} />
      <Tabs.Screen name="weakness" options={{ title: 'Weakness', headerShown: false }} />
    </Tabs>
  );
}
