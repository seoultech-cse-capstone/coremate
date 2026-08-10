/** GET /api/users/me 응답. com.coremate.backend.user.dto.response.MyProfileResponse와 1:1 대응 */
export type MyProfile = {
  userId: number;
  email: string;
  nickname: string;
  role: string;
  level: number;
  totalXp: number;
  schoolName: string | null;
  schoolEmail: string | null;
  schoolVerified: boolean;
  status: string;
  createdAt: string;
  lastLoginAt: string | null;
  /** 오늘 출석(오늘의 퀴즈 완료) 여부 */
  attendedToday: boolean;
  /** 누적 출석 일수 */
  attendanceCount: number;
  /** 가장 최근 출석일. 출석 기록이 없으면 null */
  lastAttendanceDate: string | null;
};
