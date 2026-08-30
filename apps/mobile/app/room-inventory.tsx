import { router } from "expo-router";
import { CheckCircle2, ChevronLeft } from "lucide-react-native";
import { useState } from "react";
import { Image, Pressable, ScrollView, StyleSheet, Text, View, type GestureResponderEvent } from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";

import { FONT } from "../constants/fonts";
import { useRequireMember } from "../src/features/auth";
import {
  FURNITURE_SETS,
  FURNITURE_TYPES,
  ROOM_GRID_ROWS,
  ROOM_THEMES,
  RoomBackdrop,
  useRoomFurniture,
  useRoomTheme,
  type FurnitureType,
  type RoomLayoutInfo,
} from "../src/features/avatar";
import { Toast } from "../src/shared/components/Toast";
import { useToast } from "../src/shared/hooks/useToast";

const ROOM_PREVIEW_HEIGHT = 320;

type Tab = "theme" | "furniture";

// 방 테마·가구 배치를 한 화면으로 합쳤다. 위 미리보기(RoomBackdrop)는 항상 지금
// 상태(테마+배치된 가구)를 그대로 보여주고, 아래 탭에서 고르는 즉시 반영된다.
// 가구는 종류를 먼저 고른 뒤 미리보기의 바닥을 탭해서 놓는 방식(칸 위치가
// 필요해서 아바타 코디처럼 탭 한 번으로 끝낼 수 없다)은 기존과 동일하게 유지했다.
// 치우기는 별도 도구 없이, 고른 가구가 이미 놓인 자리를 한 번 더 탭하면 된다.
export default function RoomInventoryScreen() {
  useRequireMember();
  const { themeKey, setThemeKey } = useRoomTheme();
  const { placements, setPlacement } = useRoomFurniture();
  const [tab, setTab] = useState<Tab>("theme");
  const [selectedFurnitureType, setSelectedFurnitureType] = useState<FurnitureType>("bed");
  const [layout, setLayout] = useState<RoomLayoutInfo | null>(null);
  const { message: toastMessage, opacity: toastOpacity, showToast } = useToast();

  const handleFloorPress = (event: GestureResponderEvent) => {
    if (!layout) {
      return;
    }
    const { locationX, locationY } = event.nativeEvent;
    const col = Math.floor(locationX / layout.cellWidth);
    const row = Math.floor(locationY / layout.rowHeight);
    if (row < 0 || row >= ROOM_GRID_ROWS) {
      // 격자 아래 빈 여백을 탭한 것 — 가구를 놓을 수 없는 구역이라 무시한다.
      return;
    }

    const currentPosition = placements[selectedFurnitureType];
    const footprintCols = FURNITURE_SETS[selectedFurnitureType].footprintCols;
    const tappedOwnFootprint =
      currentPosition &&
      currentPosition.row === row &&
      col >= currentPosition.col &&
      col < currentPosition.col + footprintCols;

    if (tappedOwnFootprint) {
      setPlacement(selectedFurnitureType, null);
      return;
    }

    const placed = setPlacement(selectedFurnitureType, { row, col });
    if (!placed) {
      showToast("이미 가구가 놓여져있어요");
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

            <Text style={styles.title}>마이룸 인벤토리</Text>
          </View>

          <View style={styles.previewArea}>
            <RoomBackdrop
              height={ROOM_PREVIEW_HEIGHT}
              onFloorPress={tab === "furniture" ? handleFloorPress : undefined}
              onLayoutChange={setLayout}
            />
          </View>

          <View style={styles.tabRow}>
            <Pressable
              style={[styles.tabButton, tab === "theme" && styles.tabButtonSelected]}
              onPress={() => setTab("theme")}
            >
              <Text style={[styles.tabButtonText, tab === "theme" && styles.tabButtonTextSelected]}>테마</Text>
            </Pressable>
            <Pressable
              style={[styles.tabButton, tab === "furniture" && styles.tabButtonSelected]}
              onPress={() => setTab("furniture")}
            >
              <Text style={[styles.tabButtonText, tab === "furniture" && styles.tabButtonTextSelected]}>가구</Text>
            </Pressable>
          </View>

          {tab === "theme" ? (
            <ScrollView contentContainerStyle={styles.grid} showsVerticalScrollIndicator={false}>
              {ROOM_THEMES.map((theme) => {
                const isSelected = theme.key === themeKey;

                return (
                  <Pressable
                    key={theme.key}
                    style={[styles.card, isSelected && styles.cardSelected]}
                    onPress={() => setThemeKey(theme.key)}
                  >
                    {isSelected ? (
                      <View style={styles.selectedBadge}>
                        <CheckCircle2 color="#16C45B" size={20} strokeWidth={2.4} />
                      </View>
                    ) : null}

                    <View style={styles.thumbnailBox}>
                      <Image resizeMode="cover" source={theme.wallSource} style={styles.themePreviewWall} />
                      <Image resizeMode="cover" source={theme.floorSource} style={styles.themePreviewFloor} />
                    </View>

                    <Text style={styles.cardLabel}>{theme.label}</Text>
                  </Pressable>
                );
              })}
            </ScrollView>
          ) : (
            <>
              <Text style={styles.helperText}>
                놓을 가구를 고르고, 위 미리보기의 바닥을 탭해서 놓아보세요. 이미 놓인
                자리를 한 번 더 탭하면 치워져요. 침대·책상은 앉거나 누울 수 있고,
                나머지는 장식용이에요.
              </Text>

              <ScrollView contentContainerStyle={styles.grid} showsVerticalScrollIndicator={false}>
                {FURNITURE_TYPES.map((type) => {
                  const isToolSelected = selectedFurnitureType === type;
                  const isPlaced = Boolean(placements[type]);

                  return (
                    <Pressable
                      key={type}
                      style={[styles.card, isToolSelected && styles.cardSelected]}
                      onPress={() => setSelectedFurnitureType(type)}
                    >
                      {isPlaced ? (
                        <View style={styles.selectedBadge}>
                          <CheckCircle2 color="#16C45B" size={20} strokeWidth={2.4} />
                        </View>
                      ) : null}

                      <View style={styles.thumbnailBox}>
                        <Image
                          resizeMode="contain"
                          source={FURNITURE_SETS[type].pieces[0].source}
                          style={styles.furniturePreviewImage}
                        />
                      </View>

                      <Text style={styles.cardLabel}>{FURNITURE_SETS[type].label}</Text>
                    </Pressable>
                  );
                })}
              </ScrollView>
            </>
          )}
        </View>
      </SafeAreaView>

      <Toast message={toastMessage} opacity={toastOpacity} />
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
  previewArea: {
    paddingHorizontal: 20,
    paddingTop: 12,
  },
  tabRow: {
    flexDirection: "row",
    gap: 8,
    paddingHorizontal: 20,
    paddingVertical: 16,
  },
  tabButton: {
    flex: 1,
    alignItems: "center",
    borderRadius: 999,
    borderWidth: 1,
    borderColor: "#E5E7EB",
    backgroundColor: "#ffffff",
    paddingVertical: 10,
  },
  tabButtonSelected: {
    borderColor: "#6D5DFB",
    backgroundColor: "#6D5DFB",
  },
  tabButtonText: {
    color: "#475569",
    fontFamily: FONT.pretendard,
    fontSize: 14,
    fontWeight: "700",
  },
  tabButtonTextSelected: {
    color: "#ffffff",
  },
  helperText: {
    paddingHorizontal: 20,
    color: "#64748b",
    fontFamily: FONT.pretendard,
    fontSize: 13,
    lineHeight: 19,
  },
  grid: {
    flexDirection: "row",
    flexWrap: "wrap",
    gap: 12,
    padding: 16,
  },
  card: {
    width: "30%",
    borderRadius: 14,
    borderWidth: 2,
    borderColor: "#e5e7eb",
    backgroundColor: "#F8FAFC",
    padding: 8,
    alignItems: "center",
  },
  cardSelected: {
    borderColor: "#6D5DFB",
    backgroundColor: "#F1EFFF",
  },
  selectedBadge: {
    position: "absolute",
    top: 6,
    right: 6,
    zIndex: 1,
  },
  thumbnailBox: {
    width: "100%",
    aspectRatio: 1,
    alignItems: "center",
    justifyContent: "center",
    borderRadius: 10,
    overflow: "hidden",
    marginBottom: 8,
    backgroundColor: "#ffffff",
  },
  themePreviewWall: {
    width: "100%",
    height: "55%",
  },
  themePreviewFloor: {
    width: "100%",
    height: "45%",
  },
  furniturePreviewImage: {
    width: "100%",
    height: "100%",
  },
  cardLabel: {
    color: "#111827",
    fontFamily: FONT.pretendard,
    fontSize: 13,
    fontWeight: "700",
    textAlign: "center",
  },
});
