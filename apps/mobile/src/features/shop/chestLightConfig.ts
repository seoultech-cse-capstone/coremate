// 랜덤박스 오픈 빛 연출의 모든 튜닝 값을 한 곳에 모은다.
// 좌표/타이밍/색은 assets/chest/chest_light_preview.html 레퍼런스를 그대로 옮긴 것이고,
// 색만 등급(rarity)에 따라 팔레트를 갈아끼운다.

export type Rgb = [number, number, number];

// ── 캔버스 / 스프라이트 좌표 (1x 스프라이트 76×54 기준, S 를 곱해 캔버스 px 로) ──
export const S = 4; // 스프라이트 확대 배율
export const CANVAS_W = 360;
export const CANVAS_H = 440;
export const CHEST_Y = 180; // 스프라이트 윗변 y (위쪽에 빛기둥 공간 확보)
// 상자 콘텐츠 중심(1x x=24)이 캔버스 가로 중앙에 오도록 스프라이트 왼쪽 x 를 민다.
export const CHEST_X = CANVAS_W / 2 - 24 * S;
export const OPEN_X = CHEST_X + 24 * S; // 입구 중심 x (= 캔버스 중앙)
export const OPEN_Y = CHEST_Y + 30 * S; // 입구 중심 y

// 이음새 좌표 상수 (1x)
export const OX = 20;
export const OY = 20;
export const PW = 27;
export const SX = 3;
export const NSTEP = 7;

// ── 프레임 이름별 목표값 (intensity, openness) ──
export const TARGET: Record<string, { i: number; o: number }> = {
  closed: { i: 0.0, o: 0.0 },
  hop1_a: { i: 0.18, o: 0.05 }, hop1_b: { i: 0.3, o: 0.08 }, hop1_c: { i: 0.15, o: 0.05 },
  hop2_a: { i: 0.4, o: 0.08 }, hop2_b: { i: 0.55, o: 0.12 }, hop2_c: { i: 0.3, o: 0.06 },
  hop3_a: { i: 0.65, o: 0.12 }, hop3_b: { i: 0.85, o: 0.18 }, hop3_c: { i: 0.55, o: 0.1 },
  swing1: { i: 0.7, o: 0.2 }, swing2: { i: 0.8, o: 0.35 }, swing3: { i: 0.9, o: 0.5 },
  swing4: { i: 1.0, o: 0.7 }, swing5: { i: 1.0, o: 0.85 }, swing6: { i: 1.0, o: 0.95 },
  swing7: { i: 1.0, o: 1.0 },
  open: { i: 0.75, o: 1.0 },
};
export const RESIDUAL = 0.12; // 들썩 사이 closed 구간에도 남는 잔광

// hop 프레임의 상자 점프(dx,dy)와 뚜껑 들림(lift) — 1x px
export const HOP: Record<string, { dx: number; dy: number; lift: number }> = {
  hop1_a: { dx: 0, dy: -2, lift: 1 }, hop1_b: { dx: 1, dy: -3, lift: 2 }, hop1_c: { dx: 0, dy: -1, lift: 1 },
  hop2_a: { dx: -1, dy: -3, lift: 2 }, hop2_b: { dx: 1, dy: -4, lift: 3 }, hop2_c: { dx: 0, dy: -1, lift: 1 },
  hop3_a: { dx: -1, dy: -3, lift: 3 }, hop3_b: { dx: 1, dy: -5, lift: 4 }, hop3_c: { dx: 0, dy: -2, lift: 2 },
};

// 프레임 인덱스 → 이름
export const FRAME_NAMES = [
  "closed", "hop1_a", "hop1_b", "hop1_c", "hop2_a", "hop2_b", "hop2_c",
  "hop3_a", "hop3_b", "hop3_c", "swing1", "swing2", "swing3", "swing4",
  "swing5", "swing6", "swing7", "open",
];

// ── 튜닝 계수(디버그로 바꿀 수 있는 3개 + quality) ──
export type ChestQuality = "high" | "low";
export const DEFAULT_TUNING = {
  intensity: 1.0, // 빛 세기 계수 (kInt)
  coneAngleDeg: 32, // 빛기둥 반각 (kAng)
  maxParticles: 28, // 입자 최대 개수 (kPar)
};

// ── 등급 팔레트 ──
// core: 밝은(거의 흰) 코어, mid: 등급 대표색, edge: 어두운 가장자리
export type Palette = { core: Rgb; mid: Rgb; edge: Rgb };
const PALETTES: Record<string, Palette> = {
  COMMON: { core: [245, 248, 252], mid: [184, 192, 204], edge: [150, 160, 175] },
  COIN: { core: [245, 248, 252], mid: [184, 192, 204], edge: [150, 160, 175] },
  RARE: { core: [214, 231, 255], mid: [59, 130, 246], edge: [37, 99, 235] },
  EPIC: { core: [237, 216, 255], mid: [168, 85, 247], edge: [124, 58, 237] },
  LEGENDARY: { core: [255, 244, 200], mid: [245, 179, 1], edge: [217, 119, 6] },
};
const DEFAULT_PALETTE: Palette = { core: [237, 216, 255], mid: [196, 181, 253], edge: [139, 92, 246] };

export function getPalette(rarity: string | null | undefined): Palette {
  if (!rarity) return DEFAULT_PALETTE;
  return PALETTES[rarity.toUpperCase()] ?? DEFAULT_PALETTE;
}

export function rgba(c: Rgb, a: number): string {
  return `rgba(${c[0]},${c[1]},${c[2]},${a})`;
}
