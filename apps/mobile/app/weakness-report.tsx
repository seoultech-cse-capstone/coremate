import { router, useLocalSearchParams } from "expo-router";
import { ChevronLeft } from "lucide-react-native";
import { ActivityIndicator, Pressable, ScrollView, StyleSheet, Text, View } from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";

import { FONT } from "../constants/fonts";
import { useAuth, useRequireMember } from "../src/features/auth";
import { useWeaknessReportDetail } from "../src/features/weakness";
import type { WeaknessConcept, WeaknessEvidenceLog } from "../src/features/weakness";
import { getApiErrorMessage } from "../src/shared/utils/apiError";

const BRAND_PURPLE = "#6D5DFB";

function formatDate(isoString: string | null): string {
  if (!isoString) {
    return "";
  }
  return isoString.slice(0, 10).replace(/-/g, ".");
}

export default function WeaknessReportScreen() {
  useRequireMember();
  const { accessToken } = useAuth();
  const { reportId } = useLocalSearchParams<{ reportId?: string }>();
  const parsedReportId = Number(reportId);
  const resolvedReportId = Number.isFinite(parsedReportId) ? parsedReportId : null;

  const { data: report, isLoading, isError, error } = useWeaknessReportDetail(
    resolvedReportId,
    accessToken,
  );

  return (
    <View style={styles.screen}>
      <SafeAreaView edges={["top"]} style={styles.safeArea}>
        <View style={styles.headerArea}>
          <Pressable
            accessibilityLabel="뒤로가기"
            accessibilityRole="button"
            hitSlop={10}
            style={styles.backButton}
            onPress={() => router.back()}
          >
            <ChevronLeft color="#111827" size={28} strokeWidth={2.3} />
          </Pressable>

          <Text numberOfLines={1} style={styles.title}>
            {report?.title ?? "약점 분석표"}
          </Text>
        </View>

        {isLoading ? (
          <View style={styles.stateArea}>
            <ActivityIndicator color={BRAND_PURPLE} size="large" />
          </View>
        ) : isError || !report ? (
          <View style={styles.stateArea}>
            <Text style={styles.stateText}>
              {getApiErrorMessage(error, "분석표를 불러오지 못했어요.")}
            </Text>
          </View>
        ) : (
          <ScrollView contentContainerStyle={styles.content} showsVerticalScrollIndicator={false}>
            <Text style={styles.scopeText}>{report.analysisScopeNameSnapshot}</Text>
            {report.generatedAt ? (
              <Text style={styles.dateText}>{formatDate(report.generatedAt)} 생성</Text>
            ) : null}

            <View style={styles.statsCard}>
              <StatItem label="전체 문제" value={report.totalQuestionCount} />
              <StatItem label="오답" value={report.totalWrongCount} />
              <StatItem label="오래 걸림" value={report.totalLongTimeCount} />
              <StatItem label="힌트 사용" value={report.totalHintUsedCount} />
            </View>

            {report.summaryText ? (
              <View style={styles.summaryCard}>
                <Text style={styles.summaryText}>{report.summaryText}</Text>
              </View>
            ) : null}

            {report.analysisText ? (
              <View style={styles.card}>
                <Text style={styles.sectionTitle}>분석 내용</Text>
                <Text style={styles.bodyText}>{report.analysisText}</Text>
              </View>
            ) : null}

            {report.weaknessConcepts.length > 0 ? (
              <View style={styles.section}>
                <Text style={styles.sectionTitle}>약점 개념</Text>
                {report.weaknessConcepts.map((concept) => (
                  <WeaknessConceptCard concept={concept} key={concept.analysisConceptId} />
                ))}
              </View>
            ) : null}

            {report.selectedSessions.length > 0 ? (
              <View style={styles.section}>
                <Text style={styles.sectionTitle}>분석에 사용한 문제집</Text>
                <View style={styles.card}>
                  {report.selectedSessions.map((session) => (
                    <Text key={session.solveSessionId} style={styles.sessionText}>
                      {session.title} · {session.wrongCount}/{session.totalQuestionCount} 오답
                    </Text>
                  ))}
                </View>
              </View>
            ) : null}
          </ScrollView>
        )}
      </SafeAreaView>
    </View>
  );
}

function StatItem({ label, value }: { label: string; value: number }) {
  return (
    <View style={styles.statItem}>
      <Text style={styles.statValue}>{value}</Text>
      <Text style={styles.statLabel}>{label}</Text>
    </View>
  );
}

