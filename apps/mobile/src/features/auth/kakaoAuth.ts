/**
 * 카카오 SDK 로그인 → access token 반환.
 *
 * SDK 초기화는 config plugin 이 네이티브 앱 키를 심어 처리하므로 별도 init 은 없다.
 * 카카오톡이 설치돼 있으면 앱-투-앱, 아니면 웹(계정) 로그인으로 자동 폴백한다.
 * 사용자가 취소하면 reject 되므로 호출부에서 처리한다.
 *
 * SDK는 이 함수가 실제로 호출될 때만 import 한다(googleAuth.ts 참고 — Expo Go 부팅 크래시 방지).
 */
export async function signInWithKakao(): Promise<string> {
  const { login: kakaoLogin } = await import("@react-native-seoul/kakao-login");
  const token = await kakaoLogin();
  return token.accessToken;
}
