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

/** PUT /api/users/me 요청. UpdateUserProfileRequest와 1:1 대응 */
export type UpdateMyProfileRequest = {
  nickname?: string;
  schoolName?: string;
  schoolEmail?: string;
};

/** PATCH /api/users/me/nickname 응답. UserSummaryResponse와 1:1 대응 */
export type UpdateNicknameResult = {
  userId: number;
  nickname: string;
  level: number;
  avatarImageUrl: string | null;
};

/** GET /api/users/me/subject-stats 항목. UserSubjectStatResponse와 1:1 대응 */
export type UserSubjectStat = {
  subjectId: number;
  solvedQuestionCount: number;
  correctQuestionCount: number;
  wrongQuestionCount: number;
  /** "0.75"처럼 0~1 사이 소수를 문자열로 내려준다. */
  achievementRate: string;
};
