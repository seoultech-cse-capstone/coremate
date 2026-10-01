import { Image, type ImageSourcePropType } from "react-native";

import { stemFromImageUrl } from "./itemStem";
import type { RoomPlacement, RoomResponse } from "./roomTypes";

// assets_template.md 6절 기준. 좌표는 전부 "아바타 원점" 같은 공통 기준점 하나를
// 공유한다고 보고 그대로 옮겨왔다 — 세트 내 상대 배치(바운딩박스)만 쓰므로
// 기준점 자체가 어디든 결과는 같다.
export type FurnitureType =
  | "bed"
  | "desk"
  | "dryingRack"
  | "shelf"
  | "fridge"
  | "pcTower"
  | "petCushion"
  | "washer";

export type FurniturePosition = { row: number; col: number };

type FurniturePieceSpec = {
  source: ImageSourcePropType;
  offsetXDots: number;
  offsetYDots: number;
  /**
   * 앉기/수면 상호작용 중 아바타보다 앞(front)에 그려야 하는 조각인지.
   * 책상 상판은 아바타를 가려야 하고(책상 씬: chair → 아바타 → desk),
   * 이불도 자는 아바타를 덮어야 한다(수면 씬: ... → 수면 아바타 → ... → 이불).
   * 나머지(의자, 침대 프레임)는 기본값 "back"으로 항상 아바타보다 뒤에 있다.
   */
  role?: "back" | "front";
};

type FurniturePiece = FurniturePieceSpec & {
  widthDots: number;
  heightDots: number;
};

export type FurnitureSet = {
  type: FurnitureType;
  label: string;
  /** 격자에서 차지하는 가로 칸 수. */
  footprintCols: number;
  pieces: FurniturePiece[];
  /** 아래 네 값은 조각들을 합친 바운딩박스(도트) — buildSet이 자동 계산. */
  widthDots: number;
  heightDots: number;
  minXDots: number;
  minYDots: number;
};

// 도트=8px 고정 규칙(assets_template.md 1절)이라, 파일의 실제 픽셀 크기만 알면
// 도트 수는 8로 나누면 된다. Image.resolveAssetSource는 로컬(require) 이미지는
// Metro가 번들할 때 크기를 이미 심어놔서 동기적으로 바로 읽힌다 — 그림이
// 바뀌거나 크기가 달라져도 widthDots/heightDots를 손으로 다시 잴 필요가 없다.
function resolvePieceDots(source: ImageSourcePropType): { widthDots: number; heightDots: number } {
  const { width, height } = Image.resolveAssetSource(source);
  return { widthDots: width / 8, heightDots: height / 8 };
}

function buildSet(type: FurnitureType, label: string, footprintCols: number, pieceSpecs: FurniturePieceSpec[]): FurnitureSet {
  const pieces: FurniturePiece[] = pieceSpecs.map((spec) => ({ ...spec, ...resolvePieceDots(spec.source) }));
  const minXDots = Math.min(...pieces.map((piece) => piece.offsetXDots));
  const maxXDots = Math.max(...pieces.map((piece) => piece.offsetXDots + piece.widthDots));
  const minYDots = Math.min(...pieces.map((piece) => piece.offsetYDots));
  const maxYDots = Math.max(...pieces.map((piece) => piece.offsetYDots + piece.heightDots));

  return {
    type,
    label,
    footprintCols,
    pieces,
    widthDots: maxXDots - minXDots,
    heightDots: maxYDots - minYDots,
    minXDots,
    minYDots,
  };
}

// 책상 씬 레이어 순서(아래→위)는 chair → 아바타 → desk라, 여기서도 chair를 먼저 둔다.
const DESK_SET = buildSet("desk", "책상", 2, [
  {
    source: require("../../../assets/room/furniture/furn_chair_basic.png"),
    offsetXDots: 4,
    offsetYDots: 24,
  },
  {
    source: require("../../../assets/room/furniture/furn_desk_basic.png"),
    offsetXDots: -5,
    offsetYDots: 33,
    role: "front",
  },
]);

const BED_SET = buildSet("bed", "침대", 2, [
  {
    source: require("../../../assets/room/furniture/furn_bed_basic.png"),
    offsetXDots: 0,
    offsetYDots: 0,
  },
  {
    source: require("../../../assets/room/furniture/furn_bed_blanket_basic.png"),
    offsetXDots: 0,
    offsetYDots: 0,
    role: "front",
  },
]);

// 장식용 가구(상호작용 없음) — 전부 조각 1개, 가로 1칸.
const DRYING_RACK_SET = buildSet("dryingRack", "빨래 건조대", 1, [
  {
    source: require("../../../assets/room/furniture/furn_drying_rack.png"),
    offsetXDots: 0,
    offsetYDots: 0,
  },
]);

