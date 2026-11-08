import { useQueryClient } from "@tanstack/react-query";
import { BlurView } from "expo-blur";
import { router, useLocalSearchParams, useNavigation } from "expo-router";
import { CalendarDays, Check, ChevronRight, Trash2, X } from "lucide-react-native";
import { useEffect, useMemo, useRef, useState } from "react";
import {
  type NativeScrollEvent,
  type NativeSyntheticEvent,
  ActivityIndicator,
  Alert,
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
import {
  useCategoryLv1List,
  useCategoryLv2List,
  useSubjects,
} from "../../src/features/content/useContentQueries";
import { sessionTypeLabel } from "../../src/features/logWeak/types";
import type { CompletedProblemSetSummary } from "../../src/features/logWeak/types";
import { useCompletedProblemSets } from "../../src/features/logWeak/useCompletedProblemSets";
import { useLogWeakSelection } from "../../src/features/logWeak/LogWeakSelectionContext";
import { useConceptCountSummary } from "../../src/features/progress";
import type { ConceptCountSummary } from "../../src/features/progress";
import { useLanguage } from "../../src/shared/language/LanguageContext";
import { spotlightRef } from "../../src/features/onboarding/spotlightRegistry";
import { refetchActiveQueries } from "../../src/shared/api/queryClient";
import {
  useEligibleSolveSessions,
  useWeaknessReports,
  weaknessApi,
} from "../../src/features/weakness";
import type {
  AnalysisScopeType,
  EligibleSolveSession,
  WeaknessAnalysisReportSummary,
} from "../../src/features/weakness";

type LogWeakMode = "wrongNote" | "weakness";

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
  /** "맞은 문제 수/전체 문제 수". 틀린 개수 기준이면 숫자가 정답 수인지 오답
   * 수인지 헷갈린다는 피드백이 있어 정답 수 기준으로 통일한다. */
  correct: string;
};

function formatCompletedDate(isoString: string): string {
  return isoString.slice(0, 10).replace(/-/g, ".");
}

type LocalizedNameLookups = {
  subjectNameById: Map<number, string>;
  categoryLv1NameById: Map<number, string>;
  categoryLv2NameById: Map<number, string>;
};

function toExamHistoryDisplayItem(
  summary: CompletedProblemSetSummary,
  lookups: LocalizedNameLookups,
): ExamHistoryDisplayItem {
  const subjectName =
    (summary.subjectId !== null
      ? lookups.subjectNameById.get(summary.subjectId)
      : undefined) ?? summary.subjectName;
  const categoryLv1Name =
    (summary.categoryLv1Id !== null
      ? lookups.categoryLv1NameById.get(summary.categoryLv1Id)
      : undefined) ?? summary.categoryLv1Name;
  const categoryLv2Name =
    (summary.categoryLv2Id !== null
      ? lookups.categoryLv2NameById.get(summary.categoryLv2Id)
      : undefined) ?? summary.categoryLv2Name;
  // 개념명은 이 화면에서 콘텐츠 카탈로그를 별도로 불러오지 않아 언어 설정을
  // 적용하지 않는다(필터도 개념 단계까지는 내려가지 않는 것과 같은 범위).
  const scopePath = [
    subjectName,
    categoryLv1Name,
    categoryLv2Name,
    summary.conceptName,
  ].filter((name): name is string => Boolean(name));

  return {
    solveSessionId: summary.solveSessionId,
    subject: subjectName ?? "",
    category: scopePath[scopePath.length - 1] ?? "",
    examType: sessionTypeLabel[summary.sessionType],
    scopePath,
    date: formatCompletedDate(summary.completedAt),
    correct: `${summary.correctCount}/${summary.totalQuestionCount}`,
  };
}

type AnalysisScope = {
  analysisScopeType: AnalysisScopeType;
  subjectId: number;
  categoryLv1Id: number | null;
  categoryLv2Id: number | null;
  conceptId: number | null;
};

