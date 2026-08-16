import { httpClient } from "../../shared/api/httpClient";
import type { DailyRewardLimit } from "./types";

/**
 * 보상 일일 한도 REST 호출. 이 컨트롤러는 ApiResponse로 감싸지 않고 DTO를
 * 그대로 반환한다(백엔드 RewardController 확인).
 */
export const rewardApi = {
  getDailyLimits(accessToken: string, rewardDate?: string): Promise<DailyRewardLimit[]> {
    const query = rewardDate ? `?rewardDate=${rewardDate}` : "";
    return httpClient.get<DailyRewardLimit[]>(`/api/rewards/daily-limits${query}`, {
      accessToken,
    });
  },
};
