import { useEffect, useRef } from "react";
import * as Notifications from "expo-notifications";
import { router } from "expo-router";

import { useAuth } from "../auth/AuthContext";
import { deviceStorage } from "../device/deviceStorage";
import { getDeviceInfo } from "../device/deviceInfo";
import { notificationApi } from "./notificationApi";
import { getFcmToken } from "./pushToken";
import type { NotificationTargetType } from "./types";

/** 알림 종류별로 열어 줄 화면. NONE이면 앱만 열고 홈에 머문다. */
const TARGET_ROUTES: Record<NotificationTargetType, string | null> = {
  NONE: null,
  FRIEND: "/(tabs)/friend",
  DAILY_QUIZ: "/(tabs)/home",
  MYPAGE: "/(tabs)/mypage",
  SETTING: "/device-management",
};

function openTarget(data: Record<string, unknown> | undefined) {
  const targetType = data?.targetType as NotificationTargetType | undefined;
  if (!targetType) {
    return;
  }
  const route = TARGET_ROUTES[targetType];
  if (route) {
    router.push(route as never);
  }
}

/**
 * 로그인 상태에서 기기 토큰을 서버에 등록하고, 알림을 탭했을 때 화면을 열어 준다.
 *
 * 로그아웃하면 해제한다. 해제하지 않으면 로그아웃한 기기로도 알림이 계속 가고,
 * 다른 사람이 그 기기를 쓰면 남의 알림을 보게 된다.
 */
export function usePushRegistration() {
  const { accessToken, isAuthenticated } = useAuth();
  // 로그아웃 시점에는 이미 토큰이 비어 있어, 해제에 쓸 값을 미리 들고 있어야 한다.
  const registeredRef = useRef<{ token: string; accessToken: string } | null>(null);

  useEffect(() => {
    if (!isAuthenticated || !accessToken) {
      const registered = registeredRef.current;
      registeredRef.current = null;
      if (registered) {
        // 실패해도 사용자가 할 수 있는 일이 없다. 서버는 다른 계정이 같은 토큰을
        // 등록하는 시점에도 이전 등록을 내린다.
        notificationApi
          .unregisterDeviceToken(registered.token, registered.accessToken)
          .catch(() => undefined);
      }
      return;
    }

    let cancelled = false;

    (async () => {
      const token = await getFcmToken();
      if (!token || cancelled) {
        return;
      }
      const deviceId = await deviceStorage.get();
      try {
        await notificationApi.registerDeviceToken(
          { token, platform: platformOf(), deviceId },
          accessToken,
        );
        registeredRef.current = { token, accessToken };
      } catch {
        // 등록 실패는 알림만 못 받는 상태다. 다음 실행에서 다시 시도한다.
      }
    })();

    return () => {
      cancelled = true;
    };
  }, [accessToken, isAuthenticated]);

  useEffect(() => {
    // 알림을 눌러 앱이 열린 경우(백그라운드에서 복귀).
    const subscription = Notifications.addNotificationResponseReceivedListener((response) => {
      openTarget(response.notification.request.content.data as Record<string, unknown>);
    });

    // 앱이 완전히 꺼진 상태에서 알림으로 열린 경우. 위 리스너로는 잡히지 않는다.
    Notifications.getLastNotificationResponseAsync().then((response) => {
      if (response) {
        openTarget(response.notification.request.content.data as Record<string, unknown>);
      }
    });

    return () => subscription.remove();
  }, []);
}

function platformOf(): "IOS" | "ANDROID" {
  // 서버는 IOS/ANDROID 만 받는다. 웹에서는 토큰 발급 자체가 안 되므로 여기 오지 않는다.
  return getDeviceInfo().platform === "IOS" ? "IOS" : "ANDROID";
}
