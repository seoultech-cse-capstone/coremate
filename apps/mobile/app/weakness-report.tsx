import { router, useLocalSearchParams } from "expo-router";
import {
  ChevronDown,
  ChevronLeft,
  ChevronRight,
  ChevronUp,
  CornerDownRight,
  Info,
} from "lucide-react-native";
import { useCallback, useMemo, useRef, useState } from "react";
import type { ReactNode } from "react";
import {
  ActivityIndicator,
  LayoutChangeEvent,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  View,
} from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";

import { FONT } from "../constants/fonts";
import { useAuth, useRequireMember } from "../src/features/auth";
import { useWeaknessReportDetail } from "../src/features/weakness";
import type {
  RelatedConcept,
  WeaknessConcept,
  WeaknessEvidenceLog,
} from "../src/features/weakness";
import { getApiErrorMessage } from "../src/shared/utils/apiError";

const BRAND_PURPLE = "#6D5DFB";

function formatDate(isoString: string | null): string {
  if (!isoString) {
    return "";
  }
  return isoString.slice(0, 10).replace(/-/g, ".");
}

export default function WeaknessReportScreen() {
  useRequireMember();
  const { accessToken } = useAuth();
  const { reportId } = useLocalSearchParams<{ reportId?: string }>();
  const parsedReportId = Number(reportId);
  const resolvedReportId = Number.isFinite(parsedReportId) ? parsedReportId : null;

  const { data: report, isLoading, isError, error } = useWeaknessReportDetail(
    resolvedReportId,
    accessToken,
  );

  // 약점 목록에서 개념을 누르면 해당 카드로 내려간다. 카드마다 y 좌표를 기억해 둔다.
  const scrollRef = useRef<ScrollView>(null);
  const cardOffsets = useRef<Record<number, number>>({});
  // 이동 칩이 상단에 고정되므로 그 높이만큼 더 올려야 카드 머리가 칩에 가리지 않는다.
  const stickyHeight = useRef(0);
  const scrollToConcept = useCallback((analysisConceptId: number) => {
    const y = cardOffsets.current[analysisConceptId];
    if (y !== undefined) {
      scrollRef.current?.scrollTo({ y: Math.max(y - stickyHeight.current - 8, 0), animated: true });
    }
  }, []);

  const concepts = useMemo(() => report?.weaknessConcepts ?? [], [report]);
  // 관계 칩을 누르면 같은 분석표의 약점은 그 카드로 내려가고, 아니면 개념 문제집으로 넘어간다.
  const openConcept = useCallback(
    (conceptId: number) => {
      const inReport = concepts.find((item) => item.conceptId === conceptId);
      if (inReport) {
        const y = cardOffsets.current[inReport.analysisConceptId];
        if (y !== undefined) {
          scrollRef.current?.scrollTo({ y: Math.max(y - stickyHeight.current - 8, 0), animated: true });
          return;
        }
      }
      router.push({ pathname: "/concept-detail", params: { conceptId: conceptId.toString() } });
    },
    [concepts],
  );
  const blocks: ReactNode[] = [];
  let chipsIndex: number | null = null;

  if (report) {
    blocks.push(
      <View key="scope">
        <Text style={styles.scopeText}>{report.analysisScopeNameSnapshot}</Text>
        {report.generatedAt ? (
          <Text style={styles.dateText}>
            {formatDate(report.generatedAt)} · 문제집 {report.selectedSessionCount}개
          </Text>
        ) : null}
      </View>,
    );

    if (report.summaryText) {
      blocks.push(
        <Text key="conclusion" style={styles.conclusionText}>
          {report.summaryText}
        </Text>,
      );
    }

    blocks.push(
      <ScoreCard
        key="score"
        totalCount={report.totalQuestionCount}
        wrongCount={report.totalWrongCount}
        longTimeCount={report.totalLongTimeCount}
        hintUsedCount={report.totalHintUsedCount}
      />,
    );

    if (concepts.length > 0) {
      blocks.push(
        <View key="priority">
          <View style={styles.sectionHeader}>
            <Text style={styles.sectionTitle}>먼저 볼 약점</Text>
            <Text style={styles.sectionHint}>위에서부터 보면 돼요</Text>
          </View>
          <View style={styles.card}>
            {concepts.map((concept, index) => (
              <PriorityRow
                concept={concept}
                index={index}
                key={concept.analysisConceptId}
                onPress={() => scrollToConcept(concept.analysisConceptId)}
              />
            ))}
          </View>
        </View>,
      );

      if (concepts.length > 1) {
        chipsIndex = blocks.length;
        blocks.push(
          // 개념 이름이 길면 한 줄에 다 들어가지 않는다. 줄바꿈 대신 가로로 넘겨 본다.
          <ScrollView
            key="chips"
            contentContainerStyle={styles.chipsContent}
            horizontal
            showsHorizontalScrollIndicator={false}
            style={styles.chipsRow}
            onLayout={(event: LayoutChangeEvent) => {
              stickyHeight.current = event.nativeEvent.layout.height;
            }}
          >
            {concepts.map((concept, index) => (
              <Pressable
                key={concept.analysisConceptId}
                style={styles.jumpChip}
                onPress={() => scrollToConcept(concept.analysisConceptId)}
              >
                <Text numberOfLines={1} style={styles.jumpChipText}>
                  {index + 1} {concept.conceptName}
                </Text>
              </Pressable>
            ))}
          </ScrollView>,
        );
      }

      concepts.forEach((concept, index) => {
        blocks.push(
          <View
            key={`card-${concept.analysisConceptId}`}
            onLayout={(event: LayoutChangeEvent) => {
              cardOffsets.current[concept.analysisConceptId] = event.nativeEvent.layout.y;
            }}
          >
            <WeaknessConceptCard concept={concept} onOpenConcept={openConcept} rank={index + 1} />
          </View>,
        );
      });
    }

    if (report.selectedSessions.length > 0) {
      blocks.push(
        <View key="sessions" style={styles.card}>
          <Text style={styles.sessionLabel}>분석에 쓴 문제집</Text>
          {/* 각 회차를 눌러 실제 풀이 기록으로 넘어갈 수 있어야 한다(SCR-WEAK-05). */}
          {report.selectedSessions.map((session) => (
            <Pressable
              key={session.solveSessionId}
              style={styles.sessionRow}
              onPress={() =>
                router.push({
                  pathname: "/mistake-exam-review",
                  params: { solveSessionId: session.solveSessionId.toString() },
                })
              }
            >
              <Text numberOfLines={2} style={styles.sessionText}>
                {session.title} · {session.wrongCount}/{session.totalQuestionCount} 오답
              </Text>
              <ChevronRight color="#94a3b8" size={18} strokeWidth={2.2} />
            </Pressable>
          ))}
        </View>,
      );
    }
  }

  return (
    <View style={styles.screen}>
      <SafeAreaView edges={["top"]} style={styles.safeArea}>
        <View style={styles.headerArea}>
          <Pressable
            accessibilityLabel="뒤로가기"
            accessibilityRole="button"
            hitSlop={10}
            style={styles.backButton}
            onPress={() => router.back()}
          >
            <ChevronLeft color="#111827" size={28} strokeWidth={2.3} />
          </Pressable>

          <Text numberOfLines={1} style={styles.title}>
            {report?.title ?? "약점 분석표"}
          </Text>
        </View>

        {isLoading ? (
          <View style={styles.stateArea}>
            <ActivityIndicator color={BRAND_PURPLE} size="large" />
          </View>
        ) : isError || !report ? (
          <View style={styles.stateArea}>
            <Text style={styles.stateText}>
              {getApiErrorMessage(error, "분석표를 불러오지 못했어요.")}
            </Text>
          </View>
        ) : (
          <ScrollView
            contentContainerStyle={styles.content}
            ref={scrollRef}
            showsVerticalScrollIndicator={false}
            stickyHeaderIndices={chipsIndex === null ? undefined : [chipsIndex]}
          >
            {blocks}
          </ScrollView>
        )}
      </SafeAreaView>
    </View>
  );
}

