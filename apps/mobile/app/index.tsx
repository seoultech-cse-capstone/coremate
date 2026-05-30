import { router } from 'expo-router';
import { Pressable, StyleSheet, Text, View } from 'react-native';

export default function StartScreen() {
  return (
    <View style={styles.container}>
      <View style={styles.header}>
        <Text style={styles.title}>CoreMate</Text>
        <Text style={styles.subtitle}>CS 학습을 함께 관리하는 모바일 학습 파트너</Text>
      </View>

      <View style={styles.actions}>
        <Pressable style={styles.primaryButton} onPress={() => router.push('/login')}>
          <Text style={styles.primaryButtonText}>로그인</Text>
        </Pressable>
        <Pressable style={styles.secondaryButton} onPress={() => router.push('/register')}>
          <Text style={styles.secondaryButtonText}>회원가입</Text>
        </Pressable>
        <Pressable style={styles.ghostButton} onPress={() => router.push('/(tabs)/problemset')}>
          <Text style={styles.ghostButtonText}>비로그인으로 체험하기</Text>
        </Pressable>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'center',
    padding: 24,
    backgroundColor: '#f8fafc',
  },
  header: {
    marginBottom: 40,
    gap: 12,
  },
  title: {
    fontSize: 36,
    fontWeight: '800',
    color: '#111827',
  },
  subtitle: {
    fontSize: 16,
    lineHeight: 24,
    color: '#4b5563',
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
    borderColor: '#2563eb',
    backgroundColor: '#ffffff',
    paddingVertical: 15,
  },
  secondaryButtonText: {
    color: '#2563eb',
    fontSize: 16,
    fontWeight: '700',
  },
  ghostButton: {
    alignItems: 'center',
    paddingVertical: 15,
  },
  ghostButtonText: {
    color: '#475569',
    fontSize: 15,
    fontWeight: '600',
  },
});
