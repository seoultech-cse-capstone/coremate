import type { Href } from "expo-router";

import { TUTORIAL_REPORT_ID } from "./tutorialData";

export type OnboardingStep = {
  /** 이 단계를 보여주기 전에 이동할 실제 화면. "back" 은 뒤로 한 칸. 없으면 그 자리에 머문다. */
  navigate?: Href | "back";
  /** 대상이 들어 있는 ScrollView 의 화면 id(registerSpotlightScroll). 있으면 대상을 화면 가운데로 스크롤한다. */
  scroll?: string;
  /** 강조할 spotlightRef id. null 이면 구멍 없이 가운데 안내 카드만 띄운다. */
  target: string | null;
  title: string;
  body: string;
};

// 마지막 단계(랜덤박스)는 안내만 하고 실제로 열지 않는다. 누르면 닉네임 설정으로 넘어간다.
export const ONBOARDING_STEPS: OnboardingStep[] = [
  {
    target: null,
    title: "CoreMate에 오신 걸 환영해요!",
    body: "CS 공부를 하면서 나만의 아바타도 키우는 앱이에요. 주요 기능을 한 바퀴 둘러볼게요.",
  },
  {
    scroll: "home",
    target: "dailyQuiz",
    title: "오늘의 퀴즈",
    body: "매일 6문제를 풀면 출석이 체크되고 코인과 XP를 받아요.",
  },
  {
    scroll: "home",
    target: "coin",
    title: "코인",
    body: "모은 코인으로 랜덤박스를 열거나 AI 약점 분석표를 만들 수 있어요.",
  },
  {
    target: "tab-content",
    title: "문제 풀러 가기",
    body: "문제집 탭에서 과목별 문제를 풀 수 있어요. 눌러볼까요?",
  },
  {
    navigate: "/(tabs)/content",
    scroll: "content",
    target: "problemSet",
    title: "과목별 문제집",
    body: "개념 문제집으로 공부하고, 챕터·과목 모의고사로 실력을 점검해요. 기준 정답률을 넘기면 코인을 받아요.",
  },
  {
    target: "tab-log-weak",
    title: "복습",
    body: "틀린 문제와 약점 분석표는 복습 탭에 모여요.",
  },
  {
    navigate: { pathname: "/(tabs)/log-weak", params: { mode: "weakness" } },
    target: "createReport",
    title: "AI 약점 분석표",
    body: "끝낸 모의고사를 골라 AI 약점 분석표를 만들 수 있어요. 어떻게 생겼는지 볼까요?",
  },
  {
    navigate: { pathname: "/weakness-report", params: { reportId: String(TUTORIAL_REPORT_ID) } },
    scroll: "weakness-report",
    target: "reportPriority",
    title: "내 약점을 한눈에",
    body: "AI가 자주 틀린 개념과 먼저 볼 순서를 정리해줘요.",
  },
  {
    navigate: "back",
    target: "tab-friend",
    title: "친구",
    body: "같이 공부할 친구를 만나러 가볼까요?",
  },
  {
    navigate: "/(tabs)/friend",
    scroll: "friend",
    target: "friendList",
    title: "친구와 함께",
    body: "친구의 출석과 레벨을 확인하고, 콕 찔러 응원하거나 친구 방에 놀러 갈 수 있어요.",
  },
  {
    target: "tab-mypage",
    title: "마이페이지",
    body: "내 기록과 아바타 샵은 마이페이지에 있어요.",
  },
  {
    navigate: "/(tabs)/mypage",
    scroll: "mypage",
    target: "avatarShop",
    title: "아바타 샵",
    body: "모은 코인으로 아바타를 꾸며볼까요?",
  },
  {
    navigate: "/avatar-shop",
    scroll: "avatar-shop",
    target: "randomBox",
    title: "랜덤박스",
    body: "코인 100개로 랜덤박스를 열면 아바타 아이템이나 코인이 나와요. 코인을 모아서 열어보세요!",
  },
];
