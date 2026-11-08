import type { QueryClient, QueryKey } from "@tanstack/react-query";

import type { Friend, FriendRequest } from "../friend/types";
import type {
  WeaknessAnalysisReportDetail,
  WeaknessAnalysisReportSummary,
  WeaknessConcept,
} from "../weakness/types";

// 튜토리얼 동안만 실제 화면에 보여줄 더미 데이터. 새로 가입한 회원은 친구·분석표가 비어 있어
// 화면이 무엇을 하는 곳인지 보이지 않으므로 이 값들로 채운다. 서버에는 저장하지 않는다.

/** 더미 분석표 id. 실제 id 와 겹치지 않게 음수를 쓴다. */
export const TUTORIAL_REPORT_ID = -1;

// 이 키로 시작하는 쿼리는 튜토리얼 동안 다시 불러오지 않는다(더미가 실제 응답으로 덮이지 않게).
const TUTORIAL_QUERY_KEYS: QueryKey[] = [["friends"], ["weakness"]];
// queryClient.ts 의 기본 staleTime 과 같은 값. 튜토리얼이 끝나면 이 값으로 되돌린다.
const DEFAULT_STALE_TIME = 30_000;

const NOW = new Date().toISOString();

const friend = (id: number, nickname: string, level: number, attendedToday: boolean, attendanceCount: number): Friend => ({
  friendId: -id,
  friendUserId: -id,
  nickname,
  schoolName: null,
  level,
  status: "ACCEPTED",
  attendedToday,
  attendanceCount,
  avatarProfile: null,
});

const DUMMY_FRIENDS: Friend[] = [
  friend(1, "코어짱", 7, true, 21),
  friend(2, "알고왕", 4, true, 12),
  friend(3, "디비러버", 2, false, 5),
];

const DUMMY_REQUESTS: FriendRequest[] = [
  {
    friendId: -10,
    requesterId: -10,
    requesterNickname: "네트워커",
    receiverId: 0,
    receiverNickname: "",
    status: "REQUESTED",
    direction: "RECEIVED",
    createdAt: NOW,
  },
];

const DUMMY_REPORT_SUMMARY: WeaknessAnalysisReportSummary = {
  analysisReportId: TUTORIAL_REPORT_ID,
  title: "운영체제 약점 분석표",
  analysisScopeType: "SUBJECT",
  analysisScopeNameSnapshot: "운영체제",
  status: "COMPLETED",
  selectedSessionCount: 2,
  totalQuestionCount: 40,
  totalWrongCount: 11,
  weaknessConceptCount: 2,
  coinCost: 100,
  generatedAt: NOW,
  pdfAvailable: false,
};

const concept = (
  rank: number,
  conceptName: string,
  diagnosis: string,
  likelyCause: string,
  wrongCount: number,
): WeaknessConcept => ({
  analysisConceptId: -rank,
  conceptId: -rank,
  conceptName,
  weaknessScore: 90 - rank * 15,
  evidenceStrength: 3,
  finalType: "CONCEPT_GAP",
  finalTypeSource: "AI",
  secondarySignals: [],
  causeCode: "CONCEPT_GAP",
  analysisStatus: "CONFIRMED",
  confusedRelation: null,
  diagnosis,
  likelyCause,
  questionCount: 8,
  wrongCount,
  longTimeCount: 1,
  hintUsedCount: 0,
  repeatedMisconceptions: [],
  relatedConcepts: [],
  evidenceSummary: `8문제 중 ${wrongCount}문제를 틀렸어요.`,
  detailedExplanationText: null,
  conceptTip: null,
  conceptLearning: null,
  evidenceLogs: [],
});

const DUMMY_REPORT_DETAIL: WeaknessAnalysisReportDetail = {
  analysisReportId: TUTORIAL_REPORT_ID,
  userId: 0,
  title: DUMMY_REPORT_SUMMARY.title,
  analysisScopeType: "SUBJECT",
  analysisScopeNameSnapshot: "운영체제",
  summaryText: "교착상태의 발생 조건을 헷갈려 같은 유형을 반복해서 틀렸어요. 이 개념부터 다시 보면 좋아요.",
  analysisText: "",
  reportFormat: "JSON",
  selectedSessionCount: 2,
  totalQuestionCount: 40,
  totalWrongCount: 11,
  totalLongTimeCount: 3,
  totalHintUsedCount: 1,
  coinCost: 100,
  status: "COMPLETED",
  requestedAt: NOW,
  generatedAt: NOW,
  selectedSessions: [],
  weaknessConcepts: [
    concept(
      1,
      "교착상태(Deadlock)",
      "발생 조건 4가지 중 하나만 깨면 예방된다는 점을 놓치고 있어요.",
      "상호 배제와 점유 대기를 같은 조건으로 보고 있어요.",
      5,
    ),
    concept(
      2,
      "페이지 교체 알고리즘",
      "LRU 와 FIFO 의 교체 순서를 바꿔 계산하는 실수가 있어요.",
      "참조 시점이 아니라 적재 시점으로 순서를 매기고 있어요.",
      3,
    ),
  ],
};

/** 튜토리얼 시작. 대상 쿼리를 멈추고 더미로 채운다. */
export async function seedTutorialData(queryClient: QueryClient) {
  for (const queryKey of TUTORIAL_QUERY_KEYS) {
    await queryClient.cancelQueries({ queryKey });
    queryClient.setQueryDefaults(queryKey, { staleTime: Infinity });
  }
  queryClient.setQueryData(["friends"], DUMMY_FRIENDS);
  queryClient.setQueryData(["friends", "requests"], DUMMY_REQUESTS);
  queryClient.setQueryData(["weakness", "reports"], [DUMMY_REPORT_SUMMARY]);
  queryClient.setQueryData(["weakness", "report-detail", TUTORIAL_REPORT_ID], DUMMY_REPORT_DETAIL);
}

/** 튜토리얼 끝. 더미를 버리고 실제 데이터를 다시 불러오게 한다. */
export function restoreTutorialData(queryClient: QueryClient) {
  queryClient.removeQueries({ queryKey: ["weakness", "report-detail", TUTORIAL_REPORT_ID] });
  for (const queryKey of TUTORIAL_QUERY_KEYS) {
    queryClient.setQueryDefaults(queryKey, { staleTime: DEFAULT_STALE_TIME });
    queryClient.resetQueries({ queryKey }).catch((error) => {
      console.error("[onboarding] 튜토리얼 뒤 실제 데이터 다시 불러오기 실패", error);
    });
  }
}
