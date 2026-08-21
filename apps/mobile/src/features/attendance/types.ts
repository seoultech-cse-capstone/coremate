/** GET /api/attendance/today · POST /api/attendance/{id}/reward 응답. */
export type Attendance = {
  attendanceId: number;
  userId: number;
  /** yyyy-MM-dd */
  attendanceDate: string;
  rewardClaimed: boolean;
  createdAt: string;
};

/** GET /api/attendance/calendar 응답. */
export type AttendanceCalendar = {
  userId: number;
  /** yyyy-MM */
  month: string;
  /** 해당 월에 출석한 날짜들. yyyy-MM-dd */
  attendedDates: string[];
  totalCount: number;
};

/** POST /api/attendance/{attendanceId}/reward 응답. */
export type AttendanceReward = {
  attendanceId: number;
  coinAmount: number;
  xpAmount: number;
  rewardClaimed: boolean;
  /** yyyy-MM-dd */
  rewardDate: string;
};