// 선택한 회차들이 공유하는 가장 좁은 범위를 찾는다. 과목이 다르면(백엔드가
// 어차피 WEAKNESS_MIXED_SUBJECT로 막지만) 여기서 먼저 걸러 안내한다.
function deriveAnalysisScope(sessions: EligibleSolveSession[]): AnalysisScope | null {
  const [first, ...rest] = sessions;
  if (!first || first.subjectId === null) {
    return null;
  }
  if (!rest.every((session) => session.subjectId === first.subjectId)) {
    return null;
  }

  const categoryLv1Id = rest.every(
    (session) => session.categoryLv1Id === first.categoryLv1Id,
  )
    ? first.categoryLv1Id
    : null;
  const categoryLv2Id =
    categoryLv1Id !== null &&
    rest.every((session) => session.categoryLv2Id === first.categoryLv2Id)
      ? first.categoryLv2Id
      : null;
  const conceptId =
    categoryLv2Id !== null &&
    rest.every((session) => session.conceptId === first.conceptId)
      ? first.conceptId
      : null;

  const analysisScopeType: AnalysisScopeType =
    conceptId !== null
      ? "CONCEPT"
      : categoryLv2Id !== null
        ? "CATEGORY_LV2"
        : categoryLv1Id !== null
          ? "CATEGORY_LV1"
          : "SUBJECT";

  return {
    analysisScopeType,
    subjectId: first.subjectId,
    categoryLv1Id,
    categoryLv2Id,
    conceptId,
  };
}

