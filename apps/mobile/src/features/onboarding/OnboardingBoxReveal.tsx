import { LinearGradient } from "expo-linear-gradient";
import { Sparkles } from "lucide-react-native";
import { useEffect, useState } from "react";
import { Animated, Pressable, StyleSheet, Text, View } from "react-native";

import { FONT } from "../../../constants/fonts";
import { ChestOpenCanvas } from "../shop/ChestOpenCanvas";

/**
 * 튜토리얼용 랜덤박스 열기 연출. 실제 상점과 같은 상자 애니메이션을 재생하지만
 * 서버에 구매 요청을 보내지 않으므로 코인은 그대로다.
 */
export function OnboardingBoxReveal({ onDone }: { onDone: () => void }) {
  const [revealed, setRevealed] = useState(false);
  const [reveal] = useState(() => new Animated.Value(0));

  useEffect(() => {
    if (revealed) {
      Animated.spring(reveal, { toValue: 1, friction: 6, tension: 80, useNativeDriver: true }).start();
    }
  }, [revealed, reveal]);

  return (
    <View style={styles.screen}>
      <LinearGradient
        colors={["#241B4D", "#3A2A78", "#1B1436"]}
        locations={[0, 0.55, 1]}
        style={StyleSheet.absoluteFill}
      />
      <ChestOpenCanvas playing={!revealed} rarity="EPIC" onComplete={() => setRevealed(true)} />
      <Text style={styles.hint}>{revealed ? "" : "두근두근…"}</Text>

      {revealed ? (
        <View style={styles.backdrop}>
          <Animated.View
            style={[
              styles.card,
              {
                opacity: reveal,
                transform: [{ scale: reveal.interpolate({ inputRange: [0, 1], outputRange: [0.7, 1] }) }],
              },
            ]}
          >
            <Text style={styles.eyebrow}>이렇게 열려요!</Text>
            <LinearGradient colors={["#E9D5FF", "#A855F7"]} style={styles.circle}>
              <Sparkles color="#3B0764" size={56} strokeWidth={2.2} />
            </LinearGradient>
            <Text style={styles.reward}>아바타 아이템 획득</Text>
            <Text style={styles.sub}>
              뽑은 아이템은 인벤토리에서 장착할 수 있어요.{"\n"}지금은 튜토리얼이라 코인이 쓰이지 않았어요.
            </Text>
            <Pressable accessibilityRole="button" style={styles.buttonWrap} onPress={onDone}>
              <LinearGradient
                colors={["#A78BFA", "#7C3AED"]}
                start={{ x: 0, y: 0 }}
                end={{ x: 1, y: 1 }}
                style={styles.button}
              >
                <Text style={styles.buttonText}>다음</Text>
              </LinearGradient>
            </Pressable>
          </Animated.View>
        </View>
      ) : null}
    </View>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1, alignItems: "center", justifyContent: "center", backgroundColor: "#1B1436" },
  hint: {
    marginTop: 18,
    minHeight: 20,
    color: "#C4B5FD",
    fontFamily: FONT.pretendard,
    fontSize: 14,
    fontWeight: "600",
  },
  backdrop: {
    ...StyleSheet.absoluteFill,
    alignItems: "center",
    justifyContent: "center",
    backgroundColor: "rgba(12,8,28,0.78)",
    paddingHorizontal: 32,
  },
  card: {
    width: "100%",
    maxWidth: 340,
    alignItems: "center",
    borderRadius: 24,
    backgroundColor: "#FFFFFF",
    paddingHorizontal: 24,
    paddingTop: 28,
    paddingBottom: 22,
  },
  eyebrow: { color: "#A855F7", fontFamily: FONT.pretendard, fontSize: 13, fontWeight: "800", letterSpacing: 1 },
  circle: {
    width: 120,
    height: 120,
    marginVertical: 24,
    alignItems: "center",
    justifyContent: "center",
    borderRadius: 60,
  },
  reward: { color: "#111827", fontFamily: FONT.pretendard, fontSize: 20, fontWeight: "800" },
  sub: {
    marginTop: 6,
    color: "#6B7280",
    fontFamily: FONT.pretendard,
    fontSize: 13,
    fontWeight: "500",
    lineHeight: 19,
    textAlign: "center",
  },
  buttonWrap: { width: "100%", marginTop: 22, borderRadius: 14, overflow: "hidden" },
  button: { alignItems: "center", justifyContent: "center", paddingVertical: 14 },
  buttonText: { color: "#ffffff", fontFamily: FONT.pretendard, fontSize: 15, fontWeight: "800" },
});
