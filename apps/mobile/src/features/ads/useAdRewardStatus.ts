import { useQuery } from "@tanstack/react-query";

import { rewardApi, type DailyRewardLimit } from "../reward";

const AD_REWARD_TYPE = "AD_REWARD";

// 시청이 끝난 뒤 코인이 들어왔는지 서버에 다시 묻는 간격.
const CONFIRM_POLL_MS = 1500;

// 서버의 일일 한도는 서버 날짜(한국 시간) 기준이다. 사용자가 한국에 있다고 보고 기기 날짜를 쓴다.
function localDateString(date: Date): string {
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${date.getFullYear()}-${month}-${day}`;
}

function adRewardCountOf(limits: DailyRewardLimit[] | undefined): number {
  return limits?.find((item) => item.rewardType === AD_REWARD_TYPE)?.rewardCount ?? 0;
}

/**
 * 광고 보상의 오늘 상태. 지급량과 하루 한도는 보상 정책(AD_REWARD)에서, 오늘 받은 횟수는 일일 한도
 * 기록에서 읽는다.
 *
 * awaitingCountAbove 를 주면 오늘 받은 횟수가 그 값을 넘을 때까지(= 코인이 들어올 때까지) 기록을
 * 주기적으로 다시 불러온다.
 */
export function useAdRewardStatus(accessToken: string | null, awaitingCountAbove: number | null) {
  const today = localDateString(new Date());

  const policiesQuery = useQuery({
    queryKey: ["reward", "policies"],
    queryFn: () => rewardApi.getPolicies(accessToken as string),
    enabled: accessToken !== null,
  });
  const limitsQuery = useQuery({
    queryKey: ["reward", "daily-limits", today],
    queryFn: () => rewardApi.getDailyLimits(accessToken as string, today),
    enabled: accessToken !== null,
    refetchInterval: (query) =>
      awaitingCountAbove !== null && adRewardCountOf(query.state.data) <= awaitingCountAbove
        ? CONFIRM_POLL_MS
        : false,
  });

  const policy = policiesQuery.data?.find((item) => item.rewardType === AD_REWARD_TYPE);
  const usedCount = adRewardCountOf(limitsQuery.data);
  const dailyLimit = policy?.dailyLimit ?? 0;

  return {
    isReady: policy !== undefined && limitsQuery.isSuccess,
    coinAmount: policy?.coinAmount ?? 0,
    dailyLimit,
    usedCount,
    remainingCount: Math.max(dailyLimit - usedCount, 0),
  };
}
