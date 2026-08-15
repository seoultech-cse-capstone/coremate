/**
 * 레벨 진행률 계산. 백엔드 apps/backend/.../reward/service/LevelPolicy.java의
 * 레벨 곡선(2026-07-30 확정)을 그대로 복제한 것이다. 백엔드 공식이 바뀌면
 * (BASE_XP/STEP_XP/MAX_LEVEL 등) 이 파일도 같이 고쳐야 한다.
 *
 * 레벨 n→n+1 필요 XP = BASE_XP + STEP_XP * (n - 1).
 * 레벨 L 달성에 필요한 누적 XP = 25L² + 25L - 50.
 */
const BASE_XP = 100;
const STEP_XP = 50;
const MAX_LEVEL = 50;

function cumulativeXpForLevel(level: number): number {
  return 25 * level * level + 25 * level - 50;
}

function requiredXpForLevel(level: number): number {
  return BASE_XP + STEP_XP * (level - 1);
}

export type LevelProgress = {
  /** 현재 레벨 구간에서 쌓은 XP */
  currentLevelXp: number;
  /** 다음 레벨까지 필요한 XP(이 레벨 구간의 총량) */
  requiredLevelXp: number;
  /** 현재 레벨 구간 진행률(0~100) */
  percent: number;
  isMaxLevel: boolean;
};

export function getLevelProgress(level: number, totalXp: number): LevelProgress {
  if (level >= MAX_LEVEL) {
    return { currentLevelXp: 0, requiredLevelXp: 0, percent: 100, isMaxLevel: true };
  }

  const xpAtLevelStart = cumulativeXpForLevel(level);
  const requiredLevelXp = requiredXpForLevel(level);
  const currentLevelXp = Math.max(0, totalXp - xpAtLevelStart);
  const percent = Math.min(100, Math.round((currentLevelXp / requiredLevelXp) * 1000) / 10);

  return { currentLevelXp, requiredLevelXp, percent, isMaxLevel: false };
}
