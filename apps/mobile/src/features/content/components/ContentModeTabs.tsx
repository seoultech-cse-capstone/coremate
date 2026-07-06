import { Pressable, StyleSheet, Text, View } from "react-native";

import { FONT } from "../../../../constants/fonts";

type ContentMode = "concept" | "mock";

type ContentModeTabsProps = {
  selectedMode: ContentMode;
  onSelectMode: (mode: ContentMode) => void;
};

const modes: { label: string; value: ContentMode }[] = [
  { label: "개념 문제", value: "concept" },
  { label: "모의고사", value: "mock" },
];

export function ContentModeTabs({
  selectedMode,
  onSelectMode,
}: ContentModeTabsProps) {
  return (
    <View style={styles.container}>
      {modes.map((mode) => {
        const isSelected = selectedMode === mode.value;

        return (
          <Pressable
            key={mode.value}
            accessibilityRole="tab"
            accessibilityState={{ selected: isSelected }}
            style={[styles.tab, isSelected && styles.selectedTab]}
            onPress={() => onSelectMode(mode.value)}
          >
            <Text style={[styles.tabText, isSelected && styles.selectedText]}>
              {mode.label}
            </Text>
          </Pressable>
        );
      })}
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flexDirection: "row",
    gap: 6,
    borderRadius: 18,
    backgroundColor: "#E4E4E7",
    padding: 5,
  },
  tab: {
    flex: 1,
    alignItems: "center",
    justifyContent: "center",
    minHeight: 42,
    borderRadius: 14,
  },
  selectedTab: {
    backgroundColor: "#ffffff",
    shadowColor: "#111827",
    shadowOffset: { width: 0, height: 6 },
    shadowOpacity: 0.08,
    shadowRadius: 10,
    elevation: 2,
  },
  tabText: {
    color: "#71717A",
    fontFamily: FONT.pretendard,
    fontSize: 14,
    fontWeight: "500",
  },
  selectedText: {
    color: "#6D5DFB",
    fontWeight: "600",
  },
});