// 서버 enum → 화면 문구. 빠진 값이 있으면 원시 코드가 그대로 노출되므로 서버 정의 전체를 담는다.
const WEAKNESS_TYPE_LABEL: Record<string, string> = {
  CONCEPT_CONFUSION: "개념 혼동",
  APPLICATION_GAP: "적용 부족",
  SPEED_FLUENCY: "속도·숙련 부족",
  EXECUTION_ERROR: "풀이 실행 오류",
};

const TYPE_SOURCE_LABEL: Record<string, string> = {
  AI: "AI 해석",
  BOTH: "AI·풀이 기록 일치",
  NONE: "판정 보류",
};

const CAUSE_CODE_LABEL: Record<string, string> = {
  TERM_CONFUSION: "용어 혼동",
  RELATIONSHIP_MAPPING_GAP: "개념 관계 연결 부족",
  APPLICATION_GAP: "적용 부족",
  PROCEDURE_FLUENCY_GAP: "절차 숙련 부족",
  REPEATED_MISCONCEPTION: "같은 오개념 반복",
  CARELESS_EXECUTION: "실수·부주의",
};

const ANALYSIS_STATUS_LABEL: Record<string, string> = {
  SUPPORTED: "근거 충분",
  AMBIGUOUS: "근거 모호",
  INSUFFICIENT: "근거 부족",
};

const SECONDARY_SIGNAL_LABEL: Record<string, string> = {
  REPEATED_MISCONCEPTION: "같은 오개념 반복",
  CONCEPT_RELATION_CONFUSION: "개념 관계 혼동",
  HINT_DEPENDENCY: "힌트 의존",
  SLOW_FLUENCY: "풀이 속도 느림",
  INCONSISTENT_EXECUTION: "풀이 일관성 부족",
};

const EVIDENCE_TYPE_LABEL: Record<string, string> = {
  WRONG: "오답",
  LONG_TIME: "오래 걸림",
  HINT_USED: "힌트 사용",
  ANSWER_CHANGED: "답 변경",
};

function label(map: Record<string, string>, key: string | null): string {
  if (key === null) {
    return "-";
  }
  return map[key] ?? key;
}

function WeaknessConceptCard({ concept }: { concept: WeaknessConcept }) {
  const learning = concept.conceptLearning;
  return (
    <View style={styles.card}>
      <View style={styles.conceptHeader}>
        <Text style={styles.conceptName}>{concept.conceptName}</Text>
        <Text style={styles.conceptScore}>약점 점수 {concept.weaknessScore}</Text>
      </View>

      <Text style={styles.metaText}>
        유형 {label(WEAKNESS_TYPE_LABEL, concept.finalType)} (
        {label(TYPE_SOURCE_LABEL, concept.finalTypeSource)}) · 원인{" "}
        {label(CAUSE_CODE_LABEL, concept.causeCode)} · 근거 강도 {concept.evidenceStrength} · 상태{" "}
        {label(ANALYSIS_STATUS_LABEL, concept.analysisStatus)}
      </Text>
      {concept.confusedRelation ? (
        <Text style={styles.metaText}>혼동 개념: {concept.confusedRelation}</Text>
      ) : null}
      {concept.secondarySignals.length > 0 ? (
        <Text style={styles.metaText}>
          부가 신호:{" "}
          {concept.secondarySignals.map((s) => label(SECONDARY_SIGNAL_LABEL, s)).join(", ")}
        </Text>
      ) : null}

      <Text style={styles.diagnosisText}>{concept.diagnosis}</Text>
      {concept.evidenceSummary ? (
        <Text style={styles.evidenceSummaryText}>{concept.evidenceSummary}</Text>
      ) : null}

      {concept.detailedExplanationText ? (
        <View style={styles.subBox}>
          <Text style={styles.subBoxTitle}>상세 설명</Text>
          <Text style={styles.subBoxText}>{concept.detailedExplanationText}</Text>
        </View>
      ) : null}

      {concept.conceptTip ? (
        <View style={styles.tipBox}>
          <Text style={styles.tipTitle}>학습 팁</Text>
          <Text style={styles.tipText}>{concept.conceptTip}</Text>
        </View>
      ) : null}

      {learning ? (
        <View style={styles.subBox}>
          <Text style={styles.subBoxTitle}>개념 학습 자료</Text>
          {learning.conceptCore ? (
            <Text style={styles.subBoxText}>핵심: {learning.conceptCore}</Text>
          ) : null}
          {learning.conceptExplanation ? (
            <Text style={styles.subBoxText}>설명: {learning.conceptExplanation}</Text>
          ) : null}
          {learning.keyPoints.length > 0 ? (
            <Text style={styles.subBoxText}>핵심 포인트: {learning.keyPoints.join(" / ")}</Text>
          ) : null}
          {learning.confusionComparisons.length > 0 ? (
            <Text style={styles.subBoxText}>
              혼동 비교:{" "}
              {learning.confusionComparisons
                .map((c) => `${c.comparedWith}(${c.difference})`)
                .join(" / ")}
            </Text>
          ) : null}
          {learning.prerequisiteConcepts.length > 0 ? (
            <Text style={styles.subBoxText}>
              선수 개념: {learning.prerequisiteConcepts.join(", ")}
            </Text>
          ) : null}
          {learning.learningExample ? (
            <Text style={styles.subBoxText}>예시: {learning.learningExample}</Text>
          ) : null}
          {learning.misconceptionNotes ? (
            <Text style={styles.subBoxText}>오개념 노트: {learning.misconceptionNotes}</Text>
          ) : null}
          {learning.basicReviewPoints.length > 0 ? (
            <Text style={styles.subBoxText}>
              기초 복습 포인트: {learning.basicReviewPoints.join(" / ")}
            </Text>
          ) : null}
        </View>
      ) : null}

      {concept.evidenceLogs.length > 0 ? (
        <View style={styles.subBox}>
          <Text style={styles.subBoxTitle}>근거 문제 기록 ({concept.evidenceLogs.length}건)</Text>
          {concept.evidenceLogs.map((evidence) => (
            <EvidenceLogRow evidence={evidence} key={evidence.evidenceId} />
          ))}
        </View>
      ) : null}
    </View>
  );
}

