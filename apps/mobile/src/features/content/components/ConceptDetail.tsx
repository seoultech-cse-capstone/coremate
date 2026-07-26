import { ChevronLeft } from "lucide-react-native";
import {
  ActivityIndicator,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  View,
} from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";

import { FONT } from "../../../../constants/fonts";
import { useConceptDetail } from "../useContentQueries";
import { useLanguage } from "../../../shared/language/LanguageContext";

const BRAND_PURPLE = "#6D5DFB";

function pickLocalizedName(ko: string, en: string, language: "ko" | "en") {
  if (language === "en") {
    return en || ko;
  }
  return ko || en;
}

type ConceptDetailProps = {
  conceptId: number | null;
  onBack: () => void;
};

export function ConceptDetail({ conceptId, onBack }: ConceptDetailProps) {
  const { language } = useLanguage();
  const { data, isLoading, isError } = useConceptDetail(conceptId);

  const title = data
    ? pickLocalizedName(data.conceptName, data.conceptNameEn, language)
    : "";
  const breadcrumb = data
    ? [data.subjectName, data.categoryLv1Name, data.categoryLv2Name]
        .filter(Boolean)
        .join(" · ")
    : "";

  return (
    <View style={styles.screen}>
      <SafeAreaView edges={["top"]} style={styles.safeArea}>
        <View style={styles.headerArea}>
          <Pressable
            accessibilityLabel="뒤로가기"
            accessibilityRole="button"
            hitSlop={10}
            style={styles.backButton}
            onPress={onBack}
          >
            <ChevronLeft color="#111827" size={28} strokeWidth={2.3} />
          </Pressable>

          <Text numberOfLines={1} style={styles.headerTitle}>
            {title || "개념 파악하기"}
          </Text>
        </View>

        {isLoading && (
          <View style={styles.stateContainer}>
            <ActivityIndicator color={BRAND_PURPLE} size="large" />
          </View>
        )}

        {!isLoading && isError && (
          <View style={styles.stateContainer}>
            <Text style={styles.stateText}>
              개념 정보를 불러오지 못했습니다. 잠시 후 다시 시도해주세요.
            </Text>
          </View>
        )}

        {!isLoading && !isError && data && (
          <ScrollView
            contentContainerStyle={styles.content}
            showsVerticalScrollIndicator={false}
          >
            {breadcrumb.length > 0 && (
              <Text style={styles.breadcrumb}>{breadcrumb}</Text>
            )}

            <Text style={styles.definition}>{data.oneLineDefinition}</Text>

            {data.aliases.length > 0 && (
              <View style={styles.chipRow}>
                {data.aliases.map((alias) => (
                  <View key={alias} style={styles.chip}>
                    <Text style={styles.chipText}>{alias}</Text>
                  </View>
                ))}
              </View>
            )}

            {data.coreSummary.length > 0 && (
              <View style={styles.section}>
                <Text style={styles.sectionTitle}>핵심 요약</Text>
                <Text style={styles.bodyText}>{data.coreSummary}</Text>
              </View>
            )}

            {data.coreKeywords.length > 0 && (
              <View style={styles.section}>
                <Text style={styles.sectionTitle}>핵심 키워드</Text>
                <View style={styles.chipRow}>
                  {data.coreKeywords.map((keyword) => (
                    <View key={keyword} style={[styles.chip, styles.keywordChip]}>
                      <Text style={[styles.chipText, styles.keywordChipText]}>
                        {keyword}
                      </Text>
                    </View>
                  ))}
                </View>
              </View>
            )}

            {data.keyProperties.length > 0 && (
              <View style={styles.section}>
                <Text style={styles.sectionTitle}>주요 특성</Text>
                {data.keyProperties.map((property, index) => (
                  <View key={property} style={styles.bulletRow}>
                    <Text style={styles.bulletMark}>{index + 1}</Text>
                    <Text style={styles.bulletText}>{property}</Text>
                  </View>
                ))}
              </View>
            )}
          </ScrollView>
        )}
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
  headerTitle: {
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
    color: "#6b7280",
    fontFamily: FONT.pretendard,
    fontSize: 14,
    fontWeight: "500",
    textAlign: "center",
  },
  content: {
    paddingHorizontal: 20,
    paddingTop: 8,
    paddingBottom: 40,
  },
  breadcrumb: {
    color: "#94a3b8",
    fontFamily: FONT.pretendard,
    fontSize: 12,
    fontWeight: "600",
    marginBottom: 10,
  },
  definition: {
    color: "#111827",
    fontFamily: FONT.pretendard,
    fontSize: 16,
    fontWeight: "600",
    lineHeight: 24,
  },
  chipRow: {
    flexDirection: "row",
    flexWrap: "wrap",
    gap: 8,
    marginTop: 12,
  },
  chip: {
    borderRadius: 999,
    paddingHorizontal: 12,
    paddingVertical: 6,
    backgroundColor: "#F1F5F9",
  },
  chipText: {
    color: "#475569",
    fontFamily: FONT.pretendard,
    fontSize: 12,
    fontWeight: "500",
  },
  keywordChip: {
    backgroundColor: "rgba(109, 93, 251, 0.1)",
  },
  keywordChipText: {
    color: BRAND_PURPLE,
    fontWeight: "700",
  },
  section: {
    marginTop: 26,
  },
  sectionTitle: {
    color: "#111827",
    fontFamily: FONT.pretendard,
    fontSize: 15,
    fontWeight: "700",
    marginBottom: 10,
  },
  bodyText: {
    color: "#334155",
    fontFamily: FONT.pretendard,
    fontSize: 14,
    lineHeight: 22,
  },
  bulletRow: {
    flexDirection: "row",
    gap: 10,
    marginBottom: 10,
  },
  bulletMark: {
    color: BRAND_PURPLE,
    fontFamily: FONT.pretendard,
    fontSize: 13,
    fontWeight: "700",
    marginTop: 1,
  },
  bulletText: {
    flex: 1,
    color: "#334155",
    fontFamily: FONT.pretendard,
    fontSize: 14,
    lineHeight: 21,
  },
});
