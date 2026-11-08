// 온보딩 튜토리얼 전용 정적 화면. 실제 화면의 생김새만 따라 그리고 서버에는 아무 요청도 하지 않는다.
// 사용자 본인 정보(닉네임·코인·아바타)만 실제 값을 쓰고, 나머지 목록은 모두 더미다.
// 실제 탭 화면이 크게 바뀌면 여기도 손으로 맞춰야 한다.
import { Image } from "expo-image";
import { LinearGradient } from "expo-linear-gradient";
import {
  BookOpen,
  BookOpenCheck,
  Bookmark,
  Brain,
  CalendarCheck,
  ChartNoAxesCombined,
  Coins,
  Cpu,
  Gift,
  Home,
  PackageOpen,
  Search,
  Settings,
  ShoppingBag,
  Sparkles,
  UserPlus,
  UserRound,
  Users,
} from "lucide-react-native";
import type { ReactNode } from "react";
import { StyleSheet, Text, useWindowDimensions, View, type ImageSourcePropType } from "react-native";
import { useSafeAreaInsets } from "react-native-safe-area-context";

import { FONT } from "../../../constants/fonts";
import { useAuth } from "../auth";
import { RoomScene } from "../avatar";
import { ProblemSetCard } from "../content/components/ProblemSetCard";
import { useMyCurrency } from "../currency";
import { FriendListCard } from "../friend/components/FriendListCard";
import type { Friend } from "../friend/types";
import { MyProfile } from "../mypage/components/MyProfile";
import { CHEST_CONTENT_OFFSET_RATIO, CLOSED_CHEST_IMAGE } from "../shop";
import { useMyProfile } from "../user";
import { SpotlightTarget } from "./SpotlightTarget";

const BRAND_PURPLE = "#6D5DFB";
const PANEL_GRAY = "#F3F3F3";
const TAB_GRAY = "#94a3b8";
const ROOM_OVERLAP = 28;
const NICKNAME_TICKET_IMAGE = require("../../../assets/shop/nickname-change/ticket.png");

const noop = () => {};

export type MockTab = "home" | "friend" | "content" | "log-weak" | "mypage";

// ---------- 공통 ----------

function MockHeader({ title, right }: { title: string; right?: ReactNode }) {
  const insets = useSafeAreaInsets();
  return (
    <View style={[styles.header, { minHeight: 60 + insets.top, paddingTop: insets.top }]}>
      <Text style={styles.headerTitle}>{title}</Text>
      {right ? <View style={styles.headerRight}>{right}</View> : null}
    </View>
  );
}

function MockPanel({ children }: { children: ReactNode }) {
  return <View style={styles.panel}>{children}</View>;
}

const TABS: { key: MockTab; label: string; Icon: typeof Home }[] = [
  { key: "home", label: "홈", Icon: Home },
  { key: "friend", label: "친구", Icon: Users },
  { key: "content", label: "문제집", Icon: BookOpenCheck },
  { key: "log-weak", label: "복습", Icon: ChartNoAxesCombined },
  { key: "mypage", label: "마이페이지", Icon: UserRound },
];

/** 실제 하단 탭바와 같은 모양. 각 탭은 "tab-{key}" 로 강조할 수 있다. */
export function MockTabBar({ active }: { active: MockTab }) {
  const insets = useSafeAreaInsets();
  return (
    <View style={[styles.tabBar, { height: 58 + insets.bottom, paddingBottom: insets.bottom }]}>
      {TABS.map(({ key, label, Icon }) => {
        const color = key === active ? BRAND_PURPLE : TAB_GRAY;
        return (
          <SpotlightTarget key={key} id={`tab-${key}`} style={styles.tabItem}>
            <Icon color={color} size={22} strokeWidth={2.1} />
            <Text style={[styles.tabLabel, { color }]}>{label}</Text>
          </SpotlightTarget>
        );
      })}
    </View>
  );
}

