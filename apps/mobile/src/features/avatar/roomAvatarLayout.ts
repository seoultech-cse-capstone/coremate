import {
  AVATAR_FOOT_X_RATIO,
  AVATAR_FOOT_Y_RATIO,
  AVATAR_SLEEP_STAGE_HEIGHT,
  AVATAR_SLEEP_STAGE_WIDTH,
  AVATAR_STAGE_HEIGHT,
  AVATAR_STAGE_WIDTH,
  AVATAR_WIDTH_IN_CELLS,
  getAvatarDotPx,
} from "./components/AvatarStage";
import type { FurnitureLayout } from "./roomFurniture";

// 방 안에서 아바타·펫을 어디에, 얼마나 크게 그릴지. 내 방(RoomScene)과 친구 방(RoomSnapshot)이
// 같이 써서 같은 포즈가 두 화면에서 똑같이 보이게 한다.

// 수면 캔버스(384x216, 48x27도트)는 원본이 1도트=8px라, 세트 배율(도트당 실제 px)을
// 그 8로 나눠야 AvatarSleepStage의 scale prop과 단위가 맞는다.
const SLEEP_RAW_PX_PER_DOT = 8;
// AVATAR_FE.md: 침대 원점 기준 수면 아바타 (4, -12)도트.
const SLEEP_AVATAR_OFFSET_DOTS = { x: 4, y: -12 };
// 처음 서 있는 자리: 바닥 가로 가운데, 세로 65% 지점(발 접지 기준).
const STAND_START_RATIO = { x: 0.5, y: 0.65 };
// 펫: 프레임 애니메이션 없는 정지 이미지라, 강아지 원본(18x13도트)을 기준 상자로 두고
// contain으로 맞춘다(거북이는 더 납작해서 상자 안에서 살짝 작게 보인다 — 의도된 동작).
const PET_BOX_WIDTH_DOTS = 18;
const PET_BOX_HEIGHT_DOTS = 13;
// 펫은 아바타 발밑 기준 오른쪽으로 이 정도(칸 단위) 떨어진 자리에 선다.
const PET_OFFSET_CELLS = 0.55;

/**
 * 칸 너비에 맞춘 아바타·그림자·펫 크기. 아바타 너비는 타일 1칸으로 고정한다.
 * 펫·가구는 아바타와 같은 "도트당 화면 px"(dotPx)를 써서 나란히 있어도 도트 크기가 어긋나지 않는다.
 */
export function getRoomAvatarMetrics(cellWidth: number) {
  const scale = (AVATAR_WIDTH_IN_CELLS * cellWidth) / AVATAR_STAGE_WIDTH;
  const width = AVATAR_STAGE_WIDTH * scale;
  const height = AVATAR_STAGE_HEIGHT * scale;
  const dotPx = getAvatarDotPx(cellWidth);
  return {
    scale,
    width,
    height,
    footOffsetX: width * AVATAR_FOOT_X_RATIO,
    footOffsetY: height * AVATAR_FOOT_Y_RATIO,
    shadowWidth: width * 0.6,
    shadowHeight: width * 0.6 * 0.35,
    dotPx,
    petWidth: PET_BOX_WIDTH_DOTS * dotPx,
    petHeight: PET_BOX_HEIGHT_DOTS * dotPx,
  };
}

/** 침대에 누운 아바타(AvatarSleepStage)의 자리와 배율. */
export function getSleepAvatarRect(bedLayout: FurnitureLayout) {
  const scale = bedLayout.scale / SLEEP_RAW_PX_PER_DOT;
  return {
    left: bedLayout.containerLeft + SLEEP_AVATAR_OFFSET_DOTS.x * bedLayout.scale,
    top: bedLayout.containerTop + SLEEP_AVATAR_OFFSET_DOTS.y * bedLayout.scale,
    width: AVATAR_SLEEP_STAGE_WIDTH * scale,
    height: AVATAR_SLEEP_STAGE_HEIGHT * scale,
    scale,
  };
}

/** 처음 서 있는 자리(발 접지점). */
export function getStandStart(floorWidth: number, floorHeight: number) {
  return { x: floorWidth * STAND_START_RATIO.x, y: floorHeight * STAND_START_RATIO.y };
}

/** 아바타 발 x 기준 펫이 설 x. 바닥 밖으로 나가지 않게 자른다. */
export function getPetX(footX: number, cellWidth: number, floorWidth: number) {
  return Math.min(Math.max(footX + PET_OFFSET_CELLS * cellWidth, 0), floorWidth);
}
