import { router } from "expo-router";
import { useState } from "react";
import { Pressable, StyleSheet, Text, TextInput, View } from "react-native";

import { useAuth } from "../src/features/auth";
import {
  detailStyles,
  SettingsDetailLayout,
} from "../src/features/settings/components/SettingsDetailLayout";
import {
  type InquiryCategory,
  supportApi,
} from "../src/features/support/supportApi";
import { getApiErrorMessage } from "../src/shared/utils/apiError";

const BODY_MAX = 300;
const TITLE_MAX = 50;

// 카테고리는 앱 하단 네비게이션 탭 5개로 제한한다.
const CATEGORIES: { value: InquiryCategory; label: string }[] = [
  { value: "HOME", label: "홈" },
  { value: "FRIEND", label: "친구" },
  { value: "PROBLEM_SET", label: "문제집" },
  { value: "REVIEW", label: "복습" },
  { value: "MYPAGE", label: "마이페이지" },
];

export default function InquiryScreen() {
  const { accessToken } = useAuth();
  const [category, setCategory] = useState<InquiryCategory | null>(null);
  const [title, setTitle] = useState("");
  const [body, setBody] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [isSuccess, setIsSuccess] = useState(false);

  const canSubmit =
    category !== null &&
    title.trim().length > 0 &&
    body.trim().length > 0 &&
    !isSubmitting;

  const handleSubmit = async () => {
    if (!canSubmit || !accessToken || category === null) {
      return;
    }
    setIsSubmitting(true);
    setErrorMessage(null);
    try {
      await supportApi.createInquiry(
        { category, title: title.trim(), body: body.trim() },
        accessToken,
      );
      setIsSuccess(true);
    } catch (error) {
      setErrorMessage(getApiErrorMessage(error, "문의 접수에 실패했습니다."));
    } finally {
      setIsSubmitting(false);
    }
  };

  if (isSuccess) {
    return (
      <SettingsDetailLayout title="문의하기">
        <View style={detailStyles.card}>
          <Text style={detailStyles.bodyText}>
            문의가 접수되었습니다. 확인 후 순차적으로 답변드리겠습니다. 답변은 내
            문의 내역에서 확인할 수 있습니다.
          </Text>
          <Pressable
            style={detailStyles.primaryButton}
            onPress={() => router.replace("/my-inquiries")}
          >
            <Text style={detailStyles.primaryButtonText}>내 문의 내역 보기</Text>
          </Pressable>
          <Pressable
            style={styles.secondaryButton}
            onPress={() => router.back()}
          >
            <Text style={styles.secondaryButtonText}>설정으로 돌아가기</Text>
          </Pressable>
        </View>
      </SettingsDetailLayout>
    );
  }

  return (
    <SettingsDetailLayout title="문의하기">
      <Text style={detailStyles.sectionTitle}>카테고리</Text>
      <View style={styles.chipRow}>
        {CATEGORIES.map((item) => {
          const selected = category === item.value;
          return (
            <Pressable
              key={item.value}
              style={[styles.chip, selected && styles.chipSelected]}
              onPress={() => setCategory(item.value)}
            >
              <Text style={[styles.chipText, selected && styles.chipTextSelected]}>
                {item.label}
              </Text>
            </Pressable>
          );
        })}
      </View>

      <View style={detailStyles.card}>
        <Text style={detailStyles.label}>제목</Text>
        <TextInput
          maxLength={TITLE_MAX}
          placeholder="제목을 입력해 주세요"
          placeholderTextColor="#94a3b8"
          style={detailStyles.field}
          value={title}
          onChangeText={setTitle}
        />

        <Text style={[detailStyles.label, { marginTop: 16 }]}>내용</Text>
        <TextInput
          multiline
          maxLength={BODY_MAX}
          placeholder="문의 내용을 입력해 주세요"
          placeholderTextColor="#94a3b8"
          style={[detailStyles.field, styles.multilineField]}
          value={body}
          onChangeText={setBody}
        />
        <Text style={styles.counter}>
          {body.length}/{BODY_MAX}
        </Text>

        {errorMessage ? <Text style={styles.errorText}>{errorMessage}</Text> : null}

        <Pressable
          disabled={!canSubmit}
          style={[detailStyles.primaryButton, !canSubmit && styles.disabledButton]}
          onPress={handleSubmit}
        >
          <Text style={detailStyles.primaryButtonText}>
            {isSubmitting ? "접수 중…" : "문의 접수"}
          </Text>
        </Pressable>
      </View>
    </SettingsDetailLayout>
  );
}

const styles = StyleSheet.create({
  chipRow: {
    flexDirection: "row",
    flexWrap: "wrap",
    gap: 8,
    marginBottom: 20,
  },
  chip: {
    paddingHorizontal: 16,
    paddingVertical: 9,
    borderRadius: 999,
    backgroundColor: "#F1F5F9",
    borderWidth: 1,
    borderColor: "#E5E7EB",
  },
  chipSelected: {
    backgroundColor: "rgba(109, 93, 251, 0.12)",
    borderColor: "#6D5DFB",
  },
  chipText: {
    color: "#475569",
    fontSize: 14,
    fontWeight: "600",
  },
  chipTextSelected: {
    color: "#6D5DFB",
    fontWeight: "700",
  },
  multilineField: {
    minHeight: 140,
    paddingTop: 12,
    paddingBottom: 12,
    textAlignVertical: "top",
  },
  counter: {
    marginTop: 6,
    textAlign: "right",
    color: "#94a3b8",
    fontSize: 12,
  },
  errorText: {
    marginTop: 8,
    color: "#dc2626",
    fontSize: 12,
    lineHeight: 18,
  },
  disabledButton: {
    opacity: 0.5,
  },
  secondaryButton: {
    minHeight: 52,
    alignItems: "center",
    justifyContent: "center",
    marginTop: 4,
  },
  secondaryButtonText: {
    color: "#64748b",
    fontSize: 15,
    fontWeight: "700",
  },
});
