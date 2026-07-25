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

type ContentDepth = "subject" | "category1" | "category2" | "concept";

type ProblemSetItem = {
  id: string;
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
  title?: string;
};

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

const listByDepth: Record<ContentDepth, ProblemSetItem[]> = {
  subject: [
    {
      id: "data-structure",
      title: "Data structure",
      chapterCount: 12,
      iconColor: "#2563FF",
      Icon: BookOpen,
      thumbnailColor: "#E4EEFF",
      progress: 80,
      hasSolved: true,
    },
    {
      id: "algorithm",
      title: "Algorithm",
      chapterCount: 12,
      iconColor: "#A855F7",
      Icon: Brain,
      thumbnailColor: "#F2DEFF",
      progress: 80,
      hasSolved: false,
    },
    {
      id: "database",
      title: "Database",
      chapterCount: 12,
      iconColor: "#F97316",
      Icon: Database,
      thumbnailColor: "#FFF0DC",
      progress: 80,
      hasSolved: false,
    },
    {
      id: "network",
      title: "Network",
      chapterCount: 12,
      iconColor: "#14B8A6",
      Icon: Network,
      thumbnailColor: "#D9FBF3",
      progress: 80,
      hasSolved: false,
    },
    {
      id: "operating-system",
      title: "Operating System",
      chapterCount: 12,
      iconColor: "#16C45B",
      Icon: Cpu,
      thumbnailColor: "#DDFBEA",
      progress: 80,
      hasSolved: false,
    },
    {
      id: "computer-architecture",
      title: "Computer Architecture",
      chapterCount: 12,
      iconColor: "#F43F5E",
      Icon: Monitor,
      thumbnailColor: "#FFE2E5",
      progress: 80,
      hasSolved: false,
    },
  ],
  category1: [
    {
      id: "basic-concepts",
      title: "Basic concepts",
      chapterCount: 8,
      progress: 64,
      hasSolved: false,
    },
    {
      id: "linear-structure",
      title: "Linear structure",
      chapterCount: 10,
      progress: 72,
      hasSolved: true,
    },
    {
      id: "tree-structure",
      title: "Tree structure",
      chapterCount: 7,
      progress: 45,
      hasSolved: false,
    },
    {
      id: "graph-structure",
      title: "Graph structure",
      chapterCount: 6,
      progress: 38,
      hasSolved: false,
    },
  ],
  category2: [
    {
      id: "array",
      title: "Array",
      chapterCount: 5,
      progress: 76,
      hasSolved: true,
    },
    {
      id: "linked-list",
      title: "Linked List",
      chapterCount: 6,
      progress: 52,
      hasSolved: false,
    },
    {
      id: "stack-queue",
      title: "Stack & Queue",
      chapterCount: 6,
      progress: 42,
      hasSolved: false,
    },
  ],
  concept: [
    {
      id: "static-array",
      title: "Static Array",
      chapterCount: 12,
      progress: 100,
      hasSolved: false,
    },
    {
      id: "dynamic-array",
      title: "Dynamic Array",
      chapterCount: 12,
      progress: 80,
      hasSolved: false,
    },
    {
      id: "indexing",
      title: "Indexing",
      chapterCount: 12,
      progress: 80,
      hasSolved: false,
    },
    {
      id: "insertion",
      title: "Insertion",
      chapterCount: 12,
      progress: 80,
      hasSolved: false,
    },
    {
      id: "deletion",
      title: "Deletion",
      chapterCount: 12,
      progress: 80,
      hasSolved: false,
    },
  ],
};

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
  const [bounceBackground, setBounceBackground] = useState(BRAND_PURPLE);
  const [trail, setTrail] = useState<TrailItem[]>([{ depth: "subject" }]);
  const currentTrail = trail[trail.length - 1];
  const currentDepth = currentTrail.depth;
  const selectedTitle = currentTrail.title;
  const currentItems = listByDepth[currentDepth];
  const isConceptDepth = currentDepth === "concept";
  const currentThumbnail = thumbnailByDepth[currentDepth];
  const currentCountUnit = countUnitByDepth[currentDepth];
  const currentStartButtonLabel = startButtonLabelByDepth[currentDepth];
  const headerHeight = HEADER_HEIGHT + insets.top;

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
    return selectedTitle ?? "Practice Notes";
  }, [selectedTitle]);

  const openChildTree = (item: ProblemSetItem) => {
    const nextDepth = getNextDepth(currentDepth);

    if (!nextDepth) {
      startProblemSet(item);
      return;
    }

    setTrail((prevTrail) => [
      ...prevTrail,
      { depth: nextDepth, title: item.title },
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
      },
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
              {currentItems.map((item) =>
                isConceptDepth ? (
                  <ConceptCard
                    key={item.id}
                    chapterCount={item.chapterCount}
                    countUnit={currentCountUnit}
                    hideProgress={isGuest}
                    Icon={item.Icon ?? currentThumbnail.Icon}
                    iconColor={item.iconColor ?? currentThumbnail.iconColor}
                    progress={item.progress}
                    thumbnailColor={
                      item.thumbnailColor ?? currentThumbnail.thumbnailColor
                    }
                    title={item.title}
                    onPress={() => startProblemSet(item)}
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
});
