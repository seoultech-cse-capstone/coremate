import { router, useLocalSearchParams } from "expo-router";
import { ChevronLeft } from "lucide-react-native";
import { ActivityIndicator, Pressable, ScrollView, StyleSheet, Text, View } from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";

import { FONT } from "../constants/fonts";
import { useAuth, useRequireMember } from "../src/features/auth";
import { useWeaknessReportDetail } from "../src/features/weakness";
import type { WeaknessConcept } from "../src/features/weakness";
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

function WeaknessConceptCard({ concept }: { concept: WeaknessConcept }) {
  return (
    <View style={styles.card}>
      <View style={styles.conceptHeader}>
        <Text style={styles.conceptName}>{concept.conceptName}</Text>
        <Text style={styles.conceptScore}>약점 점수 {concept.weaknessScore}</Text>
      </View>
      <Text style={styles.diagnosisText}>{concept.diagnosis}</Text>
      {concept.evidenceSummary ? (
        <Text style={styles.evidenceSummaryText}>{concept.evidenceSummary}</Text>
      ) : null}
      {concept.conceptTip ? (
        <View style={styles.tipBox}>
          <Text style={styles.tipTitle}>학습 팁</Text>
          <Text style={styles.tipText}>{concept.conceptTip}</Text>
        </View>
      ) : null}
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
});
