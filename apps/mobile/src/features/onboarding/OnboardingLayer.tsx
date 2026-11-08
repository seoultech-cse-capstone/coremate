import { useQueryClient } from "@tanstack/react-query";
import { router, usePathname } from "expo-router";
import { useEffect, useRef, useState } from "react";
import { BackHandler, StyleSheet, View } from "react-native";

import { useAuth } from "../auth";
import { OnboardingNicknameStep } from "./OnboardingNicknameStep";
import { ONBOARDING_STEPS } from "./onboardingSteps";
import { onboardingStorage } from "./onboardingStorage";
import { centerSpotlightTarget, getSpotlightTarget, type MeasurableNode } from "./spotlightRegistry";
import { SpotlightOverlay, type SpotlightRect } from "./SpotlightOverlay";
import { restoreTutorialData, seedTutorialData } from "./tutorialData";

type Phase = "idle" | "tour" | "nickname";

// 화면 전환 애니메이션이 끝나고 위치가 멈출 때까지 다시 잰다.
const MEASURE_INTERVAL_MS = 100;
const MEASURE_MAX_ATTEMPTS = 40;

/**
 * 처음 들어온 회원에게 실제 화면 위에서 진행하는 튜토리얼.
 * 루트 레이아웃에서 Stack 위에 겹쳐 그린다. 튜토리얼 동안만 친구·분석표를 더미로 채운다.
 */
export function OnboardingLayer() {
  const { status, user } = useAuth();
  const userId = user?.userId ?? null;
  const pathname = usePathname();
  const queryClient = useQueryClient();
  const [phase, setPhase] = useState<Phase>("idle");
  const [stepIndex, setStepIndex] = useState(0);
  // 잰 위치를 단계 번호와 함께 둬야 단계가 바뀐 직후 이전 단계의 구멍이 비치지 않는다.
  const [measured, setMeasured] = useState<{ stepIndex: number; rect: SpotlightRect | null } | null>(null);
  const rootRef = useRef<MeasurableNode | null>(null);
  const checkedUserRef = useRef<number | null>(null);

  const step = ONBOARDING_STEPS[stepIndex];
  const active = phase !== "idle";

  // 회원이 홈에 처음 들어왔을 때 완료 플래그가 없으면 시작한다. 로그인 화면이 홈으로 보내는 중에
  // 시작하면 이동이 꼬이므로 홈에 도착한 뒤에 확인한다.
  useEffect(() => {
    if (status !== "authenticated" || userId === null || pathname !== "/home") {
      return;
    }
    if (checkedUserRef.current === userId) {
      return;
    }
    checkedUserRef.current = userId;
    onboardingStorage.isDone(userId).then(async (done) => {
      if (done) {
        return;
      }
      await seedTutorialData(queryClient);
      setStepIndex(0);
      setMeasured(null);
      setPhase("tour");
    });
  }, [status, userId, pathname, queryClient]);

  // 튜토리얼 도중 안드로이드 뒤로가기로 빠져나가지 않게 막는다.
  useEffect(() => {
    if (!active) {
      return;
    }
    const subscription = BackHandler.addEventListener("hardwareBackPress", () => true);
    return () => subscription.remove();
  }, [active]);

  // 단계가 바뀌면 강조할 버튼 위치를 잰다. 위치가 두 번 연속 같으면 자리 잡은 것으로 본다.
  useEffect(() => {
    if (phase !== "tour" || step.target === null) {
      return;
    }
    const targetId = step.target;
    const scrollId = step.scroll;
    let scrolled = false;
    let cancelled = false;
    let attempts = 0;
    let lastRect: SpotlightRect | null = null;

    const retryOrGiveUp = () => {
      if (cancelled) {
        return;
      }
      if (attempts++ < MEASURE_MAX_ATTEMPTS) {
        setTimeout(measure, MEASURE_INTERVAL_MS);
      } else {
        // 끝내 못 재면 구멍 없는 안내 카드로 넘겨 어두운 화면에 갇히지 않게 한다.
        console.error("[onboarding] 강조 대상 위치를 재지 못함", targetId);
        setMeasured({ stepIndex, rect: lastRect });
      }
    };

    const measure = () => {
      const targetView = getSpotlightTarget(targetId);
      const rootView = rootRef.current;
      if (!targetView || !rootView) {
        retryOrGiveUp();
        return;
      }
      // 대상이 그려지면 먼저 화면 가운데로 스크롤하고, 스크롤이 멈춘 뒤의 위치를 잰다.
      if (scrollId && !scrolled) {
        scrolled = true;
        centerSpotlightTarget(scrollId, targetId);
        retryOrGiveUp();
        return;
      }
      rootView.measureInWindow((rootX, rootY) => {
        targetView.measureInWindow((x, y, width, height) => {
          if (cancelled) {
            return;
          }
          const rect = { x: x - rootX, y: y - rootY, width, height };
          const settled =
            width > 0 &&
            lastRect !== null &&
            Math.abs(lastRect.x - rect.x) < 1 &&
            Math.abs(lastRect.y - rect.y) < 1 &&
            Math.abs(lastRect.width - rect.width) < 1;
          if (settled) {
            setMeasured({ stepIndex, rect });
            return;
          }
          lastRect = width > 0 ? rect : null;
          retryOrGiveUp();
        });
      });
    };

    const timer = setTimeout(measure, MEASURE_INTERVAL_MS);
    return () => {
      cancelled = true;
      clearTimeout(timer);
    };
  }, [phase, stepIndex, step.target, step.scroll]);

  const goNext = () => {
    const nextIndex = stepIndex + 1;
    if (nextIndex >= ONBOARDING_STEPS.length) {
      setPhase("nickname");
      return;
    }
    const next = ONBOARDING_STEPS[nextIndex];
    if (next.navigate === "back") {
      router.back();
    } else if (next.navigate) {
      router.navigate(next.navigate);
    }
    setStepIndex(nextIndex);
  };

  const finish = async () => {
    if (userId !== null) {
      await onboardingStorage.markDone(userId);
    }
    restoreTutorialData(queryClient);
    setPhase("idle");
    router.navigate("/(tabs)/home");
  };

  if (!active) {
    return null;
  }

  if (phase === "nickname") {
    return (
      <View style={StyleSheet.absoluteFill}>
        <OnboardingNicknameStep onDone={finish} />
      </View>
    );
  }

  const measuring = step.target !== null && measured?.stepIndex !== stepIndex;
  const targetRect = step.target === null || measuring ? null : (measured?.rect ?? null);

  return (
    <View
      ref={(node: MeasurableNode | null) => {
        rootRef.current = node;
      }}
      collapsable={false} style={StyleSheet.absoluteFill} pointerEvents="box-none">
      <SpotlightOverlay
        target={targetRect}
        measuring={measuring}
        stepLabel={`${stepIndex + 1} / ${ONBOARDING_STEPS.length}`}
        title={step.title}
        body={step.body}
        centerActionLabel={stepIndex === 0 ? "둘러보기 시작" : "다음"}
        onTargetPress={goNext}
        onSkip={() => setPhase("nickname")}
      />
    </View>
  );
}
