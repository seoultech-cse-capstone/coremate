import type { QueryClient, QueryKey } from "@tanstack/react-query";

// 아바타·방 쓰기(장착·가구 배치·포즈)는 한 줄로 세워 차례로 보낸다. 동시에 보내면 서버가 처리하는
// 순서와 응답이 오는 순서가 엇갈려, 나중에 고른 옷·배치가 먼저 고른 것으로 되돌아갈 수 있다.
export const AVATAR_WRITE = {
  mutationKey: ["avatar", "write"],
  scope: { id: "avatar-write" },
} as const;

// 뒤에 줄 선 쓰기가 남아 있으면 이 응답은 이미 낡은 상태라 캐시에 쓰지 않는다.
// onSuccess 시점에는 자기 자신도 아직 진행 중으로 센다.
export function isLastAvatarWrite(queryClient: QueryClient): boolean {
  return queryClient.isMutating({ mutationKey: AVATAR_WRITE.mutationKey }) <= 1;
}

// 낙관적으로 바꾸기 직전에 부른다. 진행 중인 조회가 끝나면서 바꾼 값을 예전 값으로 덮어쓰지 않게 끊는다.
// 아직 한 번도 받지 못한 쿼리는 바꿀 값도 없으니 첫 조회를 끊지 않는다(끊으면 데이터 없이 멈춘다).
export async function cancelBeforeOptimisticUpdate(queryClient: QueryClient, queryKey: QueryKey): Promise<void> {
  if (queryClient.getQueryData(queryKey) !== undefined) {
    await queryClient.cancelQueries({ queryKey });
  }
}
