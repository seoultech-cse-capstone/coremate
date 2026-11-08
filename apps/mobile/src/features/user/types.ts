/** GET /api/users/me 응답. com.coremate.backend.user.dto.response.MyProfileResponse와 1:1 대응 */
export type MyProfile = {
  userId: number;
  email: string;
  /** 가입할 때 받은 이름. 소셜 로그인에서 받지 못했으면 null. 이 필드가 생기기 전 서버에는 없다 */
  name?: string | null;
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
  /** 친구에게 학습 기록(과목별 스탯·Learning History) 공개 여부. 이 필드가 생기기 전 서버에는 없다(= 공개) */
  learningPublic?: boolean;
  /** 비밀번호가 있는 계정인지. 소셜 로그인으로만 가입했으면 false. 이 필드가 생기기 전 서버에는 없다 */
  hasPassword?: boolean;
};

/** PUT /api/users/me 요청. UpdateUserProfileRequest와 1:1 대응 */
export type UpdateMyProfileRequest = {
  nickname?: string;
  schoolName?: string;
  schoolEmail?: string;
};

/** PATCH /api/users/me/nickname 요청. UpdateNicknameRequest와 1:1 대응 (최대 6글자) */
export type UpdateNicknameRequest = {
  nickname: string;
};

/** PATCH /api/users/me/nickname 응답. UserSummaryResponse와 1:1 대응 */
export type UserSummary = {
  userId: number;
  nickname: string;
  level: number;
  avatarImageUrl: string | null;
};

/** PATCH /api/users/me/privacy 요청. UpdatePrivacyRequest와 1:1 대응 */
export type UpdatePrivacyRequest = {
  learningPublic: boolean;
};

/** GET /api/users/me/subject-stats 항목. UserSubjectStatResponse와 1:1 대응 */
export type UserSubjectStat = {
  subjectId: number;
  solvedQuestionCount: number;
  correctQuestionCount: number;
  wrongQuestionCount: number;
  /** 과목별 학습 달성률(%). 0~100 실수다. */
  achievementRate: number;
};
