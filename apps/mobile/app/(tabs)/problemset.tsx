import { ScrollView, StyleSheet, Text, View } from 'react-native';

const subjects = ['자료구조', '알고리즘', '운영체제', '데이터베이스', '컴퓨터구조', '네트워크'];

export default function ProblemsetScreen() {
  return (
    <ScrollView contentContainerStyle={styles.container}>
      <View style={styles.header}>
        <Text style={styles.title}>문제집</Text>
        <Text style={styles.description}>과목을 선택하면 추후 대분류, 중분류, 하위 개념, 문제 풀이 흐름으로 확장됩니다.</Text>
      </View>

      <View style={styles.list}>
        {subjects.map((subject) => (
          <View key={subject} style={styles.subjectCard}>
            <Text style={styles.subjectName}>{subject}</Text>
            <Text style={styles.subjectMeta}>문제 풀이 화면 준비 중</Text>
          </View>
        ))}
      </View>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: {
    flexGrow: 1,
    gap: 18,
    padding: 20,
    backgroundColor: '#f8fafc',
  },
  header: {
    gap: 8,
  },
  title: {
    color: '#111827',
    fontSize: 28,
    fontWeight: '800',
  },
  description: {
    color: '#4b5563',
    fontSize: 15,
    lineHeight: 22,
  },
  list: {
    gap: 12,
  },
  subjectCard: {
    borderRadius: 8,
    borderWidth: 1,
    borderColor: '#e5e7eb',
    backgroundColor: '#ffffff',
    padding: 18,
  },
  subjectName: {
    color: '#111827',
    fontSize: 18,
    fontWeight: '800',
  },
  subjectMeta: {
    marginTop: 6,
    color: '#64748b',
    fontSize: 14,
  },
});
