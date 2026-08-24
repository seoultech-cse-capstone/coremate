import { router } from "expo-router";
import { ChevronLeft } from "lucide-react-native";
import { useState } from "react";
import {
  KeyboardAvoidingView,
  Platform,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  View,
} from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";

import { FONT } from "../constants/fonts";
import { authApi } from "../src/features/auth";
import { getApiErrorMessage } from "../src/shared/utils/apiError";
import {
  PASSWORD_RULE,
  PASSWORD_RULE_MESSAGE,
} from "../src/shared/constants/passwordRule";

// 회원가입 화면과 동일한 규약.
const PHONE_RULE = /^01[016789]\d{7,8}$/;
const EMAIL_RULE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

type Tab = "id" | "password";
type ResetStep = "email" | "code" | "newPassword" | "done";

export default function FindAccountScreen() {
  const [tab, setTab] = useState<Tab>("id");

  const [name, setName] = useState("");
  const [phoneNumber, setPhoneNumber] = useState("");
  const [isFindingEmail, setIsFindingEmail] = useState(false);
  const [foundEmails, setFoundEmails] = useState<string[] | null>(null);
  const [findEmailError, setFindEmailError] = useState<string | null>(null);

  const [resetStep, setResetStep] = useState<ResetStep>("email");
  const [resetEmail, setResetEmail] = useState("");
  const [resetCode, setResetCode] = useState("");
  const [resetToken, setResetToken] = useState<string | null>(null);
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [isResetSubmitting, setIsResetSubmitting] = useState(false);
  const [resetError, setResetError] = useState<string | null>(null);

  const switchTab = (next: Tab) => {
    setTab(next);
    setFindEmailError(null);
    setResetError(null);
  };

  const canFindEmail = name.trim().length > 0 && PHONE_RULE.test(phoneNumber) && !isFindingEmail;

  const handleFindEmail = async () => {
    if (!canFindEmail) {
      return;
    }
    setIsFindingEmail(true);
    setFindEmailError(null);
    setFoundEmails(null);
    try {
      const result = await authApi.findEmail({ name: name.trim(), phoneNumber });
      setFoundEmails(result.emails);
    } catch (error) {
      setFindEmailError(getApiErrorMessage(error, "아이디를 찾지 못했습니다."));
    } finally {
      setIsFindingEmail(false);
    }
  };

  const canSendResetCode = EMAIL_RULE.test(resetEmail) && !isResetSubmitting;

  const handleSendResetCode = async () => {
    if (!canSendResetCode) {
      return;
    }
    setIsResetSubmitting(true);
    setResetError(null);
    try {
      await authApi.sendPasswordResetCode({ email: resetEmail.trim() });
      setResetStep("code");
    } catch (error) {
      setResetError(getApiErrorMessage(error, "인증코드 발송에 실패했습니다."));
    } finally {
      setIsResetSubmitting(false);
    }
  };

  const canVerifyCode = /^\d{6}$/.test(resetCode) && !isResetSubmitting;

  const handleVerifyCode = async () => {
    if (!canVerifyCode) {
      return;
    }
    setIsResetSubmitting(true);
    setResetError(null);
    try {
      const result = await authApi.verifyPasswordResetCode({
        email: resetEmail.trim(),
        code: resetCode,
      });
      setResetToken(result.resetToken);
      setResetStep("newPassword");
    } catch (error) {
      setResetError(getApiErrorMessage(error, "인증코드가 올바르지 않습니다."));
    } finally {
      setIsResetSubmitting(false);
    }
  };

  const newPasswordInvalid = newPassword.length > 0 && !PASSWORD_RULE.test(newPassword);
  const passwordsMismatch = confirmPassword.length > 0 && newPassword !== confirmPassword;
  const canResetPassword =
    resetToken !== null &&
    newPassword.length > 0 &&
    !newPasswordInvalid &&
    !passwordsMismatch &&
    !isResetSubmitting;

  const handleResetPassword = async () => {
    if (!canResetPassword || !resetToken) {
      return;
    }
    setIsResetSubmitting(true);
    setResetError(null);
    try {
      await authApi.resetPassword({ resetToken, newPassword });
      setResetStep("done");
    } catch (error) {
      setResetError(getApiErrorMessage(error, "비밀번호 재설정에 실패했습니다."));
    } finally {
      setIsResetSubmitting(false);
    }
  };

  return (
    <View style={styles.screen}>
      <SafeAreaView edges={["top"]} style={styles.container}>
        <KeyboardAvoidingView
          behavior={Platform.OS === "ios" ? "padding" : "height"}
          style={styles.flex}
        >
          <Pressable
            accessibilityLabel="뒤로가기"
            accessibilityRole="button"
            hitSlop={10}
            style={styles.backButton}
            onPress={() => router.back()}
          >
            <ChevronLeft color="#111827" size={28} strokeWidth={2.3} />
          </Pressable>

          <ScrollView
            contentContainerStyle={styles.scrollContent}
            keyboardShouldPersistTaps="handled"
          >
            <View style={styles.form}>
              <Text style={styles.brand}>아이디/비밀번호 찾기</Text>

              <View style={styles.tabRow}>
                <Pressable
                  style={[styles.tabButton, tab === "id" && styles.tabButtonActive]}
                  onPress={() => switchTab("id")}
                >
                  <Text style={[styles.tabButtonText, tab === "id" && styles.tabButtonTextActive]}>
                    아이디 찾기
                  </Text>
                </Pressable>
                <Pressable
                  style={[styles.tabButton, tab === "password" && styles.tabButtonActive]}
                  onPress={() => switchTab("password")}
                >
                  <Text
                    style={[
                      styles.tabButtonText,
                      tab === "password" && styles.tabButtonTextActive,
                    ]}
                  >
                    비밀번호 찾기
                  </Text>
                </Pressable>
              </View>

              {tab === "id" ? (
                <View style={styles.fieldGroup}>
                  <Text style={styles.fieldLabel}>이름</Text>
                  <TextInput
                    placeholder="이름"
                    placeholderTextColor="#94a3b8"
                    style={styles.input}
                    value={name}
                    onChangeText={setName}
                  />

                  <Text style={styles.fieldLabel}>휴대폰 번호</Text>
                  <TextInput
                    keyboardType="number-pad"
                    placeholder="01012345678"
                    placeholderTextColor="#94a3b8"
                    style={styles.input}
                    value={phoneNumber}
                    onChangeText={(text) => setPhoneNumber(text.replace(/[^0-9]/g, ""))}
                  />

                  {findEmailError ? <Text style={styles.errorText}>{findEmailError}</Text> : null}

                  <Pressable
                    disabled={!canFindEmail}
                    style={[styles.submitButton, !canFindEmail && styles.submitButtonDisabled]}
                    onPress={handleFindEmail}
                  >
                    <Text style={styles.submitButtonText}>
                      {isFindingEmail ? "조회 중…" : "아이디 찾기"}
                    </Text>
                  </Pressable>

                  {foundEmails ? (
                    <View style={styles.resultBox}>
                      {foundEmails.length === 0 ? (
                        <Text style={styles.resultText}>일치하는 계정을 찾지 못했습니다.</Text>
                      ) : (
                        foundEmails.map((email) => (
                          <Text key={email} style={styles.resultText}>
                            {email}
                          </Text>
                        ))
                      )}
                    </View>
                  ) : null}
                </View>
              ) : (
                <View style={styles.fieldGroup}>
                  {resetStep === "email" ? (
                    <>
                      <Text style={styles.fieldLabel}>이메일</Text>
                      <TextInput
                        autoCapitalize="none"
                        keyboardType="email-address"
                        placeholder="Email"
                        placeholderTextColor="#94a3b8"
                        style={styles.input}
                        value={resetEmail}
                        onChangeText={setResetEmail}
                      />

                      {resetError ? <Text style={styles.errorText}>{resetError}</Text> : null}

                      <Pressable
                        disabled={!canSendResetCode}
                        style={[
                          styles.submitButton,
                          !canSendResetCode && styles.submitButtonDisabled,
                        ]}
                        onPress={handleSendResetCode}
                      >
                        <Text style={styles.submitButtonText}>
                          {isResetSubmitting ? "발송 중…" : "인증코드 받기"}
                        </Text>
                      </Pressable>
                    </>
                  ) : null}

                  {resetStep === "code" ? (
                    <>
                      <Text style={styles.fieldLabel}>인증코드</Text>
                      <TextInput
                        keyboardType="number-pad"
                        maxLength={6}
                        placeholder="6자리 코드"
                        placeholderTextColor="#94a3b8"
                        style={styles.input}
                        value={resetCode}
                        onChangeText={(text) => setResetCode(text.replace(/[^0-9]/g, ""))}
                      />

                      {resetError ? <Text style={styles.errorText}>{resetError}</Text> : null}

                      <Pressable
                        disabled={!canVerifyCode}
                        style={[styles.submitButton, !canVerifyCode && styles.submitButtonDisabled]}
                        onPress={handleVerifyCode}
                      >
                        <Text style={styles.submitButtonText}>
                          {isResetSubmitting ? "확인 중…" : "인증 확인"}
                        </Text>
                      </Pressable>
                    </>
                  ) : null}

                  {resetStep === "newPassword" ? (
                    <>
                      <Text style={styles.fieldLabel}>새 비밀번호</Text>
                      <TextInput
                        secureTextEntry
                        style={styles.input}
                        value={newPassword}
                        onChangeText={setNewPassword}
                      />
                      <Text style={newPasswordInvalid ? styles.errorText : styles.helperText}>
                        {PASSWORD_RULE_MESSAGE}
                      </Text>

                      <Text style={styles.fieldLabel}>새 비밀번호 확인</Text>
                      <TextInput
                        secureTextEntry
                        style={styles.input}
                        value={confirmPassword}
                        onChangeText={setConfirmPassword}
                      />
                      {passwordsMismatch ? (
                        <Text style={styles.errorText}>비밀번호가 일치하지 않습니다.</Text>
                      ) : null}

                      {resetError ? <Text style={styles.errorText}>{resetError}</Text> : null}

                      <Pressable
                        disabled={!canResetPassword}
                        style={[
                          styles.submitButton,
                          !canResetPassword && styles.submitButtonDisabled,
                        ]}
                        onPress={handleResetPassword}
                      >
                        <Text style={styles.submitButtonText}>
                          {isResetSubmitting ? "변경 중…" : "비밀번호 재설정"}
                        </Text>
                      </Pressable>
                    </>
                  ) : null}

                  {resetStep === "done" ? (
                    <View style={styles.resultBox}>
                      <Text style={styles.resultText}>비밀번호가 재설정되었습니다.</Text>
                      <Pressable
                        style={styles.submitButton}
                        onPress={() => router.replace("/email-login")}
                      >
                        <Text style={styles.submitButtonText}>로그인하러 가기</Text>
                      </Pressable>
                    </View>
                  ) : null}
                </View>
              )}
            </View>
          </ScrollView>
        </KeyboardAvoidingView>
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
    backgroundColor: "transparent",
  },
  flex: {
    flex: 1,
  },
  backButton: {
    alignItems: "center",
    justifyContent: "center",
    width: 42,
    height: 42,
    marginLeft: 12,
  },
  scrollContent: {
    flexGrow: 1,
    justifyContent: "center",
    paddingHorizontal: 24,
    paddingBottom: 24,
  },
  form: {
    gap: 14,
  },
  brand: {
    marginBottom: 12,
    color: "#6D5DFB",
    fontFamily: FONT.kufam,
    fontSize: 22,
    fontWeight: "700",
    textAlign: "center",
  },
  tabRow: {
    flexDirection: "row",
    borderRadius: 14,
    backgroundColor: "#F1F5F9",
    padding: 4,
    marginBottom: 6,
  },
  tabButton: {
    flex: 1,
    alignItems: "center",
    justifyContent: "center",
    borderRadius: 10,
    paddingVertical: 10,
  },
  tabButtonActive: {
    backgroundColor: "#ffffff",
  },
  tabButtonText: {
    color: "#64748b",
    fontFamily: FONT.pretendard,
    fontSize: 14,
    fontWeight: "600",
  },
  tabButtonTextActive: {
    color: "#6D5DFB",
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
  helperText: {
    color: "#64748b",
    fontFamily: FONT.pretendard,
    fontSize: 12,
    lineHeight: 18,
  },
  errorText: {
    color: "#dc2626",
    fontFamily: FONT.pretendard,
    fontSize: 12,
    lineHeight: 18,
  },
  submitButton: {
    alignItems: "center",
    borderRadius: 16,
    backgroundColor: "#6D5DFB",
    paddingVertical: 16,
    marginTop: 4,
  },
  submitButtonDisabled: {
    opacity: 0.5,
  },
  submitButtonText: {
    color: "#ffffff",
    fontFamily: FONT.kufam,
    fontSize: 15,
    fontWeight: "500",
  },
  resultBox: {
    marginTop: 8,
    gap: 8,
    borderRadius: 14,
    backgroundColor: "#F8FAFC",
    borderWidth: 1,
    borderColor: "#EEF2F7",
    padding: 16,
  },
  resultText: {
    color: "#111827",
    fontFamily: FONT.pretendard,
    fontSize: 14,
    fontWeight: "600",
    textAlign: "center",
  },
});
