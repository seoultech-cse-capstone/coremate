import { useState } from "react";
import { ScrollView, StyleSheet, Text, View } from "react-native";

import { FONT } from "../../constants/fonts";
import { ContentModeTabs } from "../../src/features/content/components/ContentModeTabs";
import { PracticeSetCard } from "../../src/features/content/components/PracticeSetCard";

type ContentMode = "concept" | "mock";

const conceptSets = [
  {
    accentColor: "#6D5DFB",
    badge: "자료구조",
    description: "스택, 큐, 트리의 핵심 개념을 문제로 점검합니다.",
    meta: "12문제",
    progressLabel: "진행률 35%",
    title: "기본 자료구조",
  },
  {
    accentColor: "#2EA86A",
    badge: "알고리즘",
    description: "정렬과 탐색 문제를 중심으로 풀이 감각을 익힙니다.",
    meta: "15문제",
    progressLabel: "진행률 20%",
    title: "정렬과 탐색",
  },
  {
    accentColor: "#F59E0B",
    badge: "운영체제",
    description: "프로세스, 스레드, 스케줄링 개념을 복습합니다.",
    meta: "10문제",
    progressLabel: "미시작",
    title: "프로세스 관리",
  },
];

const mockSets = [
  {
    accentColor: "#111827",
    badge: "실전",
    description: "전공 핵심 과목을 섞어 제한 시간 안에 풀이합니다.",
    meta: "30문제 · 40분",
    progressLabel: "추천",
    title: "CS 종합 모의고사 1회",
  },
  {
    accentColor: "#EF4444",
    badge: "약점",
    description: "최근 오답률이 높은 네트워크와 DB 문제로 구성했습니다.",
    meta: "24문제 · 30분",
    progressLabel: "오답 기반",
    title: "약점 보완 모의고사",
  },
  {
    accentColor: "#0EA5E9",
    badge: "기초",
    description: "짧은 시간에 기본 개념을 넓게 확인하는 세트입니다.",
    meta: "20문제 · 25분",
    progressLabel: "미시작",
    title: "기초 점검 모의고사",
  },
];

export default function ContentScreen() {
  const [selectedMode, setSelectedMode] = useState<ContentMode>("concept");
  const currentSets = selectedMode === "concept" ? conceptSets : mockSets;

  return (
    <View style={styles.container}>
      <View style={styles.titleArea}>
        <Text style={styles.title}>연습노트</Text>
        <Text style={styles.subtitle}>개념 문제와 모의고사를 골라 풀어보세요</Text>
      </View>

      <View style={styles.panel}>
        <ScrollView
          showsVerticalScrollIndicator={false}
          contentContainerStyle={styles.panelContent}
        >
          <ContentModeTabs
            selectedMode={selectedMode}
            onSelectMode={setSelectedMode}
          />

          <View style={styles.summary}>
            <Text style={styles.summaryLabel}>
              {selectedMode === "concept" ? "개념 문제" : "모의고사 문제집"}
            </Text>
            <Text style={styles.summaryText}>
              {selectedMode === "concept"
                ? "과목별 핵심 개념을 작은 문제 세트로 확인합니다."
                : "실전처럼 시간을 두고 종합 문제를 풀어봅니다."}
            </Text>
          </View>

          <View style={styles.cardList}>
            {currentSets.map((set) => (
              <PracticeSetCard key={set.title} {...set} />
            ))}
          </View>
        </ScrollView>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: "#6D5DFB",
  },
  titleArea: {
    minHeight: 156,
    justifyContent: "center",
    paddingHorizontal: 24,
    paddingBottom: 10,
  },
  title: {
    color: "#ffffff",
    fontFamily: FONT.pretendard,
    fontSize: 28,
    fontWeight: "800",
  },
  subtitle: {
    color: "rgba(255, 255, 255, 0.78)",
    fontFamily: FONT.pretendard,
    fontSize: 14,
    fontWeight: "400",
    marginTop: 8,
  },
  panel: {
    flex: 1,
    width: "100%",
    borderTopLeftRadius: 28,
    borderTopRightRadius: 28,
    backgroundColor: "#F3F3F3",
    overflow: "hidden",
  },
  panelContent: {
    paddingHorizontal: 24,
    paddingTop: 24,
    paddingBottom: 34,
  },
  summary: {
    marginTop: 22,
  },
  summaryLabel: {
    color: "#18181B",
    fontFamily: FONT.pretendard,
    fontSize: 20,
    fontWeight: "700",
  },
  summaryText: {
    color: "#71717A",
    fontFamily: FONT.pretendard,
    fontSize: 13,
    fontWeight: "400",
    lineHeight: 19,
    marginTop: 6,
  },
  cardList: {
    gap: 14,
    marginTop: 18,
  },
});
