import { Pressable, StyleSheet, Text, View } from "react-native";

import { FONT } from "../../../../constants/fonts";

type ProblemSetQuestionProps = {
  title: string;
  onExit: () => void;
};

export function ProblemSetQuestion({ title, onExit }: ProblemSetQuestionProps) {
  return (
    <View style={styles.container}>
      <View style={styles.card}>
        <Text style={styles.badge}>1 / 10</Text>
        <Text style={styles.title}>{title}</Text>
        <Text style={styles.question}>
          다음 중 이 범위의 핵심 개념을 가장 정확하게 설명한 보기를 선택하세요.
        </Text>

        <View style={styles.choiceList}>
          {["보기 A", "보기 B", "보기 C", "보기 D"].map((choice) => (
            <Pressable key={choice} style={styles.choice}>
              <Text style={styles.choiceText}>{choice}</Text>
            </Pressable>
          ))}
        </View>

        <Pressable style={styles.submitButton} onPress={onExit}>
          <Text style={styles.submitText}>제출</Text>
        </Pressable>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    paddingHorizontal: 24,
    paddingTop: 32,
  },
  card: {
    borderRadius: 8,
    backgroundColor: "#ffffff",
    padding: 20,
  },
  badge: {
    alignSelf: "flex-start",
    color: "#6D5DFB",
    fontFamily: FONT.pretendard,
    fontSize: 13,
    fontWeight: "700",
  },
  title: {
    color: "#111111",
    fontFamily: FONT.pretendard,
    fontSize: 20,
    fontWeight: "700",
    marginTop: 12,
  },
  question: {
    color: "#1F2937",
    fontFamily: FONT.pretendard,
    fontSize: 16,
    fontWeight: "400",
    lineHeight: 24,
    marginTop: 18,
  },
  choiceList: {
    gap: 10,
    marginTop: 22,
  },
  choice: {
    minHeight: 46,
    justifyContent: "center",
    borderRadius: 8,
    backgroundColor: "#F3F3F3",
    paddingHorizontal: 14,
  },
  choiceText: {
    color: "#111827",
    fontFamily: FONT.pretendard,
    fontSize: 15,
    fontWeight: "500",
  },
  submitButton: {
    alignItems: "center",
    justifyContent: "center",
    minHeight: 46,
    borderRadius: 999,
    backgroundColor: "#6D5DFB",
    marginTop: 24,
  },
  submitText: {
    color: "#ffffff",
    fontFamily: FONT.pretendard,
    fontSize: 15,
    fontWeight: "700",
  },
});
