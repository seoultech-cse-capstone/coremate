import { httpClient } from "../../shared/api/httpClient";
import type {
  CurrencyLog,
  DailyRewardLimit,
  RewardLog,
  RewardSummary,
  XpLog,
} from "./types";

/**
 * 보상·XP·코인 내역 REST 호출. reward/xp/currency 컨트롤러는 ApiResponse로 감싸지 않고
 * DTO를 그대로 반환한다(백엔드 RewardController/XpController/CurrencyController 확인).
 */
export const rewardApi = {
  getDailyLimits(accessToken: string, rewardDate?: string): Promise<DailyRewardLimit[]> {
    const query = rewardDate ? `?rewardDate=${rewardDate}` : "";
    return httpClient.get<DailyRewardLimit[]>(`/api/rewards/daily-limits${query}`, {
      accessToken,
    });
  },

  /** XP·레벨·코인 요약. /api/xp/summary 도 같은 DTO를 주므로 하나만 쓴다. */
  getSummary(accessToken: string): Promise<RewardSummary> {
    return httpClient.get<RewardSummary>("/api/rewards/summary", { accessToken });
  },

  /** 보상 지급 내역. 한도 초과 등으로 지급되지 않은 건도 skipReason 과 함께 내려온다. */
  getRewardLogs(accessToken: string): Promise<RewardLog[]> {
    return httpClient.get<RewardLog[]>("/api/rewards/logs", { accessToken });
  },

  getXpLogs(accessToken: string): Promise<XpLog[]> {
    return httpClient.get<XpLog[]>("/api/xp/logs", { accessToken });
  },

  getCurrencyLogs(accessToken: string): Promise<CurrencyLog[]> {
    return httpClient.get<CurrencyLog[]>("/api/currency/logs", { accessToken });
  },
};