function ScoreCard({
  totalCount,
  wrongCount,
  longTimeCount,
  hintUsedCount,
}: {
  totalCount: number;
  wrongCount: number;
  longTimeCount: number;
  hintUsedCount: number;
}) {
  const correctCount = Math.max(totalCount - wrongCount, 0);
  const correctRate = totalCount > 0 ? Math.round((correctCount / totalCount) * 100) : 0;
  return (
    <View style={styles.card}>
      <View style={styles.scoreHeader}>
        <Text style={styles.scoreTitle}>
          {totalCount}문제 중 {correctCount}개 맞혔어요
        </Text>
        <Text style={styles.scoreRate}>정답률 {correctRate}%</Text>
      </View>
      <View style={styles.scoreBar}>
        {correctCount > 0 ? (
          <View style={[styles.scoreBarCorrect, { flex: correctCount }]} />
        ) : null}
        {wrongCount > 0 ? <View style={[styles.scoreBarWrong, { flex: wrongCount }]} /> : null}
      </View>
      <View style={styles.chipGroup}>
        <Text style={[styles.countChip, styles.countChipWrong]}>오답 {wrongCount}</Text>
        <Text style={styles.countChip}>오래 걸림 {longTimeCount}</Text>
        <Text style={styles.countChip}>힌트 사용 {hintUsedCount}</Text>
      </View>
    </View>
  );
}

function PriorityRow({
  concept,
  index,
  onPress,
}: {
  concept: WeaknessConcept;
  index: number;
  onPress: () => void;
}) {
  const repeat = repeatSummaryOf(concept);
  // 선수 개념이 같은 분석표의 약점이면 "이걸 먼저 보라"는 순서를 알려 준다.
  const prerequisite = relatedOf(concept, "PREREQUISITE").find((item) => item.weaknessRank !== null);
  return (
    <View style={[styles.priorityRow, index > 0 && styles.priorityRowDivider]}>
      <Pressable style={styles.priorityMain} onPress={onPress}>
        <View style={[styles.rankBadge, index === 0 && styles.rankBadgeFirst]}>
          <Text style={[styles.rankText, index === 0 && styles.rankTextFirst]}>{index + 1}</Text>
        </View>
        <View style={styles.priorityBody}>
          <Text numberOfLines={1} style={styles.priorityName}>
            {concept.conceptName}
          </Text>
          <Text style={styles.prioritySub}>
            {evidenceSummaryOf(concept)}
            {repeat ? ` · ${repeat}` : ""}
          </Text>
        </View>
        {concept.finalType ? (
          <Text style={styles.typeChip}>{label(WEAKNESS_TYPE_LABEL, concept.finalType)}</Text>
        ) : null}
        <ChevronRight color="#B6BECB" size={16} strokeWidth={2.4} />
      </Pressable>
      {prerequisite ? (
        <View style={styles.priorityHint}>
          <CornerDownRight color="#5144cf" size={14} strokeWidth={2.2} />
          <Text style={styles.priorityHintText}>
            ‘{prerequisite.conceptName}’{objectParticle(prerequisite.conceptName)} 먼저 알아야 이해되는
            개념이에요
          </Text>
        </View>
      ) : null}
    </View>
  );
}

