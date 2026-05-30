import { router } from 'expo-router';
import { Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';

const solvedSessions = [
  {
    sessionId: 'ds-20260520',
    title: '자료구조 기본 문제집',
    type: '개념 확인',
    subject: '자료구조',
    solvedAt: '2026-05-20',
    totalCount: 20,
    correctCount: 15,
    wrongCount: 5,
    hasAnalysis: true,
  },
  {
    sessionId: 'os-20260522',
    title: '운영체제 프로세스 문제집',
    type: '실전 연습',
    subject: '운영체제',
    solvedAt: '2026-05-22',
    totalCount: 18,
    correctCount: 11,
    wrongCount: 7,
    hasAnalysis: false,
  },
  {
    sessionId: 'net-20260525',
    title: '네트워크 계층 문제집',
    type: '복습',
    subject: '네트워크',
    solvedAt: '2026-05-25',
    totalCount: 15,
    correctCount: 9,
    wrongCount: 6,
    hasAnalysis: true,
  },
];

export default function WeaknessListScreen() {
  return (
    <ScrollView contentContainerStyle={styles.container}>
      <View style={styles.header}>
        <Text style={styles.title}>푼 문제집</Text>
        <Text style={styles.description}>완료한 문제집을 기준으로 AI 약점 분석을 생성하거나 확인합니다.</Text>
      </View>

      <View style={styles.list}>
        {solvedSessions.map((session) => (
          <View key={session.sessionId} style={styles.card}>
            <View style={styles.cardHeader}>
              <Text style={styles.cardTitle}>{session.title}</Text>
              <Text style={styles.badge}>{session.hasAnalysis ? '분석 완료' : '분석 전'}</Text>
            </View>
            <Text style={styles.meta}>
              {session.subject} · {session.type} · {session.solvedAt}
            </Text>
            <View style={styles.scoreRow}>
              <Text style={styles.score}>총 {session.totalCount}</Text>
              <Text style={styles.score}>정답 {session.correctCount}</Text>
              <Text style={styles.score}>오답 {session.wrongCount}</Text>
            </View>
            <Text style={styles.sessionId}>sessionId: {session.sessionId}</Text>
            <Pressable
              style={styles.button}
              onPress={() =>
                router.push({
                  pathname: '/(tabs)/weakness/[sessionId]',
                  params: { sessionId: session.sessionId },
                })
              }>
              <Text style={styles.buttonText}>
                {session.hasAnalysis ? 'AI 약점 분석 보기' : 'AI 약점 분석 생성하기'}
              </Text>
            </Pressable>
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
    gap: 14,
  },
  card: {
    gap: 12,
    borderRadius: 8,
    borderWidth: 1,
    borderColor: '#e5e7eb',
    backgroundColor: '#ffffff',
    padding: 18,
  },
  cardHeader: {
    alignItems: 'flex-start',
    gap: 8,
  },
  cardTitle: {
    color: '#111827',
    fontSize: 18,
    fontWeight: '800',
    lineHeight: 24,
  },
  badge: {
    overflow: 'hidden',
    borderRadius: 8,
    backgroundColor: '#e0f2fe',
    color: '#0369a1',
    paddingHorizontal: 10,
    paddingVertical: 5,
    fontSize: 12,
    fontWeight: '800',
  },
  meta: {
    color: '#475569',
    fontSize: 14,
  },
  scoreRow: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 8,
  },
  score: {
    borderRadius: 8,
    backgroundColor: '#f1f5f9',
    color: '#334155',
    paddingHorizontal: 10,
    paddingVertical: 6,
    fontSize: 13,
    fontWeight: '700',
  },
  sessionId: {
    color: '#64748b',
    fontSize: 12,
  },
  button: {
    alignItems: 'center',
    borderRadius: 8,
    backgroundColor: '#2563eb',
    paddingVertical: 13,
  },
  buttonText: {
    color: '#ffffff',
    fontSize: 15,
    fontWeight: '800',
  },
});
