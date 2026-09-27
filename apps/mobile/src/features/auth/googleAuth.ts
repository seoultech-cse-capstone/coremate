/**
 * 구글 SDK 로그인 → idToken + accessToken 반환.
 *
 * webClientId 로 발급받은 idToken 의 aud 를 백엔드가 검증하므로 이 값이 반드시 필요하다.
 * 키는 앱에 심겨야 하므로 .env(EXPO_PUBLIC_GOOGLE_WEB_CLIENT_ID)에서만 주입하고 git 에 커밋하지 않는다.
 * 사용자가 취소하면 응답 type 이 "success" 가 아니므로 메시지에 "cancel" 을 담아 reject 한다
 * (카카오/네이버와 동일하게 호출부에서 조용히 무시).
 *
 * SDK는 이 함수가 실제로 호출될 때만 import 한다. 최상단에서 바로 import 하면 그 모듈의
 * 네이티브 스펙이 즉시 TurboModuleRegistry 를 찾는데, expo-router 는 부팅 시 app/ 하위 파일을
 * 전부 미리 불러오므로 로그인 화면을 열지 않아도 네이티브 모듈이 없는 환경(Expo Go)에서
 * 앱이 바로 죽는다.
 */
const webClientId = process.env.EXPO_PUBLIC_GOOGLE_WEB_CLIENT_ID ?? "";

let configured = false;

export async function signInWithGoogle(): Promise<{ idToken: string; accessToken: string }> {
  const { GoogleSignin } = await import("@react-native-google-signin/google-signin");

  if (!configured) {
    GoogleSignin.configure({ webClientId });
    configured = true;
  }

  await GoogleSignin.hasPlayServices();

  const response = await GoogleSignin.signIn();
  if (response.type !== "success") {
    throw new Error("cancelled");
  }

  // idToken·accessToken 둘 다 확실히 확보한다(백엔드는 idToken 을 검증, accessToken 은 공통 필드).
  const { idToken, accessToken } = await GoogleSignin.getTokens();
  return { idToken, accessToken };
}