// 서버 enum → 화면 문구. 빠진 값이 있으면 원시 코드가 그대로 노출되므로 서버 정의 전체를 담는다.
const WEAKNESS_TYPE_LABEL: Record<string, string> = {
  CONCEPT_CONFUSION: "개념 혼동",
  APPLICATION_GAP: "적용 부족",
  SPEED_FLUENCY: "속도 부족",
  EXECUTION_ERROR: "풀이 실수",
};

const CAUSE_CODE_LABEL: Record<string, string> = {
  TERM_CONFUSION: "용어 혼동",
  RELATIONSHIP_MAPPING_GAP: "개념 관계 연결 부족",
  APPLICATION_GAP: "적용 부족",
  PROCEDURE_FLUENCY_GAP: "절차 숙련 부족",
  REPEATED_MISCONCEPTION: "같은 오개념 반복",
  CARELESS_EXECUTION: "실수·부주의",
};

const EVIDENCE_TYPE_LABEL: Record<string, string> = {
  WRONG: "오답",
  LONG_TIME: "오래 걸림",
  HINT_USED: "힌트 사용",
  ANSWER_CHANGED: "답 변경",
};

function label(map: Record<string, string>, key: string | null): string {
  if (key === null) {
    return "-";
  }
  return map[key] ?? key;
}

/**
 * 개념별 풀이 요약. 서버가 개념별 수치를 주면 "3문제 중 3개 틀림"처럼 쓰고,
 * 수치가 없는 예전 분석표는 근거 로그를 직접 세어 채운다.
 */
function evidenceSummaryOf(concept: WeaknessConcept): string {
  const questionCount = concept.questionCount ?? 0;
  if (questionCount > 0) {
    const wrongCount = concept.wrongCount ?? 0;
    const parts = [
      wrongCount >= questionCount
        ? `${questionCount}문제 모두 틀림`
        : `${questionCount}문제 중 ${wrongCount}개 틀림`,
    ];
    if ((concept.longTimeCount ?? 0) > 0) {
      parts.push(`오래 걸림 ${concept.longTimeCount}개`);
    }
    if ((concept.hintUsedCount ?? 0) > 0) {
      parts.push(`힌트 ${concept.hintUsedCount}번`);
    }
    return parts.join(" · ");
  }

  const counts = concept.evidenceLogs.reduce<Record<string, number>>((acc, evidence) => {
    acc[evidence.evidenceType] = (acc[evidence.evidenceType] ?? 0) + 1;
    return acc;
  }, {});
  const parts = ["WRONG", "LONG_TIME", "HINT_USED", "ANSWER_CHANGED"]
    .filter((type) => counts[type] > 0)
    .map((type) => `${label(EVIDENCE_TYPE_LABEL, type)} ${counts[type]}개`);
  return parts.length > 0 ? parts.join(" · ") : "근거 기록 없음";
}

/** 반복된 착각이 있으면 "같은 착각 2번"을 요약에 덧붙인다. */
function repeatSummaryOf(concept: WeaknessConcept): string | null {
  const top = concept.repeatedMisconceptions?.[0];
  return top ? `같은 착각 ${top.count}번` : null;
}

function relatedOf(concept: WeaknessConcept, relationType: string): RelatedConcept[] {
  return (concept.relatedConcepts ?? []).filter((item) => item.relationType === relationType);
}

/** 받침 여부로 목적격 조사를 고른다. "종료성을" / "의사코드를" 처럼 문구가 어색해지지 않게. */
function objectParticle(name: string): string {
  const code = name.trim().slice(-1).charCodeAt(0);
  const isHangul = code >= 0xac00 && code <= 0xd7a3;
  return isHangul && (code - 0xac00) % 28 !== 0 ? "을" : "를";
}

