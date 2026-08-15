import { useQuery } from "@tanstack/react-query";

import { rewardApi } from "./rewardApi";

export function useDailyRewardLimits(accessToken: string | null) {
  return useQuery({
    queryKey: ["reward", "daily-limits"],
    queryFn: () => rewardApi.getDailyLimits(accessToken as string),
    enabled: accessToken !== null,
  });
}
