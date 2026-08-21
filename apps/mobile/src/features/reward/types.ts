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

/** GET /api/rewards/summary · GET /api/xp/summary 공통 응답. */
export type RewardSummary = {
  userId: number;
  totalXp: number;
  level: number;
  coin: number;
};

/** GET /api/rewards/logs 항목. */
export type RewardLog = {
  rewardLogId: number;
  userId: number;
  rewardType: string;
  rewardSource: string;
  sourceId: number | null;
  targetType: string | null;
  targetId: number | null;
  solveSessionId: number | null;
  xpAmount: number | null;
  coinAmount: number | null;
  itemId: number | null;
  randomBoxId: number | null;
  rewardStatus: string;
  /** 지급되지 않은 경우의 사유. DAILY_LIMIT_EXCEEDED 등 */
  skipReason: string | null;
  createdAt: string;
};

/** GET /api/xp/logs 항목. */
export type XpLog = {
  xpLogId: number;
  userId: number;
  amount: number;
  totalXpAfter: number;
  reason: string;
  referenceType: string | null;
  referenceId: number | null;
  createdAt: string;
};

/** GET /api/currency/logs 항목. */
export type CurrencyLog = {
  currencyLogId: number;
  userId: number;
  /** 사용은 음수로 내려온다. */
  amount: number;
  balanceAfter: number;
  transactionType: string;
  reason: string;
  referenceType: string | null;
  referenceId: number | null;
  createdAt: string;
};