function WeaknessConceptCard({
  concept,
  onOpenConcept,
  rank,
}: {
  concept: WeaknessConcept;
  onOpenConcept: (conceptId: number) => void;
  rank: number;
}) {
  const [evidenceOpen, setEvidenceOpen] = useState(false);
  const [learningOpen, setLearningOpen] = useState(false);
  const [moreOpen, setMoreOpen] = useState(false);
  const learning = concept.conceptLearning;
  const citedCount = concept.evidenceLogs.filter((evidence) => evidence.citedByAi).length;
  const repeated = concept.repeatedMisconceptions ?? [];
  const prerequisites = relatedOf(concept, "PREREQUISITE");
  const confusing = relatedOf(concept, "CONFUSING");
  // 원인 문장이 없던 예전 분석표는 원인 코드와 AI 문장으로 대신 채운다.
  const hasCause = Boolean(concept.likelyCause) || repeated.length > 0;

  return (
    <View style={styles.card}>
      <View style={styles.conceptHeader}>
        <Text style={styles.rankLabel}>{rank}순위 약점</Text>
        {concept.finalType ? (
          <Text style={styles.typeChip}>{label(WEAKNESS_TYPE_LABEL, concept.finalType)}</Text>
        ) : null}
      </View>
      <Text style={styles.conceptName}>{concept.conceptName}</Text>
      <Text style={styles.conceptStat}>{evidenceSummaryOf(concept)}</Text>

      {/* 백엔드 판정과 AI 판정이 갈리면 유형을 비워 보낸다. 내부 용어 대신 이유만 알린다. */}
      {concept.finalType ? null : (
        <View style={styles.quietRow}>
          <Info color="#94a3b8" size={14} strokeWidth={2} />
          <Text style={styles.quietText}>
            풀이 기록으로 본 유형과 AI 해석이 달라서 유형은 표시하지 않았어요.
          </Text>
        </View>
      )}

      {prerequisites.length > 0 ? (
        <View style={styles.relationBox}>
          <Text style={styles.blockLabel}>먼저 볼 개념</Text>
          {prerequisites.map((item) => (
            <RelatedConceptRow item={item} key={`pre-${item.conceptId}`} onPress={onOpenConcept} />
          ))}
        </View>
      ) : null}

      {confusing.length > 0 ? (
        <View style={styles.confusingBox}>
          <Text style={styles.blockLabel}>헷갈리는 개념</Text>
          {confusing.map((item) => (
            <RelatedConceptRow item={item} key={`conf-${item.conceptId}`} onPress={onOpenConcept} />
          ))}
        </View>
      ) : null}

      {concept.diagnosis ? (
        <View style={styles.block}>
          <Text style={styles.blockLabel}>진단</Text>
          <Text style={styles.diagnosisText}>{concept.diagnosis}</Text>
        </View>
      ) : null}

      {hasCause || concept.causeCode || concept.confusedRelation ? (
        <View style={styles.block}>
          <Text style={styles.blockLabel}>왜 틀렸을까</Text>
          {/*
            "2번"만 쓰면 바로 위 「틀린 문제 3개」와 선지 기호 때문에 문제 번호·선지 번호로 읽힌다.
            카드 맨 윗줄 요약과 같은 "같은 착각 N번"으로 통일한다.
          */}
          {repeated.map((item) => (
            <View key={item.code} style={styles.misRow}>
              <Text style={styles.countPill}>같은 착각 {item.count}번</Text>
              <Text style={styles.misText}>{item.title ?? "확인되지 않은 오개념"}</Text>
            </View>
          ))}
          {concept.likelyCause ? (
            <Text style={styles.causeText}>{concept.likelyCause}</Text>
          ) : (
            <>
              {concept.causeCode ? (
                <Text style={styles.causeChip}>{label(CAUSE_CODE_LABEL, concept.causeCode)}</Text>
              ) : null}
              {concept.confusedRelation ? (
                <Text style={styles.causeText}>{concept.confusedRelation}</Text>
              ) : null}
            </>
          )}
        </View>
      ) : null}

      {concept.conceptTip ? (
        <View style={styles.tipBox}>
          <Text style={styles.tipTitle}>이렇게 복습해요</Text>
          <Text style={styles.tipText}>{concept.conceptTip}</Text>
        </View>
      ) : null}

      {concept.evidenceLogs.length > 0 ? (
        <>
          <Disclosure
            open={evidenceOpen}
            subLabel={citedCount > 0 ? `AI가 짚은 문제 ${citedCount}` : undefined}
            title={`틀린 문제 ${concept.evidenceLogs.length}개`}
            onPress={() => setEvidenceOpen((prev) => !prev)}
          />
          {evidenceOpen
            ? concept.evidenceLogs.map((evidence) => (
                <EvidenceLogRow evidence={evidence} key={evidence.evidenceId} />
              ))
            : null}
        </>
      ) : null}

      {learning ? (
        <>
          <Disclosure
            open={learningOpen}
            title="개념 한눈에 보기"
            onPress={() => setLearningOpen((prev) => !prev)}
          />
          {learningOpen ? (
            <View style={styles.learningBox}>
              {learning.conceptCore ? (
                <LearningItem label="핵심" text={learning.conceptCore} />
              ) : null}
              {learning.keyPoints.length > 0 ? (
                <LearningItem label="꼭 짚을 포인트" text={learning.keyPoints.join("\n")} />
              ) : null}
              {learning.confusionComparisons.length > 0 ? (
                <LearningItem
                  label="헷갈리는 짝"
                  text={learning.confusionComparisons
                    .map((comparison) => `${comparison.comparedWith} — ${comparison.difference}`)
                    .join("\n")}
                />
              ) : null}

              <Pressable style={styles.moreButton} onPress={() => setMoreOpen((prev) => !prev)}>
                <Text style={styles.moreText}>
                  {moreOpen ? "학습 자료 접기" : "설명·예시·기초 복습 더 보기"}
                </Text>
                {moreOpen ? (
                  <ChevronUp color={BRAND_PURPLE} size={14} strokeWidth={2.4} />
                ) : (
                  <ChevronDown color={BRAND_PURPLE} size={14} strokeWidth={2.4} />
                )}
              </Pressable>

              {moreOpen ? (
                <>
                  {learning.conceptExplanation ? (
                    <LearningItem label="설명" text={learning.conceptExplanation} />
                  ) : null}
                  {prerequisites.length === 0 && learning.prerequisiteConcepts.length > 0 ? (
                    // 관계 테이블에 선수 관계가 있으면 그쪽이 정확하므로, 없을 때만 프로필 목록으로 채운다.
                    <LearningItem
                      label="선수 개념"
                      text={learning.prerequisiteConcepts.join(", ")}
                    />
                  ) : null}
                  {learning.learningExample ? (
                    <LearningItem label="예시" text={learning.learningExample} />
                  ) : null}
                  {learning.misconceptionNotes ? (
                    <LearningItem label="오개념 노트" text={learning.misconceptionNotes} />
                  ) : null}
                  {learning.basicReviewPoints.length > 0 ? (
                    <LearningItem
                      label="기초 복습 포인트"
                      text={learning.basicReviewPoints.join("\n")}
                    />
                  ) : null}
                </>
              ) : null}
            </View>
          ) : null}
        </>
      ) : null}

      {/* 분석표를 읽고 나면 복습으로 이어져야 한다(SCR-WEAK-05). */}
      <Pressable
        style={styles.conceptLinkButton}
        onPress={() =>
          router.push({
            pathname: "/concept-detail",
            params: { conceptId: concept.conceptId.toString() },
          })
        }
      >
        <Text style={styles.conceptLinkText}>이 개념 문제집 풀기</Text>
      </Pressable>
    </View>
  );
}

