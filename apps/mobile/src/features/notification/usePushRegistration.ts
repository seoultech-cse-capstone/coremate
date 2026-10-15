import { useEffect, useRef } from "react";
import { useQueryClient } from "@tanstack/react-query";
import * as Notifications from "expo-notifications";
import { router, useRootNavigationState } from "expo-router";

import { useAuth } from "../auth/AuthContext";
import { deviceStorage } from "../device/deviceStorage";
import { getDeviceInfo } from "../device/deviceInfo";
import { notificationApi } from "./notificationApi";
import { getFcmToken, rememberRegisteredToken } from "./pushToken";
import type { NotificationTargetType } from "./types";

/** 알림 종류별로 열어 줄 화면. NONE이면 앱만 열고 홈에 머문다. */
const TARGET_ROUTES: Record<NotificationTargetType, string | null> = {
  NONE: null,
  FRIEND: "/(tabs)/friend",
  DAILY_QUIZ: "/(tabs)/home",
  MYPAGE: "/(tabs)/mypage",
  SETTING: "/device-management",
  WEAKNESS_REPORT: "/weakness-report",
};

function openTarget(data: Record<string, unknown> | undefined) {
  const targetType = data?.targetType as NotificationTargetType | undefined;
  if (!targetType) {
    return;
  }
  // 약점분석 완료 알림은 target_id(리포트 ID)로 해당 분석표 상세를 바로 연다.
  if (targetType === "WEAKNESS_REPORT") {
    const reportId = data?.targetId;
    if (reportId !== undefined && reportId !== null) {
      router.push({
        pathname: "/weakness-report",
        params: { reportId: String(reportId) },
      } as never);
    }
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
  const { accessToken, isAuthenticated, status } = useAuth();
  const queryClient = useQueryClient();
  // 앱이 꺼진 상태에서 알림으로 열리면 화면이 준비되기 전에 이동을 시도하게 된다.
  // key 가 생기면 라우터가 첫 화면을 올린 뒤다.
  const navigationState = useRootNavigationState();
  const navigationReady = Boolean(navigationState?.key);
  useEffect(() => {
    if (!isAuthenticated || !accessToken) {
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
        // 로그아웃 때 해제할 대상. 해제 호출 자체는 로그아웃 흐름이 담당한다.
        rememberRegisteredToken(token);
      } catch {
        // 등록 실패는 알림만 못 받는 상태다. 다음 실행에서 다시 시도한다.
      }
    })();

    return () => {
      cancelled = true;
    };
  }, [accessToken, isAuthenticated]);

  // 알림을 눌러 앱이 열린 경우(백그라운드에서 복귀).
  useEffect(() => {
    const subscription = Notifications.addNotificationResponseReceivedListener((response) => {
      openTarget(response.notification.request.content.data as Record<string, unknown>);
    });
    return () => subscription.remove();
  }, []);

  // 앱이 완전히 꺼진 상태에서 알림으로 열린 경우. 위 리스너로는 잡히지 않는다.
  //
  // 라우터 준비와 인증 복원이 "둘 다" 끝난 뒤에 이동해야 한다. 앱 진입 화면(app/index.tsx)이
  // 인증 복원을 마치면 홈으로 replace 하는데, 그보다 먼저 이동하면 그 replace 가 덮어쓴다.
  // 앱을 종료해 두는 게 보통이라 실제로는 이 경로가 더 자주 쓰인다.
  const coldStartHandled = useRef(false);
  useEffect(() => {
    if (!navigationReady || status === "loading" || coldStartHandled.current) {
      return;
    }
    coldStartHandled.current = true;
    Notifications.getLastNotificationResponseAsync().then((response) => {
      if (response) {
        openTarget(response.notification.request.content.data as Record<string, unknown>);
      }
    });
  }, [navigationReady, status]);

  // 앱이 켜져 있는 동안 알림이 오면 배지와 목록을 새로 읽는다.
  // 이게 없으면 화면에 들어올 때만 갱신돼, 앱을 켜 둔 채로 받은 알림이 배지에 반영되지 않는다.
  useEffect(() => {
    const subscription = Notifications.addNotificationReceivedListener((notification) => {
      queryClient.invalidateQueries({ queryKey: ["notifications"] });
      // 약점분석 완료 알림이 앱이 켜진 채 오면, 보고 있던 목록·상세가 즉시 완료로 바뀌도록 갱신한다.
      const data = notification.request.content.data as Record<string, unknown>;
      if (data?.targetType === "WEAKNESS_REPORT") {
        queryClient.invalidateQueries({ queryKey: ["weakness"] });
      }
    });
    return () => subscription.remove();
  }, [queryClient]);
}

function platformOf(): "IOS" | "ANDROID" {
  // 서버는 IOS/ANDROID 만 받는다. 웹에서는 토큰 발급 자체가 안 되므로 여기 오지 않는다.
  return getDeviceInfo().platform === "IOS" ? "IOS" : "ANDROID";
}