// ---------- 홈 ----------

export function MockHomeScreen() {
  const { height } = useWindowDimensions();
  const { accessToken } = useAuth();
  const { data: profile } = useMyProfile(accessToken);
  const { data: currency } = useMyCurrency(accessToken);
  // 실제 홈(60%)보다 방을 낮춰야 작은 화면에서도 퀴즈 버튼이 탭바에 가리지 않는다.
  const roomHeight = Math.round(height * 0.46);

  return (
    <View style={styles.purpleRoot}>
      <View style={{ height: roomHeight }}>
        <View style={{ height: roomHeight + ROOM_OVERLAP }}>
          <RoomScene bottomOverflow={ROOM_OVERLAP} height={roomHeight} showGridLines={false} />
        </View>
      </View>
      <View style={[styles.homePanel]}>
        <View style={styles.identityRow}>
          <Text numberOfLines={1} style={styles.nicknameText}>
            {profile?.nickname ?? ""}
          </Text>
          <SpotlightTarget id="coin">
            <View style={styles.coinPill}>
              <Coins color="#B45309" size={13} strokeWidth={2.2} />
              <Text style={styles.coinText}>{currency?.coin ?? 0}</Text>
            </View>
          </SpotlightTarget>
        </View>
        <View style={styles.levelBarTrack}>
          <LinearGradient
            colors={["#6D5DFB", "#2EE6A6"]}
            start={{ x: 0, y: 0.5 }}
            end={{ x: 1, y: 0.5 }}
            style={[styles.levelBarFill, { width: "8%" }]}
          />
          <Text style={styles.levelText}>Lv.1</Text>
        </View>
        <SpotlightTarget id="dailyQuiz">
          <View style={styles.primaryButton}>
            <Text style={styles.primaryButtonText}>오늘의 퀴즈</Text>
          </View>
        </SpotlightTarget>
        <View style={styles.secondaryRow}>
          <View style={styles.secondaryButton}>
            <Text style={styles.secondaryButtonText}>연습문제 풀이</Text>
          </View>
          <View style={[styles.secondaryButton, styles.darkButton]}>
            <Text style={[styles.secondaryButtonText, styles.whiteText]}>약점 분석하기</Text>
          </View>
        </View>
      </View>
    </View>
  );
}

// ---------- 문제집 ----------

export function MockContentScreen() {
  return (
    <View style={styles.purpleRoot}>
      <MockHeader title="문제집" />
      <MockPanel>
        <View style={styles.contentList}>
          <SpotlightTarget id="problemSet">
            <ProblemSetCard
              chapterCount={6}
              countUnit="chapter"
              hasSolved
              Icon={BookOpen}
              iconColor="#2563FF"
              progress={40}
              startButtonLabel="풀기 시작"
              thumbnailColor="#E4EEFF"
              title="자료구조"
              onOpen={noop}
              onReview={noop}
              onStart={noop}
            />
          </SpotlightTarget>
          <ProblemSetCard
            chapterCount={7}
            countUnit="chapter"
            hasSolved={false}
            Icon={Brain}
            iconColor="#A855F7"
            progress={0}
            startButtonLabel="풀기 시작"
            thumbnailColor="#F2DEFF"
            title="알고리즘"
            onOpen={noop}
            onReview={noop}
            onStart={noop}
          />
        </View>
      </MockPanel>
    </View>
  );
}

// ---------- 복습(약점 분석표 목록) ----------

const DUMMY_REPORTS = [
  { subject: "운영체제", detail: "과목 모의고사 2회 · 약점 3개", date: "11.03" },
  { subject: "자료구조", detail: "챕터 모의고사 1회 · 약점 2개", date: "10.28" },
];

