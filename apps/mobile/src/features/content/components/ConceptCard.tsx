import { CircleCheckBig, type LucideIcon } from "lucide-react-native";
import { Pressable, StyleSheet, Text, View } from "react-native";

import { FONT } from "../../../../constants/fonts";
import { ProgressLine } from "./ProgressLine";

type ConceptCardProps = {
  chapterCount: number;
  countUnit: "chapter" | "concept" | "question";
  iconColor: string;
  Icon: LucideIcon;
  progress: number;
  thumbnailColor: string;
  title: string;
  onPress: () => void;
};

export function ConceptCard({
  chapterCount,
  countUnit,
  iconColor,
  Icon,
  progress,
  thumbnailColor,
  title,
  onPress,
}: ConceptCardProps) {
  const isCompleted = progress >= 100;
  const ThumbnailIcon = isCompleted ? CircleCheckBig : Icon;
  const resolvedIconColor = isCompleted ? "#16C45B" : iconColor;
  const resolvedThumbnailColor = isCompleted ? "#E5F9ED" : thumbnailColor;

  return (
    <Pressable style={styles.card} onPress={onPress}>
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

      <ProgressLine progress={progress} />
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
    paddingBottom: 20,
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
  },
});
