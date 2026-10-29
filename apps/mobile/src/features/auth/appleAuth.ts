/**
 * Apple 로그인(아이폰 전용) → identity token·authorization code·이름 반환.
 *
 * 백엔드는 identity token 을 Apple 공개키로 검증하고, authorization code 는 탈퇴할 때 Apple 연결을 끊는 데 쓴다.
 * Apple 은 이름을 처음 로그인할 때만 주므로 받은 이름을 그대로 넘긴다(서버는 처음 가입할 때만 저장한다).
 * 사용자가 취소하면 ERR_REQUEST_CANCELED 로 reject 되므로 메시지에 "cancel" 을 담아 호출부가 조용히 무시하게 한다
 * (카카오/네이버/구글과 동일).
 *
 * SDK는 이 함수가 실제로 호출될 때만 import 한다(googleAuth.ts 참고 — Expo Go 부팅 크래시 방지).
 */
export async function signInWithApple(): Promise<{
  identityToken: string;
  authorizationCode: string | null;
  name: string | null;
}> {
  const AppleAuthentication = await import("expo-apple-authentication");

  let credential;
  try {
    credential = await AppleAuthentication.signInAsync({
      requestedScopes: [
        AppleAuthentication.AppleAuthenticationScope.FULL_NAME,
        AppleAuthentication.AppleAuthenticationScope.EMAIL,
      ],
    });
  } catch (error) {
    if ((error as { code?: string }).code === "ERR_REQUEST_CANCELED") {
      throw new Error("cancelled");
    }
    throw error;
  }

  if (!credential.identityToken) {
    throw new Error("apple login failed");
  }
  // 이름 순서는 기기 언어에 맞춘다(한국어면 성+이름).
  const name = credential.fullName ? AppleAuthentication.formatFullName(credential.fullName).trim() : "";
  return {
    identityToken: credential.identityToken,
    authorizationCode: credential.authorizationCode,
    name: name || null,
  };
}
