import { router } from "expo-router";
import { CheckCircle2, ChevronLeft } from "lucide-react-native";
import { useMemo, useState } from "react";
import { Image, Pressable, ScrollView, StyleSheet, Text, View } from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";

import { FONT } from "../constants/fonts";
import { useRequireMember } from "../src/features/auth";
import {
  AvatarStage,
  DEFAULT_LOCAL_KEY_BY_SLOT,
  LOCAL_AVATAR_CATALOG,
  LOCAL_CATALOG_BY_KEY,
  LOCAL_SLOT_LABEL,
  LOCAL_SLOT_ORDER,
  REQUIRED_LOCAL_SLOTS,
  useAvatarEquip,
  type LocalAvatarSlot,
} from "../src/features/avatar";

// 백엔드 아이템 카탈로그가 아직 비어있어(#165 0단계 미확정) 실제 API로 착용 상태를
// 저장할 수 없다. AvatarEquipContext(기기 저장소)에 임시로 남겨서, 홈 화면 등
// 앱 전체에서 같은 착용 상태를 보게 만든 화면. 카탈로그가 정해지면 실제 인벤토리
// API(useInventory) 연동으로 교체한다.
export default function AvatarInventoryScreen() {
  useRequireMember();
  const [selectedSlot, setSelectedSlot] = useState<LocalAvatarSlot>("HAIR");
  const { equipped, setEquippedItem } = useAvatarEquip();

  const itemsBySlot = useMemo(() => {
    const map = new Map<LocalAvatarSlot, typeof LOCAL_AVATAR_CATALOG>();
    for (const slot of LOCAL_SLOT_ORDER) {
      map.set(
        slot,
        LOCAL_AVATAR_CATALOG.filter((item) => item.slot === slot),
      );
    }
    return map;
  }, []);

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

            <Text style={styles.title}>아이템 보관함</Text>
          </View>

          <View style={styles.previewArea}>
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
            {!isRequiredSlot ? (
              <Pressable
                style={[styles.card, effectiveKey === null && styles.cardEquipped]}
                onPress={() => setEquippedItem(selectedSlot, null)}
              >
                {effectiveKey === null ? (
                  <View style={styles.equippedBadge}>
                    <CheckCircle2 color="#16C45B" size={18} strokeWidth={2.4} />
                  </View>
                ) : null}
                <View style={styles.thumbnail}>
                  <Text style={styles.thumbnailFallback}>-</Text>
                </View>
                <Text style={styles.itemName}>없음</Text>
              </Pressable>
            ) : null}

            {currentItems.map((item) => {
              const isEquipped = item.key === effectiveKey;

              return (
                <Pressable
                  key={item.key}
                  style={[styles.card, isEquipped && styles.cardEquipped]}
                  onPress={() => setEquippedItem(selectedSlot, item.key)}
                >
                  {isEquipped ? (
                    <View style={styles.equippedBadge}>
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
                  <Text numberOfLines={1} style={styles.itemName}>
                    {item.label}
                  </Text>
                </Pressable>
              );
            })}
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
  previewArea: {
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
  cardEquipped: {
    borderColor: "#6D5DFB",
    backgroundColor: "#F1EFFF",
  },
  equippedBadge: {
    position: "absolute",
    top: 6,
    right: 6,
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
  itemName: {
    color: "#111827",
    fontFamily: FONT.pretendard,
    fontSize: 12,
    fontWeight: "700",
    textAlign: "center",
  },
});
