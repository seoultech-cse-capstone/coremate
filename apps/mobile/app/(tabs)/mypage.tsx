import { Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';

export default function MyPageScreen() {
  return (
    <ScrollView contentContainerStyle={styles.container}>
      <Text style={styles.title}>마이페이지</Text>

      <View style={styles.avatarBox}>
        <Text style={styles.avatarText}>2D 아바타 영역</Text>
      </View>

      <View style={styles.profileCard}>
        <Text style={styles.nickname}>코어메이트</Text>
        <Text style={styles.level}>Level 3</Text>
      </View>

      <View style={styles.statsCard}>
        <Text style={styles.cardTitle}>과목별 스탯</Text>
        <Text style={styles.stat}>자료구조 정확도 72%</Text>
        <Text style={styles.stat}>운영체제 정확도 64%</Text>
        <Text style={styles.stat}>네트워크 정확도 58%</Text>
      </View>

      <View style={styles.actions}>
        <Pressable style={styles.button}>
          <Text style={styles.buttonText}>상점</Text>
        </Pressable>
        <Pressable style={styles.button}>
          <Text style={styles.buttonText}>아이템 보관함</Text>
        </Pressable>
        <Pressable style={styles.button}>
          <Text style={styles.buttonText}>설정</Text>
        </Pressable>
      </View>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: {
    flexGrow: 1,
    gap: 16,
    padding: 20,
    backgroundColor: '#f8fafc',
  },
  title: {
    color: '#111827',
    fontSize: 28,
    fontWeight: '800',
  },
  avatarBox: {
    alignItems: 'center',
    justifyContent: 'center',
    minHeight: 150,
    borderRadius: 8,
    backgroundColor: '#dbeafe',
  },
  avatarText: {
    color: '#1d4ed8',
    fontSize: 16,
    fontWeight: '800',
  },
  profileCard: {
    borderRadius: 8,
    borderWidth: 1,
    borderColor: '#e5e7eb',
    backgroundColor: '#ffffff',
    padding: 18,
  },
  nickname: {
    color: '#111827',
    fontSize: 20,
    fontWeight: '800',
  },
  level: {
    marginTop: 4,
    color: '#2563eb',
    fontSize: 15,
    fontWeight: '700',
  },
  statsCard: {
    gap: 8,
    borderRadius: 8,
    borderWidth: 1,
    borderColor: '#e5e7eb',
    backgroundColor: '#ffffff',
    padding: 18,
  },
  cardTitle: {
    color: '#111827',
    fontSize: 18,
    fontWeight: '800',
  },
  stat: {
    color: '#475569',
    fontSize: 15,
  },
  actions: {
    gap: 10,
  },
  button: {
    alignItems: 'center',
    borderRadius: 8,
    borderWidth: 1,
    borderColor: '#cbd5e1',
    backgroundColor: '#ffffff',
    paddingVertical: 14,
  },
  buttonText: {
    color: '#1f2937',
    fontSize: 15,
    fontWeight: '700',
  },
});
