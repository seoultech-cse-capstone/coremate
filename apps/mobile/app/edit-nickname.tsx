import { useQueryClient } from "@tanstack/react-query";
import { router } from "expo-router";
import { useState } from "react";
import { ActivityIndicator, Pressable, StyleSheet, Text, TextInput, View } from "react-native";

import { useAuth, useRequireMember } from "../src/features/auth";
import {
  detailStyles,
  SettingsDetailLayout,
} from "../src/features/settings/components/SettingsDetailLayout";
import { useMyProfile, userApi } from "../src/features/user";
import { getApiErrorCode, getApiErrorMessage } from "../src/shared/utils/apiError";

const NICKNAME_MAX_LENGTH = 6;

export default function EditNicknameScreen() {
  useRequireMember();
  const { accessToken } = useAuth();
  const queryClient = useQueryClient();
  const profileQuery = useMyProfile(accessToken);

  const [nickname, setNickname] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [nicknameTaken, setNicknameTaken] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const canSubmit =
    nickname.trim().length > 0 && nickname.trim().length <= NICKNAME_MAX_LENGTH && !isSubmitting;

  const handleSubmit = async () => {
    if (!canSubmit || !accessToken) {
      return;
    }
    setIsSubmitting(true);
    setErrorMessage(null);
    setNicknameTaken(false);
    try {
      const result = await userApi.updateNickname(nickname.trim(), accessToken);
      queryClient.setQueryData(
        ["user", "me"],
        (current: typeof profileQuery.data) =>
          current ? { ...current, nickname: result.nickname } : current,
      );
      router.back();
    } catch (error) {
      if (getApiErrorCode(error) === "DUPLICATE_NICKNAME") {
        setNicknameTaken(true);
      } else {
        setErrorMessage(getApiErrorMessage(error, "닉네임 수정에 실패했습니다."));
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <SettingsDetailLayout title="닉네임 수정">
      <Text style={detailStyles.sectionTitle}>닉네임</Text>
      <View style={detailStyles.card}>
        {profileQuery.isLoading ? (
          <ActivityIndicator color="#6D5DFB" />
        ) : (
          <>
            <Text style={detailStyles.label}>현재 닉네임</Text>
            <View style={detailStyles.field}>
              <Text style={detailStyles.fieldText}>{profileQuery.data?.nickname}</Text>
            </View>

            <Text style={[detailStyles.label, { marginTop: 16 }]}>새 닉네임</Text>
            <TextInput
              maxLength={NICKNAME_MAX_LENGTH}
              placeholder="새 닉네임을 입력하세요"
              placeholderTextColor="#94a3b8"
              style={detailStyles.field}
              value={nickname}
              onChangeText={(text) => {
                setNickname(text);
                setNicknameTaken(false);
              }}
            />

            <Text style={detailStyles.helperText}>
              닉네임은 다른 사용자와 중복될 수 없으며, 최대 {NICKNAME_MAX_LENGTH}자까지 가능합니다.
            </Text>
            {nicknameTaken ? (
              <Text style={styles.errorText}>이미 사용 중인 닉네임입니다.</Text>
            ) : null}
            {errorMessage ? <Text style={styles.errorText}>{errorMessage}</Text> : null}

            <Pressable
              disabled={!canSubmit}
              style={[detailStyles.primaryButton, !canSubmit && styles.disabledButton]}
              onPress={handleSubmit}
            >
              <Text style={detailStyles.primaryButtonText}>
                {isSubmitting ? "저장 중…" : "중복 확인 및 저장"}
              </Text>
            </Pressable>
          </>
        )}
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
