import * as SecureStore from "expo-secure-store";

// SecureStore 키는 영숫자, ".", "-", "_" 만 허용된다.
// 계정별로 따로 둬야 한 기기에서 다른 계정으로 가입했을 때도 그 계정에 온보딩이 뜬다.
const keyFor = (userId: number) => `coremate.onboardingDone.${userId}`;

/**
 * 온보딩 완료 플래그. 값이 없으면(false) 온보딩을 띄우고, 끝나면 true 로 바꾼다.
 * 로그인 정보가 아니라 이 기기 기준 기록이라 tokenStorage.clear() 대상이 아니다.
 */
export const onboardingStorage = {
  async isDone(userId: number): Promise<boolean> {
    try {
      return (await SecureStore.getItemAsync(keyFor(userId))) === "true";
    } catch (error) {
      // 읽기 실패로 온보딩을 매번 띄우는 것보다 건너뛰는 편이 낫다.
      console.error("[onboarding] 완료 플래그 읽기 실패", error);
      return true;
    }
  },

  async markDone(userId: number): Promise<void> {
    try {
      await SecureStore.setItemAsync(keyFor(userId), "true");
    } catch (error) {
      console.error("[onboarding] 완료 플래그 저장 실패", error);
    }
  },
};
