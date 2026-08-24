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
import {
  useRedirectIfAuthenticated,
  useSendSignupCodeMutation,
  useSignupMutation,
  useVerifySignupCodeMutation,
} from "../src/features/auth";
import { getApiErrorCode, getApiErrorMessage } from "../src/shared/utils/apiError";
import {
  PASSWORD_RULE,
  PASSWORD_RULE_MESSAGE,
} from "../src/shared/constants/passwordRule";

const NAME_MAX_LENGTH = 50;
// 이름은 한글 또는 영문만 허용한다(특수문자·숫자·공백 불가).
const NAME_RULE = /^[가-힣a-zA-Z]+$/;
const NAME_RULE_MESSAGE = "이름은 한글 또는 영문만 입력할 수 있습니다.";
const NICKNAME_MAX_LENGTH = 6;
const EMAIL_RULE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
// phone 상태값은 항상 숫자만 저장한다("01012345678"). 화면 표시용 하이픈은 formatPhoneNumber에서만 붙인다.
const PHONE_RULE = /^01[016789]\d{7,8}$/;
const PHONE_RULE_MESSAGE = "올바른 휴대폰 번호 형식이 아닙니다.";
const RESEND_COOLDOWN_SEC = 60;

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

const STEPS = ["consent", "form"] as const;
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
  form: "회원 정보를\n입력해주세요.",
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