function EvidenceLogRow({ evidence }: { evidence: WeaknessEvidenceLog }) {
  return (
    <View style={[styles.evidenceRow, evidence.citedByAi && styles.citedEvidenceRow]}>
      <View style={styles.evidenceHeader}>
        <Text style={styles.evidenceType}>
          {label(EVIDENCE_TYPE_LABEL, evidence.evidenceType)}
        </Text>
        {evidence.citedByAi ? (
          <Text style={styles.citedBadge}>AI가 지목한 근거</Text>
        ) : null}
      </View>
      <Text style={styles.subBoxText}>{evidence.questionText}</Text>
      <Text style={styles.evidenceDetailText}>
        정답 여부: {evidence.isCorrect === null ? "-" : evidence.isCorrect ? "정답" : "오답"} ·
        선택: {evidence.selectedAnswer ?? "-"} · 정답: {evidence.correctAnswer ?? "-"}
      </Text>
      <Text style={styles.evidenceDetailText}>
        소요 시간: {evidence.timeSpentSec ?? "-"}초 (기준 {evidence.expectedSolveSec ?? "-"}초) ·
        힌트 사용: {evidence.hintCount ?? "-"}회
      </Text>
    </View>
  );
}

const styles = StyleSheet.create({
  screen: {
    flex: 1,
    backgroundColor: "#F3F3F3",
  },
  safeArea: {
    flex: 1,
    backgroundColor: "transparent",
  },
  headerArea: {
    position: "relative",
    minHeight: 64,
    alignItems: "center",
    justifyContent: "center",
    backgroundColor: "#ffffff",
    paddingHorizontal: 56,
  },
  backButton: {
    position: "absolute",
    left: 12,
    top: 11,
    alignItems: "center",
    justifyContent: "center",
    width: 42,
    height: 42,
  },
  title: {
    color: "#111827",
    fontFamily: FONT.pretendard,
    fontSize: 18,
    fontWeight: "700",
    textAlign: "center",
  },
  stateArea: {
    flex: 1,
    alignItems: "center",
    justifyContent: "center",
    paddingHorizontal: 32,
  },
  stateText: {
    color: "#334155",
    fontFamily: FONT.pretendard,
    fontSize: 14,
    fontWeight: "600",
    textAlign: "center",
    lineHeight: 21,
  },
  content: {
    paddingHorizontal: 20,
    paddingTop: 20,
    paddingBottom: 44,
  },
  scopeText: {
    color: "#6D5DFB",
    fontFamily: FONT.pretendard,
    fontSize: 13,
    fontWeight: "700",
  },
  dateText: {
    marginTop: 4,
    color: "#94a3b8",
    fontFamily: FONT.pretendard,
    fontSize: 12,
    fontWeight: "500",
  },
  statsCard: {
    flexDirection: "row",
    justifyContent: "space-between",
    borderRadius: 8,
    backgroundColor: "#ffffff",
    marginTop: 16,
    paddingVertical: 18,
    paddingHorizontal: 12,
  },
  statItem: {
    alignItems: "center",
  },
  statValue: {
    color: "#1f2937",
    fontFamily: FONT.pretendard,
    fontSize: 20,
    fontWeight: "700",
  },
  statLabel: {
    marginTop: 4,
    color: "#64748b",
    fontFamily: FONT.pretendard,
    fontSize: 11,
    fontWeight: "600",
  },
  summaryCard: {
    borderRadius: 8,
    backgroundColor: "#F1EFFF",
    marginTop: 14,
    padding: 16,
  },
  summaryText: {
    color: "#45405f",
    fontFamily: FONT.pretendard,
    fontSize: 14,
    fontWeight: "600",
    lineHeight: 21,
  },
  card: {
    borderRadius: 8,
    backgroundColor: "#ffffff",
    marginTop: 14,
    padding: 16,
  },
  section: {
    marginTop: 8,
  },
  sectionTitle: {
    marginTop: 20,
    marginBottom: 4,
    color: "#1f2937",
    fontFamily: FONT.pretendard,
    fontSize: 16,
    fontWeight: "700",
  },
  bodyText: {
    color: "#334155",
    fontFamily: FONT.pretendard,
    fontSize: 14,
    lineHeight: 22,
  },
  conceptHeader: {
    flexDirection: "row",
    justifyContent: "space-between",
    alignItems: "center",
  },
  conceptName: {
    color: "#1f2937",
    fontFamily: FONT.pretendard,
    fontSize: 15,
    fontWeight: "700",
  },
  conceptScore: {
    color: "#6D5DFB",
    fontFamily: FONT.pretendard,
    fontSize: 12,
    fontWeight: "700",
  },
  diagnosisText: {
    marginTop: 8,
    color: "#334155",
    fontFamily: FONT.pretendard,
    fontSize: 14,
    lineHeight: 21,
  },
  evidenceSummaryText: {
    marginTop: 8,
    color: "#64748b",
    fontFamily: FONT.pretendard,
    fontSize: 12,
    lineHeight: 18,
  },
  tipBox: {
    borderRadius: 8,
    backgroundColor: "#F1EFFF",
    marginTop: 12,
    padding: 12,
  },
  tipTitle: {
    color: "#5144cf",
    fontFamily: FONT.pretendard,
    fontSize: 12,
    fontWeight: "700",
  },
  tipText: {
    marginTop: 4,
    color: "#494365",
    fontFamily: FONT.pretendard,
    fontSize: 13,
    lineHeight: 19,
  },
  sessionText: {
    color: "#334155",
    fontFamily: FONT.pretendard,
    fontSize: 13,
    fontWeight: "500",
    marginTop: 6,
  },
  metaText: {
    marginTop: 4,
    color: "#94a3b8",
    fontFamily: FONT.pretendard,
    fontSize: 11,
    fontWeight: "500",
  },
  subBox: {
    borderRadius: 8,
    backgroundColor: "#F8FAFC",
    marginTop: 12,
    padding: 12,
  },
  subBoxTitle: {
    color: "#334155",
    fontFamily: FONT.pretendard,
    fontSize: 12,
    fontWeight: "700",
  },
  subBoxText: {
    marginTop: 6,
    color: "#475569",
    fontFamily: FONT.pretendard,
    fontSize: 12,
    lineHeight: 18,
  },
  evidenceRow: {
    marginTop: 10,
    borderTopWidth: 1,
    borderTopColor: "#E2E8F0",
    paddingTop: 10,
  },
  // AI가 직접 근거로 지목한 문항은 나머지와 구분해 보여 준다.
  citedEvidenceRow: {
    backgroundColor: "#F5F3FF",
    borderRadius: 8,
    paddingHorizontal: 10,
    marginHorizontal: -10,
  },
  evidenceHeader: {
    flexDirection: "row",
    alignItems: "center",
    gap: 8,
    flexWrap: "wrap",
  },
  citedBadge: {
    color: "#6D5DFB",
    backgroundColor: "#EDEAFE",
    fontFamily: FONT.pretendard,
    fontSize: 11,
    fontWeight: "600",
    paddingHorizontal: 7,
    paddingVertical: 2,
    borderRadius: 4,
    overflow: "hidden",
  },
  evidenceType: {
    color: "#dc2626",
    fontFamily: FONT.pretendard,
    fontSize: 11,
    fontWeight: "800",
  },
  evidenceDetailText: {
    marginTop: 4,
    color: "#64748b",
    fontFamily: FONT.pretendard,
    fontSize: 11,
    lineHeight: 16,
  },
});
