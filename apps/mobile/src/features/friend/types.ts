/** 백엔드 friend API 응답 타입. 필드는 서버 응답과 1:1로 맞춘다. */

export type FriendStatus = "REQUESTED" | "ACCEPTED" | "REJECTED" | "BLOCKED";

export type AvatarProfile = {
  userId: number;
  avatarGender: string | null;
  baseAvatarType: string | null;
};

export type Friend = {
  friendId: number;
  friendUserId: number;
  nickname: string;
  schoolName: string | null;
  level: number | null;
  status: FriendStatus;
  /** 오늘 출석 여부. 친구 목록을 두 묶음으로 나누는 기준이다. */
  attendedToday: boolean;
  /** 누적 출석 일수. 연속 일수가 아니다 */
  attendanceCount: number;
  avatarProfile: AvatarProfile | null;
};

export type FriendRequest = {
  friendId: number;
  requesterId: number;
  requesterNickname: string;
  receiverId: number;
  receiverNickname: string;
  status: FriendStatus;
  /** RECEIVED = 내가 받은 요청, SENT = 내가 보낸 요청 */
  direction: "RECEIVED" | "SENT";
  createdAt: string;
};

export type FriendSearchUser = {
  userId: number;
  nickname: string;
  schoolName: string | null;
  level: number | null;
  /** 이미 친구이거나 요청 중이면 서버가 상태를 실어 준다 */
  relationshipStatus: FriendStatus | null;
};

export type FriendPoke = {
  pokeId: number;
  fromUserId: number;
  fromNickname: string;
  toUserId: number;
  toNickname: string;
  messageType: string;
  createdAt: string;
};

export type FriendSubjectStat = {
  subjectId: number;
  solvedQuestionCount: number;
  correctQuestionCount: number;
  wrongQuestionCount: number;
  /** 0~1 사이 비율을 문자열로 준다(예: "0.75") */
  achievementRate: string | null;
};

export type FriendProfile = {
  userId: number;
  nickname: string;
  level: number | null;
  schoolName: string | null;
  schoolVerified: boolean;
  status: string;
  attendedToday: boolean;
  attendanceCount: number;
  /** 가장 최근 출석일. 출석 기록이 없으면 null */
  lastAttendanceDate: string | null;
  avatarProfile: AvatarProfile | null;
  subjectStats: FriendSubjectStat[];
};

export type FriendLearningHistory = {
  /** 모의고사 총 풀이 시간(초). 개념 문제집 제외 */
  totalMockSolveTimeSec: number;
  /** 개념 문제집에서 맞힌 서로 다른 문제 수 */
  conceptCorrectQuestionCount: number;
  /** PASS(전 문제 정답)로 통과한 개념 수 */
  completedConceptCount: number;
};
