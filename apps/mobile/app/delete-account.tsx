import { router } from "expo-router";
import { useState } from "react";
import { Alert, Pressable, StyleSheet, Text, TextInput, View } from "react-native";

import { authApi, useAuth, useRequireMember } from "../src/features/auth";
import {
  detailStyles,
  SettingsDetailLayout,
} from "../src/features/settings/components/SettingsDetailLayout";
import { getApiErrorCode, getApiErrorMessage } from "../src/shared/utils/apiError";

export default function DeleteAccountScreen() {
  useRequireMember();
  const { accessToken, signOut } = useAuth();
  const [password, setPassword] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const canSubmit = password.length > 0 && !isSubmitting;

  const submitWithdraw = async () => {
    if (!canSubmit || !accessToken) {
      return;
    }
    setIsSubmitting(true);
    setErrorMessage(null);
    try {
      await authApi.withdraw({ password }, accessToken);
      await signOut();
      router.replace("/");
    } catch (error) {
      if (getApiErrorCode(error) === "INVALID_PASSWORD") {
        setErrorMessage("비밀번호가 올바르지 않습니다.");
      } else {
        setErrorMessage(getApiErrorMessage(error, "회원탈퇴에 실패했습니다."));
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  const handlePress = () => {
    if (!canSubmit) {
      return;
    }
    Alert.alert("회원탈퇴", "정말 탈퇴하시겠습니까? 이 작업은 되돌릴 수 없습니다.", [
      { text: "취소", style: "cancel" },
      { text: "탈퇴", style: "destructive", onPress: submitWithdraw },
    ]);
  };

  return (
    <SettingsDetailLayout title="회원탈퇴">
      <Text style={detailStyles.sectionTitle}>탈퇴 전 확인</Text>
      <View style={detailStyles.card}>
        <Text style={detailStyles.bodyText}>
          회원탈퇴 시 계정 정보와 서비스 이용 권한이 비활성화됩니다. 학습 기록, 즐겨찾기, 보상 정보 처리 방식은 운영 정책에 따라 진행됩니다.
        </Text>

        <Text style={[detailStyles.label, { marginTop: 16 }]}>비밀번호 확인</Text>
        <TextInput
          placeholder="현재 비밀번호를 입력하세요"
          placeholderTextColor="#94a3b8"
          secureTextEntry
          style={detailStyles.field}
          value={password}
          onChangeText={(text) => {
            setPassword(text);
            setErrorMessage(null);
          }}
        />
        {errorMessage ? <Text style={styles.errorText}>{errorMessage}</Text> : null}

        <Pressable
          disabled={!canSubmit}
          style={[detailStyles.dangerButton, !canSubmit && styles.disabledButton]}
          onPress={handlePress}
        >
          <Text style={detailStyles.dangerButtonText}>
            {isSubmitting ? "탈퇴 처리 중…" : "회원탈퇴 진행"}
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
