import { router, useLocalSearchParams } from "expo-router";
import { useEffect, useRef, useState } from "react";
import { ActivityIndicator, BackHandler, Pressable, StyleSheet, Text, View } from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";

import { FONT } from "../constants/fonts";
import { useAuth, useRequireMember } from "../src/features/auth";
import { weaknessApi, type AnalysisScopeType } from "../src/features/weakness";
import { getApiErrorMessage } from "../src/shared/utils/apiError";

const BRAND_PURPLE = "#6D5DFB";

// 실제로 몇 단계를 거치는지 사용자가 알 방법이 없어(동기 호출 1번), 대기 시간 동안
// "만들어지고 있다"는 느낌을 주기 위해 순서대로 보여주는 문구. 실제 처리 단계와는
// 무관하다.
const GENERATING_MESSAGES = [
  "선택한 문제집의 풀이 기록을 모으고 있어요…",
  "틀린 문제, 오래 걸린 문제, 힌트 사용을 살펴보고 있어요…",
  "AI가 약점 개념을 진단하고 있어요…",
  "분석 결과를 정리하고 있어요…",
  "거의 다 됐어요. 조금만 기다려주세요…",
];

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
  const [messageIndex, setMessageIndex] = useState(0);
  const [error, setError] = useState<string | null>(null);
  const hasStartedRef = useRef(false);

  useEffect(() => {
    const intervalId = setInterval(() => {
      setMessageIndex((index) => (index + 1) % GENERATING_MESSAGES.length);
    }, 2200);
    return () => clearInterval(intervalId);
  }, []);

  // 생성 요청이 진행되는 동안에는 뒤로가기로 빠져나가지 못하게 막는다
  // (실패 화면에서는 이 화면 자체가 다른 return을 타서 뒤로가기가 다시 풀린다).
  useEffect(() => {
    if (error) {
      return;
    }
    const subscription = BackHandler.addEventListener("hardwareBackPress", () => true);
    return () => subscription.remove();
  }, [error]);

  useEffect(() => {
    if (hasStartedRef.current || !accessToken) {
      return;
    }
    hasStartedRef.current = true;

    (async () => {
      try {
        const solveSessionIds: number[] = JSON.parse(params.solveSessionIds ?? "[]");
        const report = await weaknessApi.createReport(
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
        );
        router.replace({
          pathname: "/weakness-report",
          params: { reportId: report.analysisReportId.toString() },
        });
      } catch (err) {
        console.error("[weakness] 분석표 생성 실패", err);
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
          <Text style={styles.title}>약점 분석표를 만들고 있어요</Text>
          <Text style={styles.message}>{GENERATING_MESSAGES[messageIndex]}</Text>
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
