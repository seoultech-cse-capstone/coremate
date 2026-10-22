import { useQueryClient } from "@tanstack/react-query";
import { router, useLocalSearchParams } from "expo-router";
import { useEffect, useRef, useState } from "react";
import { ActivityIndicator, Pressable, StyleSheet, Text, View } from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";

import { FONT } from "../constants/fonts";
import { useAuth, useRequireMember } from "../src/features/auth";
import { weaknessApi, type AnalysisScopeType } from "../src/features/weakness";
import { getApiErrorMessage } from "../src/shared/utils/apiError";
import { createIdempotencyKey } from "../src/shared/utils/idempotency";

const BRAND_PURPLE = "#6D5DFB";

// 생성은 비동기(#323)다. 이 화면은 "요청 접수"만 담당한다: 서버에 생성을 걸고(202, 금방 돌아온다)
// 곧바로 복습 탭 목록으로 돌아간다. 실제 분석은 백그라운드에서 진행되고, 완료되면 FCM/폴링으로
// 목록이 '분석 중' → '완료'로 바뀐다. 사용자는 기다리지 않고 다른 행동을 할 수 있다.
export default function WeaknessGeneratingScreen() {
  useRequireMember();
  const { accessToken } = useAuth();
  const params = useLocalSearchParams<{
    solveSessionIds: string;
    analysisScopeType: AnalysisScopeType;
    subjectId: string;
    categoryLv1Id?: string;
    categoryLv2Id?: string;
    conceptId?: string;
  }>();
  const [error, setError] = useState<string | null>(null);
  const hasStartedRef = useRef(false);
  // 이 화면 한 번 = 사용자가 생성을 한 번 확정한 것. 토큰 재발급으로 요청이 재전송돼도
  // 같은 키를 써야 코인이 두 번 빠지지 않는다.
  const idempotencyKeyRef = useRef(createIdempotencyKey());
  const queryClient = useQueryClient();

  useEffect(() => {
    if (hasStartedRef.current || !accessToken) {
      return;
    }
    hasStartedRef.current = true;

    (async () => {
      try {
        const solveSessionIds: number[] = JSON.parse(params.solveSessionIds ?? "[]");
        await weaknessApi.createReport(
          {
            solveSessionIds,
            analysisScopeType: params.analysisScopeType,
            subjectId: Number(params.subjectId),
            categoryLv1Id: params.categoryLv1Id ? Number(params.categoryLv1Id) : undefined,
            categoryLv2Id: params.categoryLv2Id ? Number(params.categoryLv2Id) : undefined,
            conceptId: params.conceptId ? Number(params.conceptId) : undefined,
            useCoin: true,
          },
          accessToken,
          idempotencyKeyRef.current,
        );

        // 분석표 목록·통계와 코인 잔액은 이 요청으로 서버에서 바뀌었다(생성 중 분석표 추가 + 코인 차감).
        // 무효화하지 않으면 복습 탭이 요청 전 목록을 그대로 들고 있어 '분석 중' 카드가 안 보인다.
        await Promise.all([
          queryClient.invalidateQueries({ queryKey: ["weakness", "reports"] }),
          queryClient.invalidateQueries({ queryKey: ["currency", "me"] }),
        ]);

        // 요청·결제 확인 화면을 스택에서 걷어내고 복습 탭 목록으로 돌아간다(뒤로 가도 결제 화면이 되살아나지 않게).
        if (router.canDismiss()) {
          router.dismissAll();
        }
        router.replace("/(tabs)/log-weak");
      } catch (err) {
        // 근거 부족(WEAKNESS_INSUFFICIENT_EVIDENCE) 같은 정상 분기도 400 으로 오므로
        // console.error 를 쓰지 않는다. 개발 빌드에서 화면을 덮는 에러 오버레이가 뜬다.
        setError(getApiErrorMessage(err, "분석표 생성에 실패했어요."));
      }
    })();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [accessToken]);

  if (error) {
    return (
      <View style={styles.screen}>
        <SafeAreaView edges={["top"]} style={styles.safeArea}>
          <View style={styles.centerArea}>
            <Text style={styles.errorText}>{error}</Text>
            <Pressable style={styles.backButton} onPress={() => router.back()}>
              <Text style={styles.backButtonText}>돌아가기</Text>
            </Pressable>
          </View>
        </SafeAreaView>
      </View>
    );
  }

  return (
    <View style={styles.screen}>
      <SafeAreaView edges={["top"]} style={styles.safeArea}>
        <View style={styles.centerArea}>
          <ActivityIndicator color={BRAND_PURPLE} size="large" />
          <Text style={styles.title}>분석을 요청하고 있어요</Text>
          <Text style={styles.message}>
            분석은 백그라운드에서 진행돼요. 완료되면 알림으로 알려드릴게요.
          </Text>
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
  },
  centerArea: {
    flex: 1,
    alignItems: "center",
    justifyContent: "center",
    paddingHorizontal: 32,
  },
  title: {
    marginTop: 24,
    color: "#111827",
    fontFamily: FONT.pretendard,
    fontSize: 17,
    fontWeight: "700",
  },
  message: {
    marginTop: 12,
    color: "#64748b",
    fontFamily: FONT.pretendard,
    fontSize: 14,
    fontWeight: "500",
    textAlign: "center",
  },
  errorText: {
    color: "#334155",
    fontFamily: FONT.pretendard,
    fontSize: 14,
    fontWeight: "600",
    textAlign: "center",
    lineHeight: 21,
  },
  backButton: {
    marginTop: 20,
    borderRadius: 8,
    backgroundColor: BRAND_PURPLE,
    paddingHorizontal: 24,
    paddingVertical: 12,
  },
  backButtonText: {
    color: "#ffffff",
    fontFamily: FONT.pretendard,
    fontSize: 14,
    fontWeight: "700",
  },
});
