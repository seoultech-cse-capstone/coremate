import { router } from "expo-router";
import { ChevronLeft, Eraser } from "lucide-react-native";
import { useState } from "react";
import { Pressable, ScrollView, StyleSheet, Text, View, type GestureResponderEvent } from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";

import { FONT } from "../constants/fonts";
import { useRequireMember } from "../src/features/auth";
import {
  FURNITURE_SETS,
  FURNITURE_TYPES,
  ROOM_GRID_ROWS,
  RoomBackdrop,
  useRoomFurniture,
  type FurnitureType,
  type RoomLayoutInfo,
} from "../src/features/avatar";

const ROOM_PREVIEW_HEIGHT = 380;

type Mode = FurnitureType | "remove";

export default function RoomFurnitureScreen() {
  useRequireMember();
  const { placements, setPlacement } = useRoomFurniture();
  const [mode, setMode] = useState<Mode>("bed");
  const [layout, setLayout] = useState<RoomLayoutInfo | null>(null);

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

    if (mode === "remove") {
      const hit = FURNITURE_TYPES.find((type) => {
        const position = placements[type];
        if (!position || position.row !== row) {
          return false;
        }
        const footprintCols = FURNITURE_SETS[type].footprintCols;
        return col >= position.col && col < position.col + footprintCols;
      });
      if (hit) {
        setPlacement(hit, null);
      }
      return;
    }

    setPlacement(mode, { row, col });
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

            <Text style={styles.title}>가구 배치</Text>
          </View>

          <Text style={styles.helperText}>
            아래에서 놓을 가구를 고르고, 바닥의 칸을 탭해서 놓아보세요. 침대·책상은 앉거나
            누울 수 있고, 나머지는 장식용이에요.
          </Text>

          <ScrollView
            horizontal
            contentContainerStyle={styles.modeRow}
            showsHorizontalScrollIndicator={false}
          >
            {FURNITURE_TYPES.map((type) => {
              const isSelected = mode === type;
              return (
                <Pressable
                  key={type}
                  style={[styles.modeButton, isSelected && styles.modeButtonSelected]}
                  onPress={() => setMode(type)}
                >
                  <Text style={[styles.modeButtonText, isSelected && styles.modeButtonTextSelected]}>
                    {FURNITURE_SETS[type].label} 놓기
                  </Text>
                </Pressable>
              );
            })}
            <Pressable
              style={[styles.modeButton, mode === "remove" && styles.modeButtonSelected]}
              onPress={() => setMode("remove")}
            >
              <Eraser
                color={mode === "remove" ? "#ffffff" : "#6D5DFB"}
                size={16}
                strokeWidth={2.2}
              />
              <Text
                style={[
                  styles.modeButtonText,
                  styles.removeButtonText,
                  mode === "remove" && styles.modeButtonTextSelected,
                ]}
              >
                치우기
              </Text>
            </Pressable>
          </ScrollView>

          <View style={styles.previewArea}>
            <RoomBackdrop
              height={ROOM_PREVIEW_HEIGHT}
              onFloorPress={handleFloorPress}
              onLayoutChange={setLayout}
            />
          </View>
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
  helperText: {
    marginTop: 4,
    paddingHorizontal: 20,
    color: "#64748b",
    fontFamily: FONT.pretendard,
    fontSize: 13,
    lineHeight: 19,
  },
  modeRow: {
    flexDirection: "row",
    gap: 8,
    paddingHorizontal: 20,
    paddingVertical: 16,
  },
  modeButton: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "center",
    gap: 6,
    borderRadius: 999,
    borderWidth: 1,
    borderColor: "#E5E7EB",
    backgroundColor: "#ffffff",
    paddingVertical: 10,
    paddingHorizontal: 16,
  },
  modeButtonSelected: {
    borderColor: "#6D5DFB",
    backgroundColor: "#6D5DFB",
  },
  modeButtonText: {
    color: "#475569",
    fontFamily: FONT.pretendard,
    fontSize: 13,
    fontWeight: "600",
  },
  modeButtonTextSelected: {
    color: "#ffffff",
  },
  removeButtonText: {
    color: "#6D5DFB",
  },
  previewArea: {
    flex: 1,
    paddingHorizontal: 20,
  },
});
