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

  /** 코인 없이 닉네임을 바꾼다. 온보딩의 첫 닉네임 설정에만 쓴다(이후 변경은 상점 변경권). */
  updateNickname(body: UpdateNicknameRequest, accessToken: string): Promise<UserSummary> {
    return httpClient.patch<UserSummary>("/api/users/me/nickname", body, { accessToken });
  },

  updatePrivacy(body: UpdatePrivacyRequest, accessToken: string): Promise<MyProfile> {
    return httpClient.patch<MyProfile>("/api/users/me/privacy", body, { accessToken });
  },

  getMySubjectStats(accessToken: string): Promise<UserSubjectStat[]> {
    return httpClient.get<UserSubjectStat[]>("/api/users/me/subject-stats", { accessToken });
  },
};