const SHELF_SET = buildSet("shelf", "선반", 1, [
  {
    source: require("../../../assets/room/furniture/furn_shelf.png"),
    offsetXDots: 0,
    offsetYDots: 0,
  },
]);

const FRIDGE_SET = buildSet("fridge", "냉장고", 1, [
  {
    source: require("../../../assets/room/furniture/furn_fridge.png"),
    offsetXDots: 0,
    offsetYDots: 0,
  },
]);

const PC_TOWER_SET = buildSet("pcTower", "PC 본체", 1, [
  {
    source: require("../../../assets/room/furniture/furn_pc_tower.png"),
    offsetXDots: 0,
    offsetYDots: 0,
  },
]);

const PET_CUSHION_SET = buildSet("petCushion", "반려동물 방석", 1, [
  {
    source: require("../../../assets/room/furniture/furn_pet_cushion.png"),
    offsetXDots: 0,
    offsetYDots: 0,
  },
]);

const WASHER_SET = buildSet("washer", "세탁기", 1, [
  {
    source: require("../../../assets/room/furniture/furn_washer.png"),
    offsetXDots: 0,
    offsetYDots: 0,
  },
]);

export const FURNITURE_SETS: Record<FurnitureType, FurnitureSet> = {
  bed: BED_SET,
  desk: DESK_SET,
  dryingRack: DRYING_RACK_SET,
  shelf: SHELF_SET,
  fridge: FRIDGE_SET,
  pcTower: PC_TOWER_SET,
  petCushion: PET_CUSHION_SET,
  washer: WASHER_SET,
};

export const FURNITURE_TYPES: FurnitureType[] = [
  "bed",
  "desk",
  "dryingRack",
  "shelf",
  "fridge",
  "pcTower",
  "petCushion",
  "washer",
];

// 각 가구가 대응하는 서버 아바타 아이템 image_url stem(파일명). 인벤토리 보유
// 목록(서버)과 로컬 가구 카탈로그를 잇는 유일한 키다 — require() 안의 파일명은
// 런타임에 못 읽어서 여기 명시한다. 책상은 세트로 의자를 함께 그리지만, 의자는
// 별도 아이템이 아니라 책상 세트에 포함된다(#254) — 보유 판정은 furn_desk_basic만 본다.
export const FURNITURE_ITEM_STEM: Record<FurnitureType, string> = {
  bed: "furn_bed_basic",
  desk: "furn_desk_basic",
  dryingRack: "furn_drying_rack",
  shelf: "furn_shelf",
  fridge: "furn_fridge",
  pcTower: "furn_pc_tower",
  petCushion: "furn_pet_cushion",
  washer: "furn_washer",
};

// 서버 image_url 파일명 stem ↔ 로컬 FurnitureType. 서버 배치/보유목록과 로컬 가구를 잇는 키.
const STEM_TO_FURNITURE_TYPE = new Map<string, FurnitureType>(
  FURNITURE_TYPES.map((type) => [FURNITURE_ITEM_STEM[type], type]),
);

/** 서버 가구 아이템 image_url → 로컬 가구 종류. 앱에 없는 가구면 undefined. */
export function furnitureTypeFromImageUrl(imageUrl: string | null): FurnitureType | undefined {
  const stem = stemFromImageUrl(imageUrl);
  return stem ? STEM_TO_FURNITURE_TYPE.get(stem) : undefined;
}

/** 서버 방 배치 목록 → 가구 종류별 격자 위치(gridX=col, gridY=row). 앱에 없는 가구는 빠진다. */
export function placementsFromRoom(
  placements: RoomPlacement[],
): Partial<Record<FurnitureType, FurniturePosition>> {
  const map: Partial<Record<FurnitureType, FurniturePosition>> = {};
  for (const placement of placements) {
    const type = furnitureTypeFromImageUrl(placement.imageUrl);
    if (type) {
      map[type] = { row: placement.gridY, col: placement.gridX };
    }
  }
  return map;
}

/**
 * 마지막에 앉거나 누운 가구(책상·침대). 서 있었거나, 그 가구가 방에서 치워졌거나
 * (배치가 지워지면 서버가 room_pose_placement_id 를 비운다), 포즈와 가구가 맞지 않으면
 * null 이다 — 이때는 서 있는 모습으로 그린다.
 */
export function getPosedFurniture(room: RoomResponse): "desk" | "bed" | null {
  const expected = room.roomPose === "SIT_DESK" ? "desk" : room.roomPose === "SLEEP_BED" ? "bed" : null;
  if (!expected) {
    return null;
  }
  const placement = room.placements.find((item) => item.placementId === room.roomPosePlacementId);
  return placement && furnitureTypeFromImageUrl(placement.imageUrl) === expected ? expected : null;
}

