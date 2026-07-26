import { BlurView } from "expo-blur";
import { router, useLocalSearchParams, useNavigation } from "expo-router";
import { CalendarDays, Check, ChevronRight, X } from "lucide-react-native";
import { useEffect, useMemo, useRef, useState } from "react";
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
import { useAuth, useRequireMember } from "../../src/features/auth";
import { useSubjects } from "../../src/features/content/useContentQueries";
import { sessionTypeLabel } from "../../src/features/logWeak/types";
import type { CompletedProblemSetSummary } from "../../src/features/logWeak/types";
import { useCompletedProblemSets } from "../../src/features/logWeak/useCompletedProblemSets";
import { useLogWeakSelection } from "../../src/features/logWeak/LogWeakSelectionContext";

type LogWeakMode = "wrongNote" | "weakness";
type DepthFilter = "subject" | "Chapter1" | "Chapter2" | "concept";

type TabPressNavigation = {
  addListener: (eventName: "tabPress", callback: () => void) => () => void;
};

type FilterNode = { id: string; label: string };

type ExamHistoryDisplayItem = {
  solveSessionId: number;
  subject: string;
  category: string;
  examType: string;
  scopePath: string[];
  date: string;
  wrong: string;
};

function formatCompletedDate(isoString: string): string {
  return isoString.slice(0, 10).replace(/-/g, ".");
}

function toExamHistoryDisplayItem(
  summary: CompletedProblemSetSummary,
): ExamHistoryDisplayItem {
  const scopePath = [
    summary.subjectName,
    summary.categoryLv1Name,
    summary.categoryLv2Name,
    summary.conceptName,
  ].filter((name): name is string => Boolean(name));

  return {
    solveSessionId: summary.solveSessionId,
    subject: summary.subjectName ?? "",
    category: scopePath[scopePath.length - 1] ?? "",
    examType: sessionTypeLabel[summary.sessionType],
    scopePath,
    date: formatCompletedDate(summary.completedAt),
    wrong: `${summary.wrongCount}/${summary.totalQuestionCount}`,
  };
}

type WeaknessReportItem = {
  id: number;
  subject: string;
  depth: DepthFilter;
  date: string;
  examNames: string[];
  // 분석표를 끝까지 읽었는지 여부. 읽은 분석표만 초록 체크로 표시한다.
  isRead: boolean;
};

const HEADER_HEIGHT = 60;
const OVERSCROLL_BUFFER = 96;
const BRAND_PURPLE = "#6D5DFB";
const WEAKNESS_PURPLE = "#4E44A4";
const PANEL_GRAY = "#F3F3F3";

// 총 개념 수는 DB 전체 하위 개념 수, 완료 수는 문제를 푼 하위 개념 수를 의미한다.
const conceptStats = {
  totalCount: 500,
  solvedConceptCount: 100,
};

const weaknessReports: WeaknessReportItem[] = [
  {
    id: 1,
    subject: "Data Structure",
    depth: "Chapter1",
    date: "26-04-25",
    examNames: ["Linear Structure", "Array", "Linked List"],
    isRead: true,
  },
  {
    id: 2,
    subject: "Data Structure",
    depth: "Chapter1",
    date: "26-04-25",
    examNames: ["Linear Structure", "Stack", "Queue"],
    isRead: true,
  },
  {
    id: 3,
    subject: "Data Structure",
    depth: "Chapter2",
    date: "26-04-24",
    examNames: [
      "Array",
      "Dynamic Array",
      "Static Array",
      "Indexing",
      "Linked List",
    ],
    isRead: false,
  },
];

// 약점 분석표 통계는 분석표를 정독한 횟수(isRead)를 기준으로 계산한다.
const weaknessReportStats = {
  totalCount: weaknessReports.length,
  readCount: weaknessReports.filter((report) => report.isRead).length,
};

