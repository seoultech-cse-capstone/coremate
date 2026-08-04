import * as Device from "expo-device";

export type DeviceInfo = {
  deviceName: string;
  platform: "IOS" | "ANDROID" | "UNKNOWN";
  osVersion: string | null;
};

/**
 * 기기 목록에 표시할 정보를 모은다. 인증에 쓰이는 값이 아니라 사용자가 "내 기기가
 * 맞는지" 알아보기 위한 표시용이다. 서버도 이 값을 신뢰하지 않는다.
 */
export function getDeviceInfo(): DeviceInfo {
  const osName = Device.osName?.toUpperCase();

  return {
    deviceName: Device.modelName ?? Device.deviceName ?? "알 수 없는 기기",
    platform: osName === "IOS" || osName === "IPADOS" ? "IOS" : osName === "ANDROID" ? "ANDROID" : "UNKNOWN",
    osVersion: Device.osVersion ?? null,
  };
}
