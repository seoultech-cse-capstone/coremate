import { httpClient } from "../../shared/api/httpClient";
import type { MyProfile } from "./types";

export const userApi = {
  getMyProfile(accessToken: string): Promise<MyProfile> {
    return httpClient.get<MyProfile>("/api/users/me", { accessToken });
  },
};
