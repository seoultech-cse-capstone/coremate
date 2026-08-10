import { router } from "expo-router";
import { useState } from "react";
import { Pressable, StyleSheet, Text, TextInput, View } from "react-native";

import { useAuth } from "../src/features/auth";
import {
  detailStyles,
  SettingsDetailLayout,
} from "../src/features/settings/components/SettingsDetailLayout";
import { settingsApi } from "../src/features/settings/settingsApi";
import { getApiErrorCode, getApiErrorMessage } from "../src/shared/utils/apiError";

// 회원가입 화면과 동일한 비밀번호 규약(대문자·소문자·숫자·특수문자 !@#$%^&* 포함 8자 이상).
const PASSWORD_RULE =
  /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[!@#$%^&*])[A-Za-z\d!@#$%^&*]{8,}$/;
const PASSWORD_RULE_MESSAGE =
  "비밀번호는 대문자, 소문자, 숫자, 특수문자(!@#$%^&*)를 포함해 8자 이상이어야 합니다.";

export default function ChangePasswordScreen() {
  const { accessToken } = useAuth();
  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [currentPasswordInvalid, setCurrentPasswordInvalid] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [isSuccess, setIsSuccess] = useState(false);

  const newPasswordInvalid =
    newPassword.length > 0 && !PASSWORD_RULE.test(newPassword);
  const passwordsMismatch =
    confirmPassword.length > 0 && newPassword !== confirmPassword;
  const canSubmit =
    currentPassword.length > 0 &&
    newPassword.length > 0 &&
    !newPasswordInvalid &&
    confirmPassword.length > 0 &&
    !passwordsMismatch &&
    !isSubmitting;

  const handleSubmit = async () => {
    if (!canSubmit || !accessToken) {
      return;
    }
    setIsSubmitting(true);
    setErrorMessage(null);
    setCurrentPasswordInvalid(false);
    try {
      await settingsApi.changePassword(currentPassword, newPassword, accessToken);
      setIsSuccess(true);
    } catch (error) {
      if (getApiErrorCode(error) === "INVALID_PASSWORD") {
        setCurrentPasswordInvalid(true);
      } else {
        setErrorMessage(getApiErrorMessage(error, "비밀번호 변경에 실패했습니다."));
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  if (isSuccess) {
    return (
      <SettingsDetailLayout title="비밀번호 변경">
        <View style={detailStyles.card}>
          <Text style={detailStyles.bodyText}>비밀번호가 변경되었습니다.</Text>
          <Pressable style={detailStyles.primaryButton} onPress={() => router.back()}>
            <Text style={detailStyles.primaryButtonText}>설정으로 돌아가기</Text>
          </Pressable>
        </View>
      </SettingsDetailLayout>
    );
  }

  return (
    <SettingsDetailLayout title="비밀번호 변경">
      <Text style={detailStyles.sectionTitle}>비밀번호</Text>
      <View style={detailStyles.card}>
        <Text style={detailStyles.label}>현재 비밀번호</Text>
        <TextInput
          secureTextEntry
          style={detailStyles.field}
          value={currentPassword}
          onChangeText={(text) => {
            setCurrentPassword(text);
            setCurrentPasswordInvalid(false);
          }}
        />
        {currentPasswordInvalid ? (
          <Text style={styles.errorText}>현재 비밀번호가 올바르지 않습니다.</Text>
        ) : null}

        <Text style={[detailStyles.label, { marginTop: 16 }]}>새 비밀번호</Text>
        <TextInput
          secureTextEntry
          style={detailStyles.field}
          value={newPassword}
          onChangeText={setNewPassword}
        />
        <Text
          style={newPasswordInvalid ? styles.errorText : detailStyles.helperText}
        >
          {PASSWORD_RULE_MESSAGE}
        </Text>

        <Text style={[detailStyles.label, { marginTop: 16 }]}>새 비밀번호 확인</Text>
        <TextInput
          secureTextEntry
          style={detailStyles.field}
          value={confirmPassword}
          onChangeText={setConfirmPassword}
        />
        {passwordsMismatch ? (
          <Text style={styles.errorText}>비밀번호가 일치하지 않습니다.</Text>
        ) : null}

        {errorMessage ? <Text style={styles.errorText}>{errorMessage}</Text> : null}

        <Pressable
          disabled={!canSubmit}
          style={[detailStyles.primaryButton, !canSubmit && styles.disabledButton]}
          onPress={handleSubmit}
        >
          <Text style={detailStyles.primaryButtonText}>
            {isSubmitting ? "변경 중…" : "비밀번호 변경"}
          </Text>
        </Pressable>
      </View>
    </SettingsDetailLayout>
  );
}

const styles = StyleSheet.create({
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
