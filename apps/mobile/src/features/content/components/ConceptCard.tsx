import { CircleCheckBig, type LucideIcon } from "lucide-react-native";
import { Pressable, StyleSheet, Text, View } from "react-native";

import { FONT } from "../../../../constants/fonts";
import { ProgressLine } from "./ProgressLine";

type ConceptCardProps = {
  chapterCount: number;
  /** false면 chapterCount를 아직 모른다는 뜻이라 개수 줄 자체를 숨긴다. */
  hasChapterCount?: boolean;
  countUnit: "chapter" | "concept" | "question";
  hideProgress?: boolean;
  iconColor: string;
  Icon: LucideIcon;
  progress: number;
  /** 완료된 경우 "3 / 5 정답" 같은 결과 요약. 없으면 표시하지 않는다. */
  resultLabel?: string;
  startButtonLabel: string;
  thumbnailColor: string;
  title: string;
  onStart: () => void;
  onViewDetail: () => void;
};

export function ConceptCard({
  chapterCount,
  hasChapterCount = true,
  countUnit,
  hideProgress = false,
  iconColor,
  Icon,
  progress,
  resultLabel,
  startButtonLabel,
  thumbnailColor,
  title,
  onStart,
  onViewDetail,
}: ConceptCardProps) {
  const isCompleted = !hideProgress && progress >= 100;
  const ThumbnailIcon = isCompleted ? CircleCheckBig : Icon;
  const resolvedIconColor = isCompleted ? "#16C45B" : iconColor;
  const resolvedThumbnailColor = isCompleted ? "#E5F9ED" : thumbnailColor;

  return (
    <View style={styles.card}>
      <View style={styles.body}>
        <View
          style={[
            styles.thumbnail,
            { backgroundColor: resolvedThumbnailColor },
          ]}
        >
          <ThumbnailIcon color={resolvedIconColor} size={25} strokeWidth={2.4} />
        </View>

        <View style={styles.content}>
          <Text style={styles.title}>{title}</Text>
          {hasChapterCount ? (
            <Text style={styles.chapter}>
              {chapterCount} {countUnit}
            </Text>
          ) : null}
          {resultLabel ? (
            <Text style={styles.result}>{resultLabel}</Text>
          ) : null}
        </View>
      </View>

      {!hideProgress && <ProgressLine progress={progress} />}

      <View style={styles.actions}>
        <Pressable
          style={[styles.button, styles.grayButton]}
          onPress={onViewDetail}
        >
          <Text style={styles.grayButtonText}>개념 파악하기</Text>
        </Pressable>

        <Pressable
          style={[styles.button, styles.primaryButton]}
          onPress={onStart}
        >
          <Text style={styles.primaryButtonText}>{startButtonLabel}</Text>
        </Pressable>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  card: {
    width: "100%",
    borderRadius: 8,
    backgroundColor: "#ffffff",
    paddingHorizontal: 21,
    paddingTop: 19,
    paddingBottom: 10,
  },
  body: {
    flexDirection: "row",
    gap: 17,
    marginBottom: 18,
  },
  thumbnail: {
    alignItems: "center",
    justifyContent: "center",
    width: 51,
    height: 49,
    borderRadius: 8,
    backgroundColor: "#D9D9D9",
  },
  content: {
    flex: 1,
    justifyContent: "center",
  },
  title: {
    color: "#111111",
    fontFamily: FONT.pretendard,
    fontSize: 16,
    fontWeight: "500",
    lineHeight: 21,
  },
  chapter: {
    color: "#111111",
    fontFamily: FONT.pretendard,
    fontSize: 14,
    fontWeight: "400",
    lineHeight: 20,
    marginTop: 2,
  },
  result: {
    color: "#16C45B",
    fontFamily: FONT.pretendard,
    fontSize: 13,
    fontWeight: "600",
    marginTop: 2,
  },
  actions: {
    flexDirection: "row",
    gap: 11,
    marginTop: 10,
  },
  button: {
    flex: 1,
    alignItems: "center",
    justifyContent: "center",
    minHeight: 30,
    borderRadius: 999,
    paddingHorizontal: 14,
  },
  primaryButton: {
    backgroundColor: "#6D5DFB",
  },
  grayButton: {
    backgroundColor: "#828B92",
  },
  primaryButtonText: {
    color: "#ffffff",
    fontFamily: FONT.pretendard,
    fontSize: 14,
    fontWeight: "500",
  },
  grayButtonText: {
    color: "#ffffff",
    fontFamily: FONT.pretendard,
    fontSize: 14,
    fontWeight: "500",
  },
});
