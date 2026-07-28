import { CircleCheckBig, type LucideIcon } from "lucide-react-native";
import { Pressable, StyleSheet, Text, View } from "react-native";

import { FONT } from "../../../../constants/fonts";
import { ProgressLine } from "./ProgressLine";

type ProblemSetCardProps = {
  chapterCount: number;
  countUnit: "chapter" | "concept" | "question";
  hasSolved: boolean;
  hideProgress?: boolean;
  iconColor: string;
  Icon: LucideIcon;
  progress: number;
  startButtonLabel: string;
  thumbnailColor: string;
  title: string;
  onOpen: () => void;
  onReview: () => void;
  onStart: () => void;
};

export function ProblemSetCard({
  chapterCount,
  countUnit,
  hasSolved,
  hideProgress = false,
  iconColor,
  Icon,
  progress,
  startButtonLabel,
  thumbnailColor,
  title,
  onOpen,
  onReview,
  onStart,
}: ProblemSetCardProps) {
  const isCompleted = !hideProgress && progress >= 100;
  const ThumbnailIcon = isCompleted ? CircleCheckBig : Icon;
  const resolvedIconColor = isCompleted ? "#16C45B" : iconColor;
  const resolvedThumbnailColor = isCompleted ? "#E5F9ED" : thumbnailColor;

  return (
    <Pressable style={styles.card} onPress={onOpen}>
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
          <View style={styles.titleRow}>
            <View>
              <Text style={styles.title}>{title}</Text>
              <Text style={styles.chapter}>
                {chapterCount} {countUnit}
              </Text>
            </View>

            <Text style={styles.chevron}>›</Text>
          </View>
        </View>
      </View>

      {!hideProgress && <ProgressLine progress={progress} />}

      <View style={styles.actions}>
        {hasSolved ? (
          <>
            <Pressable
              style={[styles.button, styles.primaryButton]}
              onPress={(event) => {
                event.stopPropagation();
                onReview();
              }}
            >
              <Text style={styles.primaryButtonText}>최근 풀이 및 해설</Text>
            </Pressable>

            <Pressable
              style={[styles.button, styles.grayButton]}
              onPress={(event) => {
                event.stopPropagation();
                onStart();
              }}
            >
              <Text style={styles.grayButtonText}>다시 풀기</Text>
            </Pressable>
          </>
        ) : (
          <Pressable
            style={[styles.button, styles.fullButton]}
            onPress={(event) => {
              event.stopPropagation();
              onStart();
            }}
          >
            <Text style={styles.primaryButtonText}>{startButtonLabel}</Text>
          </Pressable>
        )}
      </View>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  card: {
    width: "100%",
    minHeight: 136,
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
  },
  titleRow: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
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
  chevron: {
    color: "#707070",
    fontFamily: FONT.pretendard,
    fontSize: 30,
    fontWeight: "400",
    lineHeight: 32,
    marginRight: 1,
  },
  actions: {
    flexDirection: "row",
    gap: 11,
    marginTop: 10,
  },
  button: {
    alignItems: "center",
    justifyContent: "center",
    minHeight: 30,
    borderRadius: 999,
    paddingHorizontal: 14,
  },
  primaryButton: {
    flex: 1,
    backgroundColor: "#6D5DFB",
  },
  fullButton: {
    flex: 1,
    backgroundColor: "#6D5DFB",
  },
  grayButton: {
    width: 122,
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
