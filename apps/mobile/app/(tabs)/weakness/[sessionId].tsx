import { useLocalSearchParams } from 'expo-router';
import { Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';

const analysisBySession: Record<
  string,
  {
    title: string;
    hasAnalysis: boolean;
    weakConcepts: string[];
    reason: string;
    historySummary: string;
    reviewPlan: string;
  }
> = {
  'ds-20260520': {
    title: '자료구조 기본 문제집',
    hasAnalysis: true,
    weakConcepts: ['트리 순회', '해시 충돌 처리', '스택/큐 활용'],
    reason: '개념 정의 문제보다 응용 문제에서 오답이 집중되었습니다.',
    historySummary: '20문제 중 5문제를 틀렸고, 자료구조 선택 근거를 묻는 문항에서 시간이 길었습니다.',
    reviewPlan: '트리 순회 예제를 손으로 추적하고, 해시 테이블 충돌 해결 방식을 비교 복습하세요.',
  },
  'os-20260522': {
    title: '운영체제 프로세스 문제집',
    hasAnalysis: false,
    weakConcepts: [],
    reason: '아직 AI 분석이 생성되지 않았습니다.',
    historySummary: '18문제 풀이 기록이 저장되어 있습니다.',
    reviewPlan: '분석 생성 후 약한 개념과 복습 방향을 확인할 수 있습니다.',
  },
  'net-20260525': {
    title: '네트워크 계층 문제집',
    hasAnalysis: true,
    weakConcepts: ['TCP 흐름 제어', '라우팅', 'DNS 동작 과정'],
    reason: '계층별 역할을 구분하는 문제에서 오답이 반복되었습니다.',
    historySummary: '15문제 중 6문제를 틀렸고, 전송 계층과 네트워크 계층 비교 문항에서 약점이 보였습니다.',
    reviewPlan: 'OSI 계층별 책임을 다시 정리하고 TCP/UDP, IP 라우팅 흐름을 그림으로 복습하세요.',
  },
};

export default function WeaknessDetailScreen() {
  const { sessionId } = useLocalSearchParams<{ sessionId: string }>();
  const analysis = analysisBySession[sessionId ?? ''] ?? {
    title: '알 수 없는 문제집',
    hasAnalysis: false,
    weakConcepts: [],
    reason: '해당 sessionId의 mock data가 없습니다.',
    historySummary: '풀이 기록을 찾을 수 없습니다.',
    reviewPlan: '목록에서 다른 문제집을 선택해주세요.',
  };

  return (
    <ScrollView contentContainerStyle={styles.container}>
      <View style={styles.header}>
        <Text style={styles.sessionId}>sessionId: {sessionId}</Text>
        <Text style={styles.title}>{analysis.title}</Text>
      </View>

      <View style={styles.card}>
        <Text style={styles.cardTitle}>약한 개념 목록</Text>
        {analysis.weakConcepts.length > 0 ? (
          analysis.weakConcepts.map((concept) => (
            <Text key={concept} style={styles.bullet}>
              · {concept}
            </Text>
          ))
        ) : (
          <Text style={styles.body}>아직 분석된 약한 개념이 없습니다.</Text>
        )}
      </View>

      <View style={styles.card}>
        <Text style={styles.cardTitle}>약점이 도출된 이유</Text>
        <Text style={styles.body}>{analysis.reason}</Text>
      </View>

      <View style={styles.card}>
        <Text style={styles.cardTitle}>관련 풀이 기록 요약</Text>
        <Text style={styles.body}>{analysis.historySummary}</Text>
      </View>

      <View style={styles.card}>
        <Text style={styles.cardTitle}>이후 복습 방향</Text>
        <Text style={styles.body}>{analysis.reviewPlan}</Text>
      </View>

      {!analysis.hasAnalysis && (
        <Pressable style={styles.button}>
          <Text style={styles.buttonText}>분석 생성하기</Text>
        </Pressable>
      )}
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: {
    flexGrow: 1,
    gap: 14,
    padding: 20,
    backgroundColor: '#f8fafc',
  },
  header: {
    gap: 8,
  },
  sessionId: {
    color: '#64748b',
    fontSize: 13,
    fontWeight: '700',
  },
  title: {
    color: '#111827',
    fontSize: 26,
    fontWeight: '800',
    lineHeight: 32,
  },
  card: {
    gap: 8,
    borderRadius: 8,
    borderWidth: 1,
    borderColor: '#e5e7eb',
    backgroundColor: '#ffffff',
    padding: 18,
  },
  cardTitle: {
    color: '#111827',
    fontSize: 17,
    fontWeight: '800',
  },
  body: {
    color: '#475569',
    fontSize: 15,
    lineHeight: 23,
  },
  bullet: {
    color: '#334155',
    fontSize: 15,
    lineHeight: 23,
    fontWeight: '600',
  },
  button: {
    alignItems: 'center',
    borderRadius: 8,
    backgroundColor: '#2563eb',
    paddingVertical: 15,
  },
  buttonText: {
    color: '#ffffff',
    fontSize: 16,
    fontWeight: '800',
  },
});
