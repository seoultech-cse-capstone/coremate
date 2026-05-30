import { Stack } from 'expo-router';

export default function WeaknessLayout() {
  return (
    <Stack
      screenOptions={{
        headerStyle: { backgroundColor: '#ffffff' },
        headerTitleStyle: { fontWeight: '700' },
        headerTintColor: '#1f2937',
        contentStyle: { backgroundColor: '#f8fafc' },
      }}>
      <Stack.Screen name="index" options={{ title: 'Weakness' }} />
      <Stack.Screen name="[sessionId]" options={{ title: 'AI 약점 분석' }} />
    </Stack>
  );
}
