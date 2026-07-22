import { router } from "expo-router";
import { useState } from "react";
import { Alert, Pressable, StyleSheet, Text, TextInput, View } from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";

import { FONT } from "../constants/fonts";
import { useSignupMutation } from "../src/features/auth";
import { getApiErrorMessage } from "../src/shared/utils/apiError";

const NICKNAME_MAX_LENGTH = 50;

export default function RegisterScreen() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [nickname, setNickname] = useState("");

  const signupMutation = useSignupMutation();

  const handleSignup = () => {
    if (signupMutation.isPending) {
      return;
    }
    signupMutation.mutate(
      { email: email.trim(), password, nickname: nickname.trim() },
      {
        onSuccess: () => {
          Alert.alert("회원가입 완료", "이제 로그인해 주세요.", [
            { text: "확인", onPress: () => router.replace("/login") },
          ]);
        },
      },
    );
  };

  const canSubmit =
    email.trim().length > 0 &&
    password.length > 0 &&
    nickname.trim().length > 0 &&
    !signupMutation.isPending;

  return (
    <View style={styles.screen}>
      <SafeAreaView edges={["top"]} style={styles.container}>
        <View style={styles.form}>
          <Text style={styles.title}>회원가입</Text>

          <View style={styles.fieldGroup}>
            <Text style={styles.fieldLabel}>Email</Text>
            <TextInput
              autoCapitalize="none"
              autoComplete="email"
              keyboardType="email-address"
              placeholder="Email"
              placeholderTextColor="#94a3b8"
              style={styles.input}
              value={email}
              onChangeText={setEmail}
              editable={!signupMutation.isPending}
            />
          </View>

          <View style={styles.fieldGroup}>
            <Text style={styles.fieldLabel}>Password</Text>
            <TextInput
              placeholder="Password"
              placeholderTextColor="#94a3b8"
              secureTextEntry
              style={styles.input}
              value={password}
              onChangeText={setPassword}
              editable={!signupMutation.isPending}
            />
          </View>

          <View style={styles.fieldGroup}>
            <Text style={styles.fieldLabel}>Nickname</Text>
            <TextInput
              placeholder="Nickname"
              placeholderTextColor="#94a3b8"
              style={styles.input}
              value={nickname}
              onChangeText={setNickname}
              maxLength={NICKNAME_MAX_LENGTH}
              editable={!signupMutation.isPending}
            />
          </View>

          {signupMutation.isError ? (
            <Text style={styles.errorText}>{getApiErrorMessage(signupMutation.error)}</Text>
          ) : null}

          <Pressable
            style={[styles.button, !canSubmit && styles.buttonDisabled]}
            onPress={handleSignup}
            disabled={!canSubmit}
          >
            <Text style={styles.buttonText}>
              {signupMutation.isPending ? "가입 중…" : "회원가입"}
            </Text>
          </Pressable>

          <Pressable style={styles.backLink} onPress={() => router.back()}>
            <Text style={styles.backLinkText}>돌아가기</Text>
          </Pressable>
        </View>
      </SafeAreaView>
    </View>
  );
}

const styles = StyleSheet.create({
  screen: {
    flex: 1,
    backgroundColor: "#ffffff",
  },
  container: {
    flex: 1,
    justifyContent: "center",
    paddingHorizontal: 24,
    backgroundColor: "transparent",
  },
  form: {
    gap: 14,
  },
  title: {
    marginBottom: 8,
    color: "#6D5DFB",
    fontFamily: FONT.pretendard,
    fontSize: 30,
    fontWeight: "900",
    textAlign: "center",
  },
  fieldGroup: {
    gap: 8,
  },
  fieldLabel: {
    color: "#1f2937",
    fontFamily: FONT.kufam,
    fontSize: 14,
    fontWeight: "500",
  },
  input: {
    borderRadius: 14,
    borderWidth: 1,
    borderColor: "#d1d5db",
    backgroundColor: "#ffffff",
    color: "#111827",
    fontFamily: FONT.kufam,
    fontSize: 16,
    fontWeight: "500",
    paddingHorizontal: 16,
    paddingVertical: 14,
  },
  errorText: {
    color: "#dc2626",
    fontFamily: FONT.pretendard,
    fontSize: 13,
    lineHeight: 18,
  },
  button: {
    alignItems: "center",
    borderRadius: 16,
    backgroundColor: "#6D5DFB",
    paddingVertical: 16,
    marginTop: 4,
  },
  buttonDisabled: {
    opacity: 0.5,
  },
  buttonText: {
    color: "#ffffff",
    fontFamily: FONT.pretendard,
    fontSize: 15,
    fontWeight: "800",
  },
  backLink: {
    alignItems: "center",
    paddingVertical: 8,
  },
  backLinkText: {
    color: "#6D5DFB",
    fontFamily: FONT.kufam,
    fontSize: 14,
    fontWeight: "500",
  },
});
