import { router, useFocusEffect, useNavigation } from "expo-router";
import { BlurView } from "expo-blur";
import {
  Book,
  BookOpen,
  Brain,
  Cpu,
  Database,
  Monitor,
  Network,
  type LucideIcon,
} from "lucide-react-native";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import {
  ActivityIndicator,
  type NativeScrollEvent,
  type NativeSyntheticEvent,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  useWindowDimensions,
  View,
} from "react-native";
import { useSafeAreaInsets } from "react-native-safe-area-context";

import { FONT } from "../../constants/fonts";
import { useAuth } from "../../src/features/auth";
import { ConceptCard } from "../../src/features/content/components/ConceptCard";
import { ProblemSetCard } from "../../src/features/content/components/ProblemSetCard";
import {
  useCategoryLv1List,
  useCategoryLv2List,
  useChildCounts,
  useConcepts,
  useSubjects,
} from "../../src/features/content";
import type { ChildCountKind, Concept, Subject } from "../../src/features/content";
import { useProblemSetProgress } from "../../src/features/progress";
import { useDailyRewardLimits } from "../../src/features/reward";
import {
  useLanguage,
  type ContentLanguage,
} from "../../src/shared/language/LanguageContext";

type ContentDepth = "subject" | "category1" | "category2" | "concept";

// 하위 단계 개수(chapterCount) 조회가 아직 안 끝났을 때의 기본값.
const PLACEHOLDER_CHAPTER_COUNT = 0;

type ProblemSetItem = {
  id: number;
  title: string;
  chapterCount: number;
  iconColor?: string;
  Icon?: LucideIcon;
  thumbnailColor?: string;
  progress: number;
  hasSolved: boolean;
  /** 개념 카드 전용: 완료 시 "3 / 5 정답" 같은 결과 요약 */
  resultLabel?: string;
  /** 개념 카드 전용: false면 chapterCount(문제 수)를 아직 모른다는 뜻 */
  hasChapterCount?: boolean;
};

type TrailItem = {
  depth: ContentDepth;
  id?: number;
  title?: string;
};

const SUBJECT_VISUALS: Record<
  string,
  { Icon: LucideIcon; iconColor: string; thumbnailColor: string }
> = {
  DS: { Icon: BookOpen, iconColor: "#2563FF", thumbnailColor: "#E4EEFF" },
  ALGO: { Icon: Brain, iconColor: "#A855F7", thumbnailColor: "#F2DEFF" },
  DB: { Icon: Database, iconColor: "#F97316", thumbnailColor: "#FFF0DC" },
  NETWORK: { Icon: Network, iconColor: "#14B8A6", thumbnailColor: "#D9FBF3" },
  OS: { Icon: Cpu, iconColor: "#16C45B", thumbnailColor: "#DDFBEA" },
  CA: { Icon: Monitor, iconColor: "#F43F5E", thumbnailColor: "#FFE2E5" },
};

function pickLocalizedName(
  ko: string,
  en: string,
  language: ContentLanguage,
): string {
  if (language === "en") {
    return en || ko;
  }
  return ko || en;
}

function mapSubject(
  subject: Subject,
  chapterCount: number,
  language: ContentLanguage,
  isCompleted: boolean,
): ProblemSetItem {
  const visuals = SUBJECT_VISUALS[subject.subjectCode];
  return {
    id: subject.subjectId,
    title: pickLocalizedName(subject.subjectName, subject.subjectNameEn, language),
    chapterCount,
    progress: isCompleted ? 100 : 0,
    hasSolved: isCompleted,
    Icon: visuals?.Icon,
    iconColor: visuals?.iconColor,
    thumbnailColor: visuals?.thumbnailColor,
  };
}

function mapNode(
  id: number,
  title: string,
  chapterCount: number,
  isCompleted: boolean,
): ProblemSetItem {
  return {
    id,
    title,
    chapterCount,
    progress: isCompleted ? 100 : 0,
    hasSolved: isCompleted,
  };
}