export function MockLogWeakScreen() {
  return (
    <View style={styles.purpleRoot}>
      <MockHeader title="약점 분석" />
      <MockPanel>
        <View style={styles.panelPadding}>
          <View style={styles.statsRow}>
            {[
              { value: "2", label: "내 약점 분석표" },
              { value: "40", label: "분석한 문제" },
              { value: "5", label: "발견한 약점" },
            ].map((stat) => (
              <View key={stat.label} style={styles.statBox}>
                <Text style={styles.statValue}>{stat.value}</Text>
                <Text style={styles.statLabel}>{stat.label}</Text>
              </View>
            ))}
          </View>
          {DUMMY_REPORTS.map((report) => (
            <View key={report.subject} style={styles.reportCard}>
              <View style={styles.reportIcon}>
                <Cpu color="#16C45B" size={20} strokeWidth={2.3} />
              </View>
              <View style={styles.flex}>
                <Text style={styles.reportTitle}>{report.subject} 약점 분석표</Text>
                <Text style={styles.reportDetail}>{report.detail}</Text>
              </View>
              <Text style={styles.reportDate}>{report.date}</Text>
            </View>
          ))}
          <SpotlightTarget id="createReport">
            <View style={styles.aiButton}>
              <Sparkles color="#FFFFFF" size={16} strokeWidth={2.4} />
              <Text style={styles.aiButtonText}>새 약점 분석표 만들기</Text>
            </View>
          </SpotlightTarget>
        </View>
      </MockPanel>
    </View>
  );
}

// ---------- 약점 분석표(더미 결과) ----------

const DUMMY_WEAK_CONCEPTS = [
  { name: "교착상태(Deadlock)", rate: 30 },
  { name: "페이지 교체 알고리즘", rate: 45 },
  { name: "프로세스 동기화", rate: 55 },
];

export function MockWeaknessReportScreen() {
  const insets = useSafeAreaInsets();
  return (
    <View style={[styles.grayRoot, { paddingTop: insets.top + 12 }]}>
      <Text style={styles.reportScreenTitle}>운영체제 약점 분석표</Text>
      <Text style={styles.reportScreenSub}>과목 모의고사 2회 · 40문제 분석</Text>
      <SpotlightTarget id="reportSummary" style={styles.reportSummaryCard}>
        <Text style={styles.summaryEyebrow}>가장 약한 개념</Text>
        <Text style={styles.summaryTitle}>교착상태(Deadlock)</Text>
        <Text style={styles.summaryBody}>
          발생 조건 4가지를 헷갈려 같은 유형을 3번 틀렸어요. 조건 중 하나만 깨면 예방된다는 점부터 다시 보면
          좋아요.
        </Text>
      </SpotlightTarget>
      <View style={styles.conceptCard}>
        <Text style={styles.conceptCardTitle}>개념별 정답률</Text>
        {DUMMY_WEAK_CONCEPTS.map((concept) => (
          <View key={concept.name} style={styles.conceptRow}>
            <Text style={styles.conceptName}>{concept.name}</Text>
            <View style={styles.conceptTrack}>
              <View style={[styles.conceptFill, { width: `${concept.rate}%` }]} />
            </View>
            <Text style={styles.conceptRate}>{concept.rate}%</Text>
          </View>
        ))}
      </View>
    </View>
  );
}

// ---------- 친구 ----------

const DUMMY_FRIENDS: Friend[] = [
  { name: "코어짱", level: 7, attended: true, count: 21 },
  { name: "알고왕", level: 4, attended: true, count: 12 },
  { name: "디비러버", level: 2, attended: false, count: 5 },
].map((friend, index) => ({
  friendId: -(index + 1),
  friendUserId: -(index + 1),
  nickname: friend.name,
  schoolName: null,
  level: friend.level,
  status: "ACCEPTED" as const,
  attendedToday: friend.attended,
  attendanceCount: friend.count,
  avatarProfile: null,
}));