export default function LogWeakScreen() {
  useRequireMember();
  const navigation = useNavigation();
  const insets = useSafeAreaInsets();
  const { height } = useWindowDimensions();
  const scrollRef = useRef<ScrollView>(null);
  const { mode: modeParam } = useLocalSearchParams<{ mode?: string }>();
  const { setIsLogWeakSelecting } = useLogWeakSelection();
  const [mode, setMode] = useState<LogWeakMode>("wrongNote");
  const [selectedPath, setSelectedPath] = useState<FilterNode[]>([]);
  const [bounceBackground, setBounceBackground] = useState(BRAND_PURPLE);
  const [isSelecting, setIsSelecting] = useState(false);
  const [selectedExamIds, setSelectedExamIds] = useState<number[]>([]);
  const isWrongNote = mode === "wrongNote";
  const title = isWrongNote ? "Mistake Notes" : "Weakness Notes";
  const headerHeight = HEADER_HEIGHT + insets.top;
  const activePurple = isWrongNote ? BRAND_PURPLE : WEAKNESS_PURPLE;

  const { accessToken } = useAuth();

  // Row1(선택된 필터 목록)은 All + selectedPath. Row2(다음 단계 후보)는 selectedPath 깊이에 따라 달라진다.
  const [selectedSubject, selectedChapter1, selectedChapter2] = selectedPath;
  const selectedSubjectId = selectedSubject ? Number(selectedSubject.id) : null;
  const selectedChapter1Id = selectedChapter1 ? Number(selectedChapter1.id) : null;
  const selectedChapter2Id = selectedChapter2 ? Number(selectedChapter2.id) : null;

  // 과목은 콘텐츠 카탈로그 기준으로 항상 전부 보여준다. 완료 기록이 하나도
  // 없는 과목이라도 필터로는 선택할 수 있어야 하기 때문이다.
  const { data: subjects = [] } = useSubjects();
  // 대분류/중분류는 실제로 완료 기록이 있는 것만 필터 후보로 보여준다. 그래서
  // 선택된 과목 범위 안의 완료 기록을 미리 받아 대분류/중분류 후보를 뽑아낸다.
  const { data: subjectScopedSets = [] } = useCompletedProblemSets(accessToken, {
    subjectId: selectedSubjectId ?? undefined,
  });

  const categoryLv1Options = useMemo(() => {
    const seen = new Map<number, string>();
    for (const item of subjectScopedSets) {
      if (item.categoryLv1Id !== null && item.categoryLv1Name !== null) {
        seen.set(item.categoryLv1Id, item.categoryLv1Name);
      }
    }
    return [...seen.entries()].map(([id, label]) => ({ id: String(id), label }));
  }, [subjectScopedSets]);

  const categoryLv2Options = useMemo(() => {
    const seen = new Map<number, string>();
    for (const item of subjectScopedSets) {
      if (
        item.categoryLv1Id === selectedChapter1Id &&
        item.categoryLv2Id !== null &&
        item.categoryLv2Name !== null
      ) {
        seen.set(item.categoryLv2Id, item.categoryLv2Name);
      }
    }
    return [...seen.entries()].map(([id, label]) => ({ id: String(id), label }));
  }, [subjectScopedSets, selectedChapter1Id]);

  const row1Items: FilterNode[] = [{ id: "all", label: "All" }, ...selectedPath];
  const row2Level: "subject" | "chapter1" | "chapter2" = !selectedSubject
    ? "subject"
    : !selectedChapter1
      ? "chapter1"
      : "chapter2";
  const row2Items: FilterNode[] = !selectedSubject
    ? subjects.map((subject) => ({
        id: String(subject.subjectId),
        label: subject.subjectName,
      }))
    : !selectedChapter1
      ? categoryLv1Options
      : categoryLv2Options;

  const selectFilterTrail = (index: number) => {
    if (isSelecting) {
      return;
    }
    setSelectedPath((currentPath) => currentPath.slice(0, index));
  };

  const selectFilterChoice = (node: FilterNode) => {
    if (isSelecting) {
      return;
    }
    // chapter2 단계에서는 형제 항목끼리 교체만 하고 더 깊이 들어가지 않는다(concept 제외).
    setSelectedPath((currentPath) =>
      row2Level === "chapter2"
        ? [...currentPath.slice(0, 2), node]
        : [...currentPath, node],
    );
  };

  // 실제 카드 목록은 선택된 대분류/중분류로 한 번 더 좁힌다(서버는 subjectId까지만
  // 걸러서 응답했으므로 나머지는 이미 받아온 데이터에서 걸러낸다).
  const examHistoryItems = useMemo(() => {
    const filtered = subjectScopedSets.filter((item) => {
      if (selectedChapter1Id !== null && item.categoryLv1Id !== selectedChapter1Id) {
        return false;
      }
      if (selectedChapter2Id !== null && item.categoryLv2Id !== selectedChapter2Id) {
        return false;
      }
      return true;
    });
    return filtered.map(toExamHistoryDisplayItem);
  }, [subjectScopedSets, selectedChapter1Id, selectedChapter2Id]);

  const filteredWeaknessReports = useMemo(
    () =>
      weaknessReports.filter((item) => {
        if (selectedSubject && item.subject !== selectedSubject.label) {
          return false;
        }
        if (selectedPath.length === 2 && item.depth !== "Chapter1") {
          return false;
        }
        if (selectedPath.length === 3 && item.depth !== "Chapter2") {
          return false;
        }
        return true;
      }),
    [selectedSubject, selectedPath.length],
  );

  const updateMode = (nextMode: LogWeakMode) => {
    setMode(nextMode);
    router.setParams({ mode: nextMode });
    setIsSelecting(false);
    setSelectedExamIds([]);
    scrollRef.current?.scrollTo({ y: 0, animated: true });
  };

  const handleScroll = (event: NativeSyntheticEvent<NativeScrollEvent>) => {
    const { contentOffset, contentSize, layoutMeasurement } = event.nativeEvent;
    const maxScrollY = Math.max(
      0,
      contentSize.height - layoutMeasurement.height,
    );
    const isNearBottom =
      maxScrollY > 0 && contentOffset.y > maxScrollY - OVERSCROLL_BUFFER;
    const nextBackground = isNearBottom ? PANEL_GRAY : activePurple;

    setBounceBackground((currentBackground) =>
      currentBackground === nextBackground ? currentBackground : nextBackground,
    );
  };

  const toggleMode = () => updateMode(isWrongNote ? "weakness" : "wrongNote");

  const toggleSelectedExam = (id: number) => {
    if (!isSelecting) {
      return;
    }

    setSelectedExamIds((currentIds) =>
      currentIds.includes(id)
        ? currentIds.filter((currentId) => currentId !== id)
        : [...currentIds, id],
    );
  };

  const openExamReview = (item: ExamHistoryDisplayItem) => {
    if (isSelecting) {
      toggleSelectedExam(item.solveSessionId);
      return;
    }

    router.push({
      pathname: "/mistake-exam-review",
      params: { solveSessionId: item.solveSessionId.toString() },
    });
  };

  const handleAiButtonPress = () => {
    if (!isSelecting) {
      setIsSelecting(true);
      return;
    }

    if (selectedExamIds.length === 0) {
      return;
    }

    updateMode("weakness");
  };

  const cancelSelection = () => {
    setIsSelecting(false);
    setSelectedExamIds([]);
  };

  useEffect(() => {
    if (modeParam === "weakness") {
      setMode("weakness");
    }

    if (modeParam === "wrongNote") {
      setMode("wrongNote");
    }
  }, [modeParam]);

  useEffect(() => {
    const tabNavigation = navigation as unknown as TabPressNavigation;
    const unsubscribe = tabNavigation.addListener("tabPress", () => {
      scrollRef.current?.scrollTo({ y: 0, animated: true });
    });

    return unsubscribe;
  }, [navigation]);

  useEffect(() => {
    setBounceBackground(activePurple);
  }, [activePurple]);

  useEffect(() => {
    setIsLogWeakSelecting(isSelecting);

    return () => setIsLogWeakSelecting(false);
  }, [isSelecting, setIsLogWeakSelecting]);

  return (
    <View style={[styles.screen, { backgroundColor: activePurple }]}>
      <BlurView
        intensity={28}
        tint="dark"
        style={[
          styles.titleArea,
          {
            minHeight: headerHeight,
            paddingTop: insets.top,
          },
        ]}
      >
        <View
          style={[
            styles.headerOverlay,
            { backgroundColor: `${activePurple}D9` },
          ]}
        />
        {isSelecting && <View style={styles.selectionHeaderDim} />}

        <Text style={styles.title}>{title}</Text>

        <Pressable
          accessibilityRole="switch"
          accessibilityState={{ checked: !isWrongNote }}
          disabled={isSelecting}
          style={styles.modeSwitchArea}
          onPress={toggleMode}
        >
          <Text style={styles.switchLabel}>
            {isWrongNote ? "AI 분석하러가기" : "내 풀이 보러가기"}
          </Text>
          <View style={styles.switchTrack}>
            <View
              style={[
                styles.switchThumb,
                { backgroundColor: activePurple },
                !isWrongNote && styles.switchThumbRight,
              ]}
            />
          </View>
        </Pressable>
      </BlurView>

      <ScrollView
        bounces
        contentContainerStyle={[
          styles.scrollContent,
          { backgroundColor: activePurple },
          { minHeight: height + OVERSCROLL_BUFFER },
        ]}
        onScroll={handleScroll}
        ref={scrollRef}
        scrollEventThrottle={16}
        showsVerticalScrollIndicator={false}
        style={[styles.scroll, { backgroundColor: bounceBackground }]}
      >
        <View
          style={[
            styles.purpleArea,
            { backgroundColor: activePurple, paddingTop: headerHeight },
          ]}
        >
          <ScrollView
            horizontal
            showsHorizontalScrollIndicator={false}
            contentContainerStyle={styles.trailRow}
          >
            {row1Items.map((node, index) => {
              const isAllChip = index === 0;
              const isDimmedAll = isAllChip && selectedPath.length > 0;

              return (
                <Pressable
                  disabled={isSelecting}
                  key={`${node.id}-${index}`}
                  style={[
                    styles.trailChip,
                    isDimmedAll ? styles.trailChipAllDimmed : styles.trailChipActive,
                  ]}
                  onPress={() => selectFilterTrail(index)}
                >
                  <Text style={[styles.filterText, { color: activePurple }]}>
                    {node.label}
                  </Text>
                </Pressable>
              );
            })}
          </ScrollView>

          <ScrollView
            horizontal
            showsHorizontalScrollIndicator={false}
            contentContainerStyle={styles.choiceRow}
          >
            {row2Items.map((node) => {
              const isActive =
                row2Level === "chapter2" && selectedChapter2?.id === node.id;

              return (
                <Pressable
                  disabled={isSelecting}
                  key={node.id}
                  style={[styles.choiceChip, isActive && styles.activeWhiteTab]}
                  onPress={() => selectFilterChoice(node)}
                >
                  <Text
                    style={[
                      styles.filterText,
                      isActive && { color: activePurple },
                    ]}
                  >
                    {node.label}
                  </Text>
                </Pressable>
              );
            })}
          </ScrollView>

          {isSelecting && <View style={styles.selectionPurpleDim} />}
        </View>

        <View
          style={[
            styles.grayPanel,
            { minHeight: height + OVERSCROLL_BUFFER },
            isWrongNote && styles.grayPanelWithFloatingButton,
          ]}
        >
          {isWrongNote && isSelecting && <View style={styles.selectionDim} />}

          <StatsSummary accentColor={activePurple} isWrongNote={isWrongNote} />

          <View style={styles.divider} />

          <View style={[styles.cardList, isSelecting && styles.activeCardList]}>
            {isWrongNote
              ? examHistoryItems.map((item) => (
                  <ExamHistoryCard
                    accentColor={activePurple}
                    isSelecting={isSelecting}
                    isSelected={selectedExamIds.includes(item.solveSessionId)}
                    item={item}
                    key={item.solveSessionId}
                    onPress={() => openExamReview(item)}
                  />
                ))
              : filteredWeaknessReports.map((item) => (
                  <WeaknessReportCard
                    item={item}
                    key={item.id}
                    onPress={() =>
                      router.push({
                        pathname: "/weakness-report",
                        params: { reportId: item.id.toString() },
                      })
                    }
                  />
                ))}
          </View>
        </View>
      </ScrollView>

      {isWrongNote && (
        <View style={styles.aiButtonWrap}>
          <Pressable
            style={[
              styles.aiButton,
              isSelecting &&
                selectedExamIds.length > 0 && {
                  backgroundColor: activePurple,
                },
            ]}
            onPress={handleAiButtonPress}
          >
            <Text style={styles.aiButtonText}>
              {isSelecting
                ? selectedExamIds.length > 0
                  ? `AI 약점 분석표 만들기\n(선택한 문제집:${selectedExamIds.length})`
                  : "AI 분석할 문제집을 선택해주세요"
                : "AI 분석용 문제집 선택하기"}
            </Text>
          </Pressable>

          {isSelecting && (
            <Pressable
              accessibilityLabel="선택 취소"
              accessibilityRole="button"
              hitSlop={8}
              style={styles.cancelSelectionButton}
              onPress={cancelSelection}
            >
              <X color="#ffffff" size={18} strokeWidth={2.6} />
            </Pressable>
          )}
        </View>
      )}
    </View>
  );
}

