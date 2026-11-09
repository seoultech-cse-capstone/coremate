import { httpClient } from "../../shared/api/httpClient";
import type {
  MyProfile,
  UpdateMyProfileRequest,
  UpdateNicknameRequest,
  UpdatePrivacyRequest,
  UserSummary,
  UserSubjectStat,
} from "./types";

export const userApi = {
  getMyProfile(accessToken: string): Promise<MyProfile> {
    return httpClient.get<MyProfile>("/api/users/me", { accessToken });
  },

  updateMyProfile(body: UpdateMyProfileRequest, accessToken: string): Promise<MyProfile> {
    return httpClient.put<MyProfile>("/api/users/me", body, { accessToken });
  },

  /** 첫 닉네임 무료 설정(계정당 1회, #415). 온보딩에서만 쓴다. 이후 변경은 상점 변경권으로 한다. */
  setInitialNickname(body: UpdateNicknameRequest, accessToken: string): Promise<UserSummary> {
    return httpClient.post<UserSummary>("/api/users/me/initial-nickname", body, { accessToken });
  },

  updatePrivacy(body: UpdatePrivacyRequest, accessToken: string): Promise<MyProfile> {
    return httpClient.patch<MyProfile>("/api/users/me/privacy", body, { accessToken });
  },

  getMySubjectStats(accessToken: string): Promise<UserSubjectStat[]> {
    return httpClient.get<UserSubjectStat[]>("/api/users/me/subject-stats", { accessToken });
  },
};
