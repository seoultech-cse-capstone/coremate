import * as AppleAuthentication from "expo-apple-authentication";
import { router } from "expo-router";
import { LinearGradient } from "expo-linear-gradient";
import { Check, ChevronRight, Mail, MessageCircle } from "lucide-react-native";
import { useEffect, useRef, useState } from "react";
import { Alert, Image, Modal, Platform, Pressable, StyleSheet, Text, View } from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";
import Svg, { Path } from "react-native-svg";

import { FONT } from "../constants/fonts";
import {
  useAuth,
  useKakaoLoginMutation,
  useNaverLoginMutation,
  useGoogleLoginMutation,
  useAppleLoginMutation,
} from "../src/features/auth";
import { LoadingScreen } from "../src/shared/components/LoadingScreen";

function GoogleLogo({ size = 20 }: { size?: number }) {
  return (
    <Svg viewBox="0 0 48 48" width={size} height={size}>
      <Path
        fill="#FFC107"
        d="M43.611,20.083H42V20H24v8h11.303c-1.649,4.657-6.08,8-11.303,8c-6.627,0-12-5.373-12-12
        c0-6.627,5.373-12,12-12c3.059,0,5.842,1.154,7.961,3.039l5.657-5.657C34.046,6.053,29.268,4,24,4C12.955,4,4,12.955,4,24
        c0,11.045,8.955,20,20,20c11.045,0,20-8.955,20-20C44,22.659,43.862,21.35,43.611,20.083z"
      />
      <Path
        fill="#FF3D00"
        d="M6.306,14.691l6.571,4.819C14.655,15.108,18.961,12,24,12c3.059,0,5.842,1.154,7.961,3.039
        l5.657-5.657C34.046,6.053,29.268,4,24,4C16.318,4,9.656,8.337,6.306,14.691z"
      />
      <Path
        fill="#4CAF50"
        d="M24,44c5.166,0,9.86-1.977,13.409-5.192l-6.19-5.238C29.211,35.091,26.715,36,24,36
        c-5.202,0-9.619-3.317-11.283-7.946l-6.522,5.025C9.505,39.556,16.227,44,24,44z"
      />
      <Path
        fill="#1976D2"
        d="M43.611,20.083H42V20H24v8h11.303c-0.792,2.237-2.231,4.166-4.087,5.571
        c0.001-0.001,0.002-0.001,0.003-0.002l6.19,5.238C36.971,39.205,44,34,44,24C44,22.659,43.862,21.35,43.611,20.083z"
      />
    </Svg>
  );
}

// 네이버 로그인 버튼 가이드의 흰색 N 로고. 글자 "N"은 공식 로고와 모양이 달라 검수에서 걸릴 수 있다.
function NaverLogo({ size = 16 }: { size?: number }) {
  return (
    <Svg viewBox="0 0 24 24" width={size} height={size}>
      <Path fill="#FFFFFF" d="M16.273 12.845 7.376 0H0v24h7.726V11.156L16.624 24H24V0h-7.727v12.845Z" />
    </Svg>
  );
}

// 소셜 로그인은 가입과 로그인이 같은 버튼이라 누를 때마다 가입 동의를 먼저 받는다.
// 이메일 가입(register.tsx)의 필수 동의와 같은 세 가지다.
const SOCIAL_CONSENT_ITEMS: {
  id: string;
  label: string;
  href?: "/terms-policies" | "/privacy-policy";
}[] = [
  { id: "age", label: "만 14세 이상입니다" },
  { id: "terms", label: "서비스 이용약관 동의", href: "/terms-policies" },
  { id: "privacy", label: "개인정보 수집 및 이용 동의", href: "/privacy-policy" },
];

