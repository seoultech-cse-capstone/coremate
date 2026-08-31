import { router } from "expo-router";
import { ChevronLeft, Coins, Gift, Sparkles } from "lucide-react-native";
import { useState } from "react";
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
  usePurchaseMutation,
  useShopProducts,
  type ShopProduct,
} from "../src/features/shop";
import { getApiErrorMessage } from "../src/shared/utils/apiError";

// 성별변경권(GENDER_CHANGE)은 09/03 확정 사항에 따라 폐기되어 화면에 노출하지 않는다.
const VISIBLE_PRODUCT_TYPES = new Set(["RANDOM_BOX", "NICKNAME_CHANGE"]);

export default function AvatarShopScreen() {
  useRequireMember();
  const { accessToken } = useAuth();
  const currencyQuery = useMyCurrency(accessToken);
  const productsQuery = useShopProducts(accessToken);
  const purchaseMutation = usePurchaseMutation(accessToken);

  const [purchasingProductId, setPurchasingProductId] = useState<number | null>(null);
  const [purchaseError, setPurchaseError] = useState<string | null>(null);
  const [purchaseSuccessMessage, setPurchaseSuccessMessage] = useState<string | null>(null);

  const products = (productsQuery.data ?? []).filter(
    (product) => VISIBLE_PRODUCT_TYPES.has(product.productType) && product.isActive,
  );

  const openRandomBox = (product: ShopProduct) => {
    if (product.randomBoxId === null) {
      return;
    }
    router.push({
      pathname: "/random-box",
      params: {
        randomBoxId: String(product.randomBoxId),
        boxName: product.productName,
      },
    });
  };

  const purchaseTicket = (product: ShopProduct) => {
    setPurchaseError(null);
    setPurchaseSuccessMessage(null);
    setPurchasingProductId(product.shopProductId);
    purchaseMutation.mutate(
      { shopProductId: product.shopProductId },
      {
        onSuccess: () => {
          setPurchaseSuccessMessage(`${product.productName} 구매가 완료됐어요.`);
        },
        onError: (error) => {
          setPurchaseError(getApiErrorMessage(error, "구매하지 못했습니다."));
        },
        onSettled: () => {
          setPurchasingProductId(null);
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

            <Text style={styles.title}>상점</Text>

            <View style={styles.coinBadge}>
              <Coins color="#F59E0B" size={16} strokeWidth={2.4} />
              <Text style={styles.coinBadgeText}>
                {currencyQuery.data ? currencyQuery.data.coin.toLocaleString() : "-"}
              </Text>
            </View>
          </View>

          {purchaseSuccessMessage ? (
            <Text style={styles.successText}>{purchaseSuccessMessage}</Text>
          ) : null}
          {purchaseError ? <Text style={styles.errorText}>{purchaseError}</Text> : null}

          {productsQuery.isLoading ? (
            <View style={styles.stateContainer}>
              <ActivityIndicator color="#6D5DFB" size="large" />
            </View>
          ) : productsQuery.isError ? (
            <View style={styles.stateContainer}>
              <Text style={styles.stateText}>
                {getApiErrorMessage(productsQuery.error, "상점 정보를 불러오지 못했습니다.")}
              </Text>
            </View>
          ) : products.length === 0 ? (
            <View style={styles.stateContainer}>
              <Text style={styles.stateText}>현재 판매 중인 상품이 없어요.</Text>
            </View>
          ) : (
            <ScrollView
              contentContainerStyle={styles.list}
              showsVerticalScrollIndicator={false}
            >
              {products.map((product) => {
                const isRandomBox = product.productType === "RANDOM_BOX";
                const isPurchasing = purchasingProductId === product.shopProductId;

                return (
                  <View key={product.shopProductId} style={styles.card}>
                    <View style={styles.cardIcon}>
                      {isRandomBox ? (
                        <Gift color="#6D5DFB" size={26} strokeWidth={2} />
                      ) : (
                        <Sparkles color="#6D5DFB" size={26} strokeWidth={2} />
                      )}
                    </View>

                    <View style={styles.cardBody}>
                      <Text style={styles.cardName}>{product.productName}</Text>
                      {product.requiredLevel > 0 ? (
                        <Text style={styles.cardMeta}>
                          Lv.{product.requiredLevel} 이상
                        </Text>
                      ) : null}
                      <View style={styles.cardPriceRow}>
                        <Coins color="#F59E0B" size={14} strokeWidth={2.4} />
                        <Text style={styles.cardPriceText}>
                          {product.priceCoin.toLocaleString()}
                        </Text>
                      </View>
                    </View>

                    <Pressable
                      disabled={isPurchasing}
                      style={styles.actionButton}
                      onPress={() =>
                        isRandomBox ? openRandomBox(product) : purchaseTicket(product)
                      }
                    >
                      {isPurchasing ? (
                        <ActivityIndicator color="#ffffff" size="small" />
                      ) : (
                        <Text style={styles.actionButtonText}>
                          {isRandomBox ? "열어보기" : "구매"}
                        </Text>
                      )}
                    </Pressable>
                  </View>
                );
              })}
            </ScrollView>
          )}
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
  successText: {
    marginTop: 8,
    marginHorizontal: 16,
    color: "#16C45B",
    fontFamily: FONT.pretendard,
    fontSize: 13,
    fontWeight: "600",
  },
  errorText: {
    marginTop: 8,
    marginHorizontal: 16,
    color: "#dc2626",
    fontFamily: FONT.pretendard,
    fontSize: 13,
    fontWeight: "600",
  },
  stateContainer: {
    flex: 1,
    alignItems: "center",
    justifyContent: "center",
    paddingHorizontal: 24,
  },
  stateText: {
    color: "#64748b",
    fontFamily: FONT.pretendard,
    fontSize: 14,
    fontWeight: "500",
    textAlign: "center",
  },
  list: {
    padding: 16,
    gap: 12,
  },
  card: {
    flexDirection: "row",
    alignItems: "center",
    borderRadius: 16,
    backgroundColor: "#F8FAFC",
    padding: 14,
    gap: 12,
  },
  cardIcon: {
    width: 48,
    height: 48,
    borderRadius: 12,
    backgroundColor: "#EEEBFF",
    alignItems: "center",
    justifyContent: "center",
  },
  cardBody: {
    flex: 1,
  },
  cardName: {
    color: "#111827",
    fontFamily: FONT.pretendard,
    fontSize: 15,
    fontWeight: "700",
  },
  cardMeta: {
    marginTop: 2,
    color: "#94a3b8",
    fontFamily: FONT.pretendard,
    fontSize: 12,
    fontWeight: "500",
  },
  cardPriceRow: {
    marginTop: 6,
    flexDirection: "row",
    alignItems: "center",
    gap: 4,
  },
  cardPriceText: {
    color: "#B45309",
    fontFamily: FONT.pretendard,
    fontSize: 13,
    fontWeight: "700",
  },
  actionButton: {
    minWidth: 76,
    alignItems: "center",
    justifyContent: "center",
    borderRadius: 10,
    backgroundColor: "#6D5DFB",
    paddingVertical: 10,
    paddingHorizontal: 14,
  },
  actionButtonText: {
    color: "#ffffff",
    fontFamily: FONT.pretendard,
    fontSize: 13,
    fontWeight: "700",
  },
});
