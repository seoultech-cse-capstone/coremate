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
import type { ChildCountKind, Subject } from "../../src/features/content";
import {
  useLanguage,
  type ContentLanguage,
} from "../../src/shared/language/LanguageContext";

type ContentDepth = "subject" | "category1" | "category2" | "concept";

// 문제집 요약/진행률(chapterCount, progress, hasSolved)은 ProblemSetController/
// ProgressController가 아직 더미 데이터인 데다 인증이 필요해 이번엔 연결하지 않는다.
// 실제 서비스가 준비되면 이 자리를 채워 넣는다.
const PLACEHOLDER_CHAPTER_COUNT = 0;
const PLACEHOLDER_PROGRESS = 0;

type ProblemSetItem = {
  id: number;
  title: string;
  chapterCount: number;
  iconColor?: string;
  Icon?: LucideIcon;
  thumbnailColor?: string;
  progress: number;
  hasSolved: boolean;
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
): ProblemSetItem {
  const visuals = SUBJECT_VISUALS[subject.subjectCode];
  return {
    id: subject.subjectId,
    title: pickLocalizedName(subject.subjectName, subject.subjectNameEn, language),
    chapterCount,
    progress: PLACEHOLDER_PROGRESS,
    hasSolved: false,
    Icon: visuals?.Icon,
    iconColor: visuals?.iconColor,
    thumbnailColor: visuals?.thumbnailColor,
  };
}

function mapNode(id: number, title: string, chapterCount: number): ProblemSetItem {
  return {
    id,
    title,
    chapterCount,
    progress: PLACEHOLDER_PROGRESS,
    hasSolved: false,
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
  "SUBJECT_MOCK" | "CHAPTER_MOCK" | "CONCEPT_PRACTICE"
> = {
  subject: "SUBJECT_MOCK",
  category1: "CHAPTER_MOCK",
  category2: "CHAPTER_MOCK",
  concept: "CONCEPT_PRACTICE",
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
  const { status } = useAuth();
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
  // 개념 카드의 "문제 개수"는 ProblemSetController가 필요해 이번 범위에서는 제외한다.
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
            ),
          );
      case "concept":
        return [...(conceptsQuery.data ?? [])]
          .sort((a, b) => a.sortOrder - b.sortOrder)
          .map((concept) =>
            mapNode(
              concept.conceptId,
              pickLocalizedName(concept.conceptName, concept.conceptNameEn, language),
              PLACEHOLDER_CHAPTER_COUNT,
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
  ]);

  const resetContent = useCallback(() => {
    setTrail([{ depth: "subject" }]);
    scrollRef.current?.scrollTo({ y: 0, animated: true });
  }, []);

  const scrollToTop = useCallback(() => {
    requestAnimationFrame(() => {
      scrollRef.current?.scrollTo({ y: 0, animated: true });
    });
  }, []);

  useFocusEffect(
    useCallback(() => {
      resetContent();
    }, [resetContent]),
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

    router.push({
      pathname: "/solve",
      params: {
        title: item.title,
        sessionType,
        ...(sessionType === "CONCEPT_PRACTICE" ? { conceptId: String(item.id) } : {}),
      },
    });
  };

  const viewConceptDetail = (item: ProblemSetItem) => {
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
                    countUnit={currentCountUnit}
                    hideProgress={isGuest}
                    Icon={item.Icon ?? currentThumbnail.Icon}
                    iconColor={item.iconColor ?? currentThumbnail.iconColor}
                    progress={item.progress}
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
