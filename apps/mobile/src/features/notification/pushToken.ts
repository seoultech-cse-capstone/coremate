import * as Notifications from "expo-notifications";
import { Platform } from "react-native";

// Android 8 부터는 채널이 없으면 알림이 표시되지 않는다. 수신 전에 미리 만들어 둔다.
const ANDROID_CHANNEL_ID = "default";

async function ensureAndroidChannel() {
  if (Platform.OS !== "android") {
    return;
  }
  await Notifications.setNotificationChannelAsync(ANDROID_CHANNEL_ID, {
    name: "기본 알림",
    importance: Notifications.AndroidImportance.DEFAULT,
  });
}

// 앱이 켜져 있을 때 알림을 어떻게 다룰지. 배너와 소리를 그대로 띄운다.
// 이걸 지정하지 않으면 포그라운드에서는 알림이 앱에 전달만 되고 화면에는 보이지 않는다.
Notifications.setNotificationHandler({
  handleNotification: async () => ({
    shouldShowBanner: true,
    shouldShowList: true,
    shouldPlaySound: true,
    shouldSetBadge: false,
  }),
});

// FCM 기기 토큰을 발급받는다. 권한이 없으면 null.
//
// getExpoPushTokenAsync 가 아니라 getDevicePushTokenAsync 를 쓴다. 백엔드가 Expo 푸시
// 서비스를 경유하지 않고 FCM 에 직접 발송하므로(NFR-COMP-11), 네이티브 FCM 토큰이어야
// 한다. 두 토큰은 형식이 달라 섞어 쓰면 발송이 실패한다.
export async function getFcmToken(): Promise<string | null> {
  await ensureAndroidChannel();

  const current = await Notifications.getPermissionsAsync();
  const status = current.granted
    ? current.status
    : (await Notifications.requestPermissionsAsync()).status;

  if (status !== "granted") {
    return null;
  }

  const token = await Notifications.getDevicePushTokenAsync();
  return token.data;
}
