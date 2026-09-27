import { useRef } from "react";

import { createIdempotencyKey } from "../utils/idempotency";

/**
 * "같은 시도"에는 같은 Idempotency-Key 를, 다른 시도에는 새 키를 주는 훅.
 *
 * 매 호출마다 새 키를 만들면 서버 요구사항은 만족하지만 정작 막고 싶은 것을 못 막는다.
 * 두 번 누르면 키가 두 개라 둘 다 통과하기 때문이다. 반대로 키를 영원히 고정하면
 * 사용자가 같은 상품을 다시 사려 할 때 막힌다.
 *
 * 그래서 "무엇에 대한 시도인지"(signature)가 같은 동안만 키를 유지한다.
 * - 같은 대상으로 다시 누름(연타·실패 후 재시도) → 같은 키 → 서버가 두 번째를 막는다.
 *   첫 요청이 서버에서 실패해 롤백됐다면 키도 함께 지워지므로 재시도는 정상 통과한다.
 * - 다른 대상을 누름 → 새 키 → 별개 요청으로 처리된다.
 *
 * 성공한 뒤에는 {@link clear} 로 비워, 같은 대상을 한 번 더 사는 것을 막지 않는다.
 */
export function useIdempotencyKey() {
  const attemptRef = useRef<{ signature: string; key: string } | null>(null);

  return {
    /** signature 는 "이 시도가 무엇에 대한 것인지"를 나타내는 문자열(상품 ID, 대상 ID 등). */
    keyFor(signature: string): string {
      if (attemptRef.current?.signature !== signature) {
        attemptRef.current = { signature, key: createIdempotencyKey() };
      }
      return attemptRef.current.key;
    },
    /** 시도가 끝났다(성공). 다음 요청은 새 키를 받는다. */
    clear(): void {
      attemptRef.current = null;
    },
  };
}
