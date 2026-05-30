import { StyleSheet, Text, View } from 'react-native';

export default function FriendsScreen() {
  return (
    <View style={styles.container}>
      <Text style={styles.title}>친구</Text>
      <View style={styles.card}>
        <Text style={styles.cardTitle}>친구 목록 준비 중</Text>
        <Text style={styles.item}>친구 콕 찌르기 기능 예정</Text>
        <Text style={styles.item}>친구 아바타/스탯 조회 예정</Text>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    gap: 18,
    padding: 20,
    backgroundColor: '#f8fafc',
  },
  title: {
    color: '#111827',
    fontSize: 28,
    fontWeight: '800',
  },
  card: {
    gap: 12,
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
  item: {
    color: '#475569',
    fontSize: 15,
    lineHeight: 22,
  },
});
