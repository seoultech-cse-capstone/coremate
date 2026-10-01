import { httpClient } from "../../shared/api/httpClient";
import type {
  MyProfile,
  UpdateMyProfileRequest,
  UpdatePrivacyRequest,
  UserSubjectStat,
} from "./types";

export const userApi = {
  getMyProfile(accessToken: string): Promise<MyProfile> {
    return httpClient.get<MyProfile>("/api/users/me", { accessToken });
  },

  updateMyProfile(body: UpdateMyProfileRequest, accessToken: string): Promise<MyProfile> {
    return httpClient.put<MyProfile>("/api/users/me", body, { accessToken });
  },

  updatePrivacy(body: UpdatePrivacyRequest, accessToken: string): Promise<MyProfile> {
    return httpClient.patch<MyProfile>("/api/users/me/privacy", body, { accessToken });
  },

  getMySubjectStats(accessToken: string): Promise<UserSubjectStat[]> {
    return httpClient.get<UserSubjectStat[]>("/api/users/me/subject-stats", { accessToken });
  },
};
