import { Image } from "expo-image";
import { LinearGradient } from "expo-linear-gradient";
import { router } from "expo-router";
import { ChevronLeft, Coins, Percent, Sparkles, X } from "lucide-react-native";
import { useState } from "react";
import {
  ActivityIndicator,
  KeyboardAvoidingView,
  Modal,
  Platform,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  View,
} from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";

import { FONT } from "../constants/fonts";
import { AdRewardCard } from "../src/features/ads";
import { useAuth, useRequireMember } from "../src/features/auth";
import { useMyCurrency } from "../src/features/currency";
import {
  CHEST_CONTENT_OFFSET_RATIO,
  CLOSED_CHEST_IMAGE,
  RandomBoxOpenModal,
  usePurchaseMutation,
  useRandomBoxOdds,
  useShopProducts,
  type ShopProduct,
} from "../src/features/shop";
import { getApiErrorMessage } from "../src/shared/utils/apiError";

// 성별변경권(GENDER_CHANGE)은 09/03 확정 사항에 따라 폐기되어 화면에 노출하지 않는다.
const VISIBLE_PRODUCT_TYPES = new Set(["RANDOM_BOX", "NICKNAME_CHANGE"]);

const NICKNAME_TICKET_IMAGE = require("../assets/shop/nickname-change/ticket.png");

const GRADE_STYLE: Record<string, { label: string; color: string }> = {
  COMMON: { label: "COMMON", color: "#9CA3AF" },
  RARE: { label: "RARE", color: "#3B82F6" },
  EPIC: { label: "EPIC", color: "#A855F7" },
  LEGENDARY: { label: "LEGENDARY", color: "#F5B301" },
  COIN: { label: "코인", color: "#F59E0B" },
};

function formatProbability(probability: string): string {
  const ratio = Number.parseFloat(probability);
  if (Number.isNaN(ratio)) {
    return probability;
  }
  return `${(ratio * 100).toFixed(1)}%`;
}

