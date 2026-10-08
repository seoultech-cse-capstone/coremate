import { httpClient } from "../../shared/api/httpClient";
import { tokenStorage } from "../auth/tokenStorage";
import { getRegisteredToken, rememberRegisteredToken } from "./pushToken";
import type {
  AppNotification,
  DevicePlatform,
  NotificationCategory,
  NotificationSetting,
  UnreadCount,
} from "./types";

/** 알림 REST 호출. 전부 회원 전용이다. */
export const notificationApi = {
  registerDeviceToken(
    body: { token: string; platform: DevicePlatform; deviceId: string | null },
    accessToken: string,
  ): Promise<void> {
    return httpClient.post<void>("/api/notifications/device-tokens", body, { accessToken });
  },

  unregisterDeviceToken(token: string, accessToken: string): Promise<void> {
    return httpClient.delete<void>(
      `/api/notifications/device-tokens?token=${encodeURIComponent(token)}`,
      { accessToken },
    );
  },

  getSettings(accessToken: string): Promise<NotificationSetting[]> {
    return httpClient.get<NotificationSetting[]>("/api/notifications/settings", { accessToken });
  },

  updateSetting(
    body: { category: NotificationCategory; enabled: boolean },
    accessToken: string,
  ): Promise<NotificationSetting> {
    return httpClient.put<NotificationSetting>("/api/notifications/settings", body, { accessToken });
  },

  getNotifications(accessToken: string): Promise<AppNotification[]> {
    return httpClient.get<AppNotification[]>("/api/notifications", { accessToken });
  },

  getUnreadCount(accessToken: string): Promise<UnreadCount> {
    return httpClient.get<UnreadCount>("/api/notifications/unread-count", { accessToken });
  },

  markRead(notificationId: number, accessToken: string): Promise<AppNotification> {
    return httpClient.patch<AppNotification>(
      `/api/notifications/${notificationId}/read`,
      undefined,
      { accessToken },
    );
  },

  markAllRead(accessToken: string): Promise<void> {
    return httpClient.patch<void>("/api/notifications/read-all", undefined, { accessToken });
  },

  deleteNotification(notificationId: number, accessToken: string): Promise<void> {
    return httpClient.delete<void>(`/api/notifications/${notificationId}`, { accessToken });
  },
};

/**
 * 로그아웃 시 기기 토큰을 해제한다.
 *
 * 세션이 해지되기 "전"에 불려야 한다. 뒤에 부르면 이미 무효해진 토큰으로 요청이 나가
 * 401 로 실패하고, 서버에는 활성 토큰이 남아 로그아웃한 기기로도 알림이 계속 간다.
 * 실패해도 로그아웃 자체를 막지 않는다.
 */
export async function unregisterPushToken(): Promise<void> {
  const token = getRegisteredToken();
  if (!token) {
    return;
  }
  const accessToken = await tokenStorage.getAccessToken();
  if (!accessToken) {
    return;
  }
  try {
    await notificationApi.unregisterDeviceToken(token, accessToken);
    rememberRegisteredToken(null);
  } catch {
    // 서버가 다른 계정의 같은 토큰 등록 시점에도 이전 등록을 내리므로 치명적이지 않다.
  }
}
