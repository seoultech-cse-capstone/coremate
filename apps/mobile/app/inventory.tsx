import { router } from "expo-router";
import { CheckCircle2, ChevronLeft } from "lucide-react-native";
import { useMemo, useState } from "react";
import { Image, Pressable, ScrollView, StyleSheet, Text, View, type GestureResponderEvent } from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";

import { FONT } from "../constants/fonts";
import { useAuth, useRequireMember } from "../src/features/auth";
import {
  AvatarStage,
  DEFAULT_LOCAL_KEY_BY_SLOT,
  FURNITURE_ITEM_STEM,
  FURNITURE_SETS,
  FURNITURE_TYPES,
  LOCAL_AVATAR_CATALOG,
  LOCAL_CATALOG_BY_KEY,
  LOCAL_SLOT_LABEL,
  LOCAL_SLOT_ORDER,
  REQUIRED_LOCAL_SLOTS,
  ROOM_GRID_ROWS,
  ROOM_THEME_ITEM_STEM,
  ROOM_THEMES,
  RoomBackdrop,
  useAvatarEquip,
  useInventory,
  useRoomFurniture,
  useRoomTheme,
  useUpdateEquippedItemsMutation,
  type FurnitureType,
  type LocalAvatarSlot,
  type RoomLayoutInfo,
} from "../src/features/avatar";
import { Toast } from "../src/shared/components/Toast";
import { useToast } from "../src/shared/hooks/useToast";

const ROOM_PREVIEW_HEIGHT = 320;

type MainTab = "avatar" | "myroom";
type RoomTab = "theme" | "furniture";

// 서버 아이템 image_url("/assets/avatar/hair_basic.png")에서 파일명 stem("hair_basic")만
// 뽑아낸다. 이 stem이 로컬 카탈로그의 key/가구·테마 매핑과 이어지는 유일한 값이다.
function stemFromImageUrl(imageUrl: string | null): string | null {
  if (!imageUrl) {
    return null;
  }
  const fileName = imageUrl.split("/").pop();
  return fileName ? fileName.replace(/\.[^.]+$/, "") : null;
}

