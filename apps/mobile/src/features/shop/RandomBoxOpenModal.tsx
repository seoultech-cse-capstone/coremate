import { LinearGradient } from "expo-linear-gradient";
import { Coins, Sparkles, X } from "lucide-react-native";
import { useEffect, useMemo, useRef, useState } from "react";
import {
  ActivityIndicator,
  Animated,
  Easing,
  Modal,
  Pressable,
  StyleSheet,
  Text,
  View,
} from "react-native";

import { FONT } from "../../../constants/fonts";
import { getApiErrorMessage } from "../../shared/utils/apiError";
import { ChestAnimation } from "./ChestAnimation";
import type { RandomBoxOpenResult, ShopProduct } from "./types";
import { useOpenRandomBoxMutation, useRandomBoxItems } from "./useShopQueries";

type Phase = "idle" | "opening" | "revealed";

// 등급별 상자에서 새어나오는 빛 색. 코인 보상은 COMMON 과 같은 색으로 취급한다.
const RARITY_LIGHT: Record<string, string> = {
  COMMON: "#B8C0CC",
  RARE: "#3B82F6",
  EPIC: "#A855F7",
  LEGENDARY: "#F5B301",
  COIN: "#B8C0CC",
};
const DEFAULT_RARITY_LIGHT = "#C4B5FD";

type RandomBoxOpenModalProps = {
  /** 열 랜덤박스 상품. null 이면 모달이 닫혀 있고, 값이 들어오는 순간 자동으로 열기를 시작한다. */
  product: ShopProduct | null;
  accessToken: string | null;
  onClose: () => void;
};

