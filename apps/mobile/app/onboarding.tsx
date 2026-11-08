import { router } from "expo-router";
import { useCallback, useEffect, useRef, useState } from "react";
import { BackHandler, StyleSheet, View } from "react-native";

import { useAuth } from "../src/features/auth";
import {
  MockContentScreen,
  MockFriendScreen,
  MockHomeScreen,
  MockLogWeakScreen,
  MockMyPageScreen,
  MockShopScreen,
  MockTabBar,
  MockWeaknessReportScreen,
} from "../src/features/onboarding/OnboardingMockScreens";
import { OnboardingBoxReveal } from "../src/features/onboarding/OnboardingBoxReveal";
import { OnboardingNicknameStep } from "../src/features/onboarding/OnboardingNicknameStep";
import { onboardingStorage } from "../src/features/onboarding/onboardingStorage";
import { ONBOARDING_STEPS, type OnboardingScreen } from "../src/features/onboarding/onboardingSteps";
import { SpotlightOverlay, type SpotlightRect } from "../src/features/onboarding/SpotlightOverlay";
import { SpotlightRegistryProvider, type SpotlightRegistry } from "../src/features/onboarding/SpotlightTarget";

type Phase = "tour" | "box" | "nickname";

const MOCK_SCREENS: Record<OnboardingScreen, () => React.JSX.Element> = {
  home: MockHomeScreen,
  content: MockContentScreen,
  "log-weak": MockLogWeakScreen,
  report: MockWeaknessReportScreen,
  friend: MockFriendScreen,
  mypage: MockMyPageScreen,
  shop: MockShopScreen,
};

/** 처음 들어온 회원에게 한 번 보여주는 튜토리얼. (tabs)/_layout 이 완료 플래그를 보고 여기로 보낸다. */
export default function OnboardingRoute() {
  const { user } = useAuth();
  const [phase, setPhase] = useState<Phase>("tour");
  const [stepIndex, setStepIndex] = useState(0);
  // 잰 위치를 단계 번호와 함께 둬야 단계가 바뀐 직후 이전 단계의 구멍이 한 프레임 비치지 않는다.
  const [measured, setMeasured] = useState<{ stepIndex: number; rect: SpotlightRect | null } | null>(null);
  const [registry] = useState<SpotlightRegistry>(() => new Map());
  const rootRef = useRef<View>(null);

  const step = ONBOARDING_STEPS[stepIndex];
  const MockScreen = MOCK_SCREENS[step.screen];
  const showTabBar = step.screen !== "report" && step.screen !== "shop";
  // 강조 대상이 없는 단계는 잴 것이 없다.
  const measuring = step.target !== null && measured?.stepIndex !== stepIndex;
  const targetRect = step.target === null || measuring ? null : (measured?.rect ?? null);

  // 튜토리얼 도중 뒤로가기로 빠져나가지 않게 막는다(iOS 스와이프는 Stack 옵션에서 막는다).
  useEffect(() => {
    const subscription = BackHandler.addEventListener("hardwareBackPress", () => true);
    return () => subscription.remove();
  }, []);

  // 단계가 바뀌면 새 더미 화면이 그려진 뒤 강조할 버튼 위치를 잰다.
  useEffect(() => {
    if (phase !== "tour" || step.target === null) {
      return;
    }
    let cancelled = false;
    let attempts = 0;
    // 아직 그려지지 않았으면 잠깐 뒤 다시 잰다. 끝내 못 재면 구멍 없는 안내 카드로 넘겨
    // 사용자가 어두운 화면에 갇히지 않게 한다.
    const retryOrGiveUp = () => {
      if (cancelled) {
        return;
      }
      if (attempts++ < 20) {
        setTimeout(measure, 50);
      } else {
        console.error("[onboarding] 강조 대상 위치를 재지 못함", step.target);
        setMeasured({ stepIndex, rect: null });
      }
    };
    const measure = () => {
      const targetView = step.target ? registry.get(step.target) : undefined;
      const rootView = rootRef.current;
      if (!targetView || !rootView) {
        retryOrGiveUp();
        return;
      }
      rootView.measureInWindow((rootX, rootY) => {
        targetView.measureInWindow((x, y, width, height) => {
          if (cancelled) {
            return;
          }
          if (width === 0 && height === 0) {
            retryOrGiveUp();
            return;
          }
          setMeasured({ stepIndex, rect: { x: x - rootX, y: y - rootY, width, height } });
        });
      });
    };
    // 레이아웃이 잡힐 시간을 조금 준다(방 장면·폰트가 늦게 자리잡는 경우).
    const timer = setTimeout(measure, 120);
    return () => {
      cancelled = true;
      clearTimeout(timer);
    };
  }, [phase, stepIndex, step.target, registry]);

  const goNext = () => {
    if (stepIndex < ONBOARDING_STEPS.length - 1) {
      setStepIndex(stepIndex + 1);
    } else {
      setPhase("box");
    }
  };

  const finish = useCallback(async () => {
    if (user) {
      await onboardingStorage.markDone(user.userId);
    }
    router.replace("/(tabs)/home");
  }, [user]);

  if (phase === "box") {
    return <OnboardingBoxReveal onDone={() => setPhase("nickname")} />;
  }

  if (phase === "nickname") {
    return <OnboardingNicknameStep onDone={finish} />;
  }

  return (
    <View ref={rootRef} collapsable={false} style={styles.root}>
      <SpotlightRegistryProvider value={registry}>
        <View style={styles.root} pointerEvents="none">
          <MockScreen />
          {showTabBar ? <MockTabBar active={step.screen as Exclude<OnboardingScreen, "report" | "shop">} /> : null}
        </View>
      </SpotlightRegistryProvider>
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

const styles = StyleSheet.create({
  root: { flex: 1 },
});