/** 아바타가 걸어가서 앉거나 누울 수 있는(탭하면 상호작용하는) 가구. 나머지는 장식용이다. */
export const INTERACTIVE_FURNITURE_TYPES: FurnitureType[] = ["bed", "desk"];

export type FurnitureLayout = {
  /** 세트 전체(바운딩박스) 기준 도트→px 배율. 도트 좌표에 이 값을 곱하면 화면 px. */
  scale: number;
  /** 세트 바운딩박스의 화면상 좌상단. */
  containerLeft: number;
  containerTop: number;
  pieces: {
    source: ImageSourcePropType;
    role: "back" | "front";
    left: number;
    top: number;
    width: number;
    height: number;
  }[];
};

/**
 * 배치 좌표 + 격자 치수로 가구 세트의 화면 위치를 계산한다. RoomBackdrop과
 * 상호작용 로직이 공유한다. avatarDotPx(아바타 기준 도트당 화면 px, AvatarStage의
 * getAvatarDotPx)를 그대로 써서, 가구도 아바타·펫과 같은 굵기의 픽셀아트로
 * 보이고 원본 도트 수 비율대로 자연히 크기가 비교된다 — footprintCols에 맞춰
 * 억지로 늘리거나 줄이지 않는다(칸보다 작거나 크게 보일 수 있음, 의도된 동작).
 */
export function getFurnitureLayout(
  type: FurnitureType,
  position: FurniturePosition,
  cellWidth: number,
  rowHeight: number,
  avatarDotPx: number,
): FurnitureLayout {
  const set = FURNITURE_SETS[type];
  const scale = avatarDotPx;
  const setWidthPx = set.widthDots * scale;
  const setHeightPx = set.heightDots * scale;
  const footprintCenterX = (position.col + set.footprintCols / 2) * cellWidth;
  const footprintGroundY = (position.row + 1) * rowHeight;
  const containerLeft = footprintCenterX - setWidthPx / 2;
  const containerTop = footprintGroundY - setHeightPx;

  return {
    scale,
    containerLeft,
    containerTop,
    pieces: set.pieces.map((piece) => ({
      source: piece.source,
      role: piece.role ?? "back",
      left: containerLeft + (piece.offsetXDots - set.minXDots) * scale,
      top: containerTop + (piece.offsetYDots - set.minYDots) * scale,
      width: piece.widthDots * scale,
      height: piece.heightDots * scale,
    })),
  };
}

/** 두 배치가 격자에서 겹치는지(같은 줄에서 칸 범위가 겹치는지) 확인한다. */
export function doFootprintsOverlap(
  typeA: FurnitureType,
  positionA: FurniturePosition,
  typeB: FurnitureType,
  positionB: FurniturePosition,
): boolean {
  if (positionA.row !== positionB.row) {
    return false;
  }
  const colsA = FURNITURE_SETS[typeA].footprintCols;
  const colsB = FURNITURE_SETS[typeB].footprintCols;
  return positionA.col < positionB.col + colsB && positionB.col < positionA.col + colsA;
}

/** 배치된 가구가 격자에서 차지하는 화면 사각형(탭 판정용). */
export function getFurnitureFootprintRect(
  type: FurnitureType,
  position: FurniturePosition,
  cellWidth: number,
  rowHeight: number,
) {
  const footprintCols = FURNITURE_SETS[type].footprintCols;
  return {
    left: position.col * cellWidth,
    top: position.row * rowHeight,
    right: (position.col + footprintCols) * cellWidth,
    bottom: (position.row + 1) * rowHeight,
  };
}

/**
 * 아바타가 걸어가서 앉거나 누울 지점(발 접지 기준). 책상은 의자 위, 침대는
 * 발자국 한가운데로 잡는다.
 */
export function getFurnitureInteractionAnchor(
  type: FurnitureType,
  position: FurniturePosition,
  cellWidth: number,
  rowHeight: number,
  avatarDotPx: number,
): { x: number; y: number } {
  if (type === "desk") {
    const { pieces } = getFurnitureLayout(type, position, cellWidth, rowHeight, avatarDotPx);
    const chair = pieces.find((piece) => piece.role === "back") ?? pieces[0];
    return { x: chair.left + chair.width / 2, y: chair.top + chair.height };
  }

  const footprintCols = FURNITURE_SETS[type].footprintCols;
  return {
    x: (position.col + footprintCols / 2) * cellWidth,
    y: (position.row + 1) * rowHeight,
  };
}