export function MockFriendScreen() {
  return (
    <View style={styles.purpleRoot}>
      <MockHeader
        title="Friends"
        right={
          <>
            <Search color="#FFFFFF" size={22} strokeWidth={2.1} />
            <UserPlus color="#FFFFFF" size={22} strokeWidth={2.1} />
          </>
        }
      />
      <MockPanel>
        <View style={styles.panelPadding}>
          <View style={styles.receivedPill}>
            <View style={styles.requestDot} />
            <Text style={styles.receivedText}>요청 온 친구</Text>
            <View style={styles.requestCount}>
              <Text style={styles.requestCountText}>1</Text>
            </View>
          </View>
          {DUMMY_FRIENDS.map((friend, index) => {
            const card = (
              <FriendListCard
                friend={friend}
                isPoked={index === 1}
                isPokeDisabled={false}
                onPoke={noop}
                onPress={noop}
              />
            );
            return index === 0 ? (
              <SpotlightTarget key={friend.friendId} id="friendCard">
                {card}
              </SpotlightTarget>
            ) : (
              <View key={friend.friendId}>{card}</View>
            );
          })}
        </View>
      </MockPanel>
    </View>
  );
}

// ---------- 마이페이지 ----------

const MYPAGE_ACTIONS = [
  { label: "출석 현황", Icon: CalendarCheck },
  { label: "보상 내역", Icon: Gift },
  { label: "아바타 샵", Icon: ShoppingBag },
  { label: "인벤토리", Icon: PackageOpen },
  { label: "즐겨찾기 문제 목록", Icon: Bookmark },
];

export function MockMyPageScreen() {
  return (
    <View style={styles.purpleRoot}>
      <MockHeader title="My Page" right={<Settings color="#ffffff" size={25} strokeWidth={2.2} />} />
      <View style={styles.profileArea}>
        <MyProfile />
      </View>
      <MockPanel>
        <View style={[styles.panelPadding, styles.actionList]}>
          {MYPAGE_ACTIONS.map(({ label, Icon }) => {
            const row = (
              <View style={styles.actionButton}>
                <View style={styles.actionIconBox}>
                  <Icon color={BRAND_PURPLE} size={20} strokeWidth={2.2} />
                </View>
                <Text style={styles.actionText}>{label}</Text>
              </View>
            );
            return label === "아바타 샵" ? (
              <SpotlightTarget key={label} id="avatarShop">
                {row}
              </SpotlightTarget>
            ) : (
              <View key={label}>{row}</View>
            );
          })}
        </View>
      </MockPanel>
    </View>
  );
}

// ---------- 아바타 샵 ----------

export function MockShopScreen() {
  const insets = useSafeAreaInsets();
  const { accessToken } = useAuth();
  const { data: currency } = useMyCurrency(accessToken);

  return (
    <View style={[styles.shopRoot, { paddingTop: insets.top + 8 }]}>
      <View style={styles.shopHeader}>
        <Text style={styles.shopTitle}>아바타 샵</Text>
        <View style={styles.coinPill}>
          <Coins color="#B45309" size={13} strokeWidth={2.2} />
          <Text style={styles.coinText}>{currency?.coin ?? 0}</Text>
        </View>
      </View>
      <View style={styles.shopList}>
        <SpotlightTarget id="randomBox">
          <ShopCard
            gradient={["#7C3AED", "#9333EA", "#6D28D9"]}
            badge="랜덤박스"
            name="랜덤박스"
            desc="아이템 또는 코인을 뽑아보세요"
            cta="100 코인으로 열기"
            image={CLOSED_CHEST_IMAGE}
            imageOffset={150 * CHEST_CONTENT_OFFSET_RATIO}
          />
        </SpotlightTarget>
        <ShopCard
          gradient={["#0EA5E9", "#0891B2", "#0E7490"]}
          badge="닉네임 변경권"
          name="닉네임 변경권"
          desc="원하는 닉네임으로 바꿔보세요"
          cta="코인으로 구매"
          image={NICKNAME_TICKET_IMAGE}
          imageOffset={0}
        />
      </View>
    </View>
  );
}

