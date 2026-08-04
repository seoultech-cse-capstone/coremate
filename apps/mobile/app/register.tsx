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

const EMAIL_DOMAINS = [
  "naver.com",
  "gmail.com",
  "daum.net",
  "kakao.com",
  "nate.com",
];

// "ehdgns" → 흔한 도메인을 붙인 전체 이메일 후보들, "ehdgns@na" → "na"로 시작하는
// 도메인만 후보로. 이미 완전히 일치하는 도메인을 직접 다 입력했으면 후보를 숨긴다.
function getEmailSuggestions(value: string): string[] {
  const trimmed = value.trim();
  if (!trimmed) {
    return [];
  }

  const atIndex = trimmed.indexOf("@");
  if (atIndex === -1) {
    return EMAIL_DOMAINS.map((domain) => `${trimmed}@${domain}`);
  }

  const localPart = trimmed.slice(0, atIndex);
  const domainQuery = trimmed.slice(atIndex + 1);
  if (!localPart) {
    return [];
  }
  if (domainQuery.length === 0) {
    return EMAIL_DOMAINS.map((domain) => `${localPart}@${domain}`);
  }

  const matches = EMAIL_DOMAINS.filter((domain) =>
    domain.startsWith(domainQuery),
  );
  if (matches.length === 0 || (matches.length === 1 && matches[0] === domainQuery)) {
    return [];
  }

  return matches.map((domain) => `${localPart}@${domain}`);
}

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
  "email-verify",
  "password",
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
  nickname: "반가워요.\n이름과 닉네임을\n알려주세요.",
  phone: "휴대폰 번호를\n입력해주세요.",
  email: "이메일을\n입력해주세요.",
  "email-verify": "이메일로 받은\n인증번호를 입력해주세요.",
  password: "비밀번호를\n입력해주세요.",
};

type AnimatedStepProps = {
  heading: string;
  onReady?: () => void;
  children: (isReady: boolean) => React.ReactNode;
};