// 폼 안에서 입력칸이 하나씩 나타날 때 위에서 아래로 스르륵 나타나도록 하는 래퍼.
// 조건부 렌더링으로 마운트될 때 한 번만 재생하면 되므로 별도 key 관리가 필요 없다.
function Reveal({ children }: { children: React.ReactNode }) {
  const anim = useRef(new Animated.Value(0)).current;

  useEffect(() => {
    Animated.timing(anim, {
      toValue: 1,
      duration: 320,
      useNativeDriver: true,
    }).start();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const style = {
    opacity: anim,
    transform: [
      {
        translateY: anim.interpolate({
          inputRange: [0, 1],
          outputRange: [12, 0],
        }),
      },
    ],
  };

  return (
    <Animated.View style={[styles.revealItem, style]}>
      {children}
    </Animated.View>
  );
}


export default function RegisterScreen() {
  useRedirectIfAuthenticated();
  const [stepIndex, setStepIndex] = useState(0);
  const [isSuccess, setIsSuccess] = useState(false);
  const [checkedConsentIds, setCheckedConsentIds] = useState<string[]>([]);
  const [name, setName] = useState("");
  const [confirmedName, setConfirmedName] = useState(false);
  const [nickname, setNickname] = useState("");
  const [nicknameTaken, setNicknameTaken] = useState(false);
  const [confirmedNickname, setConfirmedNickname] = useState(false);
  const [phone, setPhone] = useState("");
  const [confirmedPhone, setConfirmedPhone] = useState(false);
  const [email, setEmail] = useState("");
  const [emailTaken, setEmailTaken] = useState(false);
  const [verificationCode, setVerificationCode] = useState("");
  const [isCodeSent, setIsCodeSent] = useState(false);
  const [codeErrorMessage, setCodeErrorMessage] = useState<string | null>(null);
  const [sendCodeErrorMessage, setSendCodeErrorMessage] = useState<string | null>(null);
  const [isEmailVerified, setIsEmailVerified] = useState(false);
  const [verifiedEmail, setVerifiedEmail] = useState<string | null>(null);
  const [resendCooldown, setResendCooldown] = useState(0);
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");

  const signupMutation = useSignupMutation();
  const sendSignupCodeMutation = useSendSignupCodeMutation();
  const verifySignupCodeMutation = useVerifySignupCodeMutation();

  const step = STEPS[stepIndex];
  const primaryInputRef = useRef<TextInput>(null);

  const nameInvalid = name.length > 0 && !NAME_RULE.test(name.trim());
  const nameValid = name.trim().length > 0 && !nameInvalid;
  const nicknameValid =
    nickname.trim().length > 0 && nickname.trim().length <= NICKNAME_MAX_LENGTH;
  const phoneInvalid = phone.length > 0 && !PHONE_RULE.test(phone);
  const phoneValid = phone.trim().length > 0 && !phoneInvalid;
  const emailInvalid = email.length > 0 && !EMAIL_RULE.test(email);
  const emailValid = email.trim().length > 0 && !emailInvalid;
  const emailSuggestions = getEmailSuggestions(email);
  // 인증 완료 후 이메일을 다시 바꾸면 자동으로 재인증이 필요해지도록 이메일과 함께 비교한다.
  const emailVerified = isEmailVerified && verifiedEmail === email.trim();
  const passwordInvalid = password.length > 0 && !PASSWORD_RULE.test(password);
  const passwordValid = password.length > 0 && !passwordInvalid;
  const passwordsMismatch =
    confirmPassword.length > 0 && password !== confirmPassword;
  const confirmValid = confirmPassword.length > 0 && !passwordsMismatch;

  // 각 입력칸을 다 채운 뒤 "다음"을 눌러야 다음 입력칸이 위에서 아래로 나타난다.
  // 이메일+인증, 비밀번호+비밀번호 확인은 각각 한 그룹으로 같이 나타난다.
  // (이메일 인증은 "확인" 버튼이, 가입하기는 그 자체가 마지막 확인 동작이라 별도 다음 버튼이 없다.)
  const showNickname = confirmedName;
  const showPhone = confirmedNickname;
  const showEmailGroup = confirmedPhone;
  const showPasswordGroup = emailVerified;
  const showSubmit = showPasswordGroup && passwordValid && confirmValid;

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

  const handleSendCode = () => {
    if (!emailValid || resendCooldown > 0 || sendSignupCodeMutation.isPending) {
      return;
    }
    setSendCodeErrorMessage(null);
    sendSignupCodeMutation.mutate(
      { email: email.trim() },
      {
        onSuccess: () => {
          setIsCodeSent(true);
          setCodeErrorMessage(null);
          setVerificationCode("");
          setResendCooldown(RESEND_COOLDOWN_SEC);
        },
        onError: (error) => {
          const code = getApiErrorCode(error);
          if (code === "EMAIL_ALREADY_EXISTS") {
            setEmailTaken(true);
            return;
          }
          setSendCodeErrorMessage(
            getApiErrorMessage(error, "인증번호 발송에 실패했어요."),
          );
        },
      },
    );
  };

  const handleVerifyCode = () => {
    if (verificationCode.trim().length !== 6 || verifySignupCodeMutation.isPending) {
      setCodeErrorMessage("인증번호 6자리를 입력해주세요.");
      return;
    }
    verifySignupCodeMutation.mutate(
      { email: email.trim(), code: verificationCode.trim() },
      {
        onSuccess: () => {
          setCodeErrorMessage(null);
          setIsEmailVerified(true);
          setVerifiedEmail(email.trim());
        },
        onError: (error) => {
          setCodeErrorMessage(
            getApiErrorMessage(error, "인증번호가 올바르지 않습니다."),
          );
        },
      },
    );
  };

  useEffect(() => {
    if (resendCooldown <= 0) {
      return;
    }
    const timer = setTimeout(() => setResendCooldown((s) => s - 1), 1000);
    return () => clearTimeout(timer);
  }, [resendCooldown]);

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

  const goToForm = () => {
    if (!requiredConsentChecked) {
      return;
    }
    setStepIndex(STEPS.indexOf("form"));
  };

  const handleSubmit = () => {
    if (!showSubmit || signupMutation.isPending) {
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
        onError: (error) => {
          const code = getApiErrorCode(error);
          if (code === "DUPLICATE_NICKNAME") {
            setNicknameTaken(true);
          } else if (code === "EMAIL_ALREADY_EXISTS") {
            setEmailTaken(true);
          } else if (code === "EMAIL_NOT_VERIFIED") {
            // 인증 후 30분이 지나 서버 인증 상태가 만료된 경우. 처음부터 다시 인증받게 한다.
            setIsEmailVerified(false);
            setVerifiedEmail(null);
            setIsCodeSent(false);
            setSendCodeErrorMessage(
              "이메일 인증이 만료되었어요. 인증번호를 다시 받아주세요.",
            );
          }
        },
      },
    );
  };

  const isBusy = signupMutation.isPending;
  const showGenericError =
    signupMutation.isError && !nicknameTaken && !emailTaken;

  // 동의 스텝의 "다음"과 동일한 위치의 버튼 하나가 폼의 각 필드를 순서대로
  // "다음"으로 확정시키다가, 모든 필드가 끝나면 "가입하기"로 바뀐다.
  const bottomAction: { label: string; enabled: boolean; onPress: () => void } =
    step === "consent"
      ? { label: "다음", enabled: requiredConsentChecked, onPress: goToForm }
      : !confirmedName
        ? {
            label: "다음",
            enabled: nameValid,
            onPress: () => setConfirmedName(true),
          }
        : !confirmedNickname
          ? {
              label: "다음",
              enabled: nicknameValid,
              onPress: () => setConfirmedNickname(true),
            }
          : !confirmedPhone
            ? {
                label: "다음",
                enabled: phoneValid,
                onPress: () => setConfirmedPhone(true),
              }
            : !emailVerified
              ? { label: "다음", enabled: false, onPress: () => {} }
              : {
                  label: isBusy ? "가입 중…" : "가입하기",
                  enabled: passwordValid && confirmValid && !isBusy,
                  onPress: handleSubmit,
                };

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

                  {step === "form" && (
                    <View>
                      <Reveal>
                        <TextInput
                          ref={primaryInputRef}
                          editable={isReady}
                          autoComplete="name"
                          textContentType="name"
                          placeholder="이름"
                          placeholderTextColor="#94a3b8"
                          style={styles.input}
                          value={name}
                          onChangeText={setName}
                          maxLength={NAME_MAX_LENGTH}
                        />
                        {nameInvalid ? (
                          <Text style={styles.errorText}>
                            {NAME_RULE_MESSAGE}
                          </Text>
                        ) : null}
                      </Reveal>

                      {showNickname ? (
                        <Reveal>
                          <TextInput
                            autoFocus
                            editable={isReady}
                            autoComplete="nickname"
                            textContentType="nickname"
                            placeholder="닉네임"
                            placeholderTextColor="#94a3b8"
                            style={styles.input}
                            value={nickname}
                            onChangeText={(text) => {
                              setNickname(text);
                              setNicknameTaken(false);
                            }}
                            maxLength={NICKNAME_MAX_LENGTH}
                          />
                          {nicknameTaken ? (
                            <Text style={styles.errorText}>
                              이미 사용 중인 닉네임입니다.
                            </Text>
                          ) : null}
                        </Reveal>
                      ) : null}

                      {showPhone ? (
                        <Reveal>
                          <TextInput
                            autoFocus
                            editable={isReady}
                            autoComplete="tel"
                            textContentType="telephoneNumber"
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
                            <Text style={styles.errorText}>
                              {PHONE_RULE_MESSAGE}
                            </Text>
                          ) : null}
                        </Reveal>
                      ) : null}

                      {showEmailGroup ? (
                        <Reveal>
                          <TextInput
                            autoFocus
                            editable={isReady}
                            autoCapitalize="none"
                            autoComplete="username"
                            autoCorrect={false}
                            spellCheck={false}
                            textContentType="username"
                            keyboardType="email-address"
                            placeholder="Email"
                            placeholderTextColor="#94a3b8"
                            style={styles.input}
                            value={email}
                            onChangeText={(text) => {
                              setEmail(text);
                              setEmailTaken(false);
                            }}
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
                          ) : emailTaken ? (
                            <Text style={styles.errorText}>
                              이미 사용 중인 이메일입니다.
                            </Text>
                          ) : emailInvalid ? (
                            <Text style={styles.errorText}>
                              올바른 이메일 형식이 아닙니다.
                            </Text>
                          ) : null}
                        </Reveal>
                      ) : null}

                      {showEmailGroup ? (
                        <Reveal>
                          {emailVerified ? (
                            <Text style={styles.verifiedText}>
                              이메일 인증이 완료되었습니다.
                            </Text>
                          ) : !isCodeSent ? (
                            <>
                              <Pressable
                                style={[
                                  styles.sendCodeButton,
                                  (!emailValid || sendSignupCodeMutation.isPending) &&
                                    styles.buttonDisabled,
                                ]}
                                onPress={handleSendCode}
                                disabled={!emailValid || sendSignupCodeMutation.isPending}
                              >
                                <Text style={styles.sendCodeButtonText}>
                                  {sendSignupCodeMutation.isPending
                                    ? "발송 중…"
                                    : "인증번호 받기"}
                                </Text>
                              </Pressable>
                              {sendCodeErrorMessage ? (
                                <Text style={styles.errorText}>
                                  {sendCodeErrorMessage}
                                </Text>
                              ) : null}
                            </>
                          ) : (
                            <>
                              <View style={styles.codeInputBox}>
                                <TextInput
                                  autoFocus
                                  editable={isReady}
                                  keyboardType="number-pad"
                                  maxLength={6}
                                  placeholder="인증번호 6자리"
                                  placeholderTextColor="#94a3b8"
                                  style={styles.codeInputField}
                                  value={verificationCode}
                                  onChangeText={(text) => {
                                    setVerificationCode(
                                      text.replace(/\D/g, "").slice(0, 6),
                                    );
                                    setCodeErrorMessage(null);
                                  }}
                                />
                                <Pressable
                                  style={[
                                    styles.verifyConfirmButtonInline,
                                    verifySignupCodeMutation.isPending &&
                                      styles.buttonDisabled,
                                  ]}
                                  disabled={verifySignupCodeMutation.isPending}
                                  onPress={handleVerifyCode}
                                >
                                  <Text style={styles.verifyConfirmButtonText}>
                                    {verifySignupCodeMutation.isPending ? "확인 중…" : "확인"}
                                  </Text>
                                </Pressable>
                              </View>
                              {codeErrorMessage ? (
                                <Text style={styles.errorText}>{codeErrorMessage}</Text>
                              ) : null}
                              {sendCodeErrorMessage ? (
                                <Text style={styles.errorText}>
                                  {sendCodeErrorMessage}
                                </Text>
                              ) : null}
                              <Pressable
                                disabled={resendCooldown > 0 || sendSignupCodeMutation.isPending}
                                style={styles.resendLink}
                                onPress={handleSendCode}
                              >
                                <Text
                                  style={[
                                    styles.resendText,
                                    resendCooldown > 0 &&
                                      styles.resendTextDisabled,
                                  ]}
                                >
                                  {resendCooldown > 0
                                    ? `인증번호 재전송 (${resendCooldown}초)`
                                    : "인증번호 재전송"}
                                </Text>
                              </Pressable>
                            </>
                          )}
                        </Reveal>
                      ) : null}

                      {showPasswordGroup ? (
                        <Reveal>
                          <TextInput
                            autoFocus
                            editable={isReady}
                            autoComplete="new-password"
                            textContentType="newPassword"
                            placeholder="Password"
                            placeholderTextColor="#94a3b8"
                            secureTextEntry
                            style={styles.input}
                            value={password}
                            onChangeText={setPassword}
                          />
                          <Text
                            style={
                              passwordInvalid
                                ? styles.errorText
                                : styles.consentCaption
                            }
                          >
                            {PASSWORD_RULE_MESSAGE}
                          </Text>

                          <TextInput
                            editable={isReady}
                            autoComplete="new-password"
                            textContentType="newPassword"
                            placeholder="Confirm Password"
                            placeholderTextColor="#94a3b8"
                            secureTextEntry
                            style={[styles.input, styles.revealItem]}
                            value={confirmPassword}
                            onChangeText={setConfirmPassword}
                          />
                          {passwordsMismatch ? (
                            <Text style={styles.errorText}>
                              비밀번호가 일치하지 않습니다.
                            </Text>
                          ) : null}
                        </Reveal>
                      ) : null}

                      {showSubmit && showGenericError ? (
                        <Reveal>
                          <Text style={styles.errorText}>
                            {getApiErrorMessage(signupMutation.error)}
                          </Text>
                        </Reveal>
                      ) : null}
                    </View>
                  )}
                </>
              )}
            </AnimatedStep>

            {/* 동의 스텝의 "다음"과 같은 위치·스타일의 버튼 하나로 폼의 각 필드를
                "다음"으로 확정해 나가다가, 마지막에는 "가입하기"로 바뀐다. */}
            <Pressable
              style={[
                styles.button,
                !bottomAction.enabled && styles.buttonDisabled,
              ]}
              onPress={bottomAction.onPress}
              disabled={!bottomAction.enabled}
            >
              <Text style={styles.buttonText}>{bottomAction.label}</Text>
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
  revealItem: {
    marginTop: 16,
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
  codeInputBox: {
    flexDirection: "row",
    alignItems: "center",
    borderRadius: 14,
    borderWidth: 1,
    borderColor: "#d1d5db",
    backgroundColor: "#ffffff",
    paddingLeft: 16,
    paddingRight: 6,
    paddingVertical: 6,
  },
  codeInputField: {
    flex: 1,
    color: "#111827",
    fontFamily: FONT.kufam,
    fontSize: 16,
    fontWeight: "500",
    paddingVertical: 8,
  },
  verifyConfirmButtonInline: {
    borderRadius: 10,
    backgroundColor: "#6D5DFB",
    paddingHorizontal: 16,
    paddingVertical: 10,
  },
  resendLink: {
    alignSelf: "flex-start",
    marginTop: 12,
  },
  resendText: {
    color: "#64748b",
    fontFamily: FONT.pretendard,
    fontSize: 13,
    textDecorationLine: "underline",
  },
  resendTextDisabled: {
    color: "#cbd5e1",
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
