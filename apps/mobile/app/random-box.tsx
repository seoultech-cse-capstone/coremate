import { router, useLocalSearchParams } from "expo-router";
import { ChevronLeft, Coins, Gift } from "lucide-react-native";
import { useMemo, useState } from "react";
import {
  ActivityIndicator,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  View,
} from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";

import { FONT } from "../constants/fonts";
import { useAuth, useRequireMember } from "../src/features/auth";
import { useMyCurrency } from "../src/features/currency";
import {
  useOpenRandomBoxMutation,
  useRandomBoxItems,
  useRandomBoxOdds,
  useRandomBoxes,
  type RandomBoxOpenResult,
} from "../src/features/shop";
import { getApiErrorMessage } from "../src/shared/utils/apiError";

const GRADE_LABEL: Record<string, string> = {
  COMMON: "COMMON",
  RARE: "RARE",
  EPIC: "EPIC",
  LEGENDARY: "LEGENDARY",
  COIN: "코인",
};

function formatProbability(probability: string): string {
  const ratio = Number.parseFloat(probability);
  if (Number.isNaN(ratio)) {
    return probability;
  }
  return `${(ratio * 100).toFixed(1)}%`;
}

export default function RandomBoxScreen() {
  useRequireMember();
  const { accessToken } = useAuth();
  const params = useLocalSearchParams<{ randomBoxId?: string; boxName?: string }>();
  const randomBoxId = params.randomBoxId ? Number(params.randomBoxId) : null;

  const currencyQuery = useMyCurrency(accessToken);
  const randomBoxesQuery = useRandomBoxes(accessToken);
  const oddsQuery = useRandomBoxOdds(randomBoxId, accessToken);
  const itemsQuery = useRandomBoxItems(randomBoxId, accessToken);
  const openMutation = useOpenRandomBoxMutation(accessToken);

  const [openError, setOpenError] = useState<string | null>(null);
  const [lastResult, setLastResult] = useState<RandomBoxOpenResult | null>(null);

  const box = useMemo(
    () => randomBoxesQuery.data?.find((candidate) => candidate.randomBoxId === randomBoxId) ?? null,
    [randomBoxesQuery.data, randomBoxId],
  );
  const boxName = box?.boxName ?? params.boxName ?? "랜덤박스";

  const rewardItemName = useMemo(() => {
    if (!lastResult || lastResult.rewardType !== "ITEM" || lastResult.rewardItemId === null) {
      return null;
    }
    return (
      itemsQuery.data?.find((item) => item.itemId === lastResult.rewardItemId)?.itemName ?? null
    );
  }, [lastResult, itemsQuery.data]);

  const handleOpen = () => {
    if (randomBoxId === null) {
      return;
    }
    setOpenError(null);
    openMutation.mutate(
      { randomBoxId },
      {
        onSuccess: (result) => {
          setLastResult(result);
        },
        onError: (error) => {
          setOpenError(getApiErrorMessage(error, "박스를 열지 못했습니다."));
        },
      },
    );
  };

  return (
    <View style={styles.screen}>
      <SafeAreaView edges={["top"]} style={styles.safeArea}>
        <View style={styles.container}>
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

            <Text style={styles.title}>{boxName}</Text>

            <View style={styles.coinBadge}>
              <Coins color="#F59E0B" size={16} strokeWidth={2.4} />
              <Text style={styles.coinBadgeText}>
                {currencyQuery.data ? currencyQuery.data.coin.toLocaleString() : "-"}
              </Text>
            </View>
          </View>

          <ScrollView contentContainerStyle={styles.content} showsVerticalScrollIndicator={false}>
            <View style={styles.boxPreview}>
              <Gift color="#6D5DFB" size={56} strokeWidth={1.6} />
            </View>

            {lastResult ? (
              <View style={styles.resultPanel}>
                <Text style={styles.resultTitle}>
                  {lastResult.rewardType === "COIN"
                    ? `코인 ${lastResult.rewardCoinAmount?.toLocaleString() ?? 0}개 획득!`
                    : `${rewardItemName ?? "아이템"} 획득!`}
                </Text>
                <Text style={styles.resultSubtitle}>보관함에서 확인할 수 있어요.</Text>
              </View>
            ) : null}

            {openError ? <Text style={styles.errorText}>{openError}</Text> : null}

            <Text style={styles.sectionLabel}>확률</Text>
            {oddsQuery.isLoading ? (
              <ActivityIndicator color="#6D5DFB" />
            ) : oddsQuery.isError ? (
              <Text style={styles.stateText}>
                {getApiErrorMessage(oddsQuery.error, "확률 정보를 불러오지 못했습니다.")}
              </Text>
            ) : (
              <View style={styles.oddsTable}>
                {(oddsQuery.data ?? []).map((odds) => (
                  <View key={odds.grade} style={styles.oddsRow}>
                    <Text style={styles.oddsGrade}>{GRADE_LABEL[odds.grade] ?? odds.grade}</Text>
                    <Text style={styles.oddsValue}>{formatProbability(odds.probability)}</Text>
                  </View>
                ))}
              </View>
            )}
          </ScrollView>

          <View style={styles.footer}>
            <Pressable
              disabled={openMutation.isPending || randomBoxId === null}
              style={styles.openButton}
              onPress={handleOpen}
            >
              {openMutation.isPending ? (
                <ActivityIndicator color="#ffffff" size="small" />
              ) : (
                <Text style={styles.openButtonText}>
                  {box ? `${box.priceCoin.toLocaleString()} 코인으로 열기` : "박스 열기"}
                </Text>
              )}
            </Pressable>
          </View>
        </View>
      </SafeAreaView>
    </View>
  );
}

