// Expo 가 app.json 을 읽어 config 로 넘겨준다. 여기서는 키가 필요한 소셜 로그인 플러그인만 동적으로
// 덧붙인다. 키는 git 에 커밋하지 않도록 .env 에서만 주입한다(EXPO_PUBLIC_KAKAO_NATIVE_APP_KEY,
// EXPO_PUBLIC_GOOGLE_IOS_CLIENT_ID). 정적 설정은 그대로 app.json 에 둔다.
module.exports = ({ config }) => {
  const kakaoAppKey = process.env.EXPO_PUBLIC_KAKAO_NATIVE_APP_KEY;
  const googleIosClientId = process.env.EXPO_PUBLIC_GOOGLE_IOS_CLIENT_ID;

  return {
    ...config,
    plugins: [
      ...(config.plugins ?? []),
      ["@react-native-seoul/kakao-login", { kakaoAppKey }],
      // 구글 로그인 네이티브 SDK. Android 는 런타임 webClientId 로 동작한다. iOS 는 iOS 클라이언트 ID 를
      // 뒤집은 URL scheme 이 있어야 로그인 창에서 앱으로 돌아오므로, ID 가 있을 때만 넣는다(없으면 오토링크만).
      // google-services.json 적용은 이 플러그인이 아니라 Expo 기본 설정(android.googleServicesFile)이 한다.
      googleIosClientId
        ? [
            "@react-native-google-signin/google-signin",
            {
              iosUrlScheme: `com.googleusercontent.apps.${googleIosClientId.replace(".apps.googleusercontent.com", "")}`,
            },
          ]
        : "@react-native-google-signin/google-signin",
      // 네이버 로그인 iOS 설정(URL scheme·앱 실행 허용 목록·AppDelegate). naverAuth.ts 의
      // serviceUrlSchemeIOS·네이버 개발자센터 iOS 설정과 같은 값이어야 한다.
      ["@react-native-seoul/naver-login", { urlScheme: "corematenaver" }],
      // 카카오 SDK 는 별도 Maven 저장소에서 받는다(Android 빌드용).
      // iOS 27 SDK(Xcode 27)로 빌드한 앱은 UIScene 생명주기를 쓰지 않으면 iOS 27 에서 실행하자마자 종료된다.
      // enableSceneSupport 가 AppDelegate 의 창 생성을 Expo 의 씬 델리게이트로 옮긴다(expo 57.0.23 이상 필요,
      // SDK 58 부터는 템플릿 기본값이라 이 줄은 지워도 된다). URL 열기는 씬 델리게이트가 AppDelegate 로 다시
      // 넘겨 주므로 카카오·네이버 로그인 처리(AppDelegate 의 open url)는 그대로 동작한다.
      [
        "expo-build-properties",
        {
          android: {
            extraMavenRepos: [
              "https://devrepo.kakao.com/nexus/content/groups/public/",
            ],
          },
          ios: {
            enableSceneSupport: true,
          },
        },
      ],
    ],
  };
};
