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
