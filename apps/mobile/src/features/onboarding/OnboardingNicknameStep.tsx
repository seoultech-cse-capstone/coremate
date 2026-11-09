import { useQueryClient } from "@tanstack/react-query";
import { Image } from "expo-image";
import { useState } from "react";
import {
  ActivityIndicator,
  KeyboardAvoidingView,
  Platform,
  Pressable,
  StyleSheet,
  Text,
  TextInput,
  View,
} from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";

import { FONT } from "../../../constants/fonts";
import { getApiErrorCode, getApiErrorMessage } from "../../shared/utils/apiError";
import { useAuth } from "../auth";
import { useMyProfile, userApi, type MyProfile } from "../user";

// 백엔드 UpdateNicknameRequest 의 @Size(max = 6) 과 같은 값.
const NICKNAME_MAX_LENGTH = 6;
const NICKNAME_TICKET_IMAGE = require("../../../assets/shop/nickname-change/ticket.png");

/** 온보딩 마지막 단계. 완료 선물로 첫 닉네임을 코인 없이 정한다. */
export function OnboardingNicknameStep({ onDone }: { onDone: () => void }) {
  const { accessToken } = useAuth();
  const queryClient = useQueryClient();
  const { data: profile } = useMyProfile(accessToken);
  const [nickname, setNickname] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const trimmed = nickname.trim();
  const canSubmit = trimmed.length > 0 && !isSubmitting && accessToken !== null;
  const currentNickname = profile?.nickname ?? null;

  const handleSubmit = async () => {
    if (!canSubmit || !accessToken) {
      return;
    }
    if (trimmed === currentNickname) {
      setErrorMessage("지금 닉네임과 같아요. 새 닉네임을 입력해주세요.");
      return;
    }
    setIsSubmitting(true);
    setErrorMessage(null);
    try {
      const updated = await userApi.setInitialNickname({ nickname: trimmed }, accessToken);
      queryClient.setQueryData<MyProfile>(["user", "me"], (previous) =>
        previous ? { ...previous, nickname: updated.nickname, initialNicknameSet: true } : previous,
      );
      onDone();
    } catch (error) {
      // 다른 기기에서 이미 썼다면 정할 게 없으니 그대로 온보딩을 마친다.
      if (getApiErrorCode(error) === "INITIAL_NICKNAME_ALREADY_SET") {
        onDone();
        return;
      }
      console.error("[onboarding] 닉네임 설정 실패", error);
      setErrorMessage(getApiErrorMessage(error, "닉네임을 정하지 못했어요. 다시 시도해주세요."));
      setIsSubmitting(false);
    }
  };

  return (
    <SafeAreaView style={styles.screen}>
      <KeyboardAvoidingView
        behavior={Platform.OS === "ios" ? "padding" : undefined}
        style={styles.body}
      >
        <View style={styles.top}>
          <Image source={NICKNAME_TICKET_IMAGE} style={styles.ticket} contentFit="contain" />
          <Text style={styles.eyebrow}>둘러보기 완료 선물</Text>
          <Text style={styles.title}>닉네임을 정해주세요</Text>
          <Text style={styles.description}>
            첫 닉네임은 무료로 정할 수 있어요.{"\n"}다음부터는 상점의 닉네임 변경권으로 바꿀 수 있어요.
          </Text>

          <View style={styles.inputRow}>
            <TextInput
              autoFocus
              maxLength={NICKNAME_MAX_LENGTH}
              placeholder={profile?.nickname ?? "닉네임"}
              placeholderTextColor="#94a3b8"
              returnKeyType="done"
              style={styles.input}
              value={nickname}
              onChangeText={(text) => {
                setNickname(text);
                setErrorMessage(null);
              }}
              onSubmitEditing={handleSubmit}
            />
            <Text style={styles.counter}>
              {trimmed.length}/{NICKNAME_MAX_LENGTH}
            </Text>
          </View>
          {errorMessage ? <Text style={styles.errorText}>{errorMessage}</Text> : null}
        </View>

        <Pressable
          accessibilityRole="button"
          disabled={!canSubmit}
          style={[styles.button, !canSubmit && styles.buttonDisabled]}
          onPress={handleSubmit}
        >
          {isSubmitting ? (
            <ActivityIndicator color="#FFFFFF" />
          ) : (
            <Text style={styles.buttonText}>CoreMate 시작하기</Text>
          )}
        </Pressable>
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: "#F3F3F3" },
  body: { flex: 1, justifyContent: "space-between", paddingHorizontal: 24, paddingBottom: 16 },
  top: { alignItems: "center", paddingTop: 48 },
  ticket: { width: 120, height: 84, marginBottom: 20 },
  eyebrow: { color: "#6D5DFB", fontFamily: FONT.pretendard, fontSize: 13, fontWeight: "800" },
  title: { marginTop: 6, color: "#111827", fontFamily: FONT.pretendard, fontSize: 24, fontWeight: "800" },
  description: {
    marginTop: 10,
    color: "#64748B",
    fontFamily: FONT.pretendard,
    fontSize: 14,
    fontWeight: "500",
    lineHeight: 21,
    textAlign: "center",
  },
  inputRow: {
    width: "100%",
    marginTop: 32,
    flexDirection: "row",
    alignItems: "center",
    borderRadius: 16,
    backgroundColor: "#FFFFFF",
    paddingHorizontal: 16,
  },
  input: {
    flex: 1,
    minHeight: 56,
    color: "#111827",
    fontFamily: FONT.pretendard,
    fontSize: 18,
    fontWeight: "600",
  },
  counter: { color: "#94A3B8", fontFamily: FONT.pretendard, fontSize: 13, fontWeight: "600" },
  errorText: {
    alignSelf: "flex-start",
    marginTop: 10,
    color: "#EF4444",
    fontFamily: FONT.pretendard,
    fontSize: 13,
    fontWeight: "600",
  },
  button: {
    alignItems: "center",
    justifyContent: "center",
    minHeight: 56,
    borderRadius: 16,
    backgroundColor: "#6D5DFB",
  },
  buttonDisabled: { backgroundColor: "#C7C2F5" },
  buttonText: { color: "#FFFFFF", fontFamily: FONT.pretendard, fontSize: 16, fontWeight: "700" },
});