const HEADER_HEIGHT = 60;
const OVERSCROLL_BUFFER = 96;
/** 한 분석표에 넣을 수 있는 회차 수(FR-WEAK-02). 서버도 같은 값으로 막지만, 여기서 먼저 알려준다. */
const MIN_SELECTION = 1;
const MAX_SELECTION = 5;
const BRAND_PURPLE = "#6D5DFB";
const WEAKNESS_PURPLE = "#4E44A4";
const PANEL_GRAY = "#F3F3F3";

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
  const [createReportError, setCreateReportError] = useState<string | null>(null);
  const [deletingReportId, setDeletingReportId] = useState<number | null>(null);
  const queryClient = useQueryClient();
  const isWrongNote = mode === "wrongNote";
  const title = isWrongNote ? "Mistake Notes" : "Weakness Notes";
  const headerHeight = HEADER_HEIGHT + insets.top;
  const activePurple = isWrongNote ? BRAND_PURPLE : WEAKNESS_PURPLE;

  const { accessToken } = useAuth();
  const { language } = useLanguage();

  // Row1(선택된 필터 목록)은 All + selectedPath. Row2(다음 단계 후보)는 selectedPath 깊이에 따라 달라진다.
  const [selectedSubject, selectedChapter1, selectedChapter2] = selectedPath;
  const selectedSubjectId = selectedSubject ? Number(selectedSubject.id) : null;
  const selectedChapter1Id = selectedChapter1 ? Number(selectedChapter1.id) : null;
  const selectedChapter2Id = selectedChapter2 ? Number(selectedChapter2.id) : null;

  // 과목은 콘텐츠 카탈로그 기준으로 항상 전부 보여준다. 완료 기록이 하나도
  // 없는 과목이라도 필터로는 선택할 수 있어야 하기 때문이다.
  const { data: subjects = [] } = useSubjects();
  // 완료 기록 API(CompletedProblemSetSummary)에는 영문명이 없어서, 언어
  // 설정에 맞는 이름을 보여주려면 콘텐츠 카탈로그를 id 기준으로 별도 조회해
  // 이름만 가져다 쓴다. 대분류/중분류를 "필터 후보로 보여줄지"는 여전히
  // 완료 기록(subjectScopedSets) 기준으로 정한다 — 이 조회는 이름 조회용일 뿐이다.
  const { data: categoryLv1List = [] } = useCategoryLv1List(selectedSubjectId);
  const { data: categoryLv2List = [] } = useCategoryLv2List(selectedChapter1Id);
  const completedProblemSetsQuery = useCompletedProblemSets(accessToken, {
    subjectId: selectedSubjectId ?? undefined,
  });
  // AI 분석용 문제집 선택 중에는 모의고사만 대상으로 하는 별도 후보 목록을 쓴다
  // (오답노트 목록은 개념 문제집까지 섞여 있어 분석 대상으로 그대로 쓸 수 없음).
  const eligibleSessionsQuery = useEligibleSolveSessions(
    accessToken,
    {
      subjectId: selectedSubjectId ?? undefined,
      categoryLv1Id: selectedChapter1Id ?? undefined,
      categoryLv2Id: selectedChapter2Id ?? undefined,
    },
    isSelecting,
  );
  const eligibleSessions = useMemo(
    () => eligibleSessionsQuery.data ?? [],
    [eligibleSessionsQuery.data],
  );
  const subjectScopedSets = useMemo(
    () => completedProblemSetsQuery.data ?? [],
    [completedProblemSetsQuery.data],
  );

  const subjectNameById = useMemo(() => {
    const map = new Map<number, string>();
    for (const subject of subjects) {
      map.set(
        subject.subjectId,
        language === "en" ? subject.subjectNameEn : subject.subjectName,
      );
    }
    return map;
  }, [subjects, language]);

  const categoryLv1NameById = useMemo(() => {
    const map = new Map<number, string>();
    for (const category of categoryLv1List) {
      map.set(
        category.categoryLv1Id,
        language === "en" ? category.categoryLv1NameEn : category.categoryLv1Name,
      );
    }
    return map;
  }, [categoryLv1List, language]);

  const categoryLv2NameById = useMemo(() => {
    const map = new Map<number, string>();
    for (const category of categoryLv2List) {
      map.set(
        category.categoryLv2Id,
        language === "en" ? category.categoryLv2NameEn : category.categoryLv2Name,
      );
    }
    return map;
  }, [categoryLv2List, language]);

  // 필터 후보로 보여줄지는 완료 기록(subjectScopedSets) 기준으로 정하되,
  // 나열 순서는 문제집 탭과 똑같이 콘텐츠 카탈로그의 sortOrder를 따른다
  // (완료 시각 순서로 나열되면 문제집 탭과 순서가 달라 보인다).
  const categoryLv1IdsWithRecords = useMemo(() => {
    const ids = new Set<number>();
    for (const item of subjectScopedSets) {
      if (item.categoryLv1Id !== null) {
        ids.add(item.categoryLv1Id);
      }
    }
    return ids;
  }, [subjectScopedSets]);

  const categoryLv2IdsWithRecords = useMemo(() => {
    const ids = new Set<number>();
    for (const item of subjectScopedSets) {
      if (item.categoryLv1Id === selectedChapter1Id && item.categoryLv2Id !== null) {
        ids.add(item.categoryLv2Id);
      }
    }
    return ids;
  }, [subjectScopedSets, selectedChapter1Id]);

  const categoryLv1Options = useMemo(() => {
    return [...categoryLv1List]
      .sort((a, b) => a.sortOrder - b.sortOrder)
      .filter((category) => categoryLv1IdsWithRecords.has(category.categoryLv1Id))
      .map((category) => ({
        id: String(category.categoryLv1Id),
        label: categoryLv1NameById.get(category.categoryLv1Id) ?? category.categoryLv1Name,
      }));
  }, [categoryLv1List, categoryLv1IdsWithRecords, categoryLv1NameById]);

  const categoryLv2Options = useMemo(() => {
    return [...categoryLv2List]
      .sort((a, b) => a.sortOrder - b.sortOrder)
      .filter((category) => categoryLv2IdsWithRecords.has(category.categoryLv2Id))
      .map((category) => ({
        id: String(category.categoryLv2Id),
        label: categoryLv2NameById.get(category.categoryLv2Id) ?? category.categoryLv2Name,
      }));
  }, [categoryLv2List, categoryLv2IdsWithRecords, categoryLv2NameById]);

  const row1Items: FilterNode[] = [{ id: "all", label: "All" }, ...selectedPath];
  const row2Level: "subject" | "chapter1" | "chapter2" = !selectedSubject
    ? "subject"
    : !selectedChapter1
      ? "chapter1"
      : "chapter2";
  const row2Items: FilterNode[] = !selectedSubject
    ? [...subjects]
        .sort((a, b) => a.sortOrder - b.sortOrder)
        .map((subject) => ({
          id: String(subject.subjectId),
          label: language === "en" ? subject.subjectNameEn : subject.subjectName,
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
    return filtered.map((item) =>
      toExamHistoryDisplayItem(item, {
        subjectNameById,
        categoryLv1NameById,
        categoryLv2NameById,
      }),
    );
  }, [
    subjectScopedSets,
    selectedChapter1Id,
    selectedChapter2Id,
    subjectNameById,
    categoryLv1NameById,
    categoryLv2NameById,
  ]);

  const conceptCountSummaryQuery = useConceptCountSummary(accessToken);

  const weaknessReportsQuery = useWeaknessReports(accessToken);
  const weaknessReports = useMemo(
    () => weaknessReportsQuery.data ?? [],
    [weaknessReportsQuery.data],
  );

  // analysisScopeNameSnapshot은 현재 과목명만 담고 있어(백엔드가 아직 대분류/
  // 중분류까지는 스냅샷하지 않음), 대분류/중분류 필터는 적용할 수 없고 과목
  // 단위로만 좁힐 수 있다.
  const filteredWeaknessReports = useMemo(
    () =>
      weaknessReports.filter((item) => {
        if (selectedSubject && item.analysisScopeNameSnapshot !== selectedSubject.label) {
          return false;
        }
        return true;
      }),
    [weaknessReports, selectedSubject],
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

  // 분석표 목록 화면(SCR-WEAK-01)에서 바로 새 분석표를 시작한다. 대상 선택 목록은
  // 반대 모드에 있어서, 모드를 바꾸고 선택 단계까지 한 번에 열어 준다.
  const startNewReport = () => {
    setMode("wrongNote");
    router.setParams({ mode: "wrongNote" });
    setSelectedExamIds([]);
    setCreateReportError(null);
    setIsSelecting(true);
    scrollRef.current?.scrollTo({ y: 0, animated: true });
  };

  const toggleSelectedExam = (id: number) => {
    if (!isSelecting) {
      return;
    }

    setSelectedExamIds((currentIds) => {
      if (currentIds.includes(id)) {
        setCreateReportError(null);
        return currentIds.filter((currentId) => currentId !== id);
      }
      // 상한을 넘겨 보내면 서버가 막지만, 사용자는 두 화면을 지나서야 실패를 본다.
      if (currentIds.length >= MAX_SELECTION) {
        setCreateReportError(`문제집은 최대 ${MAX_SELECTION}개까지 선택할 수 있어요.`);
        return currentIds;
      }
      setCreateReportError(null);
      return [...currentIds, id];
    });
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
      setCreateReportError(null);
      return;
    }

    if (selectedExamIds.length === 0) {
      return;
    }

    const selectedSessions = eligibleSessions.filter((session) =>
      selectedExamIds.includes(session.solveSessionId),
    );
    const scope = deriveAnalysisScope(selectedSessions);
    if (!scope) {
      setCreateReportError("같은 과목의 모의고사만 함께 분석할 수 있어요.");
      return;
    }

    setCreateReportError(null);
    const totals = selectedSessions.reduce(
      (sum, session) => ({
        totalQuestionCount: sum.totalQuestionCount + session.totalQuestionCount,
        totalWrongCount: sum.totalWrongCount + session.wrongCount,
        totalLongTimeCount: sum.totalLongTimeCount + session.longTimeCount,
        totalHintUsedCount: sum.totalHintUsedCount + session.hintUsedCount,
        totalAnswerChangedCount: sum.totalAnswerChangedCount + session.answerChangedCount,
      }),
      {
        totalQuestionCount: 0,
        totalWrongCount: 0,
        totalLongTimeCount: 0,
        totalHintUsedCount: 0,
        totalAnswerChangedCount: 0,
      },
    );

    router.push({
      pathname: "/weakness-confirm",
      params: {
        solveSessionIds: JSON.stringify(selectedExamIds),
        analysisScopeType: scope.analysisScopeType,
        subjectId: String(scope.subjectId),
        categoryLv1Id: scope.categoryLv1Id !== null ? String(scope.categoryLv1Id) : undefined,
        categoryLv2Id: scope.categoryLv2Id !== null ? String(scope.categoryLv2Id) : undefined,
        conceptId: scope.conceptId !== null ? String(scope.conceptId) : undefined,
        selectedSessionCount: String(selectedSessions.length),
        totalQuestionCount: String(totals.totalQuestionCount),
        totalWrongCount: String(totals.totalWrongCount),
        totalLongTimeCount: String(totals.totalLongTimeCount),
        totalHintUsedCount: String(totals.totalHintUsedCount),
        totalAnswerChangedCount: String(totals.totalAnswerChangedCount),
      },
    });
  };

  const cancelSelection = () => {
    setIsSelecting(false);
    setSelectedExamIds([]);
    setCreateReportError(null);
  };

  // 분석표는 코인 100을 써서 만든 것이고, 지운 뒤 같은 회차로 다시 만들면 다시 차감된다
  // (서버 재사용 조건이 "삭제되지 않은 완료 분석표"라서). 한 번 눌러 사라지면 안 된다.
  const confirmDeleteReport = (item: WeaknessAnalysisReportSummary) => {
    if (deletingReportId !== null) {
      return;
    }
    Alert.alert(
      "분석표를 삭제할까요?",
      `${item.title}\n\n삭제하면 되돌릴 수 없어요. 같은 문제집으로 다시 만들려면 코인 ${item.coinCost}이 다시 필요합니다.`,
      [
        { text: "취소", style: "cancel" },
        {
          text: "삭제",
          style: "destructive",
          onPress: () => {
            void handleDeleteReport(item.analysisReportId);
          },
        },
      ],
    );
  };

  const handleDeleteReport = async (analysisReportId: number) => {
    if (!accessToken || deletingReportId !== null) {
      return;
    }
    setDeletingReportId(analysisReportId);
    try {
      await weaknessApi.deleteReport(analysisReportId, accessToken);
      queryClient.setQueryData<WeaknessAnalysisReportSummary[]>(
        ["weakness", "reports"],
        (current) =>
          current?.filter((report) => report.analysisReportId !== analysisReportId),
      );
    } catch (error) {
      console.error("[weakness] 분석표 삭제 실패", error);
    } finally {
      setDeletingReportId(null);
    }
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
      refetchActiveQueries();
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
            styles.grayPanelWithFloatingButton,
          ]}
        >
          {isWrongNote && isSelecting && <View style={styles.selectionDim} />}

          <StatsSummary
            accentColor={activePurple}
            conceptCountSummary={conceptCountSummaryQuery.data}
            isWrongNote={isWrongNote}
            weaknessReports={filteredWeaknessReports}
          />

          <View style={styles.divider} />

          {isWrongNote && isSelecting && eligibleSessionsQuery.isLoading ? (
            <ActivityIndicator
              color={activePurple}
              size="large"
              style={styles.eligibleLoading}
            />
          ) : null}

          <View style={[styles.cardList, isSelecting && styles.activeCardList]}>
            {isWrongNote
              ? isSelecting
                ? eligibleSessions.map((session) => (
                    <EligibleSessionCard
                      accentColor={activePurple}
                      isSelected={selectedExamIds.includes(session.solveSessionId)}
                      item={session}
                      key={session.solveSessionId}
                      onPress={() => toggleSelectedExam(session.solveSessionId)}
                    />
                  ))
                : examHistoryItems.map((item) => (
                    <ExamHistoryCard
                      accentColor={activePurple}
                      isSelecting={isSelecting}
                      isSelected={selectedExamIds.includes(item.solveSessionId)}
                      item={item}
                      key={item.solveSessionId}
                      onPress={() => openExamReview(item)}
                    />
                  ))
              : weaknessReportsQuery.isLoading ? (
                  <ActivityIndicator
                    color={activePurple}
                    size="large"
                    style={styles.eligibleLoading}
                  />
                ) : weaknessReportsQuery.isError ? (
                  <View style={styles.listStateBox}>
                    <Text style={styles.listStateTitle}>분석표를 불러오지 못했어요.</Text>
                    <Text style={styles.listStateText}>
                      네트워크 연결을 확인한 뒤 다시 시도해 주세요.
                    </Text>
                    <Pressable
                      style={[styles.listStateButton, { borderColor: activePurple }]}
                      onPress={() => weaknessReportsQuery.refetch()}
                    >
                      <Text style={[styles.listStateButtonText, { color: activePurple }]}>
                        다시 시도
                      </Text>
                    </Pressable>
                  </View>
                ) : filteredWeaknessReports.length === 0 ? (
                  <View style={styles.listStateBox}>
                    <Text style={styles.listStateTitle}>
                      {selectedSubject ? "이 과목의 분석표가 없어요." : "아직 만든 분석표가 없어요."}
                    </Text>
                    <Text style={styles.listStateText}>
                      {selectedSubject
                        ? "다른 과목을 고르거나 새 분석표를 만들어 보세요."
                        : "완료한 모의고사를 골라 첫 약점 분석표를 만들어 보세요."}
                    </Text>
                  </View>
                ) : (
                  filteredWeaknessReports.map((item) => (
                    <WeaknessReportCard
                      isDeleting={deletingReportId === item.analysisReportId}
                      item={item}
                      key={item.analysisReportId}
                      onDelete={() => confirmDeleteReport(item)}
                      onPress={() =>
                        router.push({
                          pathname: "/weakness-report",
                          params: { reportId: item.analysisReportId.toString() },
                        })
                      }
                    />
                  ))
                )}
          </View>
        </View>
      </ScrollView>

      {isWrongNote && isSelecting && !createReportError ? (
        <Text style={styles.selectionHintText}>
          같은 과목의 모의고사를 {MIN_SELECTION}~{MAX_SELECTION}개까지 고를 수 있어요.
        </Text>
      ) : null}

      {isWrongNote && createReportError ? (
        <Text style={styles.createReportErrorText}>{createReportError}</Text>
      ) : null}

      {/* 분석표 목록 화면에는 생성 진입이 없어, 홈에서 들어온 사용자가 만들 방법을 못 찾았다. */}
      {!isWrongNote && (
        <View style={styles.aiButtonWrap}>
          <Pressable
            ref={spotlightRef("createReport")}
            style={[styles.aiButton, { backgroundColor: activePurple }]}
            onPress={startNewReport}
          >
            <Text style={styles.aiButtonText}>새 약점 분석표 만들기</Text>
          </Pressable>
        </View>
      )}

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
  conceptCountSummary,
  isWrongNote,
  weaknessReports,
}: {
  accentColor: string;
  conceptCountSummary: ConceptCountSummary | undefined;
  isWrongNote: boolean;
  weaknessReports: WeaknessAnalysisReportSummary[];
}) {
  const totalAnalyzedQuestions = weaknessReports.reduce(
    (sum, report) => sum + report.totalQuestionCount,
    0,
  );
  // "발견한 약점"은 약점으로 잡힌 개념 수다. 예전엔 오답 수를 더하고 있어 라벨과 값이 어긋났다.
  const totalWeaknessesFound = weaknessReports.reduce(
    (sum, report) => sum + report.weaknessConceptCount,
    0,
  );

  const stats = isWrongNote
    ? [
        {
          value: (conceptCountSummary?.totalConceptCount ?? 0).toString(),
          label: "총 개념",
        },
        {
          value: (conceptCountSummary?.passedConceptCount ?? 0).toString(),
          label: "풀었어요",
        },
        {
          value: (conceptCountSummary?.notPassedConceptCount ?? 0).toString(),
          label: "아직 못풀었어요",
        },
      ]
    : [
        {
          value: weaknessReports.length.toString(),
          label: "내 약점 분석표",
        },
        { value: totalAnalyzedQuestions.toString(), label: "분석한 문제" },
        { value: totalWeaknessesFound.toString(), label: "발견한 약점" },
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
          <Text style={styles.resultLabel}>Correct</Text>
          <Text style={styles.resultValue}>{item.correct}</Text>
          <Text style={styles.scopePathText}>{item.scopePath.join("  >  ")}</Text>
        </View>
      </View>
    </Pressable>
  );
}

