/**
 * 친구 아바타의 배경·강조 색.
 *
 * 서버는 아바타의 성별과 기본 타입, 장착 아이템만 내려준다. 색은 서버가 정할 값이 아니라
 * 목록에서 사람을 빠르게 구분하기 위한 화면 표현이라 클라이언트에서 만든다.
 * 닉네임으로 결정하므로 같은 친구는 어느 화면에서나 같은 색으로 보인다.
 */
const PALETTE = [
  { background: "#E9E3FF", accent: "#6D5DFB" },
  { background: "#DCFCE7", accent: "#22C55E" },
  { background: "#FEF3C7", accent: "#F59E0B" },
  { background: "#DBEAFE", accent: "#3B82F6" },
  { background: "#FCE7F3", accent: "#EC4899" },
  { background: "#E0F2FE", accent: "#0EA5E9" },
];

export type AvatarColor = { background: string; accent: string };

export function getAvatarColor(nickname: string): AvatarColor {
  let hash = 0;
  for (let i = 0; i < nickname.length; i += 1) {
    hash = (hash * 31 + nickname.charCodeAt(i)) % 1000003;
  }
  return PALETTE[hash % PALETTE.length];
}