export default function StartScreen() {
  const { continueAsGuest, status } = useAuth();
  const kakaoLogin = useKakaoLoginMutation();
  const naverLogin = useNaverLoginMutation();
  const googleLogin = useGoogleLoginMutation();
  const appleLogin = useAppleLoginMutation();
  const [pendingLogin, setPendingLogin] = useState<(() => void) | null>(null);
  const [checkedConsentIds, setCheckedConsentIds] = useState<string[]>([]);
  // iOS 는 동의 창이 다 닫힌 뒤에 로그인 창을 띄워야 한다(닫히는 중에 띄우면 무시된다).
  const loginAfterConsent = useRef<(() => void) | null>(null);

  // 세션이 복원되어 이미 로그인된 상태라면 시작화면을 보여주지 않고 바로 홈으로 보낸다.
  // 카카오 로그인 성공도 status 를 authenticated 로 바꾸므로 이 효과가 홈으로 보낸다.
  useEffect(() => {
    if (status === "authenticated") {
      router.replace("/(tabs)/home");
    }
  }, [status]);

  const handleKakaoLogin = () => {
    kakaoLogin.mutate(undefined, {
      onError: (error) => {
        // 사용자가 로그인 창을 닫은 경우는 조용히 무시한다.
        if (String(error).toLowerCase().includes("cancel")) {
          return;
        }

      },
    });
  };

  const handleNaverLogin = () => {
    naverLogin.mutate(undefined, {
      onError: (error) => {
        // 사용자가 로그인 창을 닫은 경우는 조용히 무시한다.
        if (String(error).toLowerCase().includes("cancel")) {
          return;
        }

      },
    });
  };

  const handleGoogleLogin = () => {
    googleLogin.mutate(undefined, {
      onError: (error) => {
        // 사용자가 로그인 창을 닫은 경우는 조용히 무시한다.
        if (String(error).toLowerCase().includes("cancel")) {
          return;
        }

      },
    });
  };

  const handleAppleLogin = () => {
    if (appleLogin.isPending) {
      return;
    }
    appleLogin.mutate(undefined, {
      onError: (error) => {
        // 사용자가 로그인 창을 닫은 경우는 조용히 무시한다.
        if (String(error).toLowerCase().includes("cancel")) {
          return;
        }

      },
    });
  };

  const handleGuest = async () => {
    await continueAsGuest();
    router.replace("/(tabs)/content");
  };

  const allConsentChecked = SOCIAL_CONSENT_ITEMS.every((item) =>
    checkedConsentIds.includes(item.id),
  );

  const askConsent = (login: () => void) => {
    setPendingLogin(() => login);
  };

  const toggleAllConsent = () => {
    setCheckedConsentIds(allConsentChecked ? [] : SOCIAL_CONSENT_ITEMS.map((item) => item.id));
  };

  const toggleConsentItem = (id: string) => {
    setCheckedConsentIds((current) =>
      current.includes(id) ? current.filter((i) => i !== id) : [...current, id],
    );
  };

  // 약관을 보고 돌아오면 같은 로그인 버튼을 다시 누른다. 체크한 항목은 그대로 남는다.
  const openConsentDetail = (href: "/terms-policies" | "/privacy-policy") => {
    setPendingLogin(null);
    router.push(href);
  };

  const runLoginAfterConsent = () => {
    const login = loginAfterConsent.current;
    loginAfterConsent.current = null;
    login?.();
  };

  const confirmConsent = () => {
    loginAfterConsent.current = pendingLogin;
    setPendingLogin(null);
    if (Platform.OS !== "ios") {
      runLoginAfterConsent();
    }
  };

  if (status === "loading" || status === "authenticated") {
    return <LoadingScreen />;
  }

  return (
    <LinearGradient
      colors={["#AD04F7", "#0760FD"]}
      start={{ x: 0, y: 0 }}
      end={{ x: 1, y: 1 }}
      style={styles.container}
    >
      <SafeAreaView edges={["top"]} style={styles.content}>
        <Image
          source={require("../assets/images/splash-icon-white.png")}
          style={styles.logo}
          resizeMode="contain"
        />

        <View style={styles.buttons}>
          <Pressable
            style={styles.kakaoButton}
            onPress={() => askConsent(handleKakaoLogin)}
            disabled={kakaoLogin.isPending}
          >
            <MessageCircle color="#191919" size={20} strokeWidth={2.3} />
            <Text style={styles.kakaoButtonText}>
              {kakaoLogin.isPending ? "로그인 중..." : "카카오 로그인"}
            </Text>
          </Pressable>

          <Pressable
            style={styles.naverButton}
            onPress={() => askConsent(handleNaverLogin)}
            disabled={naverLogin.isPending}
          >
            <NaverLogo size={16} />
            <Text style={styles.naverButtonText}>
              {naverLogin.isPending ? "로그인 중..." : "네이버 로그인"}
            </Text>
          </Pressable>

          <Pressable
            style={styles.googleButton}
            onPress={() => askConsent(handleGoogleLogin)}
            disabled={googleLogin.isPending}
          >
            <GoogleLogo size={20} />
            <Text style={styles.googleButtonText}>
              {googleLogin.isPending ? "로그인 중..." : "구글 로그인"}
            </Text>
          </Pressable>

          {/* App Store 심사 지침 4.8: 소셜 로그인이 있으면 Apple 로그인을 같은 크기로 함께 둔다(아이폰만). */}
          {Platform.OS === "ios" ? (
            <AppleAuthentication.AppleAuthenticationButton
              buttonType={AppleAuthentication.AppleAuthenticationButtonType.SIGN_IN}
              buttonStyle={AppleAuthentication.AppleAuthenticationButtonStyle.BLACK}
              cornerRadius={16}
              style={styles.appleButton}
              onPress={() => askConsent(handleAppleLogin)}
            />
          ) : null}

          <Pressable
            style={styles.emailButton}
            onPress={() => router.push("/email-login")}
          >
            <Mail color="#0f172a" size={20} strokeWidth={2.3} />
            <Text style={styles.emailButtonText}>이메일로 로그인</Text>
          </Pressable>

          <Pressable style={styles.guestButton} onPress={handleGuest}>
            <Text style={styles.guestButtonText}>비로그인으로 체험하기</Text>
          </Pressable>

          <Text style={styles.guestNotice}>
            게스트로 로그인할 시 기능이 제한될 수 있습니다.
          </Text>
        </View>
      </SafeAreaView>

      <Modal
        visible={pendingLogin !== null}
        transparent
        animationType="fade"
        onRequestClose={() => setPendingLogin(null)}
        onDismiss={runLoginAfterConsent}
      >
        <View style={styles.sheetBackdrop}>
          <Pressable style={StyleSheet.absoluteFill} onPress={() => setPendingLogin(null)} />
          <View style={styles.sheet}>
            <Text style={styles.sheetTitle}>시작하기 전에 동의해 주세요</Text>

            <Pressable style={styles.sheetAllRow} onPress={toggleAllConsent}>
              <Check
                color={allConsentChecked ? "#6D5DFB" : "#cbd5e1"}
                size={22}
                strokeWidth={3}
              />
              <Text style={styles.sheetAllText}>전체 동의</Text>
            </Pressable>

            <View style={styles.sheetDivider} />

            {SOCIAL_CONSENT_ITEMS.map((item) => {
              const checked = checkedConsentIds.includes(item.id);
              return (
                <View key={item.id} style={styles.sheetRow}>
                  <Pressable
                    style={styles.sheetRowMain}
                    onPress={() => toggleConsentItem(item.id)}
                  >
                    <Check color={checked ? "#6D5DFB" : "#cbd5e1"} size={20} strokeWidth={3} />
                    <Text style={styles.sheetRowText}>{item.label} (필수)</Text>
                  </Pressable>
                  {item.href ? (
                    <Pressable
                      accessibilityLabel={`${item.label} 자세히 보기`}
                      hitSlop={8}
                      onPress={() => item.href && openConsentDetail(item.href)}
                    >
                      <ChevronRight color="#9ca3af" size={20} strokeWidth={2.2} />
                    </Pressable>
                  ) : null}
                </View>
              );
            })}

            <Pressable
              style={[styles.sheetButton, !allConsentChecked && styles.sheetButtonDisabled]}
              disabled={!allConsentChecked}
              onPress={confirmConsent}
            >
              <Text style={styles.sheetButtonText}>동의하고 계속하기</Text>
            </Pressable>
          </View>
        </View>
      </Modal>
    </LinearGradient>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  content: {
    flex: 1,
    alignItems: "center",
    justifyContent: "center",
    paddingHorizontal: 24,
  },
  logo: {
    width: 160,
    height: 160,
    marginBottom: 84,
  },
  buttons: {
    width: "100%",
    gap: 12,
    marginTop: 12,
  },
  kakaoButton: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "center",
    gap: 8,
    borderRadius: 16,
    backgroundColor: "#FEE500",
    paddingVertical: 16,
  },
  kakaoButtonText: {
    color: "#191919",
    fontFamily: FONT.pretendard,
    fontSize: 16,
    fontWeight: "800",
  },
  naverButton: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "center",
    gap: 8,
    borderRadius: 16,
    backgroundColor: "#03C75A",
    paddingVertical: 16,
  },
  naverButtonText: {
    color: "#ffffff",
    fontFamily: FONT.pretendard,
    fontSize: 16,
    fontWeight: "800",
  },
  appleButton: {
    width: "100%",
    height: 52,
  },
  googleButton: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "center",
    gap: 8,
    borderRadius: 16,
    backgroundColor: "#ffffff",
    borderWidth: 1,
    borderColor: "#DADCE0",
    paddingVertical: 16,
  },
  googleButtonText: {
    color: "#3c4043",
    fontFamily: FONT.pretendard,
    fontSize: 16,
    fontWeight: "800",
  },
  emailButton: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "center",
    gap: 8,
    borderRadius: 16,
    backgroundColor: "#ffffff",
    paddingVertical: 16,
    marginTop: 20,
    shadowColor: "#000000",
    shadowOpacity: 0.14,
    shadowRadius: 16,
    shadowOffset: {
      width: 0,
      height: 8,
    },
    elevation: 6,
  },
  emailButtonText: {
    color: "#0f172a",
    fontFamily: FONT.pretendard,
    fontSize: 16,
    fontWeight: "800",
  },
  guestButton: {
    alignSelf: "center",
    paddingVertical: 4,
  },
  guestButtonText: {
    color: "#ffffff",
    fontFamily: FONT.pretendard,
    fontSize: 14,
    fontWeight: "600",
    textDecorationLine: "underline",
  },
  guestNotice: {
    color: "rgba(255, 255, 255, 0.7)",
    fontFamily: FONT.pretendard,
    fontSize: 13,
    lineHeight: 18,
    textAlign: "center",
  },
  sheetBackdrop: {
    flex: 1,
    justifyContent: "flex-end",
    backgroundColor: "rgba(15, 23, 42, 0.45)",
  },
  sheet: {
    borderTopLeftRadius: 24,
    borderTopRightRadius: 24,
    backgroundColor: "#ffffff",
    paddingHorizontal: 24,
    paddingTop: 28,
    paddingBottom: 40,
  },
  sheetTitle: {
    marginBottom: 20,
    color: "#111827",
    fontFamily: FONT.pretendard,
    fontSize: 19,
    fontWeight: "800",
  },
  sheetAllRow: {
    flexDirection: "row",
    alignItems: "center",
    gap: 12,
    paddingVertical: 4,
  },
  sheetAllText: {
    color: "#111827",
    fontFamily: FONT.pretendard,
    fontSize: 17,
    fontWeight: "800",
  },
  sheetDivider: {
    height: 1,
    marginVertical: 14,
    backgroundColor: "#e5e7eb",
  },
  sheetRow: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
    paddingVertical: 10,
  },
  sheetRowMain: {
    flex: 1,
    flexDirection: "row",
    alignItems: "center",
    gap: 12,
  },
  sheetRowText: {
    flex: 1,
    color: "#1f2937",
    fontFamily: FONT.pretendard,
    fontSize: 15,
    lineHeight: 21,
  },
  sheetButton: {
    alignItems: "center",
    marginTop: 20,
    borderRadius: 16,
    backgroundColor: "#6D5DFB",
    paddingVertical: 16,
  },
  sheetButtonDisabled: {
    opacity: 0.4,
  },
  sheetButtonText: {
    color: "#ffffff",
    fontFamily: FONT.pretendard,
    fontSize: 16,
    fontWeight: "800",
  },
});
