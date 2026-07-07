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
import { useLogWeakSelection } from "../../src/features/logWeak/LogWeakSelectionContext";

type LogWeakMode = "wrongNote" | "weakness";
type DepthFilter = "Category1" | "Category2" | "concept";

type TabPressNavigation = {
  addListener: (eventName: "tabPress", callback: () => void) => () => void;
};

type ExamHistoryItem = {
  id: number;
  subject: string;
  depth: DepthFilter;
  category: string;
  examType: "과목 모의고사" | "챕터 모의고사";
  date: string;
  wrong: string;
};

type WeaknessReportItem = {
  id: number;
  subject: string;
  depth: DepthFilter;
  category: string;
  date: string;
  wrong: string;
  completed: boolean;
};

const HEADER_HEIGHT = 60;
const OVERSCROLL_BUFFER = 96;
const BRAND_PURPLE = "#6D5DFB";
const WEAKNESS_PURPLE = "#4E44A4";
const PANEL_GRAY = "#F3F3F3";

const subjects = [
  "Data Structure",
  "Algorithm",
  "Database",
  "Network",
  "Operating System",
  "Computer Architecture",
];
const depthFilters: DepthFilter[] = ["Category1", "Category2", "concept"];

const conceptStats = {
  total: 500,
  completed: 100,
  incomplete: 400,
};

const examHistoryItems: ExamHistoryItem[] = [
  {
    id: 1,
    subject: "Data Structure",
    depth: "Category1",
    category: "Arrays",
    examType: "과목 모의고사",
    date: "26-04-25",
    wrong: "6/10",
  },
  {
    id: 2,
    subject: "Data Structure",
    depth: "Category1",
    category: "Stacks",
    examType: "챕터 모의고사",
    date: "26-04-25",
    wrong: "6/10",
  },
  {
    id: 3,
    subject: "Data Structure",
    depth: "Category2",
    category: "Queues",
    examType: "챕터 모의고사",
    date: "26-04-24",
    wrong: "4/10",
  },
  {
    id: 4,
    subject: "Algorithm",
    depth: "Category1",
    category: "Sorting",
    examType: "과목 모의고사",
    date: "26-04-23",
    wrong: "7/10",
  },
];

const weaknessReports: WeaknessReportItem[] = [
  {
    id: 1,
    subject: "Data Structure",
    depth: "Category1",
    category: "Arrays",
    date: "26-04-25",
    wrong: "6/10",
    completed: true,
  },
  {
    id: 2,
    subject: "Data Structure",
    depth: "Category1",
    category: "Stacks",
    date: "26-04-25",
    wrong: "6/10",
    completed: true,
  },
  {
    id: 3,
    subject: "Data Structure",
    depth: "Category2",
    category: "Queues",
    date: "26-04-24",
    wrong: "4/10",
    completed: false,
  },
];

