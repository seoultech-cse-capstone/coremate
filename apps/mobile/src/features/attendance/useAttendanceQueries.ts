import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { attendanceApi } from "./attendanceApi";

/**
 * 오늘 출석. 아직 출석 전이면 백엔드가 ATTENDANCE_NOT_FOUND 를 주므로 이 쿼리는 실패 상태가 된다.
 * 화면은 그것을 오류가 아니라 "오늘 아직 출석 전"으로 해석한다. 재시도는 의미가 없어 끈다.
 */
export function useTodayAttendance(accessToken: string | null) {
  return useQuery({
    queryKey: ["attendance", "today"],
    queryFn: () => attendanceApi.getToday(accessToken as string),
    enabled: accessToken !== null,
    retry: false,
  });
}

export function useAttendanceCalendar(accessToken: string | null, month: string) {
  return useQuery({
    queryKey: ["attendance", "calendar", month],
    queryFn: () => attendanceApi.getCalendar(accessToken as string, month),
    enabled: accessToken !== null,
  });
}

export function useClaimAttendanceReward(accessToken: string | null) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (attendanceId: number) =>
      attendanceApi.claimReward(accessToken as string, attendanceId),
    onSuccess: () => {
      // 보상을 받으면 출석의 rewardClaimed 와 보유 코인·XP 가 함께 바뀐다.
      queryClient.invalidateQueries({ queryKey: ["attendance"] });
      queryClient.invalidateQueries({ queryKey: ["currency"] });
      queryClient.invalidateQueries({ queryKey: ["reward"] });
      queryClient.invalidateQueries({ queryKey: ["user", "me"] });
    },
  });
}
