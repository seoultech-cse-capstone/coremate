import { router } from "expo-router";
import { BookmarkX, ChevronLeft } from "lucide-react-native";
import { useCallback, useEffect, useRef, useState } from "react";
import {
  ActivityIndicator,
  FlatList,
  Modal,
  Pressable,
  StyleSheet,
  Text,
  TextInput,
  View,
  type ViewToken,
} from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";

import { FONT } from "../constants/fonts";
import { useAuth } from "../src/features/auth";
import { bookmarkApi } from "../src/features/bookmark/bookmarkApi";
import type { BookmarkedQuestion } from "../src/features/bookmark/types";
import { contentApi } from "../src/features/content/contentApi";
import { Toast } from "../src/shared/components/Toast";
import { useToast } from "../src/shared/hooks/useToast";
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
  const [memoModal, setMemoModal] = useState<{ bookmarkId: number } | null>(null);
  const [memoDraft, setMemoDraft] = useState("");
  const [isSavingMemo, setIsSavingMemo] = useState(false);
  const { message: toastMessage, opacity: toastOpacity, showToast } = useToast();
  // accessToken은 배경 토큰 재발급이 일어날 때마다 값이 바뀐다. 이 화면에
  // 처음 진입했을 때 딱 한 번만 목록을 불러오고, 재발급으로 인한 재실행은
  // 무시해야 메모 저장/북마크 삭제 도중 목록이 통째로 다시 조회되며
  // 경합하는 것을 막을 수 있다.
  const hasLoadedRef = useRef(false);

  // 목록 API에는 문제 본문이 없어(상세 API에만 있음) 화면에 보이는 카드부터
  // 순서대로 상세를 조회해 미리보기를 채운다. 한 번 시도한 questionId는
  // 성공/실패와 무관하게 다시 요청하지 않는다.
  const [previewByQuestionId, setPreviewByQuestionId] = useState<Record<number, string>>({});
  const attemptedQuestionIdsRef = useRef<Set<number>>(new Set());

  const fetchPreview = useCallback(
    (questionId: number) => {
      if (!accessToken || attemptedQuestionIdsRef.current.has(questionId)) {
        return;
      }
      attemptedQuestionIdsRef.current.add(questionId);
      bookmarkApi
        .getBookmarkedQuestionDetail(questionId, accessToken)
        .then((detail) => {
          setPreviewByQuestionId((current) => ({ ...current, [questionId]: detail.questionText }));
        })
        .catch((error) => {
          console.error("[favorite-problems] 문제 미리보기 조회 실패", error);
        });
    },
    [accessToken],
  );

  const fetchPreviewRef = useRef(fetchPreview);
  useEffect(() => {
    fetchPreviewRef.current = fetchPreview;
  }, [fetchPreview]);

  const viewabilityConfigRef = useRef({ itemVisiblePercentThreshold: 40 });
  const onViewableItemsChanged = useRef(
    ({ viewableItems }: { viewableItems: ViewToken[] }) => {
      for (const viewToken of viewableItems) {
        const item = viewToken.item as FavoriteItem | undefined;
        if (item) {
          fetchPreviewRef.current(item.questionId);
        }
      }
    },
  ).current;

  useEffect(() => {
    if (hasLoadedRef.current) {
      return;
    }
    if (!accessToken) {
      setState({ status: "error", message: "로그인이 필요합니다." });
      return;
    }

    hasLoadedRef.current = true;
    let cancelled = false;

    (async () => {
      try {
        const bookmarks = await bookmarkApi.getBookmarkedQuestions(accessToken);
        const uniqueConceptIds = [...new Set(bookmarks.map((item) => item.conceptId))];
        const conceptDetails = await Promise.all(
          uniqueConceptIds.map((conceptId) =>
            contentApi.getConceptDetail(conceptId).catch((error) => {
              console.error("[favorite-problems] 개념 정보 조회 실패", error);
              return null;
            }),
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
        hasLoadedRef.current = false;
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
      showToast("즐겨찾기에서 삭제했어요");
    } catch (error) {
      console.error("[favorite-problems] 즐겨찾기 해제 실패", error);
      showToast("즐겨찾기 삭제에 실패했어요. 다시 시도해주세요.");
    }
  };

  const openMemoModal = (item: FavoriteItem) => {
    setMemoModal({ bookmarkId: item.bookmarkId });
    setMemoDraft(item.memo ?? "");
  };

  const closeMemoModal = () => {
    setMemoModal(null);
    setMemoDraft("");
  };

  const saveMemo = async () => {
    if (!accessToken || !memoModal || isSavingMemo) {
      return;
    }
    const bookmarkId = memoModal.bookmarkId;
    const trimmedMemo = memoDraft.trim();

    setIsSavingMemo(true);
    try {
      await bookmarkApi.updateMemo(bookmarkId, trimmedMemo, accessToken);
      setState((current) =>
        current.status === "ready"
          ? {
              status: "ready",
              items: current.items.map((item) =>
                item.bookmarkId === bookmarkId
                  ? { ...item, memo: trimmedMemo || null }
                  : item,
              ),
            }
          : current,
      );
      showToast("메모를 저장했어요");
      closeMemoModal();
    } catch (error) {
      console.error("[favorite-problems] 메모 저장 실패", error);
      showToast("메모 저장에 실패했어요. 다시 시도해주세요.");
    } finally {
      setIsSavingMemo(false);
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
            <FlatList
              contentContainerStyle={styles.content}
              data={state.items}
              ItemSeparatorComponent={() => <View style={styles.itemSeparator} />}
              keyExtractor={(item) => String(item.bookmarkId)}
              ListHeaderComponent={
                <Text style={styles.summary}>
                  저장한 문제를 다시 확인하고 개념을 복습할 수 있어요.
                </Text>
              }
              onViewableItemsChanged={onViewableItemsChanged}
              showsVerticalScrollIndicator={false}
              viewabilityConfig={viewabilityConfigRef.current}
              renderItem={({ item }) => (
                <Pressable
                  style={styles.card}
                  onPress={() =>
                    router.push({
                      pathname: "/favorite-problem-detail",
                      params: { questionId: String(item.questionId) },
                    })
                  }
                >
                  {/* 가장 중요한 건 문제 본문이라 제목 자리에 크게 두고, 과목·개념·난이도는 작게 곁들인다(#243). */}
                  <View style={styles.cardHeader}>
                    <Text numberOfLines={1} style={styles.breadcrumb}>
                      <Text style={styles.subject}>{item.subjectName}</Text>
                      {"  ·  "}
                      {item.conceptName}
                    </Text>

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

                  {previewByQuestionId[item.questionId] ? (
                    <Text numberOfLines={3} style={styles.questionText}>
                      {previewByQuestionId[item.questionId]}
                    </Text>
                  ) : (
                    // 본문은 화면에 보이는 카드부터 따로 불러온다. 오기 전이나 실패했을 때도 눌러서 볼 수 있다.
                    <Text style={styles.questionPlaceholder}>눌러서 문제 보기</Text>
                  )}

                  <View style={styles.tagRow}>
                    <Text style={styles.tag}>난이도 {item.difficultyLevel}</Text>
                    <Text style={styles.tag}>
                      {QUESTION_FORMAT_LABEL[item.questionFormat] ?? item.questionFormat}
                    </Text>
                  </View>

                  {item.memo && (
                    <Text numberOfLines={1} style={styles.memoPreview}>
                      메모: {item.memo}
                    </Text>
                  )}

                  <View style={styles.cardFooter}>
                    <Text style={styles.date}>{formatDate(item.bookmarkedAt)}</Text>

                    <View style={styles.footerActions}>
                      <Pressable
                        hitSlop={6}
                        onPress={(event) => {
                          event.stopPropagation();
                          router.push({
                            pathname: "/concept-detail",
                            params: { conceptId: String(item.conceptId) },
                          });
                        }}
                      >
                        <Text style={styles.linkText}>개념 보기</Text>
                      </Pressable>
                      <Text style={styles.footerDivider}>|</Text>
                      <Pressable
                        hitSlop={6}
                        onPress={(event) => {
                          event.stopPropagation();
                          openMemoModal(item);
                        }}
                      >
                        <Text style={styles.linkText}>
                          {item.memo ? "메모 수정" : "메모 작성"}
                        </Text>
                      </Pressable>
                    </View>
                  </View>
                </Pressable>
              )}
            />
          )}
        </View>
      </SafeAreaView>

      <Toast message={toastMessage} opacity={toastOpacity} />

      <Modal
        animationType="fade"
        transparent
        visible={memoModal !== null}
        onRequestClose={closeMemoModal}
      >
        <View style={styles.modalDim}>
          <View style={styles.modalCard}>
            <Text style={styles.modalTitle}>메모</Text>
            <TextInput
              autoFocus
              multiline
              maxLength={2000}
              placeholder="이 문제에 대한 메모를 남겨보세요"
              placeholderTextColor="#94a3b8"
              style={styles.memoInput}
              value={memoDraft}
              onChangeText={setMemoDraft}
            />

            <View style={styles.modalActions}>
              <Pressable style={styles.cancelButton} onPress={closeMemoModal}>
                <Text style={styles.cancelButtonText}>취소</Text>
              </Pressable>

              <Pressable
                disabled={isSavingMemo}
                style={styles.saveButton}
                onPress={saveMemo}
              >
                <Text style={styles.saveButtonText}>
                  {isSavingMemo ? "저장 중…" : "저장"}
                </Text>
              </Pressable>
            </View>
          </View>
        </View>
      </Modal>
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
  itemSeparator: {
    height: 14,
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
    alignItems: "center",
    justifyContent: "space-between",
    gap: 12,
  },
  breadcrumb: {
    flex: 1,
    color: "#64748b",
    fontFamily: FONT.pretendard,
    fontSize: 12,
    fontWeight: "500",
  },
  subject: {
    color: "#6D5DFB",
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
  questionText: {
    marginTop: 6,
    color: "#111827",
    fontFamily: FONT.pretendard,
    fontSize: 16,
    fontWeight: "600",
    lineHeight: 23,
  },
  questionPlaceholder: {
    marginTop: 6,
    color: "#94a3b8",
    fontFamily: FONT.pretendard,
    fontSize: 15,
    fontWeight: "500",
    lineHeight: 23,
  },
  tagRow: {
    marginTop: 10,
    flexDirection: "row",
    gap: 6,
  },
  tag: {
    overflow: "hidden",
    borderRadius: 999,
    backgroundColor: "#EEF2F7",
    paddingHorizontal: 8,
    paddingVertical: 3,
    color: "#64748b",
    fontFamily: FONT.pretendard,
    fontSize: 11,
    fontWeight: "600",
  },
  memoPreview: {
    marginTop: 10,
    color: "#6D5DFB",
    fontFamily: FONT.pretendard,
    fontSize: 13,
    fontWeight: "500",
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
  footerActions: {
    flexDirection: "row",
    alignItems: "center",
    gap: 8,
  },
  footerDivider: {
    color: "#CBD5E1",
    fontSize: 12,
  },
  linkText: {
    color: "#6D5DFB",
    fontFamily: FONT.pretendard,
    fontSize: 13,
    fontWeight: "700",
  },
  modalDim: {
    flex: 1,
    alignItems: "center",
    justifyContent: "center",
    backgroundColor: "rgba(15, 23, 42, 0.35)",
    paddingHorizontal: 28,
  },
  modalCard: {
    width: "100%",
    borderRadius: 20,
    backgroundColor: "#ffffff",
    padding: 20,
  },
  modalTitle: {
    color: "#111827",
    fontFamily: FONT.pretendard,
    fontSize: 18,
    fontWeight: "800",
  },
  memoInput: {
    minHeight: 96,
    marginTop: 14,
    borderRadius: 14,
    borderWidth: 1,
    borderColor: "#E5E7EB",
    backgroundColor: "#F8FAFC",
    paddingHorizontal: 14,
    paddingVertical: 12,
    color: "#111827",
    fontFamily: FONT.pretendard,
    fontSize: 14,
    textAlignVertical: "top",
  },
  modalActions: {
    flexDirection: "row",
    gap: 10,
    marginTop: 18,
  },
  cancelButton: {
    flex: 1,
    minHeight: 48,
    alignItems: "center",
    justifyContent: "center",
    borderRadius: 15,
    backgroundColor: "#F1F5F9",
  },
  cancelButtonText: {
    color: "#334155",
    fontFamily: FONT.pretendard,
    fontSize: 15,
    fontWeight: "700",
  },
  saveButton: {
    flex: 1,
    minHeight: 48,
    alignItems: "center",
    justifyContent: "center",
    borderRadius: 15,
    backgroundColor: "#6D5DFB",
  },
  saveButtonText: {
    color: "#ffffff",
    fontFamily: FONT.pretendard,
    fontSize: 15,
    fontWeight: "700",
  },
});
