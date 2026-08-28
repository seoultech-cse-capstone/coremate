import { router } from "expo-router";
import { CheckCircle2, ChevronLeft } from "lucide-react-native";
import { useState } from "react";
import {
  ActivityIndicator,
  Image,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  View,
} from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";

import { FONT } from "../constants/fonts";
import { useAuth, useRequireMember } from "../src/features/auth";
import {
  useInventory,
  useUpdateEquippedItemsMutation,
  type AvatarItemType,
  type InventoryItem,
} from "../src/features/avatar";
import { getApiErrorMessage } from "../src/shared/utils/apiError";

const ITEM_TYPE_LABEL: Record<AvatarItemType, string> = {
  HAT: "모자",
  HAIR: "머리",
  EYES: "눈",
  EYEWEAR: "안경",
  MOUTH: "입",
  TOP: "상의",
  BOTTOM: "하의",
  SHOES: "신발",
  BAG: "가방",
  PET: "반려동물",
  WALLPAPER: "벽지",
  FLOOR: "바닥",
  FURNITURE: "가구",
};

const ITEM_TYPE_ORDER: AvatarItemType[] = [
  "HAT",
  "HAIR",
  "EYES",
  "EYEWEAR",
  "MOUTH",
  "TOP",
  "BOTTOM",
  "SHOES",
  "BAG",
  "PET",
  "WALLPAPER",
  "FLOOR",
  "FURNITURE",
];

const RARITY_COLOR: Record<string, string> = {
  COMMON: "#94A3B8",
  RARE: "#3B82F6",
  EPIC: "#A855F7",
  LEGENDARY: "#F59E0B",
};

