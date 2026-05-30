import { Link, router } from 'expo-router';
import { Pressable, StyleSheet, Text, TextInput, View } from 'react-native';

export default function RegisterScreen() {
  return (
    <View style={styles.container}>
      <View style={styles.form}>
        <Text style={styles.title}>CoreMate 시작하기</Text>
        <TextInput
          autoCapitalize="none"
          keyboardType="email-address"
          placeholder="이메일"
          style={styles.input}
        />
        <TextInput placeholder="비밀번호" secureTextEntry style={styles.input} />
        <TextInput placeholder="비밀번호 확인" secureTextEntry style={styles.input} />
        <TextInput placeholder="닉네임" style={styles.input} />
        <Pressable style={styles.primaryButton} onPress={() => router.replace('/login')}>
          <Text style={styles.primaryButtonText}>회원가입</Text>
        </Pressable>
        <Link href="/login" style={styles.link}>
          로그인으로 이동
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
