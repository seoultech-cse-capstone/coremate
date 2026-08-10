import { router, useLocalSearchParams } from "expo-router";
import { Pressable, StyleSheet, Text, View } from "react-native";

import { FONT } from "../constants/fonts";
import { useAuth, useRequireMember } from "../src/features/auth";
import { useMyCurrency } from "../src/features/currency";
import {
  detailStyles,
  SettingsDetailLayout,
} from "../src/features/settings/components/SettingsDetailLayout";
import type { AnalysisScopeType } from "../src/features/weakness";

// TODO(#105 후속): 생성 비용을 미리 조회하는 API가 백엔드에 없다(FeatureAccessPricingService가
// 내부에서만 쓰이고 컨트롤러로 노출되지 않음). 실제 생성 시 백엔드가 적용하는 값(기본 100코인,
// weakness.analysis.coin-cost 설정)과 다를 수 있으니 조회 API가 생기면 반드시 교체할 것.
const MOCK_COIN_COST = 100;
// TODO(#105 후속): eligible-solve-sessions 응답에 회차별 답 변경 집계가 없다. 요구사항(SCR-WEAK-03)엔
// "답 변경 문제 수 또는 보조 참고값"이 표시 항목으로 있어 자리만 만들어둔 목업이다.
const MOCK_ANSWER_CHANGED_COUNT = 0;

function MockBadge() {
  return (
    <View style={styles.mockBadge}>
      <Text style={styles.mockBadgeText}>목업</Text>
    </View>
  );
}

export default function WeaknessConfirmScreen() {
  useRequireMember();
  const { accessToken } = useAuth();
  const params = useLocalSearchParams<{
    solveSessionIds: string;
    analysisScopeType: AnalysisScopeType;
    subjectId: string;
    categoryLv1Id?: string;
    categoryLv2Id?: string;
    conceptId?: string;
    selectedSessionCount: string;
    totalQuestionCount: string;
    totalWrongCount: string;
    totalLongTimeCount: string;
    totalHintUsedCount: string;
  }>();

  const { data: currency, isLoading: isCurrencyLoading, isError: isCurrencyError } =
    useMyCurrency(accessToken);

  const coinBalance = currency?.coin ?? null;
  const canAfford = coinBalance !== null && coinBalance >= MOCK_COIN_COST;

  const handleConfirm = () => {
    router.push({
      pathname: "/weakness-generating",
      params: {
        solveSessionIds: params.solveSessionIds,
        analysisScopeType: params.analysisScopeType,
        subjectId: params.subjectId,
        categoryLv1Id: params.categoryLv1Id,
        categoryLv2Id: params.categoryLv2Id,
        conceptId: params.conceptId,
      },
    });
  };

  return (
    <SettingsDetailLayout title="분석 생성 확인">
      <Text style={detailStyles.sectionTitle}>분석 대상 요약</Text>
      <View style={detailStyles.card}>
        <Row label="선택한 문제집 수" value={`${params.selectedSessionCount}개`} />
        <Row label="총 문제 수" value={`${params.totalQuestionCount}문제`} />
        <Row label="틀린 문제 수" value={`${params.totalWrongCount}문제`} />
        <Row label="오래 걸린 문제 수" value={`${params.totalLongTimeCount}문제`} />
        <Row label="힌트 사용 문제 수" value={`${params.totalHintUsedCount}문제`} />
        <Row
          label="답 변경 문제 수"
          value={`${MOCK_ANSWER_CHANGED_COUNT}문제 (보조 참고값)`}
          badge
        />
      </View>

      <Text style={detailStyles.sectionTitle}>비용 확인</Text>
      <View style={detailStyles.card}>
        <Row label="필요한 코인" value={`${MOCK_COIN_COST} 코인`} badge />
        <Row
          label="현재 보유 코인"
          value={
            isCurrencyLoading
              ? "불러오는 중…"
              : isCurrencyError || coinBalance === null
                ? "조회 실패"
                : `${coinBalance} 코인`
          }
        />
      </View>

      {!isCurrencyLoading && !isCurrencyError && !canAfford ? (
        <Text style={styles.insufficientText}>코인이 부족해요.</Text>
      ) : null}

      <Pressable
        disabled={isCurrencyLoading || !canAfford}
        style={[
          detailStyles.primaryButton,
          (isCurrencyLoading || !canAfford) && styles.disabledButton,
        ]}
        onPress={handleConfirm}
      >
        <Text style={detailStyles.primaryButtonText}>분석표 생성하기</Text>
      </Pressable>

      <Pressable style={styles.backButton} onPress={() => router.back()}>
        <Text style={styles.backButtonText}>선택 화면으로 돌아가기</Text>
      </Pressable>
    </SettingsDetailLayout>
  );
}

function Row({
  label,
  value,
  badge,
}: {
  label: string;
  value: string;
  badge?: boolean;
}) {
  return (
    <View style={styles.row}>
      <View style={styles.rowLabelArea}>
        <Text style={detailStyles.label}>{label}</Text>
        {badge ? <MockBadge /> : null}
      </View>
      <Text style={styles.rowValue}>{value}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  row: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
    paddingVertical: 8,
  },
  rowLabelArea: {
    flexDirection: "row",
    alignItems: "center",
    gap: 6,
  },
  rowValue: {
    color: "#111827",
    fontFamily: FONT.pretendard,
    fontSize: 14,
    fontWeight: "700",
  },
  mockBadge: {
    borderRadius: 999,
    backgroundColor: "#F97316",
    paddingHorizontal: 7,
    paddingVertical: 2,
  },
  mockBadgeText: {
    color: "#ffffff",
    fontFamily: FONT.pretendard,
    fontSize: 10,
    fontWeight: "800",
  },
  insufficientText: {
    marginTop: -8,
    marginBottom: 12,
    color: "#dc2626",
    fontFamily: FONT.pretendard,
    fontSize: 13,
    fontWeight: "600",
  },
  disabledButton: {
    opacity: 0.5,
  },
  backButton: {
    marginTop: 12,
    alignItems: "center",
    paddingVertical: 12,
  },
  backButtonText: {
    color: "#64748b",
    fontFamily: FONT.pretendard,
    fontSize: 14,
    fontWeight: "600",
  },
});
