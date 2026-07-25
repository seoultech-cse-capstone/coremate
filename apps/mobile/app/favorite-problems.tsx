import { router } from "expo-router";
import { BookmarkX, ChevronLeft } from "lucide-react-native";
import { useEffect, useState } from "react";
import {
  ActivityIndicator,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  View,
} from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";

import { FONT } from "../constants/fonts";
import { useAuth } from "../src/features/auth";
import { bookmarkApi } from "../src/features/bookmark/bookmarkApi";
import type { BookmarkedQuestion } from "../src/features/bookmark/types";
import { contentApi } from "../src/features/content/contentApi";
import { getApiErrorMessage } from "../src/shared/utils/apiError";

type FavoriteItem = BookmarkedQuestion & {
  subjectName: string;
  conceptName: string;
};

const QUESTION_FORMAT_LABEL: Record<string, string> = {
  MCQ_4: "객관식",
  OX: "OX",
  SHORT_ANSWER: "주관식",
  MULTI_SELECT: "복수선택",
};

function formatDate(isoString: string): string {
  return isoString.slice(0, 10).replace(/-/g, ".");
}

type ScreenState =
  | { status: "loading" }
  | { status: "error"; message: string }
  | { status: "ready"; items: FavoriteItem[] };

export default function FavoriteProblemsScreen() {
  const { accessToken } = useAuth();
  const [state, setState] = useState<ScreenState>({ status: "loading" });

  useEffect(() => {
    if (!accessToken) {
      setState({ status: "error", message: "로그인이 필요합니다." });
      return;
    }

    let cancelled = false;

    (async () => {
      try {
        const bookmarks = await bookmarkApi.getBookmarkedQuestions(accessToken);
        const uniqueConceptIds = [...new Set(bookmarks.map((item) => item.conceptId))];
        const conceptDetails = await Promise.all(
          uniqueConceptIds.map((conceptId) =>
            contentApi.getConceptDetail(conceptId).catch(() => null),
          ),
        );
        const conceptById = new Map(
          conceptDetails
            .filter((detail) => detail !== null)
            .map((detail) => [detail.conceptId, detail]),
        );

        if (cancelled) {
          return;
        }

        const items: FavoriteItem[] = bookmarks
          .map((bookmark) => {
            const detail = conceptById.get(bookmark.conceptId);
            return {
              ...bookmark,
              subjectName: detail?.subjectName ?? "",
              conceptName: detail?.conceptName ?? `개념 #${bookmark.conceptId}`,
            };
          })
          .sort((a, b) => b.bookmarkedAt.localeCompare(a.bookmarkedAt));

        setState({ status: "ready", items });
      } catch (error) {
        if (cancelled) {
          return;
        }
        setState({
          status: "error",
          message: getApiErrorMessage(error, "즐겨찾기 목록을 불러오지 못했습니다."),
        });
      }
    })();

    return () => {
      cancelled = true;
    };
  }, [accessToken]);

  const removeBookmark = async (bookmarkId: number) => {
    if (!accessToken || state.status !== "ready") {
      return;
    }
    try {
      await bookmarkApi.remove(bookmarkId, accessToken);
      setState((current) =>
        current.status === "ready"
          ? {
              status: "ready",
              items: current.items.filter((item) => item.bookmarkId !== bookmarkId),
            }
          : current,
      );
    } catch (error) {
      console.error("[favorite-problems] 즐겨찾기 해제 실패", error);
    }
  };

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

          {state.status === "loading" && (
            <View style={styles.stateContainer}>
              <ActivityIndicator color="#6D5DFB" size="large" />
            </View>
          )}

          {state.status === "error" && (
            <View style={styles.stateContainer}>
              <Text style={styles.stateText}>{state.message}</Text>
            </View>
          )}

          {state.status === "ready" && state.items.length === 0 && (
            <View style={styles.stateContainer}>
              <Text style={styles.stateText}>즐겨찾기한 문제가 없습니다.</Text>
            </View>
          )}

          {state.status === "ready" && state.items.length > 0 && (
            <ScrollView
              contentContainerStyle={styles.content}
              showsVerticalScrollIndicator={false}
            >
              <Text style={styles.summary}>
                저장한 문제를 다시 확인하고 개념을 복습할 수 있어요.
              </Text>

              <View style={styles.list}>
                {state.items.map((item) => (
                  <Pressable
                    key={item.bookmarkId}
                    style={styles.card}
                    onPress={() =>
                      router.push({
                        pathname: "/concept-detail",
                        params: { conceptId: String(item.conceptId) },
                      })
                    }
                  >
                    <View style={styles.cardHeader}>
                      <View>
                        <Text style={styles.subject}>{item.subjectName}</Text>
                        <Text style={styles.concept}>{item.conceptName}</Text>
                      </View>

                      <Pressable
                        accessibilityLabel="즐겨찾기 해제"
                        accessibilityRole="button"
                        hitSlop={8}
                        style={styles.removeButton}
                        onPress={(event) => {
                          event.stopPropagation();
                          removeBookmark(item.bookmarkId);
                        }}
                      >
                        <BookmarkX color="#EF4444" size={20} strokeWidth={2.2} />
                      </Pressable>
                    </View>

                    <Text style={styles.preview}>
                      난이도 {item.difficultyLevel} ·{" "}
                      {QUESTION_FORMAT_LABEL[item.questionFormat] ?? item.questionFormat}
                    </Text>

                    <View style={styles.cardFooter}>
                      <Text style={styles.date}>{formatDate(item.bookmarkedAt)}</Text>
                      <Text style={styles.linkText}>개념 보기</Text>
                    </View>
                  </Pressable>
                ))}
              </View>
            </ScrollView>
          )}
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
  stateContainer: {
    flex: 1,
    alignItems: "center",
    justifyContent: "center",
    paddingHorizontal: 24,
  },
  stateText: {
    color: "#64748b",
    fontFamily: FONT.pretendard,
    fontSize: 14,
    fontWeight: "500",
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
