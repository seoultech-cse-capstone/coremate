import { router } from "expo-router";
import { Pressable, StyleSheet, Text, View } from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";

import { FONT } from "../../../../constants/fonts";

// SCR-COMMON-02 로그인 필요 안내 화면/모달.
// 비로그인 사용자가 회원 전용 기능(홈, 약점 분석, 마이페이지, 친구 등)에 접근했을 때 대신 렌더링한다.
export function LoginRequiredGuide() {
  return (
    <View style={styles.screen}>
      <SafeAreaView edges={["top"]} style={styles.safeArea}>
        <View style={styles.body}>
          <Text style={styles.title}>로그인이 필요한 기능이에요</Text>
          <Text style={styles.description}>
            게스트 모드에서는 문제 풀이와 해설만 이용할 수 있어요.{"\n"}
            로그인하면 학습 기록, 약점 분석, 친구, 보상까지 모두 사용할 수 있습니다.
          </Text>

          <Pressable
            style={styles.primaryButton}
            onPress={() => router.replace("/")}
          >
            <Text style={styles.primaryButtonText}>로그인하러 가기</Text>
          </Pressable>

          <Pressable style={styles.backLink} onPress={() => router.back()}>
            <Text style={styles.backLinkText}>이전 화면으로 돌아가기</Text>
          </Pressable>
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
  body: {
    flex: 1,
    justifyContent: "center",
    paddingHorizontal: 24,
  },
  title: {
    color: "#111827",
    fontFamily: FONT.pretendard,
    fontSize: 22,
    fontWeight: "800",
    textAlign: "center",
  },
  description: {
    marginTop: 12,
    marginBottom: 32,
    color: "#64748b",
    fontFamily: FONT.pretendard,
    fontSize: 15,
    lineHeight: 22,
    textAlign: "center",
  },
  primaryButton: {
    alignItems: "center",
    borderRadius: 16,
    backgroundColor: "#6D5DFB",
    paddingVertical: 16,
  },
  primaryButtonText: {
    color: "#ffffff",
    fontFamily: FONT.pretendard,
    fontSize: 15,
    fontWeight: "800",
  },
  backLink: {
    alignItems: "center",
    paddingVertical: 16,
  },
  backLinkText: {
    color: "#94a3b8",
    fontFamily: FONT.pretendard,
    fontSize: 14,
    fontWeight: "500",
  },
});