const styles = StyleSheet.create({
  screen: {
    flex: 1,
    backgroundColor: "#ffffff",
  },
  safeArea: {
    flex: 1,
    backgroundColor: "transparent",
  },
  container: {
    flex: 1,
    backgroundColor: "#ffffff",
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
  coinBadge: {
    position: "absolute",
    right: 12,
    top: 15,
    flexDirection: "row",
    alignItems: "center",
    gap: 4,
    borderRadius: 999,
    backgroundColor: "#FFF7E6",
    paddingHorizontal: 10,
    paddingVertical: 6,
  },
  coinBadgeText: {
    color: "#B45309",
    fontFamily: FONT.pretendard,
    fontSize: 13,
    fontWeight: "700",
  },
  content: {
    padding: 20,
    alignItems: "center",
  },
  boxPreview: {
    width: 140,
    height: 140,
    borderRadius: 24,
    backgroundColor: "#EEEBFF",
    alignItems: "center",
    justifyContent: "center",
    marginBottom: 16,
  },
  resultPanel: {
    width: "100%",
    borderRadius: 14,
    backgroundColor: "#F1EFFF",
    paddingVertical: 16,
    paddingHorizontal: 16,
    alignItems: "center",
    marginBottom: 16,
  },
  resultTitle: {
    color: "#4C3FCF",
    fontFamily: FONT.pretendard,
    fontSize: 16,
    fontWeight: "700",
  },
  resultSubtitle: {
    marginTop: 4,
    color: "#7C6FEE",
    fontFamily: FONT.pretendard,
    fontSize: 12,
    fontWeight: "500",
  },
  errorText: {
    marginBottom: 12,
    color: "#dc2626",
    fontFamily: FONT.pretendard,
    fontSize: 13,
    fontWeight: "600",
    textAlign: "center",
  },
  sectionLabel: {
    alignSelf: "flex-start",
    color: "#111827",
    fontFamily: FONT.pretendard,
    fontSize: 14,
    fontWeight: "700",
    marginBottom: 8,
  },
  stateText: {
    color: "#64748b",
    fontFamily: FONT.pretendard,
    fontSize: 13,
    fontWeight: "500",
  },
  oddsTable: {
    width: "100%",
    borderRadius: 12,
    backgroundColor: "#F8FAFC",
    overflow: "hidden",
  },
  oddsRow: {
    flexDirection: "row",
    justifyContent: "space-between",
    paddingHorizontal: 14,
    paddingVertical: 10,
    borderBottomWidth: 1,
    borderBottomColor: "#EEF2F7",
  },
  oddsGrade: {
    color: "#334155",
    fontFamily: FONT.pretendard,
    fontSize: 13,
    fontWeight: "600",
  },
  oddsValue: {
    color: "#64748b",
    fontFamily: FONT.pretendard,
    fontSize: 13,
    fontWeight: "600",
  },
  footer: {
    padding: 16,
    borderTopWidth: 1,
    borderTopColor: "#F1F5F9",
  },
  openButton: {
    alignItems: "center",
    justifyContent: "center",
    borderRadius: 12,
    backgroundColor: "#6D5DFB",
    paddingVertical: 14,
  },
  openButtonText: {
    color: "#ffffff",
    fontFamily: FONT.pretendard,
    fontSize: 15,
    fontWeight: "700",
  },
});
