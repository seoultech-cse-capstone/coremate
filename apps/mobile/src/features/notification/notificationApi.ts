import { httpClient } from "../../shared/api/httpClient";
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
};
