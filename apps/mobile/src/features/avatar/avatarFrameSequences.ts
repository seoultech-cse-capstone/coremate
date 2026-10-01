// 방 아바타의 프레임 애니메이션 순서. 내 방(RoomScene)과 친구 방(RoomSnapshot)이 같이 써서
// 같은 포즈가 두 화면에서 똑같이 움직이게 한다.

// AVATAR_FE.md 2절: 서기(앞) idle은 1→2→3→2 루프, 프레임별 duration이 다르다.
export const IDLE_SEQUENCE: { value: 1 | 2 | 3; duration: number }[] = [
  { value: 1, duration: 400 },
  { value: 2, duration: 150 },
  { value: 3, duration: 400 },
  { value: 2, duration: 150 },
];
// 책상 앉기: 타이핑 손 오버레이 1→2→1→3 루프, 120ms 균일.
export const SIT_HAND_SEQUENCE: { value: 1 | 2 | 3; duration: number }[] = [
  { value: 1, duration: 120 },
  { value: 2, duration: 120 },
  { value: 1, duration: 120 },
  { value: 3, duration: 120 },
];
// 수면: 1→2→3→2 루프(원래 스펙은 4프레임이지만 에셋이 3장이라 idle과 같은 방식으로 재사용).
export const SLEEP_SEQUENCE: { value: 1 | 2 | 3; duration: number }[] = [
  { value: 1, duration: 400 },
  { value: 2, duration: 250 },
  { value: 3, duration: 550 },
  { value: 2, duration: 250 },
];
