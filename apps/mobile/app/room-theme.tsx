import { router } from "expo-router";
import { CheckCircle2, ChevronLeft } from "lucide-react-native";
import { Image, Pressable, ScrollView, StyleSheet, Text, View } from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";

import { FONT } from "../constants/fonts";
import { useRequireMember } from "../src/features/auth";
import { ROOM_THEMES, useRoomTheme } from "../src/features/avatar";

export default function RoomThemeScreen() {
  useRequireMember();
  const { themeKey, setThemeKey } = useRoomTheme();

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

            <Text style={styles.title}>방 테마</Text>
          </View>

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

                  <View style={styles.preview}>
                    <Image resizeMode="cover" source={theme.wallSource} style={styles.previewWall} />
                    <Image resizeMode="cover" source={theme.floorSource} style={styles.previewFloor} />
                  </View>

                  <Text style={styles.cardLabel}>{theme.label}</Text>
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
  grid: {
    flexDirection: "row",
    flexWrap: "wrap",
    gap: 16,
    padding: 20,
  },
  card: {
    width: "45%",
    borderRadius: 16,
    borderWidth: 2,
    borderColor: "#e5e7eb",
    backgroundColor: "#F8FAFC",
    padding: 12,
    alignItems: "center",
  },
  cardSelected: {
    borderColor: "#6D5DFB",
    backgroundColor: "#F1EFFF",
  },
  selectedBadge: {
    position: "absolute",
    top: 10,
    right: 10,
    zIndex: 1,
  },
  preview: {
    width: "100%",
    aspectRatio: 1,
    borderRadius: 10,
    overflow: "hidden",
    marginBottom: 10,
  },
  previewWall: {
    width: "100%",
    height: "55%",
  },
  previewFloor: {
    width: "100%",
    height: "45%",
  },
  cardLabel: {
    color: "#111827",
    fontFamily: FONT.pretendard,
    fontSize: 14,
    fontWeight: "700",
  },
});
