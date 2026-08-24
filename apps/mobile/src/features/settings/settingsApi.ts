import { httpClient } from "../../shared/api/httpClient";

/**
 * 계정 설정 REST 호출.
 */
export const settingsApi = {
  changePassword(
    currentPassword: string,
    newPassword: string,
    accessToken: string,
  ): Promise<void> {
    return httpClient.patch<void>(
      "/api/users/me/password",
      { currentPassword, newPassword },
      { accessToken },
    );
  },
};
