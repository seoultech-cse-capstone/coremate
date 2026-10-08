import { useQueryClient } from "@tanstack/react-query";
import { router } from "expo-router";
import { useEffect, useState } from "react";
import { ActivityIndicator, Pressable, StyleSheet, Text, TextInput, View } from "react-native";

import { useAuth, useRequireMember } from "../src/features/auth";
import {
  detailStyles,
  SettingsDetailLayout,
} from "../src/features/settings/components/SettingsDetailLayout";
import { useMyProfile, userApi } from "../src/features/user";
import { getApiErrorMessage } from "../src/shared/utils/apiError";

const SCHOOL_NAME_MAX_LENGTH = 100;

export default function EditProfileScreen() {
  useRequireMember();
  const { accessToken } = useAuth();
  const queryClient = useQueryClient();
  const profileQuery = useMyProfile(accessToken);

  const [schoolName, setSchoolName] = useState("");
  const [schoolEmail, setSchoolEmail] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [isSuccess, setIsSuccess] = useState(false);

  useEffect(() => {
    if (profileQuery.data) {
      setSchoolName(profileQuery.data.schoolName ?? "");
      setSchoolEmail(profileQuery.data.schoolEmail ?? "");
    }
  }, [profileQuery.data]);

  const canSubmit =
    schoolName.length <= SCHOOL_NAME_MAX_LENGTH && !isSubmitting && !profileQuery.isLoading;

  const handleSubmit = async () => {
    if (!canSubmit || !accessToken) {
      return;
    }
    setIsSubmitting(true);
    setErrorMessage(null);
    try {
      const updated = await userApi.updateMyProfile(
        {
          schoolName: schoolName.trim().length > 0 ? schoolName.trim() : undefined,
          schoolEmail: schoolEmail.trim().length > 0 ? schoolEmail.trim() : undefined,
        },
        accessToken,
      );
      queryClient.setQueryData(["user", "me"], updated);
      setIsSuccess(true);
    } catch (error) {
      setErrorMessage(getApiErrorMessage(error, "회원정보 수정에 실패했습니다."));
    } finally {
      setIsSubmitting(false);
    }
  };

  if (isSuccess) {
    return (
      <SettingsDetailLayout title="회원정보 수정">
        <View style={detailStyles.card}>
          <Text style={detailStyles.bodyText}>회원정보가 수정되었습니다.</Text>
          <Pressable style={detailStyles.primaryButton} onPress={() => router.back()}>
            <Text style={detailStyles.primaryButtonText}>설정으로 돌아가기</Text>
          </Pressable>
        </View>
      </SettingsDetailLayout>
    );
  }

  return (
    <SettingsDetailLayout title="회원정보 수정">
      <Text style={detailStyles.sectionTitle}>프로필 정보</Text>
      <View style={detailStyles.card}>
        {profileQuery.isLoading ? (
          <ActivityIndicator color="#6D5DFB" />
        ) : (
          <>
            <Text style={detailStyles.label}>닉네임</Text>
            <View style={detailStyles.field}>
              <Text style={detailStyles.fieldText}>{profileQuery.data?.nickname}</Text>
            </View>
            <Text style={detailStyles.helperText}>닉네임은 상점의 닉네임 변경권으로 바꿀 수 있어요.</Text>
            {/* 닉네임 수정 메뉴를 따로 두지 않고 여기서 상점으로 보낸다(#246). */}
            <Pressable
              accessibilityRole="button"
              hitSlop={6}
              style={styles.shopLink}
              onPress={() => router.push("/avatar-shop")}
            >
              <Text style={styles.shopLinkText}>상점에서 닉네임 바꾸기 ›</Text>
            </Pressable>

            <Text style={[detailStyles.label, { marginTop: 16 }]}>학교</Text>
            <TextInput
              placeholder="학교명을 입력하세요"
              placeholderTextColor="#94a3b8"
              style={detailStyles.field}
              value={schoolName}
              onChangeText={setSchoolName}
              maxLength={SCHOOL_NAME_MAX_LENGTH}
            />

            <Text style={[detailStyles.label, { marginTop: 16 }]}>학교 이메일</Text>
            <TextInput
              autoCapitalize="none"
              keyboardType="email-address"
              placeholder="학교 이메일을 입력하세요"
              placeholderTextColor="#94a3b8"
              style={detailStyles.field}
              value={schoolEmail}
              onChangeText={setSchoolEmail}
            />

            {errorMessage ? <Text style={styles.errorText}>{errorMessage}</Text> : null}

            <Pressable
              disabled={!canSubmit}
              style={[detailStyles.primaryButton, !canSubmit && styles.disabledButton]}
              onPress={handleSubmit}
            >
              <Text style={detailStyles.primaryButtonText}>
                {isSubmitting ? "저장 중…" : "저장"}
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
  shopLink: {
    alignSelf: "flex-start",
    marginTop: 6,
  },
  shopLinkText: {
    color: "#6D5DFB",
    fontSize: 13,
    fontWeight: "700",
  },
});
