import { router } from "expo-router";
import { BookmarkX, ChevronLeft } from "lucide-react-native";
import { Pressable, ScrollView, StyleSheet, Text, View } from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";

import { FONT } from "../constants/fonts";

const favoriteProblems = [
  {
    id: 1,
    subject: "Data Structure",
    concept: "Static Array",
    preview:
      "배열에서 임의 접근이 O(1)에 가능한 이유로 가장 적절한 것은 무엇인가?",
    date: "2026.07.08",
  },
  {
    id: 2,
    subject: "Operating System",
    concept: "Process Scheduling",
    preview:
      "라운드 로빈 스케줄링에서 time quantum이 너무 작을 때 발생할 수 있는 문제는?",
    date: "2026.07.06",
  },
  {
    id: 3,
    subject: "Network",
    concept: "TCP Reliability",
    preview:
      "TCP가 신뢰성 있는 전송을 제공하기 위해 사용하는 대표적인 메커니즘은?",
    date: "2026.07.02",
  },
];

export default function FavoriteProblemsScreen() {
  return (
    <View style={styles.screen}>
      <SafeAreaView edges={["top"]} style={styles.safeArea}>
        <View style={styles.container}>
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

            <Text style={styles.title}>즐겨찾기 문제 목록</Text>
          </View>

          <ScrollView
            contentContainerStyle={styles.content}
            showsVerticalScrollIndicator={false}
          >
            <Text style={styles.summary}>
              저장한 문제를 다시 확인하고 해설로 복습할 수 있어요.
            </Text>

            <View style={styles.list}>
              {favoriteProblems.map((item) => (
                <Pressable key={item.id} style={styles.card}>
                  <View style={styles.cardHeader}>
                    <View>
                      <Text style={styles.subject}>{item.subject}</Text>
                      <Text style={styles.concept}>{item.concept}</Text>
                    </View>

                    <Pressable
                      accessibilityLabel="즐겨찾기 해제"
                      accessibilityRole="button"
                      hitSlop={8}
                      style={styles.removeButton}
                    >
                      <BookmarkX color="#EF4444" size={20} strokeWidth={2.2} />
                    </Pressable>
                  </View>

                  <Text numberOfLines={2} style={styles.preview}>
                    {item.preview}
                  </Text>

                  <View style={styles.cardFooter}>
                    <Text style={styles.date}>{item.date}</Text>
                    <Text style={styles.linkText}>해설 보기</Text>
                  </View>
                </Pressable>
              ))}
            </View>
          </ScrollView>
        </View>
      </SafeAreaView>
    </View>
  );
}

const styles = StyleSheet.create({
  screen: {
    flex: 1,
    backgroundColor: "#ffffff",
  },
  safeArea: {
    flex: 1,
    backgroundColor: "transparent",
  },
  container: {
    flex: 1,
    backgroundColor: "#ffffff",
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
  content: {
    paddingHorizontal: 20,
    paddingTop: 18,
    paddingBottom: 36,
  },
  summary: {
    color: "#64748b",
    fontFamily: FONT.pretendard,
    fontSize: 14,
    fontWeight: "500",
    lineHeight: 20,
    marginBottom: 16,
  },
  list: {
    gap: 14,
  },
  card: {
    borderRadius: 18,
    backgroundColor: "#F8FAFC",
    borderWidth: 1,
    borderColor: "#EEF2F7",
    padding: 16,
  },
  cardHeader: {
    flexDirection: "row",
    alignItems: "flex-start",
    justifyContent: "space-between",
    gap: 12,
  },
  subject: {
    color: "#6D5DFB",
    fontFamily: FONT.kufam,
    fontSize: 13,
    fontWeight: "700",
  },
  concept: {
    marginTop: 4,
    color: "#111827",
    fontFamily: FONT.pretendard,
    fontSize: 16,
    fontWeight: "700",
  },
  removeButton: {
    alignItems: "center",
    justifyContent: "center",
    width: 36,
    height: 36,
    borderRadius: 12,
    backgroundColor: "#FEE2E2",
  },
  preview: {
    marginTop: 14,
    color: "#334155",
    fontFamily: FONT.pretendard,
    fontSize: 14,
    fontWeight: "500",
    lineHeight: 21,
  },
  cardFooter: {
    marginTop: 16,
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
  },
  date: {
    color: "#94a3b8",
    fontFamily: FONT.kufam,
    fontSize: 12,
    fontWeight: "500",
  },
  linkText: {
    color: "#6D5DFB",
    fontFamily: FONT.pretendard,
    fontSize: 13,
    fontWeight: "700",
  },
});