export default function LogWeakScreen() {
  const navigation = useNavigation();
  const insets = useSafeAreaInsets();
  const { height } = useWindowDimensions();
  const scrollRef = useRef<ScrollView>(null);
  const { mode: modeParam } = useLocalSearchParams<{ mode?: string }>();
  const { setIsLogWeakSelecting } = useLogWeakSelection();
  const [mode, setMode] = useState<LogWeakMode>("wrongNote");
  const [activeSubject, setActiveSubject] = useState(subjects[0]);
  const [activeDepth, setActiveDepth] = useState<DepthFilter>(depthFilters[0]);
  const [bounceBackground, setBounceBackground] = useState(BRAND_PURPLE);
  const [isSelecting, setIsSelecting] = useState(false);
  const [selectedExamIds, setSelectedExamIds] = useState<number[]>([]);
  const isWrongNote = mode === "wrongNote";
  const title = isWrongNote ? "Mistake Notes" : "Weakness Notes";
  const tabLabel = isWrongNote ? "오답노트" : "약점분석";
  const headerHeight = HEADER_HEIGHT + insets.top;
  const activePurple = isWrongNote ? BRAND_PURPLE : WEAKNESS_PURPLE;

  const filteredExamItems = useMemo(
    () =>
      examHistoryItems.filter(
        (item) => item.subject === activeSubject && item.depth === activeDepth,
      ),
    [activeDepth, activeSubject],
  );

  const filteredWeaknessReports = useMemo(
    () =>
      weaknessReports.filter(
        (item) => item.subject === activeSubject && item.depth === activeDepth,
      ),
    [activeDepth, activeSubject],
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
    navigation.setOptions({ title, tabBarLabel: tabLabel });
  }, [navigation, tabLabel, title]);

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
            contentContainerStyle={styles.subjectTabs}
          >
            {subjects.map((subject, index) => {
              const isActive = activeSubject === subject;
              const isFirst = index === 0;
              const isLast = index === subjects.length - 1;

              return (
                <Pressable
                  disabled={isSelecting}
                  key={subject}
                  style={[
                    styles.subjectTab,
                    isFirst && styles.subjectTabFirst,
                    isLast && styles.subjectTabLast,
                    isActive && styles.activeWhiteTab,
                  ]}
                  onPress={() => setActiveSubject(subject)}
                >
                  <Text
                    style={[
                      styles.filterText,
                      isActive && { color: activePurple },
                    ]}
                  >
                    {subject}
                  </Text>
                </Pressable>
              );
            })}
          </ScrollView>

          <View style={styles.categoryTabs}>
            {depthFilters.map((depth) => {
              const isActive = activeDepth === depth;

              return (
                <Pressable
                  disabled={isSelecting}
                  key={depth}
                  style={[styles.categoryTab, isActive && styles.activeWhiteTab]}
                  onPress={() => setActiveDepth(depth)}
                >
                  <Text
                    style={[
                      styles.filterText,
                      isActive && { color: activePurple },
                    ]}
                  >
                    {depth}
                  </Text>
                </Pressable>
              );
            })}
          </View>

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
              ? filteredExamItems.map((item) => (
                  <ExamHistoryCard
                    accentColor={activePurple}
                    isSelecting={isSelecting}
                    isSelected={selectedExamIds.includes(item.id)}
                    item={item}
                    key={item.id}
                    onPress={() => toggleSelectedExam(item.id)}
                  />
                ))
              : filteredWeaknessReports.map((item) => (
                  <WeaknessReportCard item={item} key={item.id} />
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
        { value: conceptStats.total.toString(), label: "총 개념" },
        { value: conceptStats.completed.toString(), label: "풀었어요" },
        { value: conceptStats.incomplete.toString(), label: "아직 못풀었어요" },
      ]
    : [
        { value: "15", label: "내 약점 분석표" },
        { value: "2", label: "완벽해요" },
        { value: "13", label: "아직 모자라요" },
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
  item: ExamHistoryItem;
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
        <Text style={styles.examTypeText}>{item.examType}</Text>
      </View>

      <View style={styles.resultRow}>
        <View>
          <Text style={styles.wrongLabel}>Wrong</Text>
          <Text style={styles.wrongValue}>{item.wrong}</Text>
        </View>
      </View>
    </Pressable>
  );
}

function WeaknessReportCard({ item }: { item: WeaknessReportItem }) {
  return (
    <Pressable style={styles.card}>
      <View style={styles.cardHeader}>
        <View style={styles.cardTitleRow}>
          <Text style={styles.subjectBadge}>{item.subject}</Text>
          <Text style={styles.dot}>·</Text>
          <Text style={styles.categoryText}>{item.category}</Text>
        </View>

        <ChevronRight color="#6b7280" size={22} strokeWidth={2.2} />
      </View>

      <View style={styles.dateRow}>
        <CalendarDays color="#6b7280" size={16} strokeWidth={2} />
        <Text style={styles.dateText}>{item.date}</Text>
      </View>

      <View style={styles.resultRow}>
        <View>
          <Text style={styles.wrongLabel}>Wrong</Text>
          <Text style={styles.wrongValue}>{item.wrong}</Text>
        </View>

        <Check
          color={item.completed ? "#20D63A" : "#9ca3af"}
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
    fontSize: 17,
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
  subjectTabs: {
    paddingHorizontal: 12,
  },
  subjectTab: {
    minWidth: 112,
    height: 40,
    alignItems: "center",
    justifyContent: "center",
    borderWidth: 1,
    borderColor: "rgba(255, 255, 255, 0.75)",
    paddingHorizontal: 16,
  },
  subjectTabFirst: {
    borderTopLeftRadius: 999,
    borderBottomLeftRadius: 999,
  },
  subjectTabLast: {
    borderTopRightRadius: 999,
    borderBottomRightRadius: 999,
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
  categoryTabs: {
    flexDirection: "row",
    alignSelf: "flex-start",
    overflow: "hidden",
    marginTop: 18,
    marginHorizontal: 12,
    borderWidth: 1,
    borderColor: "#ffffff",
    borderRadius: 999,
  },
  categoryTab: {
    minWidth: 106,
    height: 40,
    alignItems: "center",
    justifyContent: "center",
    borderRightWidth: 1,
    borderRightColor: "rgba(255, 255, 255, 0.65)",
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
    minHeight: 136,
    borderRadius: 8,
    borderWidth: 1.5,
    borderColor: "transparent",
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
  examTypeText: {
    color: "#94a3b8",
    fontFamily: FONT.pretendard,
    fontSize: 12,
    fontWeight: "700",
  },
  resultRow: {
    flex: 1,
    flexDirection: "row",
    alignItems: "flex-end",
    justifyContent: "space-between",
    marginTop: 18,
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
