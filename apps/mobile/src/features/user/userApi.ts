import { httpClient } from "../../shared/api/httpClient";
import type {
  MyProfile,
  UpdateMyProfileRequest,
  UpdateNicknameResult,
  UserSubjectStat,
} from "./types";

export const userApi = {
  getMyProfile(accessToken: string): Promise<MyProfile> {
    return httpClient.get<MyProfile>("/api/users/me", { accessToken });
  },

  updateMyProfile(body: UpdateMyProfileRequest, accessToken: string): Promise<MyProfile> {
    return httpClient.put<MyProfile>("/api/users/me", body, { accessToken });
  },

  updateNickname(nickname: string, accessToken: string): Promise<UpdateNicknameResult> {
    return httpClient.patch<UpdateNicknameResult>(
      "/api/users/me/nickname",
      { nickname },
      { accessToken },
    );
  },

  getMySubjectStats(accessToken: string): Promise<UserSubjectStat[]> {
    return httpClient.get<UserSubjectStat[]>("/api/users/me/subject-stats", { accessToken });
  },
};