function StatsSummary({
  accentColor,
  isWrongNote,
}: {
  accentColor: string;
  isWrongNote: boolean;
}) {
  const stats = isWrongNote
    ? [
        { value: conceptStats.totalCount.toString(), label: "총 개념" },
        {
          value: conceptStats.solvedConceptCount.toString(),
          label: "풀었어요",
        },
        {
          value: (
            conceptStats.totalCount - conceptStats.solvedConceptCount
          ).toString(),
          label: "아직 못풀었어요",
        },
      ]
    : [
        {
          value: weaknessReportStats.totalCount.toString(),
          label: "내 약점 분석표",
        },
        { value: weaknessReportStats.readCount.toString(), label: "완벽해요" },
        {
          value: (
            weaknessReportStats.totalCount - weaknessReportStats.readCount
          ).toString(),
          label: "아직 모자라요",
        },
      ];

  return (
    <View style={styles.statsRow}>
      {stats.map((item) => (
        <View key={item.label} style={styles.statItem}>
          <Text style={[styles.statValue, { color: accentColor }]}>
            {item.value}
          </Text>
          <Text style={styles.statLabel}>{item.label}</Text>
        </View>
      ))}
    </View>
  );
}

function ExamHistoryCard({
  accentColor,
  isSelecting,
  isSelected,
  item,
  onPress,
}: {
  accentColor: string;
  isSelecting: boolean;
  isSelected: boolean;
  item: ExamHistoryDisplayItem;
  onPress: () => void;
}) {
  return (
    <Pressable
      style={[styles.card, isSelected && { borderColor: accentColor }]}
      onPress={onPress}
    >
      <View style={styles.cardHeader}>
        <View style={styles.cardTitleRow}>
          <Text style={styles.subjectBadge}>{item.subject}</Text>
          <Text style={styles.dot}>·</Text>
          <Text style={styles.categoryText}>{item.category}</Text>
        </View>

        {isSelecting ? (
          <View
            style={[
              styles.selectCircle,
              isSelected && { backgroundColor: accentColor },
            ]}
          >
            {isSelected && <Check color="#ffffff" size={15} strokeWidth={3} />}
          </View>
        ) : (
          <ChevronRight color="#6b7280" size={22} strokeWidth={2.2} />
        )}
      </View>

      <View style={styles.dateRow}>
        <CalendarDays color="#6b7280" size={16} strokeWidth={2} />
        <Text style={styles.dateText}>{item.date}</Text>
      </View>

      <View style={styles.resultRow}>
        <View>
          <Text style={styles.wrongLabel}>Wrong</Text>
          <Text style={styles.wrongValue}>{item.wrong}</Text>
          <Text style={styles.scopePathText}>{item.scopePath.join("  >  ")}</Text>
        </View>
      </View>
    </Pressable>
  );
}

