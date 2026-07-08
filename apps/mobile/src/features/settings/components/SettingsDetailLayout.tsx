import { router } from "expo-router";
import { ChevronLeft } from "lucide-react-native";
import { PropsWithChildren } from "react";
import { Pressable, ScrollView, StyleSheet, Text, View } from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";

import { FONT } from "../../../../constants/fonts";

type SettingsDetailLayoutProps = PropsWithChildren<{
  title: string;
}>;

export function SettingsDetailLayout({
  children,
  title,
}: SettingsDetailLayoutProps) {
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

            <Text style={styles.title}>{title}</Text>
          </View>

          <ScrollView
            contentContainerStyle={styles.content}
            showsVerticalScrollIndicator={false}
          >
            {children}
          </ScrollView>
        </View>
      </SafeAreaView>
    </View>
  );
}

export const detailStyles = StyleSheet.create({
  sectionTitle: {
    marginBottom: 10,
    color: "#64748b",
    fontFamily: FONT.pretendard,
    fontSize: 13,
    fontWeight: "700",
  },
  card: {
    borderRadius: 18,
    backgroundColor: "#F8FAFC",
    borderWidth: 1,
    borderColor: "#EEF2F7",
    padding: 16,
    marginBottom: 20,
  },
  label: {
    marginBottom: 8,
    color: "#334155",
    fontFamily: FONT.pretendard,
    fontSize: 13,
    fontWeight: "700",
  },
  field: {
    minHeight: 48,
    justifyContent: "center",
    borderRadius: 14,
    backgroundColor: "#ffffff",
    borderWidth: 1,
    borderColor: "#E5E7EB",
    paddingHorizontal: 14,
  },
  fieldText: {
    color: "#111827",
    fontFamily: FONT.pretendard,
    fontSize: 15,
    fontWeight: "500",
  },
  helperText: {
    marginTop: 8,
    color: "#64748b",
    fontFamily: FONT.pretendard,
    fontSize: 12,
    lineHeight: 18,
  },
  primaryButton: {
    minHeight: 52,
    alignItems: "center",
    justifyContent: "center",
    borderRadius: 16,
    backgroundColor: "#6D5DFB",
    marginTop: 8,
  },
  primaryButtonText: {
    color: "#ffffff",
    fontFamily: FONT.pretendard,
    fontSize: 15,
    fontWeight: "700",
  },
  dangerButton: {
    minHeight: 52,
    alignItems: "center",
    justifyContent: "center",
    borderRadius: 16,
    backgroundColor: "#FEE2E2",
    marginTop: 8,
  },
  dangerButtonText: {
    color: "#EF4444",
    fontFamily: FONT.pretendard,
    fontSize: 15,
    fontWeight: "700",
  },
  bodyText: {
    color: "#475569",
    fontFamily: FONT.pretendard,
    fontSize: 14,
    lineHeight: 22,
  },
});

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
});
