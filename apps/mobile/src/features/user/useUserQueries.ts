import { useQuery } from "@tanstack/react-query";

import { userApi } from "./userApi";

export function useMyProfile(accessToken: string | null) {
  return useQuery({
    queryKey: ["user", "me"],
    queryFn: () => userApi.getMyProfile(accessToken as string),
    enabled: accessToken !== null,
  });
}
