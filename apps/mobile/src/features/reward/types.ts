/** GET /api/rewards/daily-limits 항목. DailyRewardLimitResponse와 1:1 대응 */
export type DailyRewardLimit = {
  dailyRewardLimitId: number;
  userId: number;
  rewardDate: string;
  rewardType: string;
  rewardCount: number;
  dailyLimit: number | null;
  updatedAt: string;
};
