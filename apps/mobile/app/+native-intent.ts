/**
 * 앱 밖에서 들어온 URL 을 화면 이동 전에 거른다(expo-router native intent).
 *
 * 소셜 로그인 SDK 가 앱으로 돌려보내는 콜백 URL 은 SDK 가 처리할 값이지 열 화면이 아니다.
 * - 카카오톡·네이버 앱으로 로그인하고 돌아올 때: kakao{앱키}://oauth…, corematenaver://thirdPartyLoginResult…
 * - 네이버 로그인 창을 취소할 때: coremate://thirdPartyLoginResult?…&code=2 (네이버 SDK 가 앱 기본 scheme 을 씀)
 *
 * iOS 는 씬 생명주기로 바뀐 뒤 Expo 의 씬 델리게이트가 AppDelegate 가 처리한 URL 도 RCTLinkingManager 에
 * 한 번 더 알린다(SceneEventForwarder). 거르지 않으면 로그인 도중 「Unmatched Route」 화면으로 넘어간다.
 * null 을 돌려주면 expo-router 는 이동하지 않는다(초기 URL 이면 첫 화면으로 연다).
 */
const APP_SCHEME = "coremate"; // app.json 의 scheme
const LOGIN_RESULT_HOSTS = new Set(["thirdPartyLoginResult"]);

export function redirectSystemPath({ path }: { path: string; initial: boolean }): string | null {
  const scheme = /^([a-z][a-z0-9+.-]*):/i.exec(path)?.[1]?.toLowerCase();
  if (!scheme || scheme === "http" || scheme === "https") {
    return path;
  }
  if (scheme !== APP_SCHEME) {
    return null;
  }
  const host = /^[a-z][a-z0-9+.-]*:\/\/([^/?#]*)/i.exec(path)?.[1];
  return host && LOGIN_RESULT_HOSTS.has(host) ? null : path;
}
