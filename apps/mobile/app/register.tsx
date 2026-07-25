import { router } from "expo-router";
import { Check, ChevronLeft, ChevronRight } from "lucide-react-native";
import { useEffect, useLayoutEffect, useRef, useState } from "react";
import {
  Animated,
  BackHandler,
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
import { useSignupMutation } from "../src/features/auth";
import { getApiErrorMessage } from "../src/shared/utils/apiError";

const NICKNAME_MAX_LENGTH = 50;
const EMAIL_RULE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
// 대문자, 소문자, 숫자, 특수문자(!@#$%^&*)를 각각 1개 이상 포함한 8자 이상.
// 허용 문자는 영문/숫자/이 8개 특수문자로 제한한다.
// TODO: 백엔드에도 동일 규약이 추가되면 함께 유지보수할 것 (현재 백엔드는 미검증).
const PASSWORD_RULE =
  /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[!@#$%^&*])[A-Za-z\d!@#$%^&*]{8,}$/;
const PASSWORD_RULE_MESSAGE =
  "비밀번호는 대문자, 소문자, 숫자, 특수문자(!@#$%^&*)를 포함해 8자 이상이어야 합니다.";
// phone 상태값은 항상 숫자만 저장한다("01012345678"). 화면 표시용 하이픈은 formatPhoneNumber에서만 붙인다.
const PHONE_RULE = /^01[016789]\d{7,8}$/;
const PHONE_RULE_MESSAGE = "올바른 휴대폰 번호 형식이 아닙니다.";

function formatPhoneNumber(digits: string): string {
  if (digits.length < 4) {
    return digits;
  }
  if (digits.length < 8) {
    return `${digits.slice(0, 3)}-${digits.slice(3)}`;
  }
  if (digits.length <= 10) {
    return `${digits.slice(0, 3)}-${digits.slice(3, 6)}-${digits.slice(6)}`;
  }
  return `${digits.slice(0, 3)}-${digits.slice(3, 7)}-${digits.slice(7)}`;
}

const STEPS = [
  "consent",
  "nickname",
  "phone",
  "email",
  "password",
  "school",
] as const;
type Step = (typeof STEPS)[number];

type ConsentItem = {
  id: string;
  label: string;
  required: boolean;
  href: "/terms-policies" | "/privacy-policy" | "/marketing-consent";
};

const CONSENT_ITEMS: ConsentItem[] = [
  {
    id: "terms",
    label: "서비스 이용약관 동의",
    required: true,
    href: "/terms-policies",
  },
  {
    id: "privacy",
    label: "개인정보 수집 및 이용 동의",
    required: true,
    href: "/privacy-policy",
  },
  {
    id: "marketing",
    label: "마케팅 정보 수신 동의",
    required: false,
    href: "/marketing-consent",
  },
];

const HEADINGS: Record<Step, string> = {
  consent: "서비스 이용을 위한\n동의 안내",
  nickname: "반가워요.\n이름을 알려주세요.",
  phone: "휴대폰 번호를\n입력해주세요.",
  email: "이메일을\n입력해주세요.",
  password: "비밀번호를\n입력해주세요.",
  school: "학교를\n입력해주세요.",
};

export default function RegisterScreen() {
  const [stepIndex, setStepIndex] = useState(0);
  const [isSuccess, setIsSuccess] = useState(false);
  const [checkedConsentIds, setCheckedConsentIds] = useState<string[]>([]);
  const [nickname, setNickname] = useState("");
  const [phone, setPhone] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [school, setSchool] = useState("");

  const signupMutation = useSignupMutation();

  const step = STEPS[stepIndex];

  // 스텝이 바뀔 때마다 큰 제목이 먼저, 살짝 뒤이어 입력 영역이 위에서 아래로 내려오며 나타난다.
  const headingAnim = useRef(new Animated.Value(0)).current;
  const bodyAnim = useRef(new Animated.Value(0)).current;

  useLayoutEffect(() => {
    headingAnim.setValue(0);
    bodyAnim.setValue(0);
    Animated.stagger(480, [
      Animated.timing(headingAnim, {
        toValue: 1,
        duration: 480,
        useNativeDriver: true,
      }),
      Animated.timing(bodyAnim, {
        toValue: 1,
        duration: 480,
        useNativeDriver: true,
      }),
    ]).start();
  }, [step, headingAnim, bodyAnim]);

  const headingAnimatedStyle = {
    opacity: headingAnim,
    transform: [
      {
        translateY: headingAnim.interpolate({
          inputRange: [0, 1],
          outputRange: [-16, 0],
        }),
      },
    ],
  };

  const bodyAnimatedStyle = {
    opacity: bodyAnim,
    transform: [
      {
        translateY: bodyAnim.interpolate({
          inputRange: [0, 1],
          outputRange: [-16, 0],
        }),
      },
    ],
  };
  const phoneInvalid = phone.length > 0 && !PHONE_RULE.test(phone);
  const emailInvalid = email.length > 0 && !EMAIL_RULE.test(email);
  const passwordInvalid = password.length > 0 && !PASSWORD_RULE.test(password);
  const passwordsMismatch =
    confirmPassword.length > 0 && password !== confirmPassword;
  const allConsentChecked = CONSENT_ITEMS.every((item) =>
    checkedConsentIds.includes(item.id),
  );
  const requiredConsentChecked = CONSENT_ITEMS.filter(
    (item) => item.required,
  ).every((item) => checkedConsentIds.includes(item.id));

  const toggleAllConsent = () => {
    setCheckedConsentIds(
      allConsentChecked ? [] : CONSENT_ITEMS.map((item) => item.id),
    );
  };

  const toggleConsentItem = (id: string) => {
    setCheckedConsentIds((current) =>
      current.includes(id) ? current.filter((i) => i !== id) : [...current, id],
    );
  };

  const canProceed = (): boolean => {
    switch (step) {
      case "consent":
        return requiredConsentChecked;
      case "nickname":
        return nickname.trim().length > 0;
      case "phone":
        return phone.trim().length > 0 && !phoneInvalid;
      case "email":
        return email.trim().length > 0 && !emailInvalid;
      case "password":
        return (
          password.length > 0 &&
          !passwordInvalid &&
          confirmPassword.length > 0 &&
          !passwordsMismatch
        );
      case "school":
        return !signupMutation.isPending;
    }
  };

  const goBack = () => {
    if (stepIndex === 0) {
      router.back();
      return;
    }
    setStepIndex((i) => i - 1);
  };

  // 안드로이드 하드웨어 뒤로가기도 화면 안 화살표와 동일하게 위저드를 한 스텝씩 되돌린다.
  useEffect(() => {
    if (isSuccess) {
      return;
    }
    const subscription = BackHandler.addEventListener(
      "hardwareBackPress",
      () => {
        goBack();
        return true;
      },
    );
    return () => subscription.remove();
  }, [isSuccess, stepIndex]);

  const goNext = () => {
    if (!canProceed()) {
      return;
    }

    if (step !== "school") {
      setStepIndex((i) => i + 1);
      return;
    }

    // school은 백엔드 회원가입 API가 아직 받지 않아 전송하지 않는다.
    signupMutation.mutate(
      {
        email: email.trim(),
        password,
        nickname: nickname.trim(),
        phoneNumber: phone,
      },
      {
        onSuccess: () => {
          setIsSuccess(true);
        },
      },
    );
  };

  const isLastStep = step === "school";
  const isBusy = signupMutation.isPending;

  if (isSuccess) {
    return (
      <View style={styles.screen}>
        <SafeAreaView edges={["top"]} style={styles.safeArea}>
          <View style={styles.successContainer}>
            <View style={styles.successIconCircle}>
              <Check color="#ffffff" size={40} strokeWidth={3} />
            </View>
            <Text style={styles.successHeading}>회원가입 성공!</Text>
            <Text style={styles.successSubtext}>
              이제 로그인하고 CoreMate를 시작해보세요.
            </Text>

            <Pressable
              style={[styles.button, styles.successButton]}
              onPress={() => router.replace("/email-login")}
            >
              <Text style={styles.buttonText}>로그인 하러가기</Text>
            </Pressable>
          </View>
        </SafeAreaView>
      </View>
    );
  }

  return (
    <View style={styles.screen}>
      <SafeAreaView edges={["top"]} style={styles.safeArea}>
        <KeyboardAvoidingView
          behavior={Platform.OS === "ios" ? "padding" : "height"}
          style={styles.flex}
        >
          <ScrollView
            contentContainerStyle={styles.scrollContent}
            keyboardShouldPersistTaps="handled"
          >
            <Pressable
              accessibilityLabel="뒤로가기"
              accessibilityRole="button"
              hitSlop={10}
              style={styles.backButton}
              onPress={goBack}
            >
              <ChevronLeft color="#111827" size={28} strokeWidth={2.3} />
            </Pressable>

            <Animated.Text style={[styles.heading, headingAnimatedStyle]}>
              {HEADINGS[step]}
            </Animated.Text>

            <Animated.View style={[styles.body, bodyAnimatedStyle]}>
              {step === "consent" && (
                <View>
                  <Pressable
                    style={styles.consentAllRow}
                    onPress={toggleAllConsent}
                  >
                    <Check
                      color={allConsentChecked ? "#6D5DFB" : "#cbd5e1"}
                      size={22}
                      strokeWidth={3}
                    />
                    <Text style={styles.consentAllText}>전체 동의</Text>
                  </Pressable>

                  <Text style={styles.consentCaption}>
                    * 표시는 필수 동의입니다.
                  </Text>

                  <View style={styles.consentDivider} />

                  {CONSENT_ITEMS.map((item) => {
                    const checked = checkedConsentIds.includes(item.id);
                    return (
                      <View key={item.id} style={styles.consentRow}>
                        <Pressable
                          style={styles.consentRowMain}
                          onPress={() => toggleConsentItem(item.id)}
                        >
                          <Check
                            color={checked ? "#6D5DFB" : "#cbd5e1"}
                            size={20}
                            strokeWidth={3}
                          />
                          <Text style={styles.consentText}>
                            {item.label}
                            {item.required ? " *" : ""}
                          </Text>
                        </Pressable>

                        <Pressable
                          accessibilityLabel={`${item.label} 자세히 보기`}
                          hitSlop={8}
                          onPress={() => router.push(item.href)}
                        >
                          <ChevronRight
                            color="#9ca3af"
                            size={20}
                            strokeWidth={2.2}
                          />
                        </Pressable>
                      </View>
                    );
                  })}
                </View>
              )}

              {step === "nickname" && (
                <TextInput
                  autoFocus
                  placeholder="이름"
                  placeholderTextColor="#94a3b8"
                  style={styles.input}
                  value={nickname}
                  onChangeText={setNickname}
                  maxLength={NICKNAME_MAX_LENGTH}
                />
              )}

              {step === "phone" && (
                <>
                  <TextInput
                    autoFocus
                    keyboardType="phone-pad"
                    maxLength={13}
                    placeholder="010-0000-0000"
                    placeholderTextColor="#94a3b8"
                    style={styles.input}
                    value={formatPhoneNumber(phone)}
                    onChangeText={(text) =>
                      setPhone(text.replace(/\D/g, "").slice(0, 11))
                    }
                  />
                  {phoneInvalid ? (
                    <Text style={styles.errorText}>{PHONE_RULE_MESSAGE}</Text>
                  ) : null}
                </>
              )}

              {step === "email" && (
                <>
                  <TextInput
                    autoCapitalize="none"
                    autoComplete="email"
                    autoFocus
                    keyboardType="email-address"
                    placeholder="Email"
                    placeholderTextColor="#94a3b8"
                    style={styles.input}
                    value={email}
                    onChangeText={setEmail}
                  />
                  {emailInvalid ? (
                    <Text style={styles.errorText}>
                      올바른 이메일 형식이 아닙니다.
                    </Text>
                  ) : null}
                </>
              )}

              {step === "password" && (
                <>
                  <TextInput
                    autoFocus
                    placeholder="Password"
                    placeholderTextColor="#94a3b8"
                    secureTextEntry
                    style={styles.input}
                    value={password}
                    onChangeText={setPassword}
                  />
                  {passwordInvalid ? (
                    <Text style={styles.errorText}>
                      {PASSWORD_RULE_MESSAGE}
                    </Text>
                  ) : null}

                  <TextInput
                    placeholder="Confirm Password"
                    placeholderTextColor="#94a3b8"
                    secureTextEntry
                    style={[styles.input, styles.inputSpacing]}
                    value={confirmPassword}
                    onChangeText={setConfirmPassword}
                  />
                  {passwordsMismatch ? (
                    <Text style={styles.errorText}>
                      비밀번호가 일치하지 않습니다.
                    </Text>
                  ) : null}
                </>
              )}

              {step === "school" && (
                <TextInput
                  autoFocus
                  placeholder="학교명 (선택)"
                  placeholderTextColor="#94a3b8"
                  style={styles.input}
                  value={school}
                  onChangeText={setSchool}
                />
              )}

              {isLastStep && signupMutation.isError ? (
                <Text style={styles.errorText}>
                  {getApiErrorMessage(signupMutation.error)}
                </Text>
              ) : null}
            </Animated.View>

            <Pressable
              style={[
                styles.button,
                (!canProceed() || isBusy) && styles.buttonDisabled,
              ]}
              onPress={goNext}
              disabled={!canProceed() || isBusy}
            >
              <Text style={styles.buttonText}>
                {isLastStep ? (isBusy ? "가입 중…" : "가입하기") : "다음"}
              </Text>
            </Pressable>
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
  safeArea: {
    flex: 1,
    backgroundColor: "transparent",
  },
  flex: {
    flex: 1,
  },
  scrollContent: {
    flexGrow: 1,
    paddingHorizontal: 24,
    paddingTop: 8,
    paddingBottom: 32,
  },
  backButton: {
    alignItems: "center",
    justifyContent: "center",
    width: 42,
    height: 42,
    marginLeft: -10,
  },
  heading: {
    marginTop: 24,
    marginBottom: 32,
    color: "#111827",
    fontFamily: FONT.pretendard,
    fontSize: 26,
    fontWeight: "800",
    lineHeight: 34,
  },
  body: {
    flex: 1,
    gap: 8,
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
  inputSpacing: {
    marginTop: 12,
  },
  errorText: {
    color: "#dc2626",
    fontFamily: FONT.pretendard,
    fontSize: 13,
    lineHeight: 18,
  },
  consentAllRow: {
    flexDirection: "row",
    alignItems: "center",
    gap: 12,
    paddingVertical: 4,
  },
  consentAllText: {
    color: "#111827",
    fontFamily: FONT.pretendard,
    fontSize: 17,
    fontWeight: "800",
  },
  consentCaption: {
    marginTop: 8,
    marginLeft: 4,
    color: "#94a3b8",
    fontFamily: FONT.pretendard,
    fontSize: 12,
  },
  consentDivider: {
    height: 1,
    marginVertical: 16,
    backgroundColor: "#e5e7eb",
  },
  consentRow: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
    paddingVertical: 10,
  },
  consentRowMain: {
    flex: 1,
    flexDirection: "row",
    alignItems: "center",
    gap: 12,
  },
  consentText: {
    flex: 1,
    color: "#1f2937",
    fontFamily: FONT.pretendard,
    fontSize: 15,
    lineHeight: 21,
  },
  button: {
    alignItems: "center",
    borderRadius: 16,
    backgroundColor: "#6D5DFB",
    paddingVertical: 16,
    marginTop: 24,
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
  successContainer: {
    flex: 1,
    alignItems: "center",
    justifyContent: "center",
    paddingHorizontal: 24,
  },
  successButton: {
    alignSelf: "stretch",
  },
  successIconCircle: {
    alignItems: "center",
    justifyContent: "center",
    width: 72,
    height: 72,
    borderRadius: 999,
    backgroundColor: "#6D5DFB",
    marginBottom: 24,
  },
  successHeading: {
    color: "#111827",
    fontFamily: FONT.pretendard,
    fontSize: 24,
    fontWeight: "800",
    textAlign: "center",
  },
  successSubtext: {
    marginTop: 12,
    marginBottom: 40,
    color: "#64748b",
    fontFamily: FONT.pretendard,
    fontSize: 15,
    lineHeight: 22,
    textAlign: "center",
  },
});