function ShopCard(props: {
  gradient: readonly [string, string, string];
  badge: string;
  name: string;
  desc: string;
  cta: string;
  image: ImageSourcePropType;
  imageOffset: number;
}) {
  return (
    <View style={styles.featuredWrap}>
      <LinearGradient colors={props.gradient} start={{ x: 0, y: 0 }} end={{ x: 1, y: 1 }} style={styles.featuredCard}>
        <View style={styles.featuredBadge}>
          <Sparkles color="#FDE68A" size={12} strokeWidth={2.6} />
          <Text style={styles.featuredBadgeText}>{props.badge}</Text>
        </View>
        <Image
          source={props.image}
          style={[styles.featuredImage, { transform: [{ translateX: props.imageOffset }] }]}
          contentFit="contain"
        />
        <Text style={styles.featuredName}>{props.name}</Text>
        <Text style={styles.featuredDesc}>{props.desc}</Text>
        <View style={styles.featuredCta}>
          <Coins color="#7C2D12" size={16} strokeWidth={2.6} />
          <Text style={styles.featuredCtaText}>{props.cta}</Text>
        </View>
      </LinearGradient>
    </View>
  );
}

const styles = StyleSheet.create({
  flex: { flex: 1 },
  purpleRoot: { flex: 1, backgroundColor: BRAND_PURPLE },
  grayRoot: { flex: 1, backgroundColor: PANEL_GRAY, paddingHorizontal: 24 },
  header: {
    flexDirection: "row",
    alignItems: "center",
    paddingHorizontal: 16,
    backgroundColor: BRAND_PURPLE,
  },
  headerTitle: { color: "#FFFFFF", fontFamily: FONT.pretendard, fontSize: 24, fontWeight: "700" },
  headerRight: { marginLeft: "auto", flexDirection: "row", gap: 20, paddingRight: 8 },
  panel: {
    flex: 1,
    borderTopLeftRadius: 28,
    borderTopRightRadius: 28,
    backgroundColor: PANEL_GRAY,
    overflow: "hidden",
  },
  panelPadding: { paddingHorizontal: 24, paddingTop: 28, gap: 12 },

  tabBar: {
    flexDirection: "row",
    backgroundColor: "#FFFFFF",
    paddingTop: 6,
  },
  tabItem: { flex: 1, alignItems: "center", gap: 2 },
  tabLabel: { fontFamily: FONT.pretendard, fontSize: 11, fontWeight: "600", lineHeight: 14 },

  homePanel: {
    flex: 1,
    marginTop: -ROOM_OVERLAP,
    borderTopLeftRadius: 28,
    borderTopRightRadius: 28,
    backgroundColor: PANEL_GRAY,
    paddingHorizontal: 24,
    paddingTop: 28,
  },
  identityRow: { flexDirection: "row", alignItems: "center", gap: 8, paddingBottom: 12 },
  nicknameText: { flexShrink: 1, color: "#1F2937", fontFamily: FONT.pretendard, fontSize: 16, fontWeight: "700" },
  coinPill: {
    flexDirection: "row",
    alignItems: "center",
    gap: 4,
    borderRadius: 999,
    backgroundColor: "#FEF3C7",
    paddingHorizontal: 8,
    paddingVertical: 4,
  },
  coinText: { color: "#B45309", fontFamily: FONT.kufam, fontSize: 12, fontWeight: "700" },
  levelBarTrack: {
    height: 24,
    borderRadius: 12,
    backgroundColor: "#D9D9D9",
    overflow: "hidden",
    marginBottom: 18,
  },
  levelBarFill: { height: "100%", borderRadius: 12 },
  levelText: {
    position: "absolute",
    left: 12,
    color: "#475569",
    fontFamily: FONT.kufam,
    fontSize: 12,
    fontWeight: "700",
    lineHeight: 24,
  },
  primaryButton: {
    alignItems: "center",
    justifyContent: "center",
    minHeight: 64,
    borderRadius: 18,
    backgroundColor: BRAND_PURPLE,
  },
  primaryButtonText: { color: "#ffffff", fontFamily: FONT.pretendard, fontSize: 18, fontWeight: "500" },
  secondaryRow: { flexDirection: "row", gap: 12, marginTop: 14 },
  secondaryButton: {
    flex: 1,
    alignItems: "center",
    justifyContent: "center",
    minHeight: 72,
    borderRadius: 18,
    backgroundColor: "#ffffff",
  },
  secondaryButtonText: { color: "#1f2937", fontFamily: FONT.pretendard, fontSize: 15, fontWeight: "500" },
  darkButton: { backgroundColor: "#111827" },
  whiteText: { color: "#ffffff" },

  contentList: { paddingHorizontal: 31, paddingTop: 36, gap: 21 },

  statsRow: { flexDirection: "row", gap: 10, marginBottom: 4 },
  statBox: {
    flex: 1,
    alignItems: "center",
    borderRadius: 16,
    backgroundColor: "#FFFFFF",
    paddingVertical: 14,
  },
  statValue: { color: "#111827", fontFamily: FONT.kufam, fontSize: 20, fontWeight: "700" },
  statLabel: { marginTop: 2, color: "#64748B", fontFamily: FONT.pretendard, fontSize: 12, fontWeight: "600" },
  reportCard: {
    flexDirection: "row",
    alignItems: "center",
    gap: 12,
    borderRadius: 16,
    backgroundColor: "#FFFFFF",
    padding: 16,
  },
  reportIcon: {
    alignItems: "center",
    justifyContent: "center",
    width: 40,
    height: 40,
    borderRadius: 12,
    backgroundColor: "#DDFBEA",
  },
  reportTitle: { color: "#111827", fontFamily: FONT.pretendard, fontSize: 15, fontWeight: "700" },
  reportDetail: { marginTop: 2, color: "#64748B", fontFamily: FONT.pretendard, fontSize: 12, fontWeight: "500" },
  reportDate: { color: "#94A3B8", fontFamily: FONT.pretendard, fontSize: 12, fontWeight: "600" },
  aiButton: {
    flexDirection: "row",
    gap: 6,
    minHeight: 52,
    marginTop: 8,
    alignItems: "center",
    justifyContent: "center",
    borderRadius: 999,
    backgroundColor: BRAND_PURPLE,
  },
  aiButtonText: { color: "#ffffff", fontFamily: FONT.pretendard, fontSize: 14, fontWeight: "700" },

  reportScreenTitle: { color: "#111827", fontFamily: FONT.pretendard, fontSize: 22, fontWeight: "800" },
  reportScreenSub: {
    marginTop: 4,
    marginBottom: 18,
    color: "#64748B",
    fontFamily: FONT.pretendard,
    fontSize: 13,
    fontWeight: "500",
  },
  reportSummaryCard: { borderRadius: 20, backgroundColor: "#111827", padding: 20 },
  summaryEyebrow: { color: "#A5B4FC", fontFamily: FONT.pretendard, fontSize: 12, fontWeight: "800" },
  summaryTitle: { marginTop: 6, color: "#FFFFFF", fontFamily: FONT.pretendard, fontSize: 20, fontWeight: "800" },
  summaryBody: {
    marginTop: 8,
    color: "rgba(255,255,255,0.8)",
    fontFamily: FONT.pretendard,
    fontSize: 13,
    fontWeight: "500",
    lineHeight: 20,
  },
  conceptCard: { marginTop: 14, borderRadius: 20, backgroundColor: "#FFFFFF", padding: 20, gap: 14 },
  conceptCardTitle: { color: "#111827", fontFamily: FONT.pretendard, fontSize: 15, fontWeight: "700" },
  conceptRow: { flexDirection: "row", alignItems: "center", gap: 10 },
  conceptName: { width: 120, color: "#334155", fontFamily: FONT.pretendard, fontSize: 13, fontWeight: "600" },
  conceptTrack: { flex: 1, height: 8, borderRadius: 4, backgroundColor: "#E2E8F0", overflow: "hidden" },
  conceptFill: { height: "100%", borderRadius: 4, backgroundColor: "#F43F5E" },
  conceptRate: { width: 36, textAlign: "right", color: "#F43F5E", fontFamily: FONT.kufam, fontSize: 12, fontWeight: "700" },

  receivedPill: {
    flexDirection: "row",
    alignItems: "center",
    alignSelf: "flex-start",
    gap: 8,
    borderRadius: 999,
    backgroundColor: "#FFFFFF",
    paddingHorizontal: 14,
    paddingVertical: 8,
  },
  requestDot: { width: 8, height: 8, borderRadius: 4, backgroundColor: "#F59E0B" },
  receivedText: { color: "#111827", fontFamily: FONT.pretendard, fontSize: 13, fontWeight: "700" },
  requestCount: {
    minWidth: 20,
    alignItems: "center",
    borderRadius: 10,
    backgroundColor: BRAND_PURPLE,
    paddingHorizontal: 6,
  },
  requestCountText: { color: "#FFFFFF", fontFamily: FONT.pretendard, fontSize: 12, fontWeight: "700", lineHeight: 20 },

  profileArea: { paddingHorizontal: 24, paddingTop: 12, paddingBottom: 28 },
  actionList: { gap: 12 },
  actionButton: {
    minHeight: 62,
    flexDirection: "row",
    alignItems: "center",
    borderRadius: 18,
    backgroundColor: "#ffffff",
    paddingHorizontal: 16,
  },
  actionIconBox: {
    alignItems: "center",
    justifyContent: "center",
    width: 40,
    height: 40,
    borderRadius: 14,
    backgroundColor: "rgba(109, 93, 251, 0.12)",
  },
  actionText: { marginLeft: 14, color: "#111827", fontFamily: FONT.pretendard, fontSize: 16, fontWeight: "600" },

  shopRoot: { flex: 1, backgroundColor: "#F5F3FF" },
  shopHeader: { flexDirection: "row", alignItems: "center", gap: 10, paddingHorizontal: 20, paddingBottom: 12 },
  shopTitle: { color: "#111827", fontFamily: FONT.pretendard, fontSize: 22, fontWeight: "800" },
  shopList: { paddingHorizontal: 20, gap: 16 },
  featuredWrap: { borderRadius: 22, overflow: "hidden" },
  featuredCard: { alignItems: "center", paddingHorizontal: 20, paddingVertical: 20 },
  featuredBadge: {
    flexDirection: "row",
    alignItems: "center",
    gap: 4,
    borderRadius: 999,
    backgroundColor: "rgba(0,0,0,0.22)",
    paddingHorizontal: 10,
    paddingVertical: 4,
  },
  featuredBadgeText: { color: "#FDE68A", fontFamily: FONT.pretendard, fontSize: 11, fontWeight: "800" },
  featuredImage: { width: 150, height: 96, marginVertical: 10 },
  featuredName: { color: "#ffffff", fontFamily: FONT.pretendard, fontSize: 18, fontWeight: "800" },
  featuredDesc: {
    marginTop: 4,
    color: "rgba(255,255,255,0.8)",
    fontFamily: FONT.pretendard,
    fontSize: 13,
    fontWeight: "500",
  },
  featuredCta: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "center",
    gap: 6,
    marginTop: 14,
    borderRadius: 12,
    backgroundColor: "#FDE68A",
    paddingVertical: 12,
    alignSelf: "stretch",
  },
  featuredCtaText: { color: "#7C2D12", fontFamily: FONT.pretendard, fontSize: 15, fontWeight: "800" },
});
