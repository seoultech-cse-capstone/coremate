import { httpClient } from "../../shared/api/httpClient";

/**
 * 계정 설정 REST 호출.
 * changePassword는 백엔드 PR #86(미머지)의 계약을 미리 맞춰둔 것이라 머지
 * 전까지는 실패한다. 머지 후 요청 필드명이 다르면 여기만 손보면 된다.
 */
export const settingsApi = {
  // TODO(#102 후속): 백엔드 PR #86 머지되면 실제 요청/응답 형태 확인 후 필요시 교체.
  changePassword(
    currentPassword: string,
    newPassword: string,
    accessToken: string,
  ): Promise<void> {
    return httpClient.patch<void>(
      "/api/user/me/password",
      { currentPassword, newPassword },
      { accessToken },
    );
  },
};