function Disclosure({
  open,
  subLabel,
  title,
  onPress,
}: {
  open: boolean;
  subLabel?: string;
  title: string;
  onPress: () => void;
}) {
  return (
    <Pressable accessibilityRole="button" style={styles.disclosure} onPress={onPress}>
      <Text style={styles.disclosureTitle}>{title}</Text>
      {subLabel ? <Text style={styles.disclosureSub}>{subLabel}</Text> : null}
      {open ? (
        <ChevronUp color="#94a3b8" size={18} strokeWidth={2.4} />
      ) : (
        <ChevronDown color="#94a3b8" size={18} strokeWidth={2.4} />
      )}
    </Pressable>
  );
}

function LearningItem({ label: itemLabel, text }: { label: string; text: string }) {
  return (
    <View style={styles.learningItem}>
      <Text style={styles.learningLabel}>{itemLabel}</Text>
      <Text style={styles.learningText}>{text}</Text>
    </View>
  );
}

function RelatedConceptRow({
  item,
  onPress,
}: {
  item: RelatedConcept;
  onPress: (conceptId: number) => void;
}) {
  return (
    <Pressable style={styles.relationRow} onPress={() => onPress(item.conceptId)}>
      <View style={styles.relationHead}>
        <Text style={styles.relationName}>{item.conceptName}</Text>
        {item.weaknessRank !== null ? (
          <Text style={styles.relationRankTag}>{item.weaknessRank}순위 약점</Text>
        ) : null}
        <View style={styles.relationSpacer} />
        <ChevronRight color="#B6BECB" size={14} strokeWidth={2.4} />
      </View>
      {item.relationNote ? <Text style={styles.relationNote}>{item.relationNote}</Text> : null}
    </Pressable>
  );
}

function EvidenceLogRow({ evidence }: { evidence: WeaknessEvidenceLog }) {
  return (
    <View style={[styles.evidenceRow, evidence.citedByAi && styles.citedEvidenceRow]}>
      <View style={styles.evidenceHeader}>
        <Text style={styles.evidenceType}>{label(EVIDENCE_TYPE_LABEL, evidence.evidenceType)}</Text>
        {evidence.citedByAi ? <Text style={styles.citedBadge}>AI가 짚은 문제</Text> : null}
        <View style={styles.relationSpacer} />
        <Text style={styles.evidenceMeta}>
          {evidence.timeSpentSec ?? "-"}초 · 힌트 {evidence.hintCount ?? 0}회
        </Text>
      </View>
      <Text style={styles.evidenceQuestion}>{evidence.questionText}</Text>

      <View style={styles.answerRow}>
        <Text style={[styles.answerKey, styles.answerKeyMine]}>{evidence.selectedAnswer ?? "-"}</Text>
        <View style={styles.answerBody}>
          <Text style={[styles.answerLabel, styles.answerLabelMine]}>내 답</Text>
          {evidence.selectedAnswerText ? (
            <Text style={styles.answerText}>{evidence.selectedAnswerText}</Text>
          ) : null}
        </View>
      </View>
      <View style={styles.answerRow}>
        <Text style={[styles.answerKey, styles.answerKeyCorrect]}>{evidence.correctAnswer ?? "-"}</Text>
        <View style={styles.answerBody}>
          <Text style={[styles.answerLabel, styles.answerLabelCorrect]}>정답</Text>
          {evidence.correctAnswerText ? (
            <Text style={styles.answerText}>{evidence.correctAnswerText}</Text>
          ) : null}
        </View>
      </View>

      {evidence.selectedAnswerReason ? (
        <View style={styles.reasonBox}>
          <Text style={styles.reasonLabel}>이 답을 고른 이유</Text>
          <Text style={styles.reasonText}>{evidence.selectedAnswerReason}</Text>
          {evidence.misconceptionTitle ? (
            <Text style={styles.reasonTag}>{evidence.misconceptionTitle}</Text>
          ) : null}
        </View>
      ) : null}
    </View>
  );
}

