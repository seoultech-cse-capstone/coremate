import { router } from "expo-router";
import { useState } from "react";
import { Pressable, StyleSheet, Text, TextInput, View } from "react-native";

import { useAuth } from "../src/features/auth";
import {
  detailStyles,
  SettingsDetailLayout,
} from "../src/features/settings/components/SettingsDetailLayout";
import { supportApi } from "../src/features/support/supportApi";
import { getApiErrorMessage } from "../src/shared/utils/apiError";

const BODY_MAX = 1000;

export default function BugReportScreen() {
  const { accessToken } = useAuth();
  const [body, setBody] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [isSuccess, setIsSuccess] = useState(false);

  const canSubmit = body.trim().length > 0 && !isSubmitting;

  const handleSubmit = async () => {
    if (!canSubmit || !accessToken) {
      return;
    }
    setIsSubmitting(true);
    setErrorMessage(null);
    try {
      await supportApi.createBugReport(body.trim(), accessToken);
      setIsSuccess(true);
    } catch (error) {
      setErrorMessage(getApiErrorMessage(error, "버그 신고에 실패했습니다."));
    } finally {
      setIsSubmitting(false);
    }
  };

  if (isSuccess) {
    return (
      <SettingsDetailLayout title="버그 신고하기">
        <View style={detailStyles.card}>
          <Text style={detailStyles.bodyText}>
            신고가 접수되었습니다. 확인 후 빠르게 반영하겠습니다.
          </Text>
          <Pressable
            style={detailStyles.primaryButton}
            onPress={() => router.back()}
          >
            <Text style={detailStyles.primaryButtonText}>설정으로 돌아가기</Text>
          </Pressable>
        </View>
      </SettingsDetailLayout>
    );
  }

  return (
    <SettingsDetailLayout title="버그 신고하기">
      <Text style={detailStyles.sectionTitle}>어떤 문제가 있었나요?</Text>
      <View style={detailStyles.card}>
        <TextInput
          autoFocus
          multiline
          maxLength={BODY_MAX}
          placeholder="앱을 사용하면서 겪은 오류 증상을 알려주세요."
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
            {isSubmitting ? "접수 중…" : "신고하기"}
          </Text>
        </Pressable>
      </View>
    </SettingsDetailLayout>
  );
}

const styles = StyleSheet.create({
  multilineField: {
    minHeight: 160,
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
});
