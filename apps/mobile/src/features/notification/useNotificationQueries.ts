import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { useAuth } from "../auth/AuthContext";
import { notificationApi } from "./notificationApi";
import type { AppNotification, NotificationCategory, NotificationSetting } from "./types";

const NOTIFICATIONS_QUERY_KEY = ["notifications"];
const UNREAD_COUNT_QUERY_KEY = ["notifications", "unread-count"];
const SETTINGS_QUERY_KEY = ["notifications", "settings"];

export function useNotificationsQuery() {
  const { accessToken } = useAuth();

  return useQuery<AppNotification[]>({
    queryKey: NOTIFICATIONS_QUERY_KEY,
    queryFn: () => notificationApi.getNotifications(accessToken as string),
    enabled: Boolean(accessToken),
  });
}

/** 홈 화면 배지용. 화면에 들어올 때마다 새로 읽는다. */
export function useUnreadCountQuery() {
  const { accessToken } = useAuth();

  return useQuery<number>({
    queryKey: UNREAD_COUNT_QUERY_KEY,
    queryFn: async () => (await notificationApi.getUnreadCount(accessToken as string)).unreadCount,
    enabled: Boolean(accessToken),
  });
}

export function useMarkNotificationReadMutation() {
  const { accessToken } = useAuth();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (notificationId: number) =>
      notificationApi.markRead(notificationId, accessToken as string),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: NOTIFICATIONS_QUERY_KEY });
      queryClient.invalidateQueries({ queryKey: UNREAD_COUNT_QUERY_KEY });
    },
  });
}

export function useMarkAllNotificationsReadMutation() {
  const { accessToken } = useAuth();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: () => notificationApi.markAllRead(accessToken as string),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: NOTIFICATIONS_QUERY_KEY });
      queryClient.invalidateQueries({ queryKey: UNREAD_COUNT_QUERY_KEY });
    },
  });
}

export function useNotificationSettingsQuery() {
  const { accessToken } = useAuth();

  return useQuery<NotificationSetting[]>({
    queryKey: SETTINGS_QUERY_KEY,
    queryFn: () => notificationApi.getSettings(accessToken as string),
    enabled: Boolean(accessToken),
  });
}

/**
 * 수신 설정 변경. 스위치는 즉시 반응해야 해서 응답을 기다리지 않고 먼저 바꾸고,
 * 실패하면 되돌린다.
 */
export function useUpdateNotificationSettingMutation() {
  const { accessToken } = useAuth();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (variables: { category: NotificationCategory; enabled: boolean }) =>
      notificationApi.updateSetting(variables, accessToken as string),
    onMutate: async (variables) => {
      await queryClient.cancelQueries({ queryKey: SETTINGS_QUERY_KEY });
      const previous = queryClient.getQueryData<NotificationSetting[]>(SETTINGS_QUERY_KEY);
      queryClient.setQueryData<NotificationSetting[]>(SETTINGS_QUERY_KEY, (current) =>
        (current ?? []).map((setting) =>
          setting.category === variables.category
            ? { ...setting, enabled: variables.enabled }
            : setting,
        ),
      );
      return { previous };
    },
    onError: (_error, _variables, context) => {
      if (context?.previous) {
        queryClient.setQueryData(SETTINGS_QUERY_KEY, context.previous);
      }
    },
    onSettled: () => queryClient.invalidateQueries({ queryKey: SETTINGS_QUERY_KEY }),
  });
}