const styles = StyleSheet.create({
  screen: {
    flex: 1,
    backgroundColor: "#F3F3F3",
  },
  safeArea: {
    flex: 1,
    backgroundColor: "transparent",
  },
  headerArea: {
    position: "relative",
    minHeight: 64,
    alignItems: "center",
    justifyContent: "center",
    backgroundColor: "#ffffff",
    paddingHorizontal: 56,
  },
  backButton: {
    position: "absolute",
    left: 12,
    top: 11,
    alignItems: "center",
    justifyContent: "center",
    width: 42,
    height: 42,
  },
  title: {
    color: "#111827",
    fontFamily: FONT.pretendard,
    fontSize: 18,
    fontWeight: "700",
    textAlign: "center",
  },
  stateArea: {
    flex: 1,
    alignItems: "center",
    justifyContent: "center",
    paddingHorizontal: 32,
  },
  stateText: {
    color: "#334155",
    fontFamily: FONT.pretendard,
    fontSize: 14,
    fontWeight: "600",
    textAlign: "center",
    lineHeight: 21,
  },
  content: {
    paddingHorizontal: 20,
    paddingTop: 20,
    paddingBottom: 44,
    gap: 12,
  },
  scopeText: {
    color: BRAND_PURPLE,
    fontFamily: FONT.pretendard,
    fontSize: 13,
    fontWeight: "700",
  },
  dateText: {
    marginTop: 4,
    color: "#94a3b8",
    fontFamily: FONT.pretendard,
    fontSize: 12,
    fontWeight: "500",
  },
  conclusionText: {
    color: "#111827",
    fontFamily: FONT.pretendard,
    fontSize: 19,
    fontWeight: "800",
    lineHeight: 28,
    letterSpacing: -0.3,
  },
  card: {
    borderRadius: 14,
    backgroundColor: "#ffffff",
    padding: 16,
  },
  scoreHeader: {
    flexDirection: "row",
    alignItems: "baseline",
    justifyContent: "space-between",
    gap: 8,
  },
  scoreTitle: {
    color: "#1f2937",
    fontFamily: FONT.pretendard,
    fontSize: 15,
    fontWeight: "700",
  },
  scoreRate: {
    color: "#94a3b8",
    fontFamily: FONT.pretendard,
    fontSize: 12,
    fontWeight: "600",
  },
  scoreBar: {
    flexDirection: "row",
    gap: 2,
    height: 10,
    marginTop: 10,
  },
  scoreBarCorrect: {
    backgroundColor: BRAND_PURPLE,
    borderTopLeftRadius: 4,
    borderBottomLeftRadius: 4,
  },
  scoreBarWrong: {
    backgroundColor: "#FCA5A5",
    borderTopRightRadius: 4,
    borderBottomRightRadius: 4,
  },
  chipGroup: {
    flexDirection: "row",
    flexWrap: "wrap",
    gap: 6,
    marginTop: 12,
  },
  countChip: {
    color: "#334155",
    backgroundColor: "#F1F2F5",
    fontFamily: FONT.pretendard,
    fontSize: 11.5,
    fontWeight: "600",
    paddingHorizontal: 8,
    paddingVertical: 3,
    borderRadius: 6,
    overflow: "hidden",
  },
  countChipWrong: {
    color: "#dc2626",
    backgroundColor: "#FEF2F2",
  },
  sectionHeader: {
    flexDirection: "row",
    alignItems: "baseline",
    justifyContent: "space-between",
    marginBottom: 8,
    paddingHorizontal: 2,
  },
  sectionTitle: {
    color: "#1f2937",
    fontFamily: FONT.pretendard,
    fontSize: 16,
    fontWeight: "800",
  },
  sectionHint: {
    color: "#94a3b8",
    fontFamily: FONT.pretendard,
    fontSize: 11.5,
    fontWeight: "600",
  },
  priorityRow: {
    paddingVertical: 12,
  },
  priorityMain: {
    flexDirection: "row",
    alignItems: "center",
    gap: 10,
  },
  priorityHint: {
    flexDirection: "row",
    alignItems: "flex-start",
    gap: 6,
    marginTop: 8,
    marginLeft: 34,
    borderRadius: 9,
    backgroundColor: "#F1EFFF",
    paddingVertical: 7,
    paddingHorizontal: 9,
  },
  priorityHintText: {
    flex: 1,
    color: "#5144cf",
    fontFamily: FONT.pretendard,
    fontSize: 12,
    fontWeight: "600",
    lineHeight: 18,
  },
  relationBox: {
    marginTop: 14,
    borderRadius: 12,
    borderWidth: 1,
    borderColor: "#DAD5FF",
    padding: 12,
  },
  confusingBox: {
    marginTop: 10,
    borderRadius: 12,
    backgroundColor: "#F7F8FA",
    padding: 12,
  },
  relationRow: {
    paddingVertical: 5,
  },
  relationHead: {
    flexDirection: "row",
    alignItems: "center",
    gap: 6,
  },
  relationName: {
    color: "#1f2937",
    fontFamily: FONT.pretendard,
    fontSize: 14.5,
    fontWeight: "700",
  },
  relationRankTag: {
    color: "#dc2626",
    backgroundColor: "#FEF2F2",
    fontFamily: FONT.pretendard,
    fontSize: 11,
    fontWeight: "700",
    paddingHorizontal: 6,
    paddingVertical: 1,
    borderRadius: 5,
    overflow: "hidden",
  },
  relationSpacer: {
    flex: 1,
  },
  relationNote: {
    marginTop: 4,
    color: "#64748b",
    fontFamily: FONT.pretendard,
    fontSize: 12.5,
    lineHeight: 19,
  },
  misRow: {
    flexDirection: "row",
    alignItems: "flex-start",
    gap: 8,
    paddingVertical: 3,
  },
  // 백엔드가 2회 이상만 내려주므로(enrichEvidence 의 count >= 2 필터) 색 분기가 따로 없다.
  countPill: {
    color: "#dc2626",
    backgroundColor: "#FEF2F2",
    fontFamily: FONT.pretendard,
    fontSize: 11,
    fontWeight: "800",
    paddingHorizontal: 6,
    paddingVertical: 1,
    borderRadius: 5,
    overflow: "hidden",
  },
  misText: {
    flex: 1,
    color: "#1f2937",
    fontFamily: FONT.pretendard,
    fontSize: 13.5,
    fontWeight: "600",
    lineHeight: 20,
  },
  answerRow: {
    flexDirection: "row",
    alignItems: "flex-start",
    gap: 8,
    marginTop: 8,
  },
  answerKey: {
    width: 22,
    height: 22,
    borderRadius: 11,
    textAlign: "center",
    lineHeight: 22,
    fontFamily: FONT.pretendard,
    fontSize: 11.5,
    fontWeight: "800",
    overflow: "hidden",
  },
  answerKeyMine: {
    color: "#ffffff",
    backgroundColor: "#dc2626",
  },
  answerKeyCorrect: {
    color: "#15803D",
    backgroundColor: "#ECFDF3",
  },
  answerBody: {
    flex: 1,
  },
  answerLabel: {
    fontFamily: FONT.pretendard,
    fontSize: 11,
    fontWeight: "800",
  },
  answerLabelMine: {
    color: "#dc2626",
  },
  answerLabelCorrect: {
    color: "#15803D",
  },
  answerText: {
    marginTop: 2,
    color: "#334155",
    fontFamily: FONT.pretendard,
    fontSize: 13,
    lineHeight: 20,
  },
  reasonBox: {
    marginTop: 10,
    borderRadius: 10,
    backgroundColor: "#F8FAFC",
    padding: 11,
  },
  reasonLabel: {
    color: "#94a3b8",
    fontFamily: FONT.pretendard,
    fontSize: 11,
    fontWeight: "700",
  },
  reasonText: {
    marginTop: 3,
    color: "#475569",
    fontFamily: FONT.pretendard,
    fontSize: 13,
    lineHeight: 20,
  },
  reasonTag: {
    alignSelf: "flex-start",
    marginTop: 6,
    color: "#5144cf",
    backgroundColor: "#F1EFFF",
    fontFamily: FONT.pretendard,
    fontSize: 11,
    fontWeight: "700",
    paddingHorizontal: 7,
    paddingVertical: 2,
    borderRadius: 5,
    overflow: "hidden",
  },
  evidenceMeta: {
    color: "#94a3b8",
    fontFamily: FONT.pretendard,
    fontSize: 11,
    fontWeight: "600",
  },
  priorityRowDivider: {
    borderTopWidth: 1,
    borderTopColor: "#EEF0F3",
  },
  rankBadge: {
    alignItems: "center",
    justifyContent: "center",
    width: 24,
    height: 24,
    borderRadius: 12,
    borderWidth: 1.5,
    borderColor: BRAND_PURPLE,
  },
  rankBadgeFirst: {
    backgroundColor: BRAND_PURPLE,
    borderColor: BRAND_PURPLE,
  },
  rankText: {
    color: "#5144cf",
    fontFamily: FONT.pretendard,
    fontSize: 12,
    fontWeight: "800",
  },
  rankTextFirst: {
    color: "#ffffff",
  },
  priorityBody: {
    flex: 1,
  },
  priorityName: {
    color: "#1f2937",
    fontFamily: FONT.pretendard,
    fontSize: 15,
    fontWeight: "700",
  },
  prioritySub: {
    marginTop: 1,
    color: "#64748b",
    fontFamily: FONT.pretendard,
    fontSize: 12,
    fontWeight: "500",
  },
  typeChip: {
    color: "#5144cf",
    backgroundColor: "#F1EFFF",
    fontFamily: FONT.pretendard,
    fontSize: 11,
    fontWeight: "700",
    paddingHorizontal: 7,
    paddingVertical: 3,
    borderRadius: 6,
    overflow: "hidden",
  },
  chipsRow: {
    flexGrow: 0,
    backgroundColor: "#F3F3F3",
    paddingVertical: 6,
  },
  chipsContent: {
    flexDirection: "row",
    alignItems: "center",
    gap: 6,
    paddingRight: 20,
  },
  jumpChip: {
    borderRadius: 999,
    backgroundColor: "#ffffff",
    borderWidth: 1,
    borderColor: "#E2E8F0",
    paddingHorizontal: 10,
    paddingVertical: 5,
  },
  jumpChipText: {
    color: "#475569",
    fontFamily: FONT.pretendard,
    fontSize: 12,
    fontWeight: "600",
  },
  conceptHeader: {
    flexDirection: "row",
    alignItems: "center",
    gap: 8,
  },
  rankLabel: {
    color: BRAND_PURPLE,
    fontFamily: FONT.pretendard,
    fontSize: 12,
    fontWeight: "800",
  },
  conceptName: {
    marginTop: 4,
    color: "#111827",
    fontFamily: FONT.pretendard,
    fontSize: 20,
    fontWeight: "800",
    letterSpacing: -0.4,
  },
  conceptStat: {
    marginTop: 6,
    color: "#64748b",
    fontFamily: FONT.pretendard,
    fontSize: 12.5,
    fontWeight: "500",
  },
  quietRow: {
    flexDirection: "row",
    alignItems: "flex-start",
    gap: 6,
    marginTop: 10,
  },
  quietText: {
    flex: 1,
    color: "#94a3b8",
    fontFamily: FONT.pretendard,
    fontSize: 12,
    fontWeight: "500",
    lineHeight: 18,
  },
  block: {
    marginTop: 16,
  },
  blockLabel: {
    color: "#94a3b8",
    fontFamily: FONT.pretendard,
    fontSize: 11.5,
    fontWeight: "700",
    marginBottom: 5,
  },
  diagnosisText: {
    color: "#111827",
    fontFamily: FONT.pretendard,
    fontSize: 15,
    fontWeight: "600",
    lineHeight: 23,
  },
  causeChip: {
    alignSelf: "flex-start",
    color: "#dc2626",
    backgroundColor: "#FEF2F2",
    fontFamily: FONT.pretendard,
    fontSize: 11,
    fontWeight: "700",
    paddingHorizontal: 7,
    paddingVertical: 3,
    borderRadius: 6,
    overflow: "hidden",
  },
  causeText: {
    marginTop: 6,
    color: "#334155",
    fontFamily: FONT.pretendard,
    fontSize: 13.5,
    lineHeight: 21,
  },
  tipBox: {
    borderRadius: 12,
    backgroundColor: "#F1EFFF",
    marginTop: 16,
    padding: 13,
  },
  tipTitle: {
    color: "#5144cf",
    fontFamily: FONT.pretendard,
    fontSize: 11.5,
    fontWeight: "700",
  },
  tipText: {
    marginTop: 4,
    color: "#3B3565",
    fontFamily: FONT.pretendard,
    fontSize: 13.5,
    lineHeight: 21,
  },
  disclosure: {
    flexDirection: "row",
    alignItems: "center",
    gap: 8,
    marginTop: 12,
    paddingTop: 12,
    borderTopWidth: 1,
    borderTopColor: "#EEF0F3",
  },
  disclosureTitle: {
    flex: 1,
    color: "#1f2937",
    fontFamily: FONT.pretendard,
    fontSize: 14,
    fontWeight: "700",
  },
  disclosureSub: {
    color: "#94a3b8",
    fontFamily: FONT.pretendard,
    fontSize: 12,
    fontWeight: "600",
  },
  learningBox: {
    borderRadius: 12,
    backgroundColor: "#F8FAFC",
    marginTop: 10,
    padding: 13,
    gap: 12,
  },
  learningItem: {
    gap: 4,
  },
  learningLabel: {
    color: "#94a3b8",
    fontFamily: FONT.pretendard,
    fontSize: 11.5,
    fontWeight: "700",
  },
  learningText: {
    color: "#475569",
    fontFamily: FONT.pretendard,
    fontSize: 13,
    lineHeight: 20,
  },
  moreButton: {
    flexDirection: "row",
    alignItems: "center",
    gap: 3,
  },
  moreText: {
    color: BRAND_PURPLE,
    fontFamily: FONT.pretendard,
    fontSize: 13,
    fontWeight: "700",
  },
  evidenceRow: {
    marginTop: 10,
    borderRadius: 12,
    borderWidth: 1,
    borderColor: "#EEF0F3",
    padding: 12,
  },
  // AI가 직접 근거로 지목한 문항은 나머지와 구분해 보여 준다.
  citedEvidenceRow: {
    backgroundColor: "#F8F7FF",
    borderColor: "#E3DEFF",
  },
  evidenceHeader: {
    flexDirection: "row",
    alignItems: "center",
    gap: 8,
    flexWrap: "wrap",
  },
  citedBadge: {
    color: BRAND_PURPLE,
    backgroundColor: "#EDEAFE",
    fontFamily: FONT.pretendard,
    fontSize: 11,
    fontWeight: "600",
    paddingHorizontal: 7,
    paddingVertical: 2,
    borderRadius: 4,
    overflow: "hidden",
  },
  evidenceType: {
    color: "#dc2626",
    fontFamily: FONT.pretendard,
    fontSize: 11,
    fontWeight: "800",
  },
  evidenceQuestion: {
    marginTop: 8,
    color: "#111827",
    fontFamily: FONT.pretendard,
    fontSize: 13.5,
    fontWeight: "600",
    lineHeight: 20,
  },
  evidenceDetailText: {
    marginTop: 4,
    color: "#64748b",
    fontFamily: FONT.pretendard,
    fontSize: 11.5,
    lineHeight: 17,
  },
  conceptLinkButton: {
    marginTop: 16,
    alignItems: "center",
    justifyContent: "center",
    minHeight: 46,
    borderRadius: 12,
    backgroundColor: BRAND_PURPLE,
  },
  conceptLinkText: {
    color: "#ffffff",
    fontFamily: FONT.pretendard,
    fontSize: 14.5,
    fontWeight: "700",
  },
  sessionLabel: {
    color: "#94a3b8",
    fontFamily: FONT.pretendard,
    fontSize: 11.5,
    fontWeight: "700",
  },
  sessionRow: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
    gap: 8,
    paddingTop: 6,
  },
  sessionText: {
    flex: 1,
    color: "#334155",
    fontFamily: FONT.pretendard,
    fontSize: 13,
    fontWeight: "600",
    lineHeight: 19,
  },
});
