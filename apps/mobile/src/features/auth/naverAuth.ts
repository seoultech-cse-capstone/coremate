import NaverLogin from "@react-native-seoul/naver-login";

/**
 * 네이버 SDK 로그인 → access token 반환.
 *
 * 카카오와 달리 네이티브 초기화가 config plugin 이 아니라 런타임 initialize 로 이뤄진다.
 * consumerKey/secret 은 앱에 심겨야 하므로 .env(EXPO_PUBLIC_NAVER_*)에서만 주입하고 git 에 커밋하지 않는다.
 * 사용자가 취소하면 successResponse 가 비고 failureResponse.isCancel 이 서므로, 호출부가 구분할 수 있도록
 * 메시지에 "cancel" 을 담아 reject 한다(카카오와 동일하게 호출부에서 조용히 무시).
 */
const consumerKey = process.env.EXPO_PUBLIC_NAVER_CONSUMER_KEY ?? "";
const consumerSecret = process.env.EXPO_PUBLIC_NAVER_CONSUMER_SECRET ?? "";

let initialized = false;

function ensureInitialized() {
  if (initialized) {
    return;
  }
  NaverLogin.initialize({
    appName: "CoreMate",
    consumerKey,
    consumerSecret,
    // iOS 앱-투-앱 로그인 후 되돌아올 URL scheme. app.json 의 scheme 과 일치시킨다.
    serviceUrlSchemeIOS: "coremate",
  });
  initialized = true;
}

export async function signInWithNaver(): Promise<string> {
  ensureInitialized();
  const { successResponse, failureResponse } = await NaverLogin.login();
  if (!successResponse) {
    throw new Error(failureResponse?.isCancel ? "cancelled" : failureResponse?.message ?? "naver login failed");
  }
  return successResponse.accessToken;
}
