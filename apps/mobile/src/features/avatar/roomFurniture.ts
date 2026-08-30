import type { ImageSourcePropType } from "react-native";

// assets_template.md 6절 기준. 좌표는 전부 "아바타 원점" 같은 공통 기준점 하나를
// 공유한다고 보고 그대로 옮겨왔다 — 세트 내 상대 배치(바운딩박스)만 쓰므로
// 기준점 자체가 어디든 결과는 같다.
export type FurnitureType = "bed" | "desk";

type FurniturePieceSpec = {
  source: ImageSourcePropType;
  offsetXDots: number;
  offsetYDots: number;
  widthDots: number;
  heightDots: number;
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
  },
]);

export const FURNITURE_SETS: Record<FurnitureType, FurnitureSet> = {
  bed: BED_SET,
  desk: DESK_SET,
};

export const FURNITURE_TYPES: FurnitureType[] = ["bed", "desk"];