function WeaknessReportCard({
  item,
  onPress,
}: {
  item: WeaknessReportItem;
  onPress: () => void;
}) {
  const primaryExamNames = item.examNames.slice(0, 3);
  const secondaryExamNames = item.examNames.slice(3, 5);

  return (
    <Pressable style={styles.weaknessCard} onPress={onPress}>
      <View style={styles.cardHeader}>
        <Text style={styles.reportNumber}>#{item.id}</Text>

        <ChevronRight color="#6b7280" size={22} strokeWidth={2.2} />
      </View>

      <View style={styles.dateRow}>
        <CalendarDays color="#6b7280" size={16} strokeWidth={2} />
        <Text style={styles.dateText}>{item.date}</Text>
      </View>

      <View style={styles.reportExamRow}>
        <View style={styles.reportExamColumn}>
          {primaryExamNames.map((examName) => (
            <Text
              adjustsFontSizeToFit
              key={`primary-${examName}`}
              minimumFontScale={0.82}
              numberOfLines={1}
              style={styles.reportExamName}
            >
              {examName}
            </Text>
          ))}
        </View>
        {secondaryExamNames.length > 0 && (
          <View style={styles.reportExamColumn}>
            {secondaryExamNames.map((examName) => (
              <Text
                adjustsFontSizeToFit
                key={`secondary-${examName}`}
                minimumFontScale={0.82}
                numberOfLines={1}
                style={styles.reportExamName}
              >
                {examName}
              </Text>
            ))}
          </View>
        )}
        <Check
          color={item.isRead ? "#20D63A" : "#9ca3af"}
          size={24}
          strokeWidth={3}
        />
      </View>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  screen: {
    flex: 1,
    backgroundColor: BRAND_PURPLE,
  },
  titleArea: {
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
  },
  headerOverlay: {
    ...StyleSheet.absoluteFillObject,
    backgroundColor: "rgba(109, 93, 251, 0.85)",
  },
  title: {
    flex: 1,
    color: "#ffffff",
    fontFamily: FONT.pretendard,
    fontSize: 24,
    fontWeight: "700",
  },
  modeSwitchArea: {
    flexDirection: "row",
    alignItems: "center",
    gap: 10,
  },
  switchLabel: {
    color: "#ffffff",
    fontFamily: FONT.pretendard,
    fontSize: 12,
    fontWeight: "700",
  },
  switchTrack: {
    width: 52,
    height: 32,
    justifyContent: "center",
    borderRadius: 999,
    backgroundColor: "#ffffff",
    paddingHorizontal: 5,
  },
  switchThumb: {
    width: 17,
    height: 17,
    borderRadius: 999,
    backgroundColor: BRAND_PURPLE,
  },
  switchThumbRight: {
    alignSelf: "flex-end",
  },
  scroll: {
    flex: 1,
  },
  scrollContent: {
    backgroundColor: BRAND_PURPLE,
  },
  purpleArea: {
    position: "relative",
    paddingBottom: 18,
  },
  trailRow: {
    paddingHorizontal: 12,
    gap: 8,
  },
  trailChip: {
    height: 36,
    alignItems: "center",
    justifyContent: "center",
    borderRadius: 999,
    paddingHorizontal: 16,
  },
  trailChipActive: {
    backgroundColor: "#ffffff",
  },
  trailChipAllDimmed: {
    backgroundColor: "rgba(255, 255, 255, 0.55)",
  },
  activeWhiteTab: {
    backgroundColor: "#ffffff",
    borderColor: "#ffffff",
  },
  filterText: {
    color: "#ffffff",
    fontFamily: FONT.kufam,
    fontSize: 13,
    fontWeight: "500",
  },
  choiceRow: {
    paddingHorizontal: 12,
    marginTop: 12,
    gap: 8,
  },
  choiceChip: {
    height: 40,
    alignItems: "center",
    justifyContent: "center",
    borderRadius: 999,
    borderWidth: 1,
    borderColor: "rgba(255, 255, 255, 0.75)",
    paddingHorizontal: 16,
  },
  grayPanel: {
    position: "relative",
    minHeight: 620,
    borderTopLeftRadius: 18,
    borderTopRightRadius: 18,
    backgroundColor: PANEL_GRAY,
    paddingHorizontal: 32,
    paddingTop: 22,
    paddingBottom: 28,
  },
  grayPanelWithFloatingButton: {
    paddingBottom: 116,
  },
  statsRow: {
    flexDirection: "row",
    justifyContent: "space-between",
    paddingHorizontal: 8,
  },
  statItem: {
    minWidth: 88,
    alignItems: "center",
  },
  statValue: {
    color: BRAND_PURPLE,
    fontFamily: FONT.kufam,
    fontSize: 22,
    fontWeight: "700",
  },
  statLabel: {
    marginTop: 2,
    color: "#8a8a8a",
    fontFamily: FONT.pretendard,
    fontSize: 13,
    fontWeight: "700",
  },
  divider: {
    height: 1,
    marginTop: 24,
    marginBottom: 22,
    backgroundColor: "#c9c9c9",
  },
  cardList: {
    gap: 16,
  },
  activeCardList: {
    zIndex: 2,
    elevation: 2,
  },
  card: {
    minHeight: 158,
    borderRadius: 8,
    borderWidth: 1.5,
    borderColor: "transparent",
    backgroundColor: "#ffffff",
    paddingHorizontal: 20,
    paddingVertical: 14,
  },
  weaknessCard: {
    minHeight: 152,
    borderRadius: 8,
    backgroundColor: "#ffffff",
    paddingHorizontal: 20,
    paddingVertical: 14,
  },
  cardHeader: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
  },
  cardTitleRow: {
    flex: 1,
    flexDirection: "row",
    alignItems: "center",
  },
  subjectBadge: {
    overflow: "hidden",
    borderRadius: 3,
    backgroundColor: "#DCEBFF",
    color: "#0F7CFF",
    fontFamily: FONT.kufam,
    fontSize: 12,
    fontWeight: "600",
    paddingHorizontal: 8,
    paddingVertical: 5,
  },
  dot: {
    marginHorizontal: 7,
    color: "#9ca3af",
    fontFamily: FONT.pretendard,
    fontSize: 15,
  },
  categoryText: {
    color: "#6b7280",
    fontFamily: FONT.kufam,
    fontSize: 13,
    fontWeight: "500",
  },
  reportNumber: {
    color: "#6D5DFB",
    fontFamily: FONT.kufam,
    fontSize: 17,
    fontWeight: "700",
  },
  dateRow: {
    flexDirection: "row",
    alignItems: "center",
    gap: 6,
    marginTop: 9,
  },
  dateText: {
    color: "#7d7d7d",
    fontFamily: FONT.kufam,
    fontSize: 13,
    fontWeight: "500",
  },
  resultRow: {
    flex: 1,
    flexDirection: "row",
    alignItems: "flex-end",
    justifyContent: "space-between",
    marginTop: 18,
  },
  reportExamRow: {
    flexDirection: "row",
    alignItems: "flex-end",
    gap: 12,
    marginTop: 14,
  },
  reportExamColumn: {
    flex: 1,
    gap: 4,
  },
  reportExamName: {
    alignSelf: "flex-start",
    color: "#475569",
    fontFamily: FONT.kufam,
    fontSize: 13,
    fontWeight: "500",
    lineHeight: 19,
  },
  wrongLabel: {
    color: "#6b7280",
    fontFamily: FONT.kufam,
    fontSize: 13,
    fontWeight: "500",
  },
  wrongValue: {
    marginTop: 5,
    color: "#FF0000",
    fontFamily: FONT.kufam,
    fontSize: 15,
    fontWeight: "600",
  },
  scopePathText: {
    color: "#64748b",
    fontFamily: FONT.pretendard,
    fontSize: 11,
    fontWeight: "500",
    lineHeight: 17,
    marginTop: 8,
  },
  selectCircle: {
    alignItems: "center",
    justifyContent: "center",
    width: 24,
    height: 24,
    borderRadius: 999,
    borderWidth: 1.5,
    borderColor: "#cbd5e1",
    backgroundColor: "#ffffff",
  },
  aiButtonWrap: {
    position: "absolute",
    left: 50,
    right: 50,
    bottom: 12,
    zIndex: 20,
    minHeight: 48,
    flexDirection: "row",
    alignItems: "center",
    gap: 8,
    justifyContent: "center",
  },
  aiButton: {
    flex: 1,
    minHeight: 52,
    alignItems: "center",
    justifyContent: "center",
    borderRadius: 999,
    backgroundColor: "#8493A0",
    paddingHorizontal: 18,
  },
  aiButtonText: {
    color: "#ffffff",
    fontFamily: FONT.pretendard,
    fontSize: 14,
    fontWeight: "700",
    lineHeight: 19,
    textAlign: "center",
  },
  cancelSelectionButton: {
    alignItems: "center",
    justifyContent: "center",
    width: 42,
    height: 42,
    borderRadius: 999,
    backgroundColor: "rgba(15, 23, 42, 0.72)",
  },
  selectionDim: {
    ...StyleSheet.absoluteFillObject,
    zIndex: 1,
    borderTopLeftRadius: 18,
    borderTopRightRadius: 18,
    backgroundColor: "rgba(15, 23, 42, 0.36)",
  },
  selectionHeaderDim: {
    ...StyleSheet.absoluteFillObject,
    zIndex: 1,
    backgroundColor: "rgba(15, 23, 42, 0.28)",
  },
  selectionPurpleDim: {
    ...StyleSheet.absoluteFillObject,
    zIndex: 1,
    backgroundColor: "rgba(15, 23, 42, 0.32)",
  },
});
