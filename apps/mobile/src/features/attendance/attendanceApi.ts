import { httpClient } from "../../shared/api/httpClient";
import type { Attendance, AttendanceCalendar, AttendanceReward } from "./types";

/**
 * 출석 REST 호출. AttendanceController는 ApiResponse로 감싸지 않고 DTO를 그대로 반환한다.
 *
 * 출석 기록 자체는 오늘의 CS 퀴즈를 제출할 때 백엔드가 자동으로 만든다.
 * 별도의 "출석 체크" 동작은 없다(POST /checkin 은 오늘 출석 조회와 같은 결과를 준다).
 */
export const attendanceApi = {
  /** 오늘 출석 기록. 아직 출석 전이면 백엔드가 ATTENDANCE_NOT_FOUND 로 응답한다. */
  getToday(accessToken: string): Promise<Attendance> {
    return httpClient.get<Attendance>("/api/attendance/today", { accessToken });
  },

  /** month 생략 시 백엔드가 이번 달로 처리한다. yyyy-MM */
  getCalendar(accessToken: string, month?: string): Promise<AttendanceCalendar> {
    const query = month ? `?month=${month}` : "";
    return httpClient.get<AttendanceCalendar>(`/api/attendance/calendar${query}`, {
      accessToken,
    });
  },

  /** 출석은 있는데 보상만 미지급인 경우의 수동 재수령. */
  claimReward(accessToken: string, attendanceId: number): Promise<AttendanceReward> {
    return httpClient.post<AttendanceReward>(
      `/api/attendance/${attendanceId}/reward`,
      undefined,
      { accessToken },
    );
  },
};
