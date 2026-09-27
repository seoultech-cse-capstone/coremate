import { useQuery } from "@tanstack/react-query";

import { rewardApi } from "./rewardApi";

export function useDailyRewardLimits(accessToken: string | null) {
  return useQuery({
    queryKey: ["reward", "daily-limits"],
    queryFn: () => rewardApi.getDailyLimits(accessToken as string),
    enabled: accessToken !== null,
  });
}

export function useRewardSummary(accessToken: string | null) {
  return useQuery({
    queryKey: ["reward", "summary"],
    queryFn: () => rewardApi.getSummary(accessToken as string),
    enabled: accessToken !== null,
  });
}

export function useRewardLogs(accessToken: string | null) {
  return useQuery({
    queryKey: ["reward", "logs"],
    queryFn: () => rewardApi.getRewardLogs(accessToken as string),
    enabled: accessToken !== null,
  });
}

export function useXpLogs(accessToken: string | null) {
  return useQuery({
    queryKey: ["reward", "xp-logs"],
    queryFn: () => rewardApi.getXpLogs(accessToken as string),
    enabled: accessToken !== null,
  });
}

export function useCurrencyLogs(accessToken: string | null) {
  return useQuery({
    queryKey: ["reward", "currency-logs"],
    queryFn: () => rewardApi.getCurrencyLogs(accessToken as string),
    enabled: accessToken !== null,
  });
}
