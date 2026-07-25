import { Check } from "lucide-react-native";
import { Pressable, StyleSheet, Text, View } from "react-native";

import { FONT } from "../constants/fonts";
import {
  useLanguage,
  type ContentLanguage,
} from "../src/shared/language/LanguageContext";
import {
  detailStyles,
  SettingsDetailLayout,
} from "../src/features/settings/components/SettingsDetailLayout";

const LANGUAGE_OPTIONS: { value: ContentLanguage; label: string }[] = [
  { value: "ko", label: "한국어" },
  { value: "en", label: "English" },
];

export default function LanguageSettingsScreen() {
  const { language, setLanguage } = useLanguage();

  return (
    <SettingsDetailLayout title="언어 설정">
      <Text style={detailStyles.sectionTitle}>콘텐츠 언어</Text>
      <Text style={detailStyles.helperText}>
        과목·대분류·중분류·개념 이름이 선택한 언어로 표시됩니다.
      </Text>

      <View style={[detailStyles.card, styles.card]}>
        {LANGUAGE_OPTIONS.map((option, index) => {
          const isSelected = option.value === language;

          return (
            <Pressable
              key={option.value}
              style={[
                styles.row,
                index < LANGUAGE_OPTIONS.length - 1 && styles.rowDivider,
              ]}
              onPress={() => setLanguage(option.value)}
            >
              <Text style={styles.rowText}>{option.label}</Text>
              {isSelected && (
                <Check color="#6D5DFB" size={20} strokeWidth={2.4} />
              )}
            </Pressable>
          );
        })}
      </View>
    </SettingsDetailLayout>
  );
}

const styles = StyleSheet.create({
  card: {
    marginTop: 16,
    padding: 0,
  },
  row: {
    minHeight: 54,
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
    paddingHorizontal: 16,
  },
  rowDivider: {
    borderBottomWidth: 1,
    borderBottomColor: "#E5E7EB",
  },
  rowText: {
    color: "#111827",
    fontFamily: FONT.pretendard,
    fontSize: 15,
    fontWeight: "600",
  },
});