// 상점에서 "예"를 눌러 열기를 확정하면 이 모달이 뜨면서 곧바로 상자 애니메이션을 재생하고,
// 서버 결과가 도착하면 보상을 리빌한다. (상세 페이지로 이동하지 않는다)
export function RandomBoxOpenModal({ product, accessToken, onClose }: RandomBoxOpenModalProps) {
  const randomBoxId = product?.randomBoxId ?? null;
  const itemsQuery = useRandomBoxItems(randomBoxId, accessToken);
  const openMutation = useOpenRandomBoxMutation(accessToken);

  const [phase, setPhase] = useState<Phase>("idle");
  const [result, setResult] = useState<RandomBoxOpenResult | null>(null);
  const [animDone, setAnimDone] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const startedForRef = useRef<number | null>(null);

  // product 가 새로 들어오면 열기를 한 번만 시작하고, null 이 되면 상태를 초기화한다.
  useEffect(() => {
    if (product === null || randomBoxId === null) {
      startedForRef.current = null;
      setPhase("idle");
      setResult(null);
      setAnimDone(false);
      setError(null);
      return;
    }
    if (startedForRef.current === product.shopProductId) {
      return;
    }
    startedForRef.current = product.shopProductId;
    setResult(null);
    setAnimDone(false);
    setError(null);
    setPhase("opening");
    openMutation.mutate(
      { randomBoxId },
      {
        onSuccess: (data) => setResult(data),
        onError: (err) => setError(getApiErrorMessage(err, "박스를 열지 못했습니다.")),
      },
    );
    // openMutation 은 매 렌더 새 참조라 deps 에서 제외한다. product 변경 시에만 시작.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [product]);

  // 애니메이션이 끝나고 서버 결과도 오면(둘 중 늦은 쪽 기준), 0.5초 더 기다렸다가 보상을 리빌한다.
  useEffect(() => {
    if (phase === "opening" && animDone && result) {
      const timer = setTimeout(() => setPhase("revealed"), 100);
      return () => clearTimeout(timer);
    }
  }, [phase, animDone, result]);

  const rewardItemName = useMemo(() => {
    if (!result || result.rewardType !== "ITEM" || result.rewardItemId === null) {
      return null;
    }
    return itemsQuery.data?.find((item) => item.itemId === result.rewardItemId)?.itemName ?? null;
  }, [result, itemsQuery.data]);

  // 받침 글로우 맥동.
  const glow = useRef(new Animated.Value(0)).current;
  useEffect(() => {
    const loop = Animated.loop(
      Animated.sequence([
        Animated.timing(glow, {
          toValue: 1,
          duration: 1400,
          easing: Easing.inOut(Easing.quad),
          useNativeDriver: true,
        }),
        Animated.timing(glow, {
          toValue: 0,
          duration: 1400,
          easing: Easing.inOut(Easing.quad),
          useNativeDriver: true,
        }),
      ]),
    );
    loop.start();
    return () => loop.stop();
  }, [glow]);
  const glowScale = glow.interpolate({ inputRange: [0, 1], outputRange: [0.9, 1.08] });
  const glowOpacity = glow.interpolate({ inputRange: [0, 1], outputRange: [0.45, 0.8] });

  // 등급 색 빛: 서버 결과(등급)를 알게 되면, 상자가 들썩이는 동안 서서히 밝아지며 커진다.
  const rarityColor = result?.rewardRarity
    ? (RARITY_LIGHT[result.rewardRarity.toUpperCase()] ?? DEFAULT_RARITY_LIGHT)
    : DEFAULT_RARITY_LIGHT;
  const rarityLight = useRef(new Animated.Value(0)).current;
  useEffect(() => {
    if (phase === "idle") {
      rarityLight.setValue(0);
      return;
    }
    if (phase === "opening" && result) {
      Animated.timing(rarityLight, {
        toValue: 1,
        duration: 1400,
        easing: Easing.in(Easing.quad),
        useNativeDriver: true,
      }).start();
    }
  }, [phase, result, rarityLight]);
  const rarityLightOpacity = rarityLight.interpolate({ inputRange: [0, 1], outputRange: [0, 1] });
  const rarityLightScale = rarityLight.interpolate({ inputRange: [0, 1], outputRange: [0.5, 1.35] });
  // 등급 빛이 밝아질수록 보라색 기본 글로우를 죽여, 등급 색이 배경에 묻히지 않게 한다.
  const baseGlowOpacity = Animated.multiply(
    glowOpacity,
    rarityLight.interpolate({ inputRange: [0, 1], outputRange: [1, 0.1] }),
  );

  // 보상 리빌 팝인.
  const reveal = useRef(new Animated.Value(0)).current;
  useEffect(() => {
    if (phase === "revealed") {
      reveal.setValue(0);
      Animated.spring(reveal, {
        toValue: 1,
        friction: 6,
        tension: 80,
        useNativeDriver: true,
      }).start();
    }
  }, [phase, reveal]);

  const isOpening = phase === "opening";

  return (
    <Modal
      transparent
      visible={product !== null}
      animationType="fade"
      onRequestClose={onClose}
    >
      <View style={styles.screen}>
        <LinearGradient
          colors={["#241B4D", "#3A2A78", "#1B1436"]}
          locations={[0, 0.55, 1]}
          style={StyleSheet.absoluteFill}
        />

        {phase !== "revealed" ? (
          <Pressable
            accessibilityLabel="닫기"
            hitSlop={10}
            style={styles.closeButton}
            onPress={onClose}
            disabled={isOpening && error === null}
          >
            <X color="#C4B5FD" size={26} strokeWidth={2.2} />
          </Pressable>
        ) : null}

        <View style={styles.stage}>
          <Animated.View
            style={[styles.stageGlow, { opacity: baseGlowOpacity, transform: [{ scale: glowScale }] }]}
          >
            <LinearGradient
              colors={["rgba(167,139,250,0.9)", "rgba(139,92,246,0.15)", "transparent"]}
              style={styles.stageGlowInner}
            />
          </Animated.View>

          {/* 등급 색 빛 — 상자에서 새어나오듯 뒤에서 밝아진다 */}
          <Animated.View
            pointerEvents="none"
            style={[
              styles.rarityGlow,
              { opacity: rarityLightOpacity, transform: [{ scale: rarityLightScale }] },
            ]}
          >
            <LinearGradient
              colors={[rarityColor, `${rarityColor}99`, `${rarityColor}00`]}
              style={styles.rarityGlowInner}
            />
          </Animated.View>

          <ChestAnimation size={220} playing={isOpening} onComplete={() => setAnimDone(true)} />

          <Text style={styles.stageHint}>
            {error ? " " : isOpening ? "두근두근…" : ""}
          </Text>

          {error ? (
            <>
              <Text style={styles.errorText}>{error}</Text>
              <Pressable style={styles.errorButton} onPress={onClose}>
                <Text style={styles.errorButtonText}>닫기</Text>
              </Pressable>
            </>
          ) : null}
        </View>

        {phase === "revealed" && result ? (
          <View style={styles.revealBackdrop}>
            <Animated.View
              style={[
                styles.revealCard,
                {
                  opacity: reveal,
                  transform: [
                    { scale: reveal.interpolate({ inputRange: [0, 1], outputRange: [0.7, 1] }) },
                  ],
                },
              ]}
            >
              <Text style={styles.revealEyebrow}>축하해요!</Text>
              <View style={styles.revealIcon}>
                <LinearGradient colors={["#FDE68A", "#F59E0B"]} style={styles.revealIconInner}>
                  {result.rewardType === "COIN" ? (
                    <Coins color="#7C2D12" size={40} strokeWidth={2.2} />
                  ) : (
                    <Sparkles color="#7C2D12" size={40} strokeWidth={2.2} />
                  )}
                </LinearGradient>
              </View>

              <Text style={styles.revealReward}>
                {result.rewardType === "COIN"
                  ? `코인 ${result.rewardCoinAmount?.toLocaleString() ?? 0}개`
                  : (rewardItemName ?? "새로운 아이템")}
              </Text>
              <Text style={styles.revealSub}>
                {result.rewardType === "COIN"
                  ? "코인이 지갑에 들어왔어요."
                  : "보관함에서 확인할 수 있어요."}
              </Text>

              <Pressable style={styles.revealButtonWrap} onPress={onClose}>
                <LinearGradient
                  colors={["#A78BFA", "#7C3AED"]}
                  start={{ x: 0, y: 0 }}
                  end={{ x: 1, y: 1 }}
                  style={styles.revealButton}
                >
                  <Text style={styles.revealButtonText}>확인</Text>
                </LinearGradient>
              </Pressable>
            </Animated.View>
          </View>
        ) : null}
      </View>
    </Modal>
  );
}

const styles = StyleSheet.create({
  screen: {
    flex: 1,
    backgroundColor: "#1B1436",
    alignItems: "center",
    justifyContent: "center",
  },
  closeButton: {
    position: "absolute",
    top: 48,
    right: 16,
    padding: 6,
    zIndex: 2,
  },
  stage: {
    alignItems: "center",
    justifyContent: "center",
  },
  stageGlow: {
    position: "absolute",
    width: 300,
    height: 300,
    alignItems: "center",
    justifyContent: "center",
  },
  stageGlowInner: {
    width: "100%",
    height: "100%",
    borderRadius: 150,
  },
  rarityGlow: {
    position: "absolute",
    width: 320,
    height: 320,
    alignItems: "center",
    justifyContent: "center",
  },
  rarityGlowInner: {
    width: "100%",
    height: "100%",
    borderRadius: 160,
  },
  stageHint: {
    marginTop: 18,
    minHeight: 20,
    color: "#C4B5FD",
    fontFamily: FONT.pretendard,
    fontSize: 14,
    fontWeight: "600",
  },
  errorText: {
    marginTop: 8,
    color: "#FCA5A5",
    fontFamily: FONT.pretendard,
    fontSize: 13,
    fontWeight: "600",
    textAlign: "center",
    paddingHorizontal: 24,
  },
  errorButton: {
    marginTop: 16,
    borderRadius: 12,
    backgroundColor: "rgba(255,255,255,0.14)",
    paddingVertical: 12,
    paddingHorizontal: 28,
  },
  errorButtonText: {
    color: "#F4F1FF",
    fontFamily: FONT.pretendard,
    fontSize: 14,
    fontWeight: "700",
  },
  revealBackdrop: {
    ...StyleSheet.absoluteFillObject,
    backgroundColor: "rgba(12,8,28,0.78)",
    alignItems: "center",
    justifyContent: "center",
    paddingHorizontal: 32,
  },
  revealCard: {
    width: "100%",
    maxWidth: 320,
    borderRadius: 24,
    backgroundColor: "#FFFFFF",
    paddingHorizontal: 24,
    paddingTop: 28,
    paddingBottom: 22,
    alignItems: "center",
  },
  revealEyebrow: {
    color: "#A855F7",
    fontFamily: FONT.pretendard,
    fontSize: 13,
    fontWeight: "800",
    letterSpacing: 1,
  },
  revealIcon: {
    marginTop: 16,
    marginBottom: 16,
  },
  revealIconInner: {
    width: 88,
    height: 88,
    borderRadius: 44,
    alignItems: "center",
    justifyContent: "center",
  },
  revealReward: {
    color: "#111827",
    fontFamily: FONT.pretendard,
    fontSize: 20,
    fontWeight: "800",
    textAlign: "center",
  },
  revealSub: {
    marginTop: 6,
    color: "#6B7280",
    fontFamily: FONT.pretendard,
    fontSize: 13,
    fontWeight: "500",
    textAlign: "center",
  },
  revealButtonWrap: {
    marginTop: 22,
    width: "100%",
    borderRadius: 14,
    overflow: "hidden",
  },
  revealButton: {
    alignItems: "center",
    justifyContent: "center",
    paddingVertical: 14,
  },
  revealButtonText: {
    color: "#ffffff",
    fontFamily: FONT.pretendard,
    fontSize: 15,
    fontWeight: "800",
  },
});
