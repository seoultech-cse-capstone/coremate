import { Link, router } from 'expo-router';
import { Pressable, StyleSheet, Text, TextInput, View } from 'react-native';

export default function LoginScreen() {
  return (
    <View style={styles.container}>
      <View style={styles.form}>
        <Text style={styles.title}>다시 만나서 반가워요</Text>
        <TextInput
          autoCapitalize="none"
          keyboardType="email-address"
          placeholder="이메일"
          style={styles.input}
        />
        <TextInput placeholder="비밀번호" secureTextEntry style={styles.input} />
        <Pressable style={styles.primaryButton} onPress={() => router.replace('/(tabs)/home')}>
          <Text style={styles.primaryButtonText}>로그인</Text>
        </Pressable>
        <Link href="/register" style={styles.link}>
          회원가입으로 이동
        </Link>
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
  form: {
    gap: 14,
  },
  title: {
    marginBottom: 8,
    fontSize: 26,
    fontWeight: '800',
    color: '#111827',
  },
  input: {
    borderRadius: 8,
    borderWidth: 1,
    borderColor: '#d1d5db',
    backgroundColor: '#ffffff',
    paddingHorizontal: 14,
    paddingVertical: 13,
    fontSize: 16,
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
  link: {
    paddingVertical: 8,
    textAlign: 'center',
    color: '#2563eb',
    fontSize: 15,
    fontWeight: '600',
  },
});