export default function AvatarShopScreen() {
  useRequireMember();
  const { accessToken, user } = useAuth();
  const currencyQuery = useMyCurrency(accessToken);
  const productsQuery = useShopProducts(accessToken);
  const purchaseMutation = usePurchaseMutation(accessToken);

  const [purchasingProductId, setPurchasingProductId] = useState<number | null>(null);
  const [purchaseError, setPurchaseError] = useState<string | null>(null);
  const [purchaseSuccessMessage, setPurchaseSuccessMessage] = useState<string | null>(null);

  // 닉네임 변경권은 새 닉네임을 함께 보내야 해서, 카드를 누르면 입력 모달을 먼저 띄운다.
  const [nicknameProduct, setNicknameProduct] = useState<ShopProduct | null>(null);
  const [nicknameInput, setNicknameInput] = useState("");
  const [nicknameError, setNicknameError] = useState<string | null>(null);

  // 확률 확인 모달 대상 랜덤박스. null 이면 모달을 닫고 odds 쿼리도 비활성화된다.
  const [oddsProduct, setOddsProduct] = useState<ShopProduct | null>(null);
  const [insufficientVisible, setInsufficientVisible] = useState(false);
  // 랜덤박스 열기 확인 팝업 대상, 그리고 실제 열기 애니메이션을 재생할 대상.
  const [confirmBox, setConfirmBox] = useState<ShopProduct | null>(null);
  const [openingProduct, setOpeningProduct] = useState<ShopProduct | null>(null);

  const oddsQuery = useRandomBoxOdds(oddsProduct?.randomBoxId ?? null, accessToken);

  const myCoin = currencyQuery.data?.coin ?? 0;

  const products = (productsQuery.data ?? []).filter(
    (product) => VISIBLE_PRODUCT_TYPES.has(product.productType) && product.isActive,
  );

  // 상품 카드의 주 액션(열기/구매). 코인이 부족하면 요청 전에 안내 팝업으로 막는다.
  const handleCardPress = (product: ShopProduct) => {
    if (myCoin < product.priceCoin) {
      setInsufficientVisible(true);
      return;
    }
    if (product.productType === "RANDOM_BOX") {
      // 상세 페이지로 가지 않고 확인 팝업을 먼저 띄운다.
      setConfirmBox(product);
    } else {
      openNicknameModal(product);
    }
  };

  const confirmOpenBox = () => {
    if (confirmBox === null) {
      return;
    }
    setPurchaseSuccessMessage(null);
    setPurchaseError(null);
    setOpeningProduct(confirmBox);
    setConfirmBox(null);
  };

  const openNicknameModal = (product: ShopProduct) => {
    setPurchaseError(null);
    setPurchaseSuccessMessage(null);
    setNicknameInput("");
    setNicknameError(null);
    setNicknameProduct(product);
  };

  const confirmNicknamePurchase = () => {
    if (nicknameProduct === null) {
      return;
    }
    const trimmed = nicknameInput.trim();
    if (trimmed.length === 0) {
      setNicknameError("새 닉네임을 입력해주세요.");
      return;
    }
    setNicknameError(null);
    setPurchasingProductId(nicknameProduct.shopProductId);
    purchaseMutation.mutate(
      { shopProductId: nicknameProduct.shopProductId, newNickname: trimmed },
      {
        onSuccess: () => {
          setPurchaseSuccessMessage(`닉네임을 '${trimmed}'(으)로 변경했어요.`);
          setNicknameProduct(null);
        },
        onError: (error) => {
          setNicknameError(getApiErrorMessage(error, "구매하지 못했습니다."));
        },
        onSettled: () => {
          setPurchasingProductId(null);
        },
      },
    );
  };

  return (
    <View style={styles.screen}>
      <LinearGradient
        colors={["#2E2260", "#4B369B", "#F4F2FB"]}
        locations={[0, 0.28, 0.62]}
        style={StyleSheet.absoluteFill}
      />
      <SafeAreaView edges={["top"]} style={styles.safeArea}>
        <View style={styles.headerArea}>
          <Pressable
            accessibilityLabel="뒤로가기"
            accessibilityRole="button"
            hitSlop={10}
            style={styles.backButton}
            onPress={() => router.back()}
          >
            <ChevronLeft color="#F4F1FF" size={28} strokeWidth={2.3} />
          </Pressable>

          <Text style={styles.title}>상점</Text>

          <View style={styles.coinBadge}>
            <Coins color="#FBBF24" size={16} strokeWidth={2.4} />
            <Text style={styles.coinBadgeText}>{myCoin.toLocaleString()}</Text>
          </View>
        </View>

        <View style={styles.heroArea}>
          <Text style={styles.heroTitle}>무엇을 갖고 싶으세요?</Text>
          <Text style={styles.heroSub}>코인으로 랜덤박스와 아이템을 만나보세요</Text>
        </View>

        {purchaseSuccessMessage ? (
          <View style={[styles.banner, styles.bannerSuccess]}>
            <Text style={styles.bannerSuccessText}>{purchaseSuccessMessage}</Text>
          </View>
        ) : null}
        {purchaseError ? (
          <View style={[styles.banner, styles.bannerError]}>
            <Text style={styles.bannerErrorText}>{purchaseError}</Text>
          </View>
        ) : null}

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

              const theme = isRandomBox
                ? {
                    gradient: ["#7C3AED", "#9333EA", "#6D28D9"] as const,
                    badge: "랜덤박스",
                    desc: "아이템 또는 코인을 뽑아보세요",
                    cta: "코인으로 열기",
                    image: CLOSED_CHEST_IMAGE,
                    imageOffset: 150 * CHEST_CONTENT_OFFSET_RATIO,
                  }
                : {
                    gradient: ["#0EA5E9", "#0891B2", "#0E7490"] as const,
                    badge: "닉네임 변경권",
                    desc: "원하는 닉네임으로 바꿔보세요",
                    cta: "코인으로 구매",
                    image: NICKNAME_TICKET_IMAGE,
                    imageOffset: 0,
                  };

              return (
                <Pressable
                  key={product.shopProductId}
                  disabled={isPurchasing}
                  style={styles.featuredWrap}
                  onPress={() => handleCardPress(product)}
                >
                  <LinearGradient
                    colors={theme.gradient}
                    start={{ x: 0, y: 0 }}
                    end={{ x: 1, y: 1 }}
                    style={styles.featuredCard}
                  >
                    <View style={styles.featuredBadge}>
                      <Sparkles color="#FDE68A" size={12} strokeWidth={2.6} />
                      <Text style={styles.featuredBadgeText}>{theme.badge}</Text>
                    </View>

                    {isRandomBox ? (
                      <Pressable
                        accessibilityLabel="확률 확인"
                        hitSlop={8}
                        style={styles.oddsButton}
                        onPress={() => setOddsProduct(product)}
                      >
                        <Percent color="#ffffff" size={13} strokeWidth={2.6} />
                        <Text style={styles.oddsButtonText}>확률 확인</Text>
                      </Pressable>
                    ) : null}

                    <Image
                      source={theme.image}
                      style={[
                        styles.featuredImage,
                        { transform: [{ translateX: theme.imageOffset }] },
                      ]}
                      contentFit="contain"
                    />

                    <Text style={styles.featuredName}>{product.productName}</Text>
                    <Text style={styles.featuredDesc}>{theme.desc}</Text>

                    <View style={styles.featuredCta}>
                      {isPurchasing ? (
                        <ActivityIndicator color="#7C2D12" size="small" />
                      ) : (
                        <>
                          <Coins color="#7C2D12" size={16} strokeWidth={2.6} />
                          <Text style={styles.featuredCtaText}>
                            {product.priceCoin.toLocaleString()} {theme.cta}
                          </Text>
                        </>
                      )}
                    </View>
                  </LinearGradient>
                </Pressable>
              );
            })}

            <AdRewardCard accessToken={accessToken} userId={user?.userId ?? null} />
          </ScrollView>
        )}
      </SafeAreaView>

      <Modal
        transparent
        visible={nicknameProduct !== null}
        animationType="fade"
        onRequestClose={() => setNicknameProduct(null)}
      >
        <KeyboardAvoidingView
          behavior={Platform.OS === "ios" ? "padding" : undefined}
          style={styles.modalBackdrop}
        >
          <View style={styles.modalCard}>
            <Pressable
              accessibilityLabel="닫기"
              hitSlop={10}
              style={styles.modalClose}
              onPress={() => setNicknameProduct(null)}
            >
              <X color="#9CA3AF" size={22} strokeWidth={2.2} />
            </Pressable>

            <Text style={styles.modalTitle}>닉네임 변경</Text>
            <Text style={styles.modalSub}>새로 사용할 닉네임을 입력하세요 (최대 6글자)</Text>

            <TextInput
              value={nicknameInput}
              onChangeText={setNicknameInput}
              placeholder="새 닉네임"
              placeholderTextColor="#9CA3AF"
              maxLength={6}
              autoFocus
              style={styles.modalInput}
              returnKeyType="done"
              onSubmitEditing={confirmNicknamePurchase}
            />

            {nicknameError ? (
              <Text style={styles.modalErrorText}>{nicknameError}</Text>
            ) : null}

            <Pressable
              disabled={purchasingProductId !== null}
              style={styles.modalButtonWrap}
              onPress={confirmNicknamePurchase}
            >
              <LinearGradient
                colors={["#A78BFA", "#7C3AED"]}
                start={{ x: 0, y: 0 }}
                end={{ x: 1, y: 1 }}
                style={styles.modalButton}
              >
                {purchasingProductId !== null ? (
                  <ActivityIndicator color="#ffffff" size="small" />
                ) : (
                  <Text style={styles.modalButtonText}>
                    {nicknameProduct
                      ? `${nicknameProduct.priceCoin.toLocaleString()} 코인으로 변경`
                      : "변경"}
                  </Text>
                )}
              </LinearGradient>
            </Pressable>
          </View>
        </KeyboardAvoidingView>
      </Modal>

      <Modal
        transparent
        visible={oddsProduct !== null}
        animationType="fade"
        onRequestClose={() => setOddsProduct(null)}
      >
        <Pressable style={styles.modalBackdrop} onPress={() => setOddsProduct(null)}>
          <Pressable style={styles.modalCard}>
            <Pressable
              accessibilityLabel="닫기"
              hitSlop={10}
              style={styles.modalClose}
              onPress={() => setOddsProduct(null)}
            >
              <X color="#9CA3AF" size={22} strokeWidth={2.2} />
            </Pressable>

            <Text style={styles.modalTitle}>획득 확률</Text>
            <Text style={styles.modalSub}>{oddsProduct?.productName}</Text>

            <View style={styles.oddsList}>
              {oddsQuery.isLoading ? (
                <ActivityIndicator color="#7C3AED" style={styles.oddsLoading} />
              ) : oddsQuery.isError ? (
                <Text style={styles.modalErrorText}>
                  {getApiErrorMessage(oddsQuery.error, "확률 정보를 불러오지 못했습니다.")}
                </Text>
              ) : (
                (oddsQuery.data ?? []).map((odds) => {
                  const gradeStyle = GRADE_STYLE[odds.grade] ?? {
                    label: odds.grade,
                    color: "#6B7280",
                  };
                  return (
                    <View key={odds.grade} style={styles.oddsRow}>
                      <View style={styles.oddsGradeWrap}>
                        <View style={[styles.oddsDot, { backgroundColor: gradeStyle.color }]} />
                        <Text style={[styles.oddsGrade, { color: gradeStyle.color }]}>
                          {gradeStyle.label}
                        </Text>
                      </View>
                      <Text style={styles.oddsValue}>{formatProbability(odds.probability)}</Text>
                    </View>
                  );
                })
              )}
            </View>
          </Pressable>
        </Pressable>
      </Modal>

      <Modal
        transparent
        visible={insufficientVisible}
        animationType="fade"
        onRequestClose={() => setInsufficientVisible(false)}
      >
        <View style={styles.modalBackdrop}>
          <View style={styles.modalCard}>
            <Text style={styles.modalTitle}>코인이 부족해요</Text>
            <Text style={styles.modalSub}>코인이 부족하여 구매할 수 없습니다.</Text>

            <Pressable
              style={styles.modalButtonWrap}
              onPress={() => setInsufficientVisible(false)}
            >
              <LinearGradient
                colors={["#A78BFA", "#7C3AED"]}
                start={{ x: 0, y: 0 }}
                end={{ x: 1, y: 1 }}
                style={styles.modalButton}
              >
                <Text style={styles.modalButtonText}>확인</Text>
              </LinearGradient>
            </Pressable>
          </View>
        </View>
      </Modal>

      <Modal
        transparent
        visible={confirmBox !== null}
        animationType="fade"
        onRequestClose={() => setConfirmBox(null)}
      >
        <View style={styles.modalBackdrop}>
          <View style={styles.modalCard}>
            <Text style={styles.modalTitle}>랜덤박스 열기</Text>
            <Text style={styles.modalSub}>
              {confirmBox
                ? `${confirmBox.priceCoin.toLocaleString()}코인을 지불하고 랜덤박스를 여시겠습니까?`
                : ""}
            </Text>

            <View style={styles.confirmRow}>
              <Pressable
                style={[styles.confirmButtonWrap, styles.confirmButtonGhost]}
                onPress={() => setConfirmBox(null)}
              >
                <Text style={styles.confirmGhostText}>아니오</Text>
              </Pressable>
              <Pressable style={styles.confirmButtonWrap} onPress={confirmOpenBox}>
                <LinearGradient
                  colors={["#A78BFA", "#7C3AED"]}
                  start={{ x: 0, y: 0 }}
                  end={{ x: 1, y: 1 }}
                  style={styles.modalButton}
                >
                  <Text style={styles.modalButtonText}>예</Text>
                </LinearGradient>
              </Pressable>
            </View>
          </View>
        </View>
      </Modal>

      <RandomBoxOpenModal
        product={openingProduct}
        accessToken={accessToken}
        onClose={() => setOpeningProduct(null)}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  screen: {
    flex: 1,
    backgroundColor: "#F4F2FB",
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
    color: "#F4F1FF",
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
    backgroundColor: "rgba(0,0,0,0.24)",
    borderWidth: 1,
    borderColor: "rgba(251,191,36,0.35)",
    paddingHorizontal: 10,
    paddingVertical: 6,
  },
  coinBadgeText: {
    color: "#FDE68A",
    fontFamily: FONT.pretendard,
    fontSize: 13,
    fontWeight: "700",
  },
  heroArea: {
    paddingHorizontal: 20,
    paddingTop: 6,
    paddingBottom: 16,
  },
  heroTitle: {
    color: "#F8F7FF",
    fontFamily: FONT.pretendard,
    fontSize: 22,
    fontWeight: "800",
  },
  heroSub: {
    marginTop: 4,
    color: "rgba(244,241,255,0.75)",
    fontFamily: FONT.pretendard,
    fontSize: 13,
    fontWeight: "500",
  },
  banner: {
    marginHorizontal: 16,
    marginBottom: 4,
    borderRadius: 12,
    paddingVertical: 10,
    paddingHorizontal: 14,
  },
  bannerSuccess: {
    backgroundColor: "#DCFCE7",
  },
  bannerSuccessText: {
    color: "#15803D",
    fontFamily: FONT.pretendard,
    fontSize: 13,
    fontWeight: "600",
  },
  bannerError: {
    backgroundColor: "#FEE2E2",
  },
  bannerErrorText: {
    color: "#B91C1C",
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
    gap: 14,
  },
  featuredWrap: {
    borderRadius: 22,
    overflow: "hidden",
    shadowColor: "#7C3AED",
    shadowOffset: { width: 0, height: 8 },
    shadowOpacity: 0.35,
    shadowRadius: 16,
    elevation: 8,
  },
  featuredCard: {
    alignItems: "center",
    paddingHorizontal: 20,
    paddingVertical: 22,
  },
  featuredBadge: {
    flexDirection: "row",
    alignItems: "center",
    gap: 4,
    borderRadius: 999,
    backgroundColor: "rgba(0,0,0,0.22)",
    paddingHorizontal: 10,
    paddingVertical: 4,
  },
  featuredBadgeText: {
    color: "#FDE68A",
    fontFamily: FONT.pretendard,
    fontSize: 11,
    fontWeight: "800",
    letterSpacing: 0.5,
  },
  featuredImage: {
    width: 150,
    height: 108,
    marginVertical: 12,
  },
  featuredName: {
    color: "#ffffff",
    fontFamily: FONT.pretendard,
    fontSize: 18,
    fontWeight: "800",
  },
  featuredDesc: {
    marginTop: 4,
    color: "rgba(255,255,255,0.8)",
    fontFamily: FONT.pretendard,
    fontSize: 13,
    fontWeight: "500",
  },
  featuredCta: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "center",
    gap: 6,
    marginTop: 16,
    borderRadius: 12,
    backgroundColor: "#FDE68A",
    paddingVertical: 12,
    paddingHorizontal: 20,
    alignSelf: "stretch",
  },
  featuredCtaText: {
    color: "#7C2D12",
    fontFamily: FONT.pretendard,
    fontSize: 15,
    fontWeight: "800",
  },
  modalBackdrop: {
    flex: 1,
    backgroundColor: "rgba(12,8,28,0.6)",
    alignItems: "center",
    justifyContent: "center",
    paddingHorizontal: 32,
  },
  modalCard: {
    width: "100%",
    maxWidth: 340,
    borderRadius: 22,
    backgroundColor: "#ffffff",
    paddingHorizontal: 22,
    paddingTop: 26,
    paddingBottom: 20,
  },
  modalClose: {
    position: "absolute",
    right: 12,
    top: 12,
    padding: 4,
  },
  modalTitle: {
    color: "#111827",
    fontFamily: FONT.pretendard,
    fontSize: 18,
    fontWeight: "800",
  },
  modalSub: {
    marginTop: 6,
    color: "#6B7280",
    fontFamily: FONT.pretendard,
    fontSize: 13,
    fontWeight: "500",
  },
  modalInput: {
    marginTop: 16,
    borderRadius: 12,
    borderWidth: 1,
    borderColor: "#E5E7EB",
    backgroundColor: "#F9FAFB",
    paddingHorizontal: 14,
    paddingVertical: 12,
    fontFamily: FONT.pretendard,
    fontSize: 15,
    fontWeight: "600",
    color: "#111827",
  },
  modalErrorText: {
    marginTop: 8,
    color: "#dc2626",
    fontFamily: FONT.pretendard,
    fontSize: 13,
    fontWeight: "600",
  },
  modalButtonWrap: {
    marginTop: 18,
    borderRadius: 12,
    overflow: "hidden",
  },
  modalButton: {
    alignItems: "center",
    justifyContent: "center",
    paddingVertical: 14,
  },
  modalButtonText: {
    color: "#ffffff",
    fontFamily: FONT.pretendard,
    fontSize: 15,
    fontWeight: "800",
  },
  confirmRow: {
    marginTop: 20,
    flexDirection: "row",
    gap: 10,
  },
  confirmButtonWrap: {
    flex: 1,
    borderRadius: 12,
    overflow: "hidden",
  },
  confirmButtonGhost: {
    alignItems: "center",
    justifyContent: "center",
    paddingVertical: 14,
    backgroundColor: "#F1F5F9",
  },
  confirmGhostText: {
    color: "#475569",
    fontFamily: FONT.pretendard,
    fontSize: 15,
    fontWeight: "800",
  },
  oddsButton: {
    position: "absolute",
    top: 14,
    right: 14,
    zIndex: 2,
    flexDirection: "row",
    alignItems: "center",
    gap: 4,
    borderRadius: 999,
    backgroundColor: "rgba(0,0,0,0.28)",
    paddingHorizontal: 10,
    paddingVertical: 5,
  },
  oddsButtonText: {
    color: "#ffffff",
    fontFamily: FONT.pretendard,
    fontSize: 11,
    fontWeight: "800",
    letterSpacing: 0.3,
  },
  oddsList: {
    marginTop: 14,
  },
  oddsLoading: {
    paddingVertical: 16,
  },
  oddsRow: {
    flexDirection: "row",
    justifyContent: "space-between",
    alignItems: "center",
    paddingVertical: 9,
    borderBottomWidth: 1,
    borderBottomColor: "#F1F5F9",
  },
  oddsGradeWrap: {
    flexDirection: "row",
    alignItems: "center",
    gap: 8,
  },
  oddsDot: {
    width: 8,
    height: 8,
    borderRadius: 4,
  },
  oddsGrade: {
    fontFamily: FONT.pretendard,
    fontSize: 13,
    fontWeight: "700",
  },
  oddsValue: {
    color: "#111827",
    fontFamily: FONT.pretendard,
    fontSize: 13,
    fontWeight: "700",
  },
});
