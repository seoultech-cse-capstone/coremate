import { router, useLocalSearchParams } from "expo-router";
import { Pressable, StyleSheet, Text, View } from "react-native";

import { FONT } from "../constants/fonts";
import { useAuth, useRequireMember } from "../src/features/auth";
import { useMyCurrency } from "../src/features/currency";
import {
  detailStyles,
  SettingsDetailLayout,
} from "../src/features/settings/components/SettingsDetailLayout";
import { useWeaknessAnalysisCost } from "../src/features/weakness";
import type { AnalysisScopeType } from "../src/features/weakness";

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
    totalAnswerChangedCount: string;
  }>();

  const { data: currency, isLoading: isCurrencyLoading, isError: isCurrencyError } =
    useMyCurrency(accessToken);
  const { data: cost, isLoading: isCostLoading, isError: isCostError } =
    useWeaknessAnalysisCost(accessToken);

  const coinBalance = currency?.coin ?? null;
  const coinCost = cost?.coinCost ?? null;
  const isLoading = isCurrencyLoading || isCostLoading;
  const isError = isCurrencyError || isCostError;
  // 비용이나 잔액을 모르는 상태에서 생성으로 넘기면 코인 부족을 서버에서야 알게 된다.
  const canAfford = coinBalance !== null && coinCost !== null && coinBalance >= coinCost;

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
          value={`${params.totalAnswerChangedCount ?? 0}문제 (보조 참고값)`}
        />
      </View>

      <Text style={detailStyles.sectionTitle}>비용 확인</Text>
      <View style={detailStyles.card}>
        <Row
          label="필요한 코인"
          value={
            isCostLoading
              ? "불러오는 중…"
              : isCostError || coinCost === null
                ? "조회 실패"
                : `${coinCost} 코인`
          }
        />
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

      {!isLoading && !isError && !canAfford ? (
        <Text style={styles.insufficientText}>코인이 부족해요.</Text>
      ) : null}

      <Pressable
        disabled={isLoading || !canAfford}
        style={[
          detailStyles.primaryButton,
          (isLoading || !canAfford) && styles.disabledButton,
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

function Row({ label, value }: { label: string; value: string }) {
  return (
    <View style={styles.row}>
      <View style={styles.rowLabelArea}>
        <Text style={detailStyles.label}>{label}</Text>
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