// AI 분석용 문제집 선택 화면 전용 카드. eligible-solve-sessions는 모의고사만
// 후보로 내려주며, 회차별로 선택 가능 여부(eligible)와 안내 문구를 함께 준다.
function EligibleSessionCard({
  accentColor,
  isSelected,
  item,
  onPress,
}: {
  accentColor: string;
  isSelected: boolean;
  item: EligibleSolveSession;
  onPress: () => void;
}) {
  const scopePath = [item.subjectName, item.categoryLv1Name, item.categoryLv2Name].filter(
    (name): name is string => Boolean(name),
  );

  return (
    <Pressable
      disabled={!item.eligible}
      style={[
        styles.card,
        isSelected && { borderColor: accentColor },
        !item.eligible && styles.ineligibleCard,
      ]}
      onPress={onPress}
    >
      <View style={styles.cardHeader}>
        <View style={styles.cardTitleRow}>
          <Text style={styles.subjectBadge}>{item.subjectName ?? ""}</Text>
          <Text style={styles.dot}>·</Text>
          <Text style={styles.categoryText}>{item.title}</Text>
        </View>

        <View
          style={[
            styles.selectCircle,
            isSelected && { backgroundColor: accentColor },
          ]}
        >
          {isSelected && <Check color="#ffffff" size={15} strokeWidth={3} />}
        </View>
      </View>

      <View style={styles.dateRow}>
        <CalendarDays color="#6b7280" size={16} strokeWidth={2} />
        <Text style={styles.dateText}>{formatCompletedDate(item.completedAt)}</Text>
      </View>

      <View style={styles.resultRow}>
        <View>
          {/* "내 풀이" 화면(ExamHistoryCard)과 같은 지표·라벨(Correct n/N)을 써야
              같은 회차가 반전된 것처럼 보이지 않는다(#272). */}
          <Text style={styles.resultLabel}>Correct</Text>
          <Text style={styles.resultValue}>
            {item.totalQuestionCount - item.wrongCount}/{item.totalQuestionCount}
          </Text>
          {/* 분석 근거는 오답만이 아니라 오래 걸림·힌트까지 셋이다. 고를 때 셋 다 보여야
              어느 회차가 분석할 거리가 많은지 판단할 수 있다(SCR-WEAK-02). */}
          <Text style={styles.signalText}>
            오답 {item.wrongCount} · 오래 걸림 {item.longTimeCount} · 힌트 사용 {item.hintUsedCount}
          </Text>
          <Text style={styles.scopePathText}>{scopePath.join("  >  ")}</Text>
        </View>
      </View>

      {!item.eligible && item.warningMessage ? (
        <Text style={styles.ineligibleWarningText}>{item.warningMessage}</Text>
      ) : null}
    </Pressable>
  );
}

