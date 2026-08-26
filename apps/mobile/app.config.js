// Expo 가 app.json 을 읽어 config 로 넘겨준다. 여기서는 네이티브 앱 키가 필요한 카카오
// 플러그인만 동적으로 덧붙인다. 키는 git 에 커밋하지 않도록 .env 에서만 주입한다
// (EXPO_PUBLIC_KAKAO_NATIVE_APP_KEY). 정적 설정은 그대로 app.json 에 둔다.
module.exports = ({ config }) => {
  const kakaoAppKey = process.env.EXPO_PUBLIC_KAKAO_NATIVE_APP_KEY;

  return {
    ...config,
    plugins: [
      ...(config.plugins ?? []),
      ["@react-native-seoul/kakao-login", { kakaoAppKey }],
      // 구글 로그인 네이티브 SDK. Android 는 런타임 webClientId 로 동작하고, iOS 는 URL scheme 이
      // 필요하지만 현재 검증은 Android 라 별도 파라미터 없이 오토링크만 시킨다.
      "@react-native-google-signin/google-signin",
      // 카카오 SDK 는 별도 Maven 저장소에서 받는다(Android 빌드용).
      [
        "expo-build-properties",
        {
          android: {
            extraMavenRepos: [
              "https://devrepo.kakao.com/nexus/content/groups/public/",
            ],
          },
        },
      ],
    ],
  };
};