// 아이템 보관함(코디)과 마이룸 인벤토리(테마+가구)를 한 화면으로 합쳤다. 상단
// 아바타/마이룸 탭으로 큰 영역을 나누고, 마이룸 탭 안에서 다시 테마/가구를
// 고른다. 각 영역의 세부 동작(착용 반영, 가구 배치·치우기 방식)은 기존과 동일.
export default function InventoryScreen() {
  useRequireMember();
  const [mainTab, setMainTab] = useState<MainTab>("avatar");

  // 아바타 탭 상태
  const [selectedSlot, setSelectedSlot] = useState<LocalAvatarSlot>("HAIR");
  const { equipped, setEquippedItem } = useAvatarEquip();

  // 서버 보유 목록 — 이 화면의 아바타/테마/가구는 전부 이 집합으로 필터링한다.
  // 기본 지급 보장(getOrCreateProfile → ensureGranted)은 앱 진입 시 AvatarProfileSync가
  // 담당하므로, 여기서는 보유 목록만 바로 불러온다.
  const { accessToken } = useAuth();
  const inventoryQuery = useInventory(accessToken);
  const ownedStems = useMemo(() => {
    const set = new Set<string>();
    for (const group of inventoryQuery.data?.groups ?? []) {
      for (const item of group.items) {
        const stem = stemFromImageUrl(item.imageUrl);
        if (stem) {
          set.add(stem);
        }
      }
    }
    return set;
  }, [inventoryQuery.data]);

  // 로컬 카탈로그 key → 서버 userItemId. 착용 상태를 서버에 저장할 때
  // PUT /api/avatar/equipped-items 가 userItemId 를 요구하는데, 프론트 착용은
  // 로컬 key 로 동작해서 이 매핑으로 잇는다.
  const keyToUserItemId = useMemo(() => {
    const map = new Map<string, number>();
    for (const group of inventoryQuery.data?.groups ?? []) {
      for (const item of group.items) {
        const stem = stemFromImageUrl(item.imageUrl);
        if (stem) {
          map.set(stem, item.userItemId);
        }
      }
    }
    return map;
  }, [inventoryQuery.data]);

  const equipMutation = useUpdateEquippedItemsMutation(accessToken);

  // 착용 변경은 로컬(AvatarEquipContext)에 즉시 반영해 화면을 갱신하고, 동시에
  // 서버에도 저장한다. key 가 null 이면 해제(userItemId=null), 아니면 매핑된 userItemId 전송.
  const handleEquip = (slot: LocalAvatarSlot, key: string | null) => {
    setEquippedItem(slot, key);
    const userItemId = key === null ? null : (keyToUserItemId.get(key) ?? null);
    // 보유 아이템만 노출되므로 key→userItemId 매핑은 정상적으로 잡힌다. 혹시
    // 매핑이 없으면(비정상) 서버 저장만 건너뛰고 로컬 반영은 유지한다.
    if (key !== null && userItemId === null) {
      return;
    }
    equipMutation.mutate({ equippedItems: [{ slot, userItemId }] });
  };

  // 마이룸 탭 상태
  const { themeKey, setThemeKey } = useRoomTheme();
  const { placements, setPlacement } = useRoomFurniture();
  const [roomTab, setRoomTab] = useState<RoomTab>("theme");
  const [selectedFurnitureType, setSelectedFurnitureType] = useState<FurnitureType>("bed");
  const [layout, setLayout] = useState<RoomLayoutInfo | null>(null);
  const { message: toastMessage, opacity: toastOpacity, showToast } = useToast();

  const itemsBySlot = useMemo(() => {
    const map = new Map<LocalAvatarSlot, typeof LOCAL_AVATAR_CATALOG>();
    for (const slot of LOCAL_SLOT_ORDER) {
      map.set(
        slot,
        LOCAL_AVATAR_CATALOG.filter((item) => item.slot === slot && ownedStems.has(item.key)),
      );
    }
    return map;
  }, [ownedStems]);

  const ownedThemes = useMemo(
    () => ROOM_THEMES.filter((theme) => ownedStems.has(ROOM_THEME_ITEM_STEM[theme.key])),
    [ownedStems],
  );
  const ownedFurnitureTypes = useMemo(
    () => FURNITURE_TYPES.filter((type) => ownedStems.has(FURNITURE_ITEM_STEM[type])),
    [ownedStems],
  );

  const currentItems = itemsBySlot.get(selectedSlot) ?? [];
  const isRequiredSlot = REQUIRED_LOCAL_SLOTS.includes(selectedSlot);
  const rawSelection = equipped[selectedSlot];
  const effectiveKey =
    rawSelection !== undefined ? rawSelection : (DEFAULT_LOCAL_KEY_BY_SLOT[selectedSlot] ?? null);

  // 펫은 옷처럼 아바타 캔버스에 겹쳐 그리는 게 아니라 방에서 아바타를 뒤따라 다니는
  // 별도 스프라이트라, AvatarStage에는 안 넘기고 미리보기 옆에 따로 그려서 보여준다.
  const rawPetSelection = equipped.PET;
  const equippedPetKey = rawPetSelection !== undefined ? rawPetSelection : (DEFAULT_LOCAL_KEY_BY_SLOT.PET ?? null);
  const equippedPetItem = equippedPetKey ? LOCAL_CATALOG_BY_KEY.get(equippedPetKey) : undefined;

  const handleFloorPress = (event: GestureResponderEvent) => {
    if (!layout) {
      return;
    }
    // 보유하지 않은 가구는 놓을 수 없다. 목록이 비면 기본 선택값(bed)이 남아 있어도
    // 여기서 막지 않으면 미보유 가구가 배치돼 버린다.
    if (!ownedStems.has(FURNITURE_ITEM_STEM[selectedFurnitureType])) {
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

            <Text style={styles.title}>인벤토리</Text>
          </View>

          <View style={styles.tabRow}>
            <Pressable
              style={[styles.tabButton, mainTab === "avatar" && styles.tabButtonSelected]}
              onPress={() => setMainTab("avatar")}
            >
              <Text style={[styles.tabButtonText, mainTab === "avatar" && styles.tabButtonTextSelected]}>
                아바타
              </Text>
            </Pressable>
            <Pressable
              style={[styles.tabButton, mainTab === "myroom" && styles.tabButtonSelected]}
              onPress={() => setMainTab("myroom")}
            >
              <Text style={[styles.tabButtonText, mainTab === "myroom" && styles.tabButtonTextSelected]}>
                마이룸
              </Text>
            </Pressable>
          </View>

          {mainTab === "avatar" ? (
            <>
              <View style={styles.avatarPreviewArea}>
                <AvatarStage equipped={equipped} scale={0.55} />
                {equippedPetItem ? (
                  <Image resizeMode="contain" source={equippedPetItem.source} style={styles.petPreviewImage} />
                ) : null}
              </View>

              <ScrollView
                horizontal
                contentContainerStyle={styles.chipRow}
                showsHorizontalScrollIndicator={false}
                style={styles.chipScroller}
              >
                {LOCAL_SLOT_ORDER.map((slot) => {
                  const isSelected = slot === selectedSlot;
                  return (
                    <Pressable
                      key={slot}
                      style={[styles.chip, isSelected && styles.chipSelected]}
                      onPress={() => setSelectedSlot(slot)}
                    >
                      <Text style={[styles.chipText, isSelected && styles.chipTextSelected]}>
                        {LOCAL_SLOT_LABEL[slot]}
                      </Text>
                    </Pressable>
                  );
                })}
              </ScrollView>

              <ScrollView contentContainerStyle={styles.grid} showsVerticalScrollIndicator={false}>
                {inventoryQuery.isPending ? (
                  <Text style={styles.statusText}>불러오는 중…</Text>
                ) : null}

                {!inventoryQuery.isPending && !isRequiredSlot ? (
                  <Pressable
                    style={[styles.card, effectiveKey === null && styles.cardSelected]}
                    onPress={() => handleEquip(selectedSlot, null)}
                  >
                    {effectiveKey === null ? (
                      <View style={styles.selectedBadge}>
                        <CheckCircle2 color="#16C45B" size={18} strokeWidth={2.4} />
                      </View>
                    ) : null}
                    <View style={styles.thumbnail}>
                      <Text style={styles.thumbnailFallback}>-</Text>
                    </View>
                    <Text style={styles.cardLabel}>없음</Text>
                  </Pressable>
                ) : null}

                {currentItems.map((item) => {
                  const isEquipped = item.key === effectiveKey;

                  return (
                    <Pressable
                      key={item.key}
                      style={[styles.card, isEquipped && styles.cardSelected]}
                      onPress={() => handleEquip(selectedSlot, item.key)}
                    >
                      {isEquipped ? (
                        <View style={styles.selectedBadge}>
                          <CheckCircle2 color="#16C45B" size={18} strokeWidth={2.4} />
                        </View>
                      ) : null}
                      <View style={styles.thumbnail}>
                        {item.slot === "PET" ? (
                          <Image resizeMode="contain" source={item.source} style={styles.petThumbnailImage} />
                        ) : (
                          <AvatarStage equipped={{ [item.slot]: item.key }} scale={0.16} />
                        )}
                      </View>
                      <Text numberOfLines={1} style={styles.cardLabel}>
                        {item.label}
                      </Text>
                    </Pressable>
                  );
                })}
              </ScrollView>
            </>
          ) : (
            <>
              <View style={styles.roomPreviewArea}>
                <RoomBackdrop
                  height={ROOM_PREVIEW_HEIGHT}
                  onFloorPress={roomTab === "furniture" ? handleFloorPress : undefined}
                  onLayoutChange={setLayout}
                />
              </View>

              <View style={styles.tabRow}>
                <Pressable
                  style={[styles.tabButton, roomTab === "theme" && styles.tabButtonSelected]}
                  onPress={() => setRoomTab("theme")}
                >
                  <Text style={[styles.tabButtonText, roomTab === "theme" && styles.tabButtonTextSelected]}>
                    테마
                  </Text>
                </Pressable>
                <Pressable
                  style={[styles.tabButton, roomTab === "furniture" && styles.tabButtonSelected]}
                  onPress={() => setRoomTab("furniture")}
                >
                  <Text style={[styles.tabButtonText, roomTab === "furniture" && styles.tabButtonTextSelected]}>
                    가구
                  </Text>
                </Pressable>
              </View>

              {roomTab === "theme" ? (
                <ScrollView contentContainerStyle={styles.grid} showsVerticalScrollIndicator={false}>
                  {inventoryQuery.isPending ? (
                    <Text style={styles.statusText}>불러오는 중…</Text>
                  ) : ownedThemes.length === 0 ? (
                    <Text style={styles.statusText}>보유한 테마가 없어요</Text>
                  ) : null}

                  {ownedThemes.map((theme) => {
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
                    {inventoryQuery.isPending ? (
                      <Text style={styles.statusText}>불러오는 중…</Text>
                    ) : ownedFurnitureTypes.length === 0 ? (
                      <Text style={styles.statusText}>보유한 가구가 없어요</Text>
                    ) : null}

                    {ownedFurnitureTypes.map((type) => {
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
  avatarPreviewArea: {
    flexDirection: "row",
    alignItems: "flex-end",
    justifyContent: "center",
    gap: 8,
    paddingVertical: 12,
    backgroundColor: "#F8FAFC",
  },
  petPreviewImage: {
    width: 48,
    height: 40,
  },
  petThumbnailImage: {
    width: 48,
    height: 40,
  },
  chipScroller: {
    flexGrow: 0,
    borderBottomWidth: 1,
    borderBottomColor: "#F1F5F9",
  },
  chipRow: {
    paddingHorizontal: 16,
    paddingVertical: 12,
    gap: 8,
  },
  chip: {
    borderRadius: 999,
    borderWidth: 1,
    borderColor: "#E5E7EB",
    backgroundColor: "#ffffff",
    paddingHorizontal: 14,
    paddingVertical: 8,
  },
  chipSelected: {
    borderColor: "#6D5DFB",
    backgroundColor: "#6D5DFB",
  },
  chipText: {
    color: "#475569",
    fontFamily: FONT.pretendard,
    fontSize: 13,
    fontWeight: "600",
  },
  chipTextSelected: {
    color: "#ffffff",
  },
  roomPreviewArea: {
    paddingHorizontal: 20,
    paddingTop: 12,
  },
  helperText: {
    paddingHorizontal: 20,
    color: "#64748b",
    fontFamily: FONT.pretendard,
    fontSize: 13,
    lineHeight: 19,
  },
  statusText: {
    width: "100%",
    paddingVertical: 40,
    textAlign: "center",
    color: "#94a3b8",
    fontFamily: FONT.pretendard,
    fontSize: 14,
    fontWeight: "600",
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
  thumbnail: {
    width: 60,
    height: 88,
    alignItems: "center",
    justifyContent: "center",
    marginBottom: 6,
    overflow: "hidden",
  },
  thumbnailFallback: {
    color: "#94a3b8",
    fontFamily: FONT.pretendard,
    fontSize: 20,
    fontWeight: "700",
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
