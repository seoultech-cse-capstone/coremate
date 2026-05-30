import { router } from 'expo-router';
import { Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';

export default function HomeScreen() {
  return (
    <ScrollView contentContainerStyle={styles.container}>
      <View style={styles.header}>
        <Text style={styles.eyebrow}>오늘의 학습</Text>
        <Text style={styles.title}>CoreMate에 오신 것을 환영합니다</Text>
        <Text style={styles.description}>문제 풀이와 약점 분석 흐름을 MVP 화면으로 확인할 수 있어요.</Text>
      </View>

      <View style={styles.quizCard}>
        <Text style={styles.cardLabel}>오늘의 CS 퀴즈</Text>
        <Text style={styles.cardTitle}>프로세스와 스레드의 차이를 설명할 수 있나요?</Text>
        <Text style={styles.cardBody}>짧은 개념 점검으로 오늘의 학습을 시작해보세요.</Text>
      </View>

      <View style={styles.actions}>
        <Pressable style={styles.primaryButton} onPress={() => router.push('/(tabs)/problemset')}>
          <Text style={styles.primaryButtonText}>문제 풀러가기</Text>
        </Pressable>
        <Pressable style={styles.secondaryButton} onPress={() => router.push('/(tabs)/weakness')}>
          <Text style={styles.secondaryButtonText}>Weakness 확인하기</Text>
        </Pressable>
        <Pressable style={styles.secondaryButton} onPress={() => router.push('/(tabs)/mypage')}>
          <Text style={styles.secondaryButtonText}>마이페이지 이동</Text>
        </Pressable>
      </View>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: {
    flexGrow: 1,
    gap: 20,
    padding: 20,
    backgroundColor: '#f8fafc',
  },
  header: {
    gap: 8,
  },
  eyebrow: {
    color: '#2563eb',
    fontSize: 13,
    fontWeight: '700',
  },
  title: {
    color: '#111827',
    fontSize: 28,
    fontWeight: '800',
    lineHeight: 34,
  },
  description: {
    color: '#4b5563',
    fontSize: 15,
    lineHeight: 22,
  },
  quizCard: {
    gap: 8,
    borderRadius: 8,
    borderWidth: 1,
    borderColor: '#dbeafe',
    backgroundColor: '#ffffff',
    padding: 18,
  },
  cardLabel: {
    color: '#2563eb',
    fontSize: 13,
    fontWeight: '700',
  },
  cardTitle: {
    color: '#111827',
    fontSize: 20,
    fontWeight: '800',
    lineHeight: 27,
  },
  cardBody: {
    color: '#475569',
    fontSize: 14,
    lineHeight: 21,
  },
  actions: {
    gap: 12,
  },
  primaryButton: {
    alignItems: 'center',
    borderRadius: 8,
    backgroundColor: '#2563eb',
    paddingVertical: 15,
  },
  primaryButtonText: {
    color: '#ffffff',
    fontSize: 16,
    fontWeight: '700',
  },
  secondaryButton: {
    alignItems: 'center',
    borderRadius: 8,
    borderWidth: 1,
    borderColor: '#cbd5e1',
    backgroundColor: '#ffffff',
    paddingVertical: 15,
  },
  secondaryButtonText: {
    color: '#1f2937',
    fontSize: 16,
    fontWeight: '700',
  },
});
