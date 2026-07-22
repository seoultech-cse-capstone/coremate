import { router } from "expo-router";
import { useState } from "react";
import { Pressable, StyleSheet, Text, TextInput, View } from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";

import { FONT } from "../constants/fonts";
import { useAuth, useLoginMutation } from "../src/features/auth";
import { getApiErrorMessage } from "../src/shared/utils/apiError";

export default function LoginScreen() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");

  const { continueAsGuest } = useAuth();
  const loginMutation = useLoginMutation();

  const handleLogin = () => {
    if (loginMutation.isPending) {
      return;
    }
    loginMutation.mutate(
      { email: email.trim(), password },
      {
        onSuccess: () => {
          router.replace("/(tabs)/home");
        },
      },
    );
  };

  const handleGuest = () => {
    continueAsGuest();
    router.replace("/(tabs)/home");
  };

  const canSubmit = email.trim().length > 0 && password.length > 0 && !loginMutation.isPending;

  return (
    <View style={styles.screen}>
      <SafeAreaView edges={["top"]} style={styles.container}>
        <View style={styles.form}>
          <Text style={styles.brand}>CoreMate</Text>

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
              editable={!loginMutation.isPending}
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
              editable={!loginMutation.isPending}
              onSubmitEditing={handleLogin}
              returnKeyType="go"
            />
          </View>

          {loginMutation.isError ? (
            <Text style={styles.errorText}>{getApiErrorMessage(loginMutation.error)}</Text>
          ) : null}

          <Pressable
            style={[styles.loginButton, !canSubmit && styles.loginButtonDisabled]}
            onPress={handleLogin}
            disabled={!canSubmit}
          >
            <Text style={styles.loginButtonText}>
              {loginMutation.isPending ? "로그인 중…" : "Login"}
            </Text>
          </Pressable>

          <View style={styles.inlineLinksRow}>
            <Pressable onPress={() => router.push("/register")}>
              <Text style={styles.inlineLinkText}>Register</Text>
            </Pressable>

            <Pressable onPress={() => {}}>
              <Text style={styles.inlineLinkText}>Forgot ID/PW</Text>
            </Pressable>
          </View>

          <View style={styles.separatorRow}>
            <View style={styles.separatorLine} />
          </View>

          <Pressable style={styles.guestButton} onPress={handleGuest}>
            <Text style={styles.guestButtonText}>Guest</Text>
          </Pressable>

          <Text style={styles.guestNotice}>
            게스트로 로그인할 시 기능이 제한될 수 있습니다.
          </Text>
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
  brand: {
    marginBottom: 12,
    marginTop: -40,
    color: "#6D5DFB",
    fontFamily: FONT.kufam,
    fontSize: 34,
    fontWeight: "700",
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
  loginButton: {
    alignItems: "center",
    borderRadius: 16,
    backgroundColor: "#6D5DFB",
    paddingVertical: 16,
    marginTop: 4,
  },
  loginButtonDisabled: {
    opacity: 0.5,
  },
  errorText: {
    color: "#dc2626",
    fontFamily: FONT.pretendard,
    fontSize: 13,
    lineHeight: 18,
  },
  loginButtonText: {
    color: "#ffffff",
    fontFamily: FONT.kufam,
    fontSize: 15,
    fontWeight: "500",
  },
  inlineLinksRow: {
    flexDirection: "row",
    justifyContent: "space-between",
    alignItems: "center",
    marginTop: 2,
  },
  inlineLinkText: {
    color: "#6D5DFB",
    fontFamily: FONT.kufam,
    fontSize: 14,
    fontWeight: "500",
  },
  separatorRow: {
    marginTop: 4,
    marginBottom: 4,
    paddingHorizontal: 8,
  },
  separatorLine: {
    height: 1,
    backgroundColor: "#d1d5db",
  },
  guestButton: {
    alignItems: "center",
    borderRadius: 16,
    borderWidth: 1,
    borderColor: "#6D5DFB",
    backgroundColor: "transparent",
    paddingVertical: 16,
  },
  guestButtonText: {
    color: "#6D5DFB",
    fontFamily: FONT.kufam,
    fontSize: 15,
    fontWeight: "500",
  },
  guestNotice: {
    marginTop: 2,
    color: "#64748b",
    fontFamily: FONT.pretendard,
    fontSize: 13,
    lineHeight: 18,
    textAlign: "center",
  },
});
