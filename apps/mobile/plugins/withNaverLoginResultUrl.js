// 네이버 로그인 창에서 「취소」를 눌렀을 때의 결과 URL 도 네이버 SDK 로 넘긴다.
//
// 네이버 SDK 는 로그인 창(ASWebAuthenticationSession)을 취소하면 결과를 serviceUrlScheme(corematenaver)이
// 아니라 앱 기본 scheme 으로 보낸다(coremate://thirdPartyLoginResult?version=2&code=2). 네이버 플러그인이
// AppDelegate 에 넣는 처리는 corematenaver 만 SDK 로 넘기므로, 이 URL 은 expo-router 로 가서 「Unmatched Route」
// 화면이 뜨고 SDK 는 결과를 받지 못해 네이버 버튼이 「로그인 중...」에 멈춘다.
// thirdPartyLoginResult 는 scheme 을 corematenaver 로 바꿔 SDK 로 넘긴다.
//
// 네이버 플러그인이 넣은 줄 앞에 끼워 넣으므로, app.config.js 에서 네이버 플러그인보다 앞에 둔다
// (같은 파일을 고치는 플러그인은 뒤에 등록된 것부터 실행된다).
const { withAppDelegate } = require("expo/config-plugins");

const NAVER_SCHEME = "corematenaver";
const ANCHOR = `if (url.scheme == "${NAVER_SCHEME}") {`;
const MARKER = "// withNaverLoginResultUrl";

module.exports = function withNaverLoginResultUrl(config) {
  return withAppDelegate(config, (config) => {
    const { contents, language } = config.modResults;
    if (language !== "swift" || contents.includes(MARKER)) {
      return config;
    }
    if (!contents.includes(ANCHOR)) {
      throw new Error(
        "withNaverLoginResultUrl: AppDelegate 에서 네이버 플러그인의 처리 줄을 찾지 못했다. 플러그인 순서를 확인할 것.",
      );
    }
    config.modResults.contents = contents.replace(
      ANCHOR,
      `${MARKER}
    if url.host == "thirdPartyLoginResult", url.scheme != "${NAVER_SCHEME}",
       var naverComponents = URLComponents(url: url, resolvingAgainstBaseURL: false) {
      naverComponents.scheme = "${NAVER_SCHEME}"
      if let naverURL = naverComponents.url {
        return NaverThirdPartyLoginConnection.getSharedInstance().application(app, open: naverURL, options: options)
      }
    }
    ${ANCHOR}`,
    );
    return config;
  });
};