// 스텝이 바뀔 때마다 부모가 key={step}으로 이 컴포넌트를 통째로 새로 마운트한다.
// headingAnim/bodyAnim이 매번 새 Animated.Value로 만들어지므로, 이전 스텝에서
// 네이티브 쪽에 굳어있던 애니메이션 값이 새 스텝까지 남아있는 문제가 없다.
function AnimatedStep({ heading, onReady, children }: AnimatedStepProps) {
  const headingAnim = useRef(new Animated.Value(0)).current;
  const bodyAnim = useRef(new Animated.Value(0)).current;
  const [isReady, setIsReady] = useState(false);

  useLayoutEffect(() => {
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
    ]).start(({ finished }) => {
      if (finished) {
        setIsReady(true);
      }
    });
    // 이 컴포넌트는 스텝마다 새로 마운트되므로 마운트 시 한 번만 재생하면 된다.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // isReady가 true로 반영되어 입력칸의 editable이 실제로 바뀐 뒤에만 포커스한다.
  // (애니메이션 완료 콜백에서 곧바로 focus()를 부르면, editable=true 렌더가
  // 아직 커밋되기 전이라 포커스가 씹힐 수 있다.)
  useEffect(() => {
    if (isReady) {
      onReady?.();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isReady]);

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

  return (
    <>
      <Animated.Text style={[styles.heading, headingAnimatedStyle]}>
        {heading}
      </Animated.Text>
      <Animated.View style={[styles.body, bodyAnimatedStyle]}>
        {children(isReady)}
      </Animated.View>
    </>
  );
}

export default function RegisterScreen() {
  const [stepIndex, setStepIndex] = useState(0);
  const [isSuccess, setIsSuccess] = useState(false);
  const [checkedConsentIds, setCheckedConsentIds] = useState<string[]>([]);
  const [name, setName] = useState("");
  const [nickname, setNickname] = useState("");
  const [phone, setPhone] = useState("");
  const [email, setEmail] = useState("");
  const [verificationCode, setVerificationCode] = useState("");
  const [isCodeSent, setIsCodeSent] = useState(false);
  const [codeInvalid, setCodeInvalid] = useState(false);
  const [isEmailVerified, setIsEmailVerified] = useState(false);
  const [verifiedEmail, setVerifiedEmail] = useState<string | null>(null);
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");

  const signupMutation = useSignupMutation();

  const step = STEPS[stepIndex];
  const primaryInputRef = useRef<TextInput>(null);

  const phoneInvalid = phone.length > 0 && !PHONE_RULE.test(phone);
  const emailInvalid = email.length > 0 && !EMAIL_RULE.test(email);
  const emailSuggestions = getEmailSuggestions(email);
  const passwordInvalid = password.length > 0 && !PASSWORD_RULE.test(password);
  const passwordsMismatch =
    confirmPassword.length > 0 && password !== confirmPassword;
  // 인증 완료 후 이메일을 다시 바꾸면 자동으로 재인증이 필요해지도록 이메일과 함께 비교한다.
  const emailVerified = isEmailVerified && verifiedEmail === email.trim();
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
        return name.trim().length > 0 && nickname.trim().length > 0;
      case "phone":
        return phone.trim().length > 0 && !phoneInvalid;
      case "email":
        return email.trim().length > 0 && !emailInvalid;
      case "email-verify":
        return emailVerified;
      case "password":
        return (
          password.length > 0 &&
          !passwordInvalid &&
          confirmPassword.length > 0 &&
          !passwordsMismatch &&
          !signupMutation.isPending
        );
    }
  };

  // TODO(#99 후속): 회원가입용 이메일 인증 API가 머지되면 실제 발송 API 호출로 교체.
  const handleSendCode = () => {
    setIsCodeSent(true);
    setCodeInvalid(false);
    setVerificationCode("");
  };

  // TODO(#99 후속): 회원가입용 이메일 인증 API가 머지되면 실제 검증 API 호출로 교체.
  // 그 전까지는 UI 흐름 확인용으로 6자리 숫자면 통과시킨다.
  const handleVerifyCode = () => {
    if (verificationCode.trim().length !== 6) {
      setCodeInvalid(true);
      return;
    }
    setCodeInvalid(false);
    setIsEmailVerified(true);
    setVerifiedEmail(email.trim());
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

    if (step !== "password") {
      setStepIndex((i) => i + 1);
      return;
    }

    signupMutation.mutate(
      {
        email: email.trim(),
        password,
        nickname: nickname.trim(),
        name: name.trim(),
        phoneNumber: phone,
      },
      {
        onSuccess: () => {
          setIsSuccess(true);
        },
      },
    );
  };

  const isLastStep = step === "password";
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

            <AnimatedStep
              key={step}
              heading={HEADINGS[step]}
              onReady={() => primaryInputRef.current?.focus()}
            >
              {(isReady) => (
                <>
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
                <>
                  <TextInput
                    ref={primaryInputRef}
                    editable={isReady}
                    placeholder="이름"
                    placeholderTextColor="#94a3b8"
                    style={styles.input}
                    value={name}
                    onChangeText={setName}
                    maxLength={NICKNAME_MAX_LENGTH}
                  />
                  <TextInput
                    editable={isReady}
                    placeholder="닉네임"
                    placeholderTextColor="#94a3b8"
                    style={[styles.input, styles.inputSpacing]}
                    value={nickname}
                    onChangeText={setNickname}
                    maxLength={NICKNAME_MAX_LENGTH}
                  />
                </>
              )}

              {step === "phone" && (
                <>
                  <TextInput
                    ref={primaryInputRef}
                    editable={isReady}
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
                    ref={primaryInputRef}
                    editable={isReady}
                    autoCapitalize="none"
                    autoComplete="email"
                    keyboardType="email-address"
                    placeholder="Email"
                    placeholderTextColor="#94a3b8"
                    style={styles.input}
                    value={email}
                    onChangeText={setEmail}
                  />
                  {emailSuggestions.length > 0 ? (
                    <View style={styles.suggestionList}>
                      {emailSuggestions.map((suggestion, index) => (
                        <Pressable
                          key={suggestion}
                          style={[
                            styles.suggestionItem,
                            index === emailSuggestions.length - 1 &&
                              styles.suggestionItemLast,
                          ]}
                          onPress={() => setEmail(suggestion)}
                        >
                          <Text style={styles.suggestionText}>
                            {suggestion}
                          </Text>
                        </Pressable>
                      ))}
                    </View>
                  ) : emailInvalid ? (
                    <Text style={styles.errorText}>
                      올바른 이메일 형식이 아닙니다.
                    </Text>
                  ) : null}
                </>
              )}

              {step === "email-verify" && (
                <>
                  {!isCodeSent ? (
                    <Pressable
                      style={styles.sendCodeButton}
                      onPress={handleSendCode}
                    >
                      <Text style={styles.sendCodeButtonText}>
                        인증번호 받기
                      </Text>
                    </Pressable>
                  ) : (
                    <>
                      <TextInput
                        ref={primaryInputRef}
                        editable={isReady}
                        keyboardType="number-pad"
                        maxLength={6}
                        placeholder="인증번호 6자리"
                        placeholderTextColor="#94a3b8"
                        style={styles.input}
                        value={verificationCode}
                        onChangeText={(text) => {
                          setVerificationCode(text.replace(/\D/g, "").slice(0, 6));
                          setCodeInvalid(false);
                        }}
                      />
                      {codeInvalid ? (
                        <Text style={styles.errorText}>
                          인증번호가 올바르지 않습니다.
                        </Text>
                      ) : emailVerified ? (
                        <Text style={styles.verifiedText}>
                          이메일 인증이 완료되었습니다.
                        </Text>
                      ) : null}
                      <View style={styles.verifyActionRow}>
                        <Pressable onPress={handleSendCode}>
                          <Text style={styles.resendText}>인증번호 재전송</Text>
                        </Pressable>
                        <Pressable
                          style={styles.verifyConfirmButton}
                          onPress={handleVerifyCode}
                        >
                          <Text style={styles.verifyConfirmButtonText}>
                            확인
                          </Text>
                        </Pressable>
                      </View>
                    </>
                  )}
                </>
              )}

              {step === "password" && (
                <>
                  <TextInput
                    ref={primaryInputRef}
                    editable={isReady}
                    placeholder="Password"
                    placeholderTextColor="#94a3b8"
                    secureTextEntry
                    style={styles.input}
                    value={password}
                    onChangeText={setPassword}
                  />
                  <Text
                    style={
                      passwordInvalid ? styles.errorText : styles.consentCaption
                    }
                  >
                    {PASSWORD_RULE_MESSAGE}
                  </Text>

                  <TextInput
                    editable={isReady}
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

              {isLastStep && signupMutation.isError ? (
                <Text style={styles.errorText}>
                  {getApiErrorMessage(signupMutation.error)}
                </Text>
              ) : null}
                </>
              )}
            </AnimatedStep>

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
  suggestionList: {
    marginTop: 8,
    borderRadius: 14,
    borderWidth: 1,
    borderColor: "#e5e7eb",
    backgroundColor: "#ffffff",
    overflow: "hidden",
  },
  suggestionItem: {
    paddingHorizontal: 16,
    paddingVertical: 12,
    borderBottomWidth: 1,
    borderBottomColor: "#f1f5f9",
  },
  suggestionItemLast: {
    borderBottomWidth: 0,
  },
  suggestionText: {
    color: "#111827",
    fontFamily: FONT.kufam,
    fontSize: 15,
    fontWeight: "500",
  },
  errorText: {
    color: "#dc2626",
    fontFamily: FONT.pretendard,
    fontSize: 13,
    lineHeight: 18,
  },
  verifiedText: {
    color: "#16a34a",
    fontFamily: FONT.pretendard,
    fontSize: 13,
    lineHeight: 18,
  },
  sendCodeButton: {
    alignItems: "center",
    borderRadius: 14,
    borderWidth: 1,
    borderColor: "#6D5DFB",
    paddingVertical: 14,
  },
  sendCodeButtonText: {
    color: "#6D5DFB",
    fontFamily: FONT.pretendard,
    fontSize: 15,
    fontWeight: "800",
  },
  verifyActionRow: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
    marginTop: 12,
  },
  resendText: {
    color: "#64748b",
    fontFamily: FONT.pretendard,
    fontSize: 13,
    textDecorationLine: "underline",
  },
  verifyConfirmButton: {
    borderRadius: 12,
    backgroundColor: "#6D5DFB",
    paddingHorizontal: 20,
    paddingVertical: 10,
  },
  verifyConfirmButtonText: {
    color: "#ffffff",
    fontFamily: FONT.pretendard,
    fontSize: 14,
    fontWeight: "800",
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
