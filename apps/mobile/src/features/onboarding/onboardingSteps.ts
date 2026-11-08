import type { MockTab } from "./OnboardingMockScreens";

export type OnboardingScreen = MockTab | "report" | "shop";

export type OnboardingStep = {
  screen: OnboardingScreen;
  /** 강조할 SpotlightTarget id. null 이면 구멍 없이 가운데 안내 카드만 띄운다. */
  target: string | null;
  title: string;
  body: string;
};

// 마지막 단계(랜덤박스)를 누르면 연출을 보여주고 닉네임 설정으로 넘어간다.
export const ONBOARDING_STEPS: OnboardingStep[] = [
  {
    screen: "home",
    target: null,
    title: "CoreMate에 오신 걸 환영해요!",
    body: "CS 공부를 하면서 나만의 아바타도 키우는 앱이에요. 주요 기능을 한 바퀴 둘러볼게요.",
  },
  {
    screen: "home",
    target: "dailyQuiz",
    title: "오늘의 퀴즈",
    body: "매일 6문제를 풀면 출석이 체크되고 코인과 XP를 받아요.",
  },
  {
    screen: "home",
    target: "coin",
    title: "코인",
    body: "모은 코인으로 랜덤박스를 열거나 AI 약점 분석표를 만들 수 있어요.",
  },
  {
    screen: "home",
    target: "tab-content",
    title: "문제 풀러 가기",
    body: "문제집 탭에서 과목별 문제를 풀 수 있어요. 눌러볼까요?",
  },
  {
    screen: "content",
    target: "problemSet",
    title: "과목별 문제집",
    body: "개념 문제집으로 공부하고, 챕터·과목 모의고사로 실력을 점검해요. 기준 정답률을 넘기면 코인을 받아요.",
  },
  {
    screen: "content",
    target: "tab-log-weak",
    title: "복습",
    body: "틀린 문제와 약점 분석표는 복습 탭에 모여요.",
  },
  {
    screen: "log-weak",
    target: "createReport",
    title: "AI 약점 분석표",
    body: "끝낸 모의고사를 골라 AI 약점 분석표를 만들 수 있어요. 어떻게 생겼는지 볼까요?",
  },
  {
    screen: "report",
    target: "reportSummary",
    title: "내 약점을 한눈에",
    body: "AI가 자주 틀린 개념과 다시 볼 내용을 정리해줘요.",
  },
  {
    screen: "log-weak",
    target: "tab-friend",
    title: "친구",
    body: "같이 공부할 친구를 만나러 가볼까요?",
  },
  {
    screen: "friend",
    target: "friendCard",
    title: "친구와 함께",
    body: "친구의 출석과 레벨을 확인하고, 콕 찔러 응원하거나 친구 방에 놀러 갈 수 있어요.",
  },
  {
    screen: "friend",
    target: "tab-mypage",
    title: "마이페이지",
    body: "내 기록과 아바타 샵은 마이페이지에 있어요.",
  },
  {
    screen: "mypage",
    target: "avatarShop",
    title: "아바타 샵",
    body: "모은 코인으로 아바타를 꾸며볼까요?",
  },
  {
    screen: "shop",
    target: "randomBox",
    title: "가입 선물 150코인!",
    body: "랜덤박스를 열면 아바타 아이템이나 코인이 나와요. 한번 열어볼까요?",
  },
];
