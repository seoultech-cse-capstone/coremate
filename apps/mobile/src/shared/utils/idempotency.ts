/**
 * 재화 차감·보상 지급처럼 두 번 실행되면 안 되는 요청에 붙이는 Idempotency-Key.
 *
 * 서버(IdempotencyGuard)는 (userId, key) 조합이 이미 있으면 DUPLICATE_REQUEST 로 막고,
 * 헤더가 비어 있으면 IDEMPOTENCY_KEY_REQUIRED 로 아예 거절한다. 즉 이 키가 없으면
 * 해당 API 는 호출 자체가 실패한다.
 *
 * 키는 "한 번의 사용자 확정"마다 하나여야 한다. 화면이 재시도(401 토큰 재발급 등)로
 * 같은 요청을 다시 보낼 때는 같은 키를 써야 중복 실행이 막히고, 사용자가 다시 확정한
 * 경우에만 새 키를 만든다.
 */
export function createIdempotencyKey(): string {
  const random = () => Math.random().toString(36).slice(2, 10);
  return `${Date.now().toString(36)}-${random()}-${random()}`;
}
