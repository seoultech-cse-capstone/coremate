import type { ImageSourcePropType } from "react-native";

// assets_template.md 6절 기준. 좌표는 전부 "아바타 원점" 같은 공통 기준점 하나를
// 공유한다고 보고 그대로 옮겨왔다 — 세트 내 상대 배치(바운딩박스)만 쓰므로
// 기준점 자체가 어디든 결과는 같다.
export type FurnitureType =
  | "bed"
  | "desk"
  | "clothesRack"
  | "figureCabinet"
  | "fridge"
  | "pcTower"
  | "petCushion"
  | "washer";

export type FurniturePosition = { row: number; col: number };

type FurniturePieceSpec = {
  source: ImageSourcePropType;
  offsetXDots: number;
  offsetYDots: number;
  widthDots: number;
  heightDots: number;
  /**
   * 앉기/수면 상호작용 중 아바타보다 앞(front)에 그려야 하는 조각인지.
   * 책상 상판은 아바타를 가려야 하고(책상 씬: chair → 아바타 → desk),
   * 이불도 자는 아바타를 덮어야 한다(수면 씬: ... → 수면 아바타 → ... → 이불).
   * 나머지(의자, 침대 프레임)는 기본값 "back"으로 항상 아바타보다 뒤에 있다.
   */
  role?: "back" | "front";
};

export type FurnitureSet = {
  type: FurnitureType;
  label: string;
  /** 격자에서 차지하는 가로 칸 수. */
  footprintCols: number;
  pieces: FurniturePieceSpec[];
  /** 아래 네 값은 조각들을 합친 바운딩박스(도트) — buildSet이 자동 계산. */
  widthDots: number;
  heightDots: number;
  minXDots: number;
  minYDots: number;
};

function buildSet(
  type: FurnitureType,
  label: string,
  footprintCols: number,
  pieces: FurniturePieceSpec[],
): FurnitureSet {
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
    source: require("../../../assets/room/furniture/chair_basic.png"),
    offsetXDots: 4,
    offsetYDots: 24,
    widthDots: 31,
    heightDots: 26,
  },
  {
    source: require("../../../assets/room/furniture/desk_basic.png"),
    offsetXDots: -5,
    offsetYDots: 33,
    widthDots: 49,
    heightDots: 17,
    role: "front",
  },
]);

const BED_SET = buildSet("bed", "침대", 2, [
  {
    source: require("../../../assets/room/furniture/bed_basic.png"),
    offsetXDots: 0,
    offsetYDots: 0,
    widthDots: 58,
    heightDots: 28,
  },
  {
    source: require("../../../assets/room/furniture/bed_blanket_basic.png"),
    offsetXDots: 0,
    offsetYDots: 0,
    widthDots: 58,
    heightDots: 28,
    role: "front",
  },
]);

// 장식용 가구(상호작용 없음) — 전부 조각 1개, 가로 1칸.
const CLOTHES_RACK_SET = buildSet("clothesRack", "행거", 1, [
  {
    source: require("../../../assets/room/furniture/furn_clothes_rack_1.png"),
    offsetXDots: 0,
    offsetYDots: 0,
    widthDots: 17,
    heightDots: 13,
  },
]);

const FIGURE_CABINET_SET = buildSet("figureCabinet", "피규어 진열장", 1, [
  {
    source: require("../../../assets/room/furniture/furn_figure_cabinet.png"),
    offsetXDots: 0,
    offsetYDots: 0,
    widthDots: 16,
    heightDots: 20,
  },
]);

const FRIDGE_SET = buildSet("fridge", "미니 냉장고", 1, [
  {
    source: require("../../../assets/room/furniture/furn_fridge_mini.png"),
    offsetXDots: 0,
    offsetYDots: 0,
    widthDots: 12,
    heightDots: 16,
  },
]);

const PC_TOWER_SET = buildSet("pcTower", "PC 본체", 1, [
  {
    source: require("../../../assets/room/furniture/furn_pc_tower.png"),
    offsetXDots: 0,
    offsetYDots: 0,
    widthDots: 10,
    heightDots: 17,
  },
]);

const PET_CUSHION_SET = buildSet("petCushion", "반려동물 방석", 1, [
  {
    source: require("../../../assets/room/furniture/furn_pet_cushion.png"),
    offsetXDots: 0,
    offsetYDots: 0,
    widthDots: 15,
    heightDots: 7,
  },
]);

const WASHER_SET = buildSet("washer", "세탁기", 1, [
  {
    source: require("../../../assets/room/furniture/furn_washer.png"),
    offsetXDots: 0,
    offsetYDots: 0,
    widthDots: 12,
    heightDots: 15,
  },
]);

export const FURNITURE_SETS: Record<FurnitureType, FurnitureSet> = {
  bed: BED_SET,
  desk: DESK_SET,
  clothesRack: CLOTHES_RACK_SET,
  figureCabinet: FIGURE_CABINET_SET,
  fridge: FRIDGE_SET,
  pcTower: PC_TOWER_SET,
  petCushion: PET_CUSHION_SET,
  washer: WASHER_SET,
};

export const FURNITURE_TYPES: FurnitureType[] = [
  "bed",
  "desk",
  "clothesRack",
  "figureCabinet",
  "fridge",
  "pcTower",
  "petCushion",
  "washer",
];

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

/** 배치 좌표 + 격자 치수로 가구 세트의 화면 위치를 계산한다. RoomBackdrop과 상호작용 로직이 공유한다. */
export function getFurnitureLayout(
  type: FurnitureType,
  position: FurniturePosition,
  cellWidth: number,
  rowHeight: number,
): FurnitureLayout {
  const set = FURNITURE_SETS[type];
  // dotPx(배경과 같은 스케일)를 그대로 쓰면 세트 폭이 footprintCols칸보다 커진다 —
  // 세트 너비가 정확히 footprintCols칸에 맞도록 세트 전용 배율을 쓴다.
  const scale = (set.footprintCols * cellWidth) / set.widthDots;
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
): { x: number; y: number } {
  if (type === "desk") {
    const { pieces } = getFurnitureLayout(type, position, cellWidth, rowHeight);
    const chair = pieces.find((piece) => piece.role === "back") ?? pieces[0];
    return { x: chair.left + chair.width / 2, y: chair.top + chair.height };
  }

  const footprintCols = FURNITURE_SETS[type].footprintCols;
  return {
    x: (position.col + footprintCols / 2) * cellWidth,
    y: (position.row + 1) * rowHeight,
  };
}
