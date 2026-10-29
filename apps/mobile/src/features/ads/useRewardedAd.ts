import { useCallback, useEffect, useRef, useState } from "react";
import { Platform, TurboModuleRegistry } from "react-native";
import type { RewardedAd } from "react-native-google-mobile-ads";

/**
 * 상점 「광고 보고 코인 받기」의 보상형 광고(AdMob)를 불러오고 띄운다.
 *
 * 코인은 앱이 아니라 서버가 준다. 광고를 만들 때 사용자 ID 를 넘기면, 시청이 끝났을 때 구글이 그 ID 를 담아
 * 우리 서버로 서명된 콜백(서버 측 확인)을 보내고 서버가 코인을 준다(#363). 그래서 이 훅은 광고가 닫힐 때
 * 끝까지 봤는지만 onClosed 로 알리고, 코인이 들어왔는지는 화면이 서버에 다시 물어 확인한다.
 *
 * SDK 는 광고를 불러올 때만 import 한다(googleAuth.ts 참고 — Expo Go 부팅 크래시 방지).
 * 네이티브 모듈이 없는 Expo Go 에서는 import 하지 않고 "unavailable" 로 끝난다. import 해 버리면 모듈을
 * 읽는 순간 오류가 나는데, Metro 가 이걸 try/catch 보다 먼저 치명적 오류로 띄워 전체 화면 오류가 뜬다.
 */
export type RewardedAdState = "idle" | "loading" | "ready" | "showing" | "error" | "no-fill" | "unavailable";

// 우리 AdMob 보상형 광고 단위 「상점_광고보상」. 공개값이라 그대로 둔다(서버 admob.rewarded-ad-unit-ids 와 같은 값).
// 구글 공용 테스트 단위로는 서버 측 확인 콜백이 오지 않아 개발 중에도 이 값을 쓴다. 그래서 개발에 쓰는 폰은
// AdMob 테스트 기기로 등록해 둬야 한다. 등록하지 않은 폰에서 실제 광고를 누르면 계정이 정지될 수 있다.
const REWARDED_AD_UNIT_ID = Platform.select({
  ios: "ca-app-pub-1897328042001393/7246621462",
  default: "ca-app-pub-1897328042001393/9301105018",
});

// SDK 초기화는 앱이 켜져 있는 동안 한 번이면 된다.
let sdkInitialization: Promise<unknown> | null = null;

export function useRewardedAd(
  userId: number | null,
  enabled: boolean,
  onClosed: (earned: boolean) => void,
) {
  const [state, setState] = useState<RewardedAdState>("idle");
  // 실패 뒤 다시 시도할 때 올린다. 바뀌면 SDK 준비부터 광고 불러오기까지 처음부터 다시 한다.
  const [attempt, setAttempt] = useState(0);
  const adRef = useRef<RewardedAd | null>(null);
  const onClosedRef = useRef(onClosed);

  useEffect(() => {
    onClosedRef.current = onClosed;
  }, [onClosed]);

  useEffect(() => {
    if (!enabled || userId === null) {
      return;
    }
    let cancelled = false;
    let unsubscribers: (() => void)[] = [];

    const load = async () => {
      if (TurboModuleRegistry.get("RNGoogleMobileAdsModule") === null) {
        setState("unavailable");
        return;
      }
      setState("loading");
      const ads = await import("react-native-google-mobile-ads");

      try {
        sdkInitialization ??= ads.default().initialize();
        await sdkInitialization;
      } catch {
        sdkInitialization = null;
        if (!cancelled) {
          setState("error");
        }
        return;
      }
      if (cancelled) {
        return;
      }

      const ad = ads.RewardedAd.createForAdRequest(REWARDED_AD_UNIT_ID, {
        serverSideVerificationOptions: { userId: String(userId) },
      });
      let earned = false;
      unsubscribers = [
        ad.addAdEventListener(ads.RewardedAdEventType.LOADED, () => setState("ready")),
        ad.addAdEventListener(ads.RewardedAdEventType.EARNED_REWARD, () => {
          earned = true;
        }),
        ad.addAdEventListener(ads.AdEventType.CLOSED, () => {
          onClosedRef.current(earned);
          earned = false;
          // 한 번 보여 준 광고는 다시 띄울 수 없다. 다음 시청을 위해 새로 불러 둔다.
          setState("loading");
          ad.load();
        }),
        ad.addAdEventListener(ads.AdEventType.ERROR, (error) =>
          // 보여 줄 광고가 없다는 응답(no-fill)은 다시 시도해도 그대로라 일반 실패와 나눈다.
          // 새 앱·스토어 미연결 앱은 AdMob 이 광고를 제한해 계속 이렇게 온다.
          setState(String((error as { code?: string }).code ?? "").endsWith("no-fill") ? "no-fill" : "error"),
        ),
      ];
      adRef.current = ad;
      ad.load();
    };
    void load();

    return () => {
      cancelled = true;
      unsubscribers.forEach((unsubscribe) => unsubscribe());
      adRef.current = null;
      setState("idle");
    };
  }, [enabled, userId, attempt]);

  // 불러온 광고를 띄운다. 실패한 상태면 처음부터 다시 불러온다.
  const showOrRetry = useCallback(() => {
    if (state === "error") {
      setAttempt((count) => count + 1);
      return;
    }
    const ad = adRef.current;
    if (state === "ready" && ad !== null) {
      setState("showing");
      ad.show().catch(() => setState("error"));
    }
  }, [state]);

  return { state, showOrRetry };
}
