import * as SecureStore from "expo-secure-store";

// SecureStore 키는 영숫자, ".", "-", "_" 만 허용된다.
const DEVICE_ID_KEY = "coremate.deviceId";

/**
 * 기기 식별자 보관소.
 *
 * ⚠️ tokenStorage와 물리적으로 분리된 모듈이다. 이 값을 tokenStorage.clear() 대상에
 * 절대 넣지 말 것. 넣으면 로그아웃할 때마다 새 기기로 잡혀서 서버의 기기 목록이
 * 무한히 불어난다. 기기 식별자는 로그인 여부와 무관하게 "이 설치본"을 가리키는 값이다.
 */
export const deviceStorage = {
  async get(): Promise<string | null> {
    try {
      return await SecureStore.getItemAsync(DEVICE_ID_KEY);
    } catch {
      // 키스토어 손상 등. 서버가 새 식별자를 발급해 주므로 없는 것으로 취급한다.
      return null;
    }
  },

  async save(deviceId: string): Promise<void> {
    try {
      await SecureStore.setItemAsync(DEVICE_ID_KEY, deviceId, {
        // 기본값(WHEN_UNLOCKED)은 iCloud/암호화 백업에 포함된다. 그대로 두면 백업을
        // 새 폰에 복원했을 때 구기기와 같은 식별자를 갖게 되어 서로 세션을 밀어낸다.
        keychainAccessible: SecureStore.WHEN_UNLOCKED_THIS_DEVICE_ONLY,
      });
    } catch {
      // 저장에 실패해도 이번 세션은 그대로 진행된다. 다음 로그인에 서버가 새 식별자를
      // 발급하므로 기기 목록에 행이 하나 더 생기는 정도의 영향만 있다.
    }
  },
};