function WeaknessReportCard({
  isDeleting,
  item,
  onDelete,
  onPress,
}: {
  isDeleting: boolean;
  item: WeaknessAnalysisReportSummary;
  onDelete: () => void;
  onPress: () => void;
}) {
  const generatedDate = item.generatedAt
    ? item.generatedAt.slice(0, 10).replace(/-/g, ".")
    : null;

  return (
    <Pressable
      disabled={isDeleting}
      style={[styles.weaknessCard, isDeleting && styles.deletingCard]}
      onPress={onPress}
    >
      <View style={styles.cardHeader}>
        <Text numberOfLines={1} style={styles.reportTitleText}>
          {item.title}
        </Text>

        <Pressable
          accessibilityLabel="분석표 삭제"
          accessibilityRole="button"
          disabled={isDeleting}
          hitSlop={8}
          onPress={(event) => {
            event.stopPropagation();
            onDelete();
          }}
        >
          <Trash2 color="#9ca3af" size={20} strokeWidth={2} />
        </Pressable>
      </View>

      {item.status === "GENERATING" ? (
        <View style={styles.reportStatusRow}>
          <ActivityIndicator color={BRAND_PURPLE} size="small" />
          <Text style={styles.reportGeneratingText}>분석 중이에요. 완료되면 알려드릴게요.</Text>
        </View>
      ) : item.status === "FAILED" ? (
        <Text style={styles.reportFailedText}>분석에 실패했어요. 코인은 환불됐어요.</Text>
      ) : null}

      {generatedDate ? (
        <View style={styles.dateRow}>
          <CalendarDays color="#6b7280" size={16} strokeWidth={2} />
          <Text style={styles.dateText}>{generatedDate}</Text>
        </View>
      ) : null}

      <View style={styles.reportStatsRow}>
        <Text style={styles.reportScopeText}>{item.analysisScopeNameSnapshot}</Text>
        <Text style={styles.reportWrongText}>
          오답 {item.totalWrongCount}/{item.totalQuestionCount}
        </Text>
      </View>

      <Text style={styles.reportSessionCountText}>
        분석에 사용한 문제집 {item.selectedSessionCount}개
      </Text>
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
    ...StyleSheet.absoluteFill,
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
  reportTitleText: {
    flex: 1,
    marginRight: 12,
    color: "#1f2937",
    fontFamily: FONT.kufam,
    fontSize: 15,
    fontWeight: "700",
  },
  deletingCard: {
    opacity: 0.5,
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
  reportStatsRow: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
    marginTop: 14,
  },
  reportScopeText: {
    color: "#6D5DFB",
    fontFamily: FONT.pretendard,
    fontSize: 13,
    fontWeight: "700",
  },
  reportWrongText: {
    color: "#475569",
    fontFamily: FONT.kufam,
    fontSize: 13,
    fontWeight: "600",
  },
  reportSessionCountText: {
    marginTop: 6,
    color: "#9ca3af",
    fontFamily: FONT.pretendard,
    fontSize: 12,
    fontWeight: "500",
  },
  reportStatusRow: {
    marginTop: 8,
    flexDirection: "row",
    alignItems: "center",
    gap: 8,
  },
  reportGeneratingText: {
    color: BRAND_PURPLE,
    fontFamily: FONT.pretendard,
    fontSize: 13,
    fontWeight: "700",
  },
  reportFailedText: {
    marginTop: 8,
    color: "#dc2626",
    fontFamily: FONT.pretendard,
    fontSize: 13,
    fontWeight: "600",
  },
  resultLabel: {
    color: "#6b7280",
    fontFamily: FONT.kufam,
    fontSize: 13,
    fontWeight: "500",
  },
  resultValue: {
    marginTop: 5,
    color: "#16C45B",
    fontFamily: FONT.kufam,
    fontSize: 15,
    fontWeight: "600",
  },
  signalText: {
    marginTop: 6,
    color: "#475569",
    fontFamily: FONT.pretendard,
    fontSize: 12,
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
  ineligibleCard: {
    opacity: 0.5,
  },
  ineligibleWarningText: {
    marginTop: 10,
    color: "#dc2626",
    fontFamily: FONT.pretendard,
    fontSize: 12,
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
  // 카드(흰 배경) 위에 겹쳐 뜨므로 오류 문구와 같은 알약 배경을 써야 읽힌다.
  selectionHintText: {
    position: "absolute",
    left: 50,
    right: 50,
    bottom: 70,
    zIndex: 20,
    color: "#ffffff",
    backgroundColor: "rgba(30, 27, 60, 0.88)",
    borderRadius: 12,
    paddingHorizontal: 12,
    paddingVertical: 8,
    fontFamily: FONT.pretendard,
    fontSize: 12,
    fontWeight: "600",
    textAlign: "center",
  },
  createReportErrorText: {
    position: "absolute",
    left: 50,
    right: 50,
    bottom: 70,
    zIndex: 20,
    color: "#ffffff",
    backgroundColor: "rgba(220, 38, 38, 0.92)",
    borderRadius: 12,
    paddingHorizontal: 12,
    paddingVertical: 8,
    fontFamily: FONT.pretendard,
    fontSize: 12,
    fontWeight: "600",
    textAlign: "center",
  },
  // 분석표 목록의 로딩/오류/없음 상태(SCR-WEAK-01). 카드가 없을 때 화면이 통째로 비어
  // 있으면 "고장난 건지 안 만든 건지"를 구분할 수 없다.
  listStateBox: {
    alignItems: "center",
    justifyContent: "center",
    paddingHorizontal: 24,
    paddingVertical: 40,
    gap: 8,
  },
  listStateTitle: {
    color: "#334155",
    fontFamily: FONT.pretendard,
    fontSize: 15,
    fontWeight: "700",
    textAlign: "center",
  },
  listStateText: {
    color: "#64748b",
    fontFamily: FONT.pretendard,
    fontSize: 13,
    fontWeight: "500",
    lineHeight: 20,
    textAlign: "center",
  },
  listStateButton: {
    marginTop: 8,
    minHeight: 44,
    justifyContent: "center",
    paddingHorizontal: 22,
    borderRadius: 12,
    borderWidth: 1,
  },
  listStateButtonText: {
    fontFamily: FONT.pretendard,
    fontSize: 14,
    fontWeight: "700",
  },
  eligibleLoading: {
    marginTop: 24,
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
    ...StyleSheet.absoluteFill,
    zIndex: 1,
    borderTopLeftRadius: 18,
    borderTopRightRadius: 18,
    backgroundColor: "rgba(15, 23, 42, 0.36)",
  },
  selectionHeaderDim: {
    ...StyleSheet.absoluteFill,
    zIndex: 1,
    backgroundColor: "rgba(15, 23, 42, 0.28)",
  },
  selectionPurpleDim: {
    ...StyleSheet.absoluteFill,
    zIndex: 1,
    backgroundColor: "rgba(15, 23, 42, 0.32)",
  },
});