export default function AvatarInventoryScreen() {
  useRequireMember();
  const { accessToken } = useAuth();
  const inventoryQuery = useInventory(accessToken);
  const updateEquippedMutation = useUpdateEquippedItemsMutation(accessToken);
  const [selectedType, setSelectedType] = useState<AvatarItemType>("HAIR");
  const [equipErrorMessage, setEquipErrorMessage] = useState<string | null>(null);

  const groups = inventoryQuery.data?.groups ?? [];
  const groupByType = new Map(groups.map((group) => [group.itemType, group]));
  const currentGroup = groupByType.get(selectedType);

  const toggleEquip = (item: InventoryItem) => {
    if (!item.slot || item.itemType === "FURNITURE") {
      // 가구는 착용이 아니라 방 배치 대상이라 여기서는 다루지 않는다.
      return;
    }
    setEquipErrorMessage(null);
    updateEquippedMutation.mutate(
      {
        equippedItems: [
          {
            slot: item.slot,
            userItemId: item.equipped ? null : item.userItemId,
          },
        ],
      },
      {
        onError: (error) => {
          setEquipErrorMessage(
            getApiErrorMessage(error, "장착 상태를 바꾸지 못했습니다."),
          );
        },
      },
    );
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

            <Text style={styles.title}>아이템 보관함</Text>
          </View>

          <ScrollView
            horizontal
            contentContainerStyle={styles.chipRow}
            showsHorizontalScrollIndicator={false}
            style={styles.chipScroller}
          >
            {ITEM_TYPE_ORDER.map((type) => {
              const group = groupByType.get(type);
              const isSelected = type === selectedType;
              return (
                <Pressable
                  key={type}
                  style={[styles.chip, isSelected && styles.chipSelected]}
                  onPress={() => setSelectedType(type)}
                >
                  <Text
                    style={[styles.chipText, isSelected && styles.chipTextSelected]}
                  >
                    {ITEM_TYPE_LABEL[type]}
                    {group ? ` ${group.count}` : ""}
                  </Text>
                </Pressable>
              );
            })}
          </ScrollView>

          {equipErrorMessage ? (
            <Text style={styles.equipErrorText}>{equipErrorMessage}</Text>
          ) : null}

          {inventoryQuery.isLoading ? (
            <View style={styles.stateContainer}>
              <ActivityIndicator color="#6D5DFB" size="large" />
            </View>
          ) : inventoryQuery.isError ? (
            <View style={styles.stateContainer}>
              <Text style={styles.stateText}>
                {getApiErrorMessage(inventoryQuery.error, "보관함을 불러오지 못했습니다.")}
              </Text>
            </View>
          ) : !currentGroup || currentGroup.items.length === 0 ? (
            <View style={styles.stateContainer}>
              <Text style={styles.stateText}>
                보유한 {ITEM_TYPE_LABEL[selectedType]} 아이템이 없어요.
              </Text>
            </View>
          ) : (
            <ScrollView
              contentContainerStyle={styles.grid}
              showsVerticalScrollIndicator={false}
            >
              {currentGroup.items.map((item) => (
                <Pressable
                  disabled={item.itemType === "FURNITURE" || updateEquippedMutation.isPending}
                  key={item.userItemId}
                  style={[
                    styles.card,
                    { borderColor: RARITY_COLOR[item.rarity] ?? "#e5e7eb" },
                    item.equipped && styles.cardEquipped,
                  ]}
                  onPress={() => toggleEquip(item)}
                >
                  {item.equipped ? (
                    <View style={styles.equippedBadge}>
                      <CheckCircle2 color="#16C45B" size={18} strokeWidth={2.4} />
                    </View>
                  ) : null}

                  <View style={styles.thumbnail}>
                    {item.imageUrl ? (
                      <Image
                        resizeMode="contain"
                        source={{ uri: item.imageUrl }}
                        style={styles.thumbnailImage}
                      />
                    ) : (
                      <Text style={styles.thumbnailFallback}>
                        {ITEM_TYPE_LABEL[item.itemType].slice(0, 1)}
                      </Text>
                    )}
                  </View>

                  <Text numberOfLines={1} style={styles.itemName}>
                    {item.itemName}
                  </Text>
                  <Text
                    style={[
                      styles.rarityText,
                      { color: RARITY_COLOR[item.rarity] ?? "#64748b" },
                    ]}
                  >
                    {item.rarity}
                  </Text>
                  {item.quantity > 1 ? (
                    <Text style={styles.quantityText}>보유 {item.quantity}개</Text>
                  ) : null}
                </Pressable>
              ))}
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
  equipErrorText: {
    marginTop: 8,
    marginHorizontal: 16,
    color: "#dc2626",
    fontFamily: FONT.pretendard,
    fontSize: 13,
    fontWeight: "600",
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
  grid: {
    flexDirection: "row",
    flexWrap: "wrap",
    gap: 12,
    padding: 16,
  },
  card: {
    width: "47%",
    borderRadius: 14,
    borderWidth: 2,
    backgroundColor: "#F8FAFC",
    padding: 12,
    alignItems: "center",
  },
  cardEquipped: {
    backgroundColor: "#F1EFFF",
  },
  equippedBadge: {
    position: "absolute",
    top: 8,
    right: 8,
  },
  thumbnail: {
    width: 72,
    height: 72,
    borderRadius: 12,
    backgroundColor: "#ffffff",
    alignItems: "center",
    justifyContent: "center",
    marginBottom: 8,
  },
  thumbnailImage: {
    width: 56,
    height: 56,
  },
  thumbnailFallback: {
    color: "#94a3b8",
    fontFamily: FONT.pretendard,
    fontSize: 24,
    fontWeight: "700",
  },
  itemName: {
    color: "#111827",
    fontFamily: FONT.pretendard,
    fontSize: 13,
    fontWeight: "700",
    textAlign: "center",
  },
  rarityText: {
    marginTop: 2,
    fontFamily: FONT.pretendard,
    fontSize: 11,
    fontWeight: "700",
  },
  quantityText: {
    marginTop: 2,
    color: "#94a3b8",
    fontFamily: FONT.pretendard,
    fontSize: 11,
    fontWeight: "500",
  },
});
