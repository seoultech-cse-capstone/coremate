/** 백엔드 friend API 응답 타입. 필드는 서버 응답과 1:1로 맞춘다. */

// 아바타·방은 avatar 도메인 응답(AvatarProfileResponse·RoomResponse)을 그대로 실어 온다.
import type { AvatarEquippedItem, AvatarProfile, RoomResponse } from "../avatar";

export type FriendStatus = "REQUESTED" | "ACCEPTED" | "REJECTED" | "BLOCKED";

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
  /** 요청을 보낸 사람이 지금 장착한 아이템(받은 요청 아이콘용). 이 필드가 생기기 전 서버에는 없다 */
  requesterEquippedItems?: AvatarEquippedItem[];
};

export type FriendSearchUser = {
  userId: number;
  nickname: string;
  schoolName: string | null;
  level: number | null;
  /** 검색한 사람 기준 관계 상태. 내가 보낸 요청이 거절됐어도 REQUESTED 로 온다(거절은 알리지 않는다) */
  relationshipStatus: FriendStatus | null;
  /** 요청을 누가 보냈는지. SENT = 내가 보냄, RECEIVED = 상대가 보냄. 관계가 없으면 null */
  direction: "SENT" | "RECEIVED" | null;
  /** 지금 장착한 아이템(목록 아이콘용). 이 필드가 생기기 전 서버에는 없다 */
  equippedItems?: AvatarEquippedItem[];
};

/** GET /api/friends/blocks. 내가 차단한 사용자(최근에 차단한 순서) */
export type BlockedUser = {
  userId: number;
  nickname: string;
  blockedAt: string;
  /** 지금 장착한 아이템(목록 아이콘용) */
  equippedItems: AvatarEquippedItem[];
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
  /** 과목별 학습 달성률(%). 0~100 실수다. */
  achievementRate: number | null;
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
  /** 친구의 방(벽지·가구 배치·마지막 포즈). 읽기 전용이다 */
  room: RoomResponse;
  /** 친구가 학습 기록을 공개하지 않으면 빈 목록이다 */
  subjectStats: FriendSubjectStat[];
  /** 친구가 학습 기록(과목별 스탯·Learning History)을 공개했는지. 이 필드가 생기기 전 서버에는 없다(= 공개) */
  learningPublic?: boolean;
};

export type FriendLearningHistory = {
  /** 모의고사 총 풀이 시간(초). 개념 문제집 제외 */
  totalMockSolveTimeSec: number;
  /** 개념 문제집에서 맞힌 서로 다른 문제 수 */
  conceptCorrectQuestionCount: number;
  /** PASS(전 문제 정답)로 통과한 개념 수 */
  passedConceptCount: number;
};
