import { Pressable, StyleSheet, Text, View } from "react-native";

import { FONT } from "../../../../constants/fonts";

type PracticeSetCardProps = {
  accentColor: string;
  badge: string;
  description: string;
  meta: string;
  progressLabel: string;
  title: string;
};

export function PracticeSetCard({
  accentColor,
  badge,
  description,
  meta,
  progressLabel,
  title,
}: PracticeSetCardProps) {
  return (
    <Pressable style={styles.card}>
      <View style={styles.header}>
        <View style={[styles.badge, { backgroundColor: `${accentColor}1A` }]}>
          <Text style={[styles.badgeText, { color: accentColor }]}>{badge}</Text>
        </View>
        <Text style={styles.progress}>{progressLabel}</Text>
      </View>

      <Text style={styles.title}>{title}</Text>
      <Text style={styles.description}>{description}</Text>

      <View style={styles.footer}>
        <Text style={styles.meta}>{meta}</Text>
        <Text style={[styles.action, { color: accentColor }]}>풀기</Text>
      </View>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  card: {
    borderRadius: 8,
    backgroundColor: "#ffffff",
    padding: 18,
    shadowColor: "#111827",
    shadowOffset: { width: 0, height: 8 },
    shadowOpacity: 0.06,
    shadowRadius: 14,
    elevation: 2,
  },
  header: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
    gap: 12,
  },
  badge: {
    borderRadius: 999,
    paddingHorizontal: 10,
    paddingVertical: 5,
  },
  badgeText: {
    fontFamily: FONT.pretendard,
    fontSize: 12,
    fontWeight: "600",
  },
  progress: {
    color: "#71717A",
    fontFamily: FONT.pretendard,
    fontSize: 12,
    fontWeight: "500",
  },
  title: {
    color: "#18181B",
    fontFamily: FONT.pretendard,
    fontSize: 18,
    fontWeight: "700",
    marginTop: 14,
  },
  description: {
    color: "#71717A",
    fontFamily: FONT.pretendard,
    fontSize: 13,
    fontWeight: "400",
    lineHeight: 19,
    marginTop: 7,
  },
  footer: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
    marginTop: 18,
  },
  meta: {
    color: "#52525B",
    fontFamily: FONT.pretendard,
    fontSize: 13,
    fontWeight: "500",
  },
  action: {
    fontFamily: FONT.pretendard,
    fontSize: 14,
    fontWeight: "700",
  },
});