// 개념 카드의 총 문제 수/정답 수는 진행률 API(ProgressController, 아직 더미 - #166)가
// 아니라 개념 목록 API(ContentController)가 최고 점수 완료 회차 기준으로 이미 내려준다.
// 단, 백엔드가 이 값을 "완료한 적 있는 개념"에만 채워 보내고(NOT_STARTED/IN_PROGRESS면
// null) 있어서, 아직 안 푼 개념은 문제 수 자체를 모른다 - 0으로 잘못 보여주는 대신
// 그 줄을 아예 숨긴다.
function mapConcept(concept: Concept, title: string): ProblemSetItem {
  const isCompleted = concept.status === "COMPLETED";
  const hasResult = concept.totalQuestionCount !== null && concept.correctCount !== null;
  return {
    id: concept.conceptId,
    title,
    chapterCount: concept.totalQuestionCount ?? 0,
    hasChapterCount: concept.totalQuestionCount !== null,
    progress: isCompleted ? 100 : 0,
    hasSolved: isCompleted,
    resultLabel: isCompleted && hasResult
      ? `${concept.correctCount} / ${concept.totalQuestionCount} 정답`
      : undefined,
  };
}

type TabPressNavigation = {
  addListener: (eventName: "tabPress", callback: () => void) => () => void;
};

const HEADER_HEIGHT = 60;
const OVERSCROLL_BUFFER = 96;
const BRAND_PURPLE = "#6D5DFB";
const PANEL_GRAY = "#F3F3F3";

const depthOrder: ContentDepth[] = [
  "subject",
  "category1",
  "category2",
  "concept",
];

const thumbnailByDepth: Record<
  ContentDepth,
  {
    iconColor: string;
    Icon: LucideIcon;
    thumbnailColor: string;
  }
> = {
  subject: {
    iconColor: "#2563FF",
    Icon: BookOpen,
    thumbnailColor: "#E4EEFF",
  },
  category1: {
    iconColor: "#2563FF",
    Icon: Book,
    thumbnailColor: "#E4F0FF",
  },
  category2: {
    iconColor: "#16C45B",
    Icon: Book,
    thumbnailColor: "#DFF8EA",
  },
  concept: {
    iconColor: "#F97316",
    Icon: Book,
    thumbnailColor: "#FFF0DC",
  },
};

const countUnitByDepth: Record<
  ContentDepth,
  "chapter" | "concept" | "question"
> = {
  subject: "chapter",
  category1: "chapter",
  category2: "concept",
  concept: "question",
};

const startButtonLabelByDepth: Record<ContentDepth, string> = {
  subject: "과목 모의고사 풀기",
  category1: "챕터 모의고사 풀기",
  category2: "챕터 모의고사 풀기",
  concept: "개념 문제집 풀기",
};

const sessionTypeByDepth: Record<
  ContentDepth,
  "SUBJECT_MOCK" | "CATEGORY_LV1_MOCK" | "CATEGORY_LV2_MOCK" | "CONCEPT_PRACTICE"
> = {
  subject: "SUBJECT_MOCK",
  category1: "CATEGORY_LV1_MOCK",
  category2: "CATEGORY_LV2_MOCK",
  concept: "CONCEPT_PRACTICE",
};

// SCR-CONTENT-01/02 완료 기준에 명시된 일일 XP 지급 한도(정책값). 오늘 아직
// 한 번도 지급받지 않아 daily_reward_limits 행이 없는 경우의 기본값으로 쓰고,
// 실제 행이 있으면 그 행의 dailyLimit을 우선한다.
const DAILY_XP_LIMIT_FALLBACK: Partial<Record<ContentDepth, number>> = {
  subject: 5,
  category1: 7,
  category2: 7,
};

const REWARD_TYPE_BY_DEPTH: Partial<Record<ContentDepth, string>> = {
  subject: "SUBJECT_MOCK_XP",
  category1: "CATEGORY_MOCK_XP",
  category2: "CATEGORY_MOCK_XP",
};

const getNextDepth = (depth: ContentDepth) => {
  const currentIndex = depthOrder.indexOf(depth);
  return depthOrder[currentIndex + 1] ?? null;
};

