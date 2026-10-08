import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { HttpError } from "../../shared/api/httpClient";
import { useAuth } from "../auth/AuthContext";
import { notificationApi } from "./notificationApi";
import type { AppNotification, NotificationCategory, NotificationSetting } from "./types";

const NOTIFICATIONS_QUERY_KEY = ["notifications"];
const UNREAD_COUNT_QUERY_KEY = ["notifications", "unread-count"];
const SETTINGS_QUERY_KEY = ["notifications", "settings"];
const DELETE_MUTATION_KEY = ["notifications", "delete"];

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

/**
 * 알림 한 건 삭제. 스와이프 뒤 삭제 버튼을 누르면 바로 목록에서 빠져야 해서 응답을 기다리지
 * 않고 먼저 지우고, 실패하면 되돌린다. 안 읽은 알림이었다면 홈 배지 숫자도 함께 줄인다.
 */
export function useDeleteNotificationMutation() {
  const { accessToken } = useAuth();
  const queryClient = useQueryClient();

  return useMutation({
    mutationKey: DELETE_MUTATION_KEY,
    mutationFn: async (notificationId: number) => {
      try {
        await notificationApi.deleteNotification(notificationId, accessToken as string);
      } catch (error) {
        // 이미 없는 알림(다른 기기에서 지운 경우 등)이면 지우려던 결과와 같다.
        // 실패로 처리하면 목록에 되살렸다가 재조회로 다시 사라지고 오류 안내까지 뜬다.
        if (error instanceof HttpError && error.status === 404) {
          return;
        }
        throw error;
      }
    },
    onMutate: async (notificationId) => {
      await queryClient.cancelQueries({ queryKey: NOTIFICATIONS_QUERY_KEY, exact: true });
      await queryClient.cancelQueries({ queryKey: UNREAD_COUNT_QUERY_KEY });
      const previousList = queryClient.getQueryData<AppNotification[]>(NOTIFICATIONS_QUERY_KEY);
      const previousUnread = queryClient.getQueryData<number>(UNREAD_COUNT_QUERY_KEY);

      const removed = previousList?.find((item) => item.notificationId === notificationId);
      queryClient.setQueryData<AppNotification[]>(NOTIFICATIONS_QUERY_KEY, (current) =>
        current?.filter((item) => item.notificationId !== notificationId),
      );
      if (removed && !removed.isRead) {
        queryClient.setQueryData<number>(UNREAD_COUNT_QUERY_KEY, (current) =>
          current === undefined ? current : Math.max(0, current - 1),
        );
      }
      return { previousList, previousUnread };
    },
    onError: (_error, _notificationId, context) => {
      if (context?.previousList) {
        queryClient.setQueryData(NOTIFICATIONS_QUERY_KEY, context.previousList);
      }
      if (context?.previousUnread !== undefined) {
        queryClient.setQueryData(UNREAD_COUNT_QUERY_KEY, context.previousUnread);
      }
    },
    onSettled: () => {
      // 여러 건을 연달아 지우는 중이면 마지막 삭제가 끝난 뒤에 한 번만 재조회한다.
      // 중간에 재조회하면 아직 서버에서 안 지워진 항목이 잠깐 되살아난다.
      if (queryClient.isMutating({ mutationKey: DELETE_MUTATION_KEY }) > 1) {
        return;
      }
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