export default function ContentScreen() {
  const navigation = useNavigation();
  const scrollRef = useRef<ScrollView>(null);
  const { height } = useWindowDimensions();
  const insets = useSafeAreaInsets();
  const { status, accessToken, user } = useAuth();
  const isGuest = status === "guest";
  const { language } = useLanguage();
  const [bounceBackground, setBounceBackground] = useState(BRAND_PURPLE);
  const [trail, setTrail] = useState<TrailItem[]>([{ depth: "subject" }]);
  const currentTrail = trail[trail.length - 1];
  const currentDepth = currentTrail.depth;
  const parentId = currentTrail.id ?? null;
  const selectedTitle = currentTrail.title;
  const isConceptDepth = currentDepth === "concept";
  const currentThumbnail = thumbnailByDepth[currentDepth];
  const currentCountUnit = countUnitByDepth[currentDepth];
  const currentStartButtonLabel = startButtonLabelByDepth[currentDepth];
  const headerHeight = HEADER_HEIGHT + insets.top;

  const subjectsQuery = useSubjects();
  const categoryLv1Query = useCategoryLv1List(
    currentDepth === "category1" ? parentId : null,
  );
  const categoryLv2Query = useCategoryLv2List(
    currentDepth === "category2" ? parentId : null,
  );
  const conceptsQuery = useConcepts(currentDepth === "concept" ? parentId : null);

  const activeQuery =
    currentDepth === "subject"
      ? subjectsQuery
      : currentDepth === "category1"
        ? categoryLv1Query
        : currentDepth === "category2"
          ? categoryLv2Query
          : conceptsQuery;

  // 카드에 보여줄 "바로 하위 단계 개수"(예: 과목 카드엔 대분류 개수)를 병렬 조회한다.
  // 개념 카드의 "문제 개수"는 여기 대상이 아니다 - concepts 목록 API가 이미
  // totalQuestionCount/correctCount를 함께 내려줘서 mapConcept에서 바로 쓴다.
  const childCountKind: ChildCountKind =
    currentDepth === "subject"
      ? "category-lv1"
      : currentDepth === "category1"
        ? "category-lv2"
        : currentDepth === "category2"
          ? "concepts"
          : null;

  const childCountIds = useMemo(() => {
    switch (currentDepth) {
      case "subject":
        return (subjectsQuery.data ?? []).map((subject) => subject.subjectId);
      case "category1":
        return (categoryLv1Query.data ?? []).map((category) => category.categoryLv1Id);
      case "category2":
        return (categoryLv2Query.data ?? []).map((category) => category.categoryLv2Id);
      default:
        return [];
    }
  }, [currentDepth, subjectsQuery.data, categoryLv1Query.data, categoryLv2Query.data]);

  const childCounts = useChildCounts(childCountKind, childCountIds);

  // 현재 깊이의 각 항목이 "그 항목 자신의" 모의고사/문제집을 완료했는지는
  // 깊이별 problemSetType + 부모 필터로 한 번에 조회해 targetId로 찾아본다.
  const progressFilters = useMemo(() => {
    switch (currentDepth) {
      case "subject":
        return {};
      case "category1":
        return { subjectId: parentId ?? undefined };
      case "category2":
        return { categoryLv1Id: parentId ?? undefined };
      case "concept":
        return { categoryLv2Id: parentId ?? undefined };
    }
  }, [currentDepth, parentId]);

  const problemSetProgressQuery = useProblemSetProgress(accessToken, user?.userId ?? null, {
    problemSetType: sessionTypeByDepth[currentDepth],
    ...progressFilters,
  });

  const completedByTargetId = useMemo(() => {
    const map = new Map<number, boolean>();
    for (const item of problemSetProgressQuery.data ?? []) {
      map.set(item.targetId, item.isCompleted);
    }
    return map;
  }, [problemSetProgressQuery.data]);

  const dailyRewardLimitsQuery = useDailyRewardLimits(accessToken);
  const rewardTypeForDepth = REWARD_TYPE_BY_DEPTH[currentDepth];
  const remainingMockXpCount = useMemo(() => {
    if (!rewardTypeForDepth) {
      return null;
    }
    const row = (dailyRewardLimitsQuery.data ?? []).find(
      (item) => item.rewardType === rewardTypeForDepth,
    );
    const limit = row?.dailyLimit ?? DAILY_XP_LIMIT_FALLBACK[currentDepth] ?? null;
    if (limit === null) {
      return null;
    }
    return Math.max(0, limit - (row?.rewardCount ?? 0));
  }, [dailyRewardLimitsQuery.data, rewardTypeForDepth, currentDepth]);

  const currentItems = useMemo<ProblemSetItem[]>(() => {
    switch (currentDepth) {
      case "subject":
        return [...(subjectsQuery.data ?? [])]
          .sort((a, b) => a.sortOrder - b.sortOrder)
          .map((subject) =>
            mapSubject(
              subject,
              childCounts.get(subject.subjectId) ?? PLACEHOLDER_CHAPTER_COUNT,
              language,
              completedByTargetId.get(subject.subjectId) ?? false,
            ),
          );
      case "category1":
        return [...(categoryLv1Query.data ?? [])]
          .sort((a, b) => a.sortOrder - b.sortOrder)
          .map((category) =>
            mapNode(
              category.categoryLv1Id,
              pickLocalizedName(
                category.categoryLv1Name,
                category.categoryLv1NameEn,
                language,
              ),
              childCounts.get(category.categoryLv1Id) ?? PLACEHOLDER_CHAPTER_COUNT,
              completedByTargetId.get(category.categoryLv1Id) ?? false,
            ),
          );
      case "category2":
        return [...(categoryLv2Query.data ?? [])]
          .sort((a, b) => a.sortOrder - b.sortOrder)
          .map((category) =>
            mapNode(
              category.categoryLv2Id,
              pickLocalizedName(
                category.categoryLv2Name,
                category.categoryLv2NameEn,
                language,
              ),
              childCounts.get(category.categoryLv2Id) ?? PLACEHOLDER_CHAPTER_COUNT,
              completedByTargetId.get(category.categoryLv2Id) ?? false,
            ),
          );
      case "concept":
        return [...(conceptsQuery.data ?? [])]
          .sort((a, b) => a.sortOrder - b.sortOrder)
          .map((concept) =>
            mapConcept(
              concept,
              pickLocalizedName(concept.conceptName, concept.conceptNameEn, language),
            ),
          );
    }
  }, [
    currentDepth,
    subjectsQuery.data,
    categoryLv1Query.data,
    categoryLv2Query.data,
    conceptsQuery.data,
    childCounts,
    language,
    completedByTargetId,
  ]);

  const { refetch: refetchSubjects } = subjectsQuery;
  const { refetch: refetchCategoryLv1 } = categoryLv1Query;
  const { refetch: refetchCategoryLv2 } = categoryLv2Query;
  const { refetch: refetchConcepts } = conceptsQuery;

  const resetContent = useCallback(() => {
    setTrail([{ depth: "subject" }]);
    scrollRef.current?.scrollTo({ y: 0, animated: true });
    // 다른 깊이(대분류/중분류/개념)의 쿼리는 parentId가 null로 바뀌며
    // enabled 전환으로 다음에 드릴다운할 때 알아서 새로 불러온다. 항상
    // 켜져 있는 과목 목록만 여기서 직접 다시 불러온다.
    refetchSubjects();
  }, [refetchSubjects]);

  // 드릴다운 위치(trail)는 그대로 두고 현재 깊이의 목록만 새로 불러온다.
  // 문제를 풀고 돌아왔을 때 방금 푼 개념의 완료 상태·문제 수·정답 수를
  // 반영하려면 필요하다 - 같은 컴포넌트 인스턴스가 계속 마운트돼 있어서
  // (trail을 안 바꾸니 쿼리 파라미터도 안 바뀜) 가만히 두면 30초 캐시가
  // 만료되기 전까진 풀기 전 데이터를 계속 보여준다.
  const refreshCurrentDepth = useCallback(() => {
    switch (currentDepth) {
      case "subject":
        refetchSubjects();
        return;
      case "category1":
        refetchCategoryLv1();
        return;
      case "category2":
        refetchCategoryLv2();
        return;
      case "concept":
        refetchConcepts();
        return;
    }
  }, [currentDepth, refetchSubjects, refetchCategoryLv1, refetchCategoryLv2, refetchConcepts]);

  const scrollToTop = useCallback(() => {
    requestAnimationFrame(() => {
      scrollRef.current?.scrollTo({ y: 0, animated: true });
    });
  }, []);

  // 개념 상세/최근 기록/문제 풀이처럼 이 화면이 직접 push한 자식 화면에서
  // 뒤로가기로 돌아온 경우엔 드릴다운 위치(trail)를 유지해야 한다. 그 경우가
  // 아니라 다른 탭이나 화면에서 새로 들어온 경우에만 과목 선택으로 초기화한다.
  const suppressNextResetRef = useRef(false);

  useFocusEffect(
    useCallback(() => {
      if (suppressNextResetRef.current) {
        suppressNextResetRef.current = false;
        refreshCurrentDepth();
        return;
      }
      resetContent();
    }, [resetContent, refreshCurrentDepth]),
  );

  useEffect(() => {
    const tabNavigation = navigation as unknown as TabPressNavigation;
    const unsubscribe = tabNavigation.addListener("tabPress", resetContent);

    return unsubscribe;
  }, [navigation, resetContent]);

  const screenTitle = useMemo(() => {
    return selectedTitle ?? "문제집";
  }, [selectedTitle]);

  const openChildTree = (item: ProblemSetItem) => {
    const nextDepth = getNextDepth(currentDepth);

    if (!nextDepth) {
      startProblemSet(item);
      return;
    }

    setTrail((prevTrail) => [
      ...prevTrail,
      { depth: nextDepth, id: item.id, title: item.title },
    ]);
    scrollToTop();
  };

  const startProblemSet = (item: ProblemSetItem) => {
    const sessionType = sessionTypeByDepth[currentDepth];

    // 게스트는 개념 문제집만 풀 수 있고, 과목/챕터 모의고사는 로그인이 필요하다.
    if (isGuest && sessionType !== "CONCEPT_PRACTICE") {
      router.push("/login-required");
      return;
    }

    suppressNextResetRef.current = true;
    router.push({
      pathname: "/solve",
      params: {
        title: item.title,
        sessionType,
        ...(sessionType === "CONCEPT_PRACTICE"
          ? { conceptId: String(item.id) }
          : { targetId: String(item.id) }),
      },
    });
  };

  const viewConceptDetail = (item: ProblemSetItem) => {
    suppressNextResetRef.current = true;
    router.push({
      pathname: "/concept-detail",
      params: { conceptId: String(item.id) },
    });
  };

  const openRecentReview = (item: ProblemSetItem) => {
    // 해설/기록 확인은 회원 전용이라 게스트는 로그인 필요 안내로 보낸다.
    if (isGuest) {
      router.push("/login-required");
      return;
    }

    suppressNextResetRef.current = true;
    router.push({
      pathname: "/mistake-exam-review",
      params: {
        subject: "Data Structure",
        category: item.title,
        sessionType: sessionTypeByDepth[currentDepth],
      },
    });
  };

  const goBack = () => {
    setTrail((prevTrail) =>
      prevTrail.length > 1 ? prevTrail.slice(0, -1) : prevTrail,
    );
    scrollToTop();
  };

  const handleScroll = (event: NativeSyntheticEvent<NativeScrollEvent>) => {
    const { contentOffset, contentSize, layoutMeasurement } = event.nativeEvent;
    const maxScrollY = Math.max(
      0,
      contentSize.height - layoutMeasurement.height,
    );
    const isNearBottom =
      maxScrollY > 0 && contentOffset.y > maxScrollY - OVERSCROLL_BUFFER;
    const nextBackground = isNearBottom ? PANEL_GRAY : BRAND_PURPLE;

    setBounceBackground((currentBackground) =>
      currentBackground === nextBackground ? currentBackground : nextBackground,
    );
  };

  return (
    <View style={[styles.container, { backgroundColor: bounceBackground }]}>
      <ScrollView
        bounces
        onScroll={handleScroll}
        ref={scrollRef}
        scrollEventThrottle={16}
        style={[styles.scroller, { backgroundColor: bounceBackground }]}
        showsVerticalScrollIndicator={false}
        contentContainerStyle={styles.scrollContent}
      >
        <View
          style={[
            styles.purpleArea,
            { height: headerHeight + OVERSCROLL_BUFFER },
          ]}
        />

        <View
          style={[styles.grayBackground, { minHeight: height - headerHeight }]}
        >
          <View style={styles.listContent}>
              {!isGuest && remainingMockXpCount !== null && (
                <Text style={styles.dailyLimitText}>
                  오늘 남은 {currentDepth === "subject" ? "과목" : "챕터"} 모의고사 XP 지급
                  가능 횟수: {remainingMockXpCount}회
                </Text>
              )}

              {activeQuery.isLoading && (
                <View style={styles.stateContainer}>
                  <ActivityIndicator color={BRAND_PURPLE} size="large" />
                </View>
              )}

              {!activeQuery.isLoading && activeQuery.isError && (
                <View style={styles.stateContainer}>
                  <Text style={styles.stateText}>
                    목록을 불러오지 못했습니다. 잠시 후 다시 시도해주세요.
                  </Text>
                </View>
              )}

              {!activeQuery.isLoading &&
                !activeQuery.isError &&
                currentItems.length === 0 && (
                  <View style={styles.stateContainer}>
                    <Text style={styles.stateText}>표시할 항목이 없습니다.</Text>
                  </View>
                )}

              {!activeQuery.isLoading &&
              !activeQuery.isError &&
              currentItems.map((item) =>
                isConceptDepth ? (
                  <ConceptCard
                    key={item.id}
                    chapterCount={item.chapterCount}
                    hasChapterCount={item.hasChapterCount}
                    countUnit={currentCountUnit}
                    hideProgress={isGuest}
                    Icon={item.Icon ?? currentThumbnail.Icon}
                    iconColor={item.iconColor ?? currentThumbnail.iconColor}
                    progress={item.progress}
                    resultLabel={item.resultLabel}
                    startButtonLabel={currentStartButtonLabel}
                    thumbnailColor={
                      item.thumbnailColor ?? currentThumbnail.thumbnailColor
                    }
                    title={item.title}
                    onStart={() => startProblemSet(item)}
                    onViewDetail={() => viewConceptDetail(item)}
                  />
                ) : (
                  <ProblemSetCard
                    key={item.id}
                    chapterCount={item.chapterCount}
                    countUnit={currentCountUnit}
                    hasSolved={item.hasSolved}
                    hideProgress={isGuest}
                    Icon={item.Icon ?? currentThumbnail.Icon}
                    iconColor={item.iconColor ?? currentThumbnail.iconColor}
                    progress={item.progress}
                    startButtonLabel={currentStartButtonLabel}
                    thumbnailColor={
                      item.thumbnailColor ?? currentThumbnail.thumbnailColor
                    }
                    title={item.title}
                    onOpen={() => openChildTree(item)}
                    onReview={() => openRecentReview(item)}
                    onStart={() => startProblemSet(item)}
                  />
                ),
              )}
          </View>
        </View>
      </ScrollView>

      <BlurView
        intensity={28}
        tint="dark"
        style={[
          styles.fixedHeader,
          {
            minHeight: headerHeight,
            paddingTop: insets.top,
          },
        ]}
      >
        <View style={styles.headerOverlay} />
        {trail.length > 1 && (
          <Pressable
            accessibilityLabel="Back"
            accessibilityRole="button"
            hitSlop={10}
            style={styles.backButton}
            onPress={goBack}
          >
            <Text style={styles.backButtonText}>{"<"}</Text>
          </Pressable>
        )}

        <Text style={styles.title}>{screenTitle}</Text>
      </BlurView>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  scroller: {
    flex: 1,
  },
  scrollContent: {
    flexGrow: 1,
  },
  purpleArea: {
    height: HEADER_HEIGHT + OVERSCROLL_BUFFER,
    backgroundColor: BRAND_PURPLE,
    marginBottom: -OVERSCROLL_BUFFER,
  },
  fixedHeader: {
    position: "absolute",
    top: 0,
    left: 0,
    right: 0,
    zIndex: 10,
    minHeight: HEADER_HEIGHT,
    flexDirection: "row",
    alignItems: "center",
    overflow: "hidden",
    paddingHorizontal: 16,
    paddingTop: 8,
  },
  headerOverlay: {
    ...StyleSheet.absoluteFillObject,
    backgroundColor: "rgba(109, 93, 251, 0.85)",
  },
  backButton: {
    alignItems: "center",
    justifyContent: "center",
    width: 32,
    height: 32,
    marginRight: 8,
  },
  backButtonText: {
    color: "#ffffff",
    fontFamily: FONT.pretendard,
    fontSize: 34,
    fontWeight: "400",
    lineHeight: 34,
  },
  title: {
    flexShrink: 1,
    color: "#ffffff",
    fontFamily: FONT.pretendard,
    fontSize: 24,
    fontWeight: "700",
  },
  grayBackground: {
    flex: 1,
    width: "100%",
    borderTopLeftRadius: 18,
    borderTopRightRadius: 18,
    backgroundColor: PANEL_GRAY,
  },
  listContent: {
    paddingHorizontal: 31,
    paddingTop: 45,
    paddingBottom: 32 + OVERSCROLL_BUFFER,
    gap: 21,
  },
  dailyLimitText: {
    color: "#6b7280",
    fontFamily: FONT.pretendard,
    fontSize: 12,
    fontWeight: "600",
  },
  stateContainer: {
    alignItems: "center",
    paddingVertical: 48,
  },
  stateText: {
    color: "#6b7280",
    fontFamily: FONT.pretendard,
    fontSize: 14,
    fontWeight: "500",
    textAlign: "center",
  },
});
