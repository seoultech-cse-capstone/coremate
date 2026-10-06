// 방 바닥 격자 지오메트리 — 이 위치·크기 규칙은 고정이다.
// 지금은 RoomScene이 안내선으로만 그리지만, 나중에 가구가 이 칸 단위로
// 배치되므로 좌표 계산은 반드시 이 파일 하나만 거치게 한다. 안내선을 지우게
// 되더라도(#165 실제 벽지/바닥 에셋 적용 시) 이 지오메트리 자체는 그대로 둔다.

// 프론트 기준 방 격자: 가로 7 × 세로 5. #165에 적힌 10x7(백엔드/기획 확정치)과
// 다르다 — 화면에서 실제로 맞춰본 결과 7x5로 통일했고, #165에는 언질만 남겨뒀다.
// 백엔드 방 배치 API가 나오면 두 수치를 다시 맞춰야 한다.
export const ROOM_GRID_COLS = 7;
export const ROOM_GRID_ROWS = 5;
// 칸을 정사각형이 아니라 납작한 직사각형으로 그려서 내려다본 듯한 입체감을 준다.
const ROW_HEIGHT_RATIO = 0.75;
// 격자 마지막 줄 아래로 가구를 놓을 수 없는 빈 바닥을 한 칸만큼 남겨둔다.
const GRID_BOTTOM_MARGIN_ROWS = 1;
// assets_template.md 1절: 한 칸 = 16x12도트(16*0.75=12, ROW_HEIGHT_RATIO와 정확히 일치).
// dotPx = cellWidth/16 이 배경·아바타·가구 전체가 공유하는 화면 표시 스케일이다.
const DOTS_PER_CELL_WIDTH = 16;

export type RoomGridLayout = {
  cellWidth: number;
  rowHeight: number;
  gridHeight: number;
  gridBottomMargin: number;
  floorHeight: number;
  wallHeight: number;
  /** 도트 하나를 화면에서 몇 px로 그릴지. 배경·아바타·가구가 전부 이 값으로 스케일을 맞춘다. */
  dotPx: number;
};

/** 방 너비·전체 높이가 주어졌을 때 격자/바닥/벽 치수를 계산한다. */
export function getRoomGridLayout(width: number, roomHeight: number): RoomGridLayout {
  const cellWidth = width / ROOM_GRID_COLS;
  const rowHeight = cellWidth * ROW_HEIGHT_RATIO;
  const gridHeight = rowHeight * ROOM_GRID_ROWS;
  const gridBottomMargin = rowHeight * GRID_BOTTOM_MARGIN_ROWS;
  const floorHeight = Math.min(roomHeight, gridHeight + gridBottomMargin);
  const wallHeight = roomHeight - floorHeight;
  const dotPx = cellWidth / DOTS_PER_CELL_WIDTH;

  return { cellWidth, rowHeight, gridHeight, gridBottomMargin, floorHeight, wallHeight, dotPx };
}

/**
 * 바닥 전체(격자+아래 여백)와 벽 아래쪽 wallDots 도트만큼이 보이는 방 높이. 친구 방처럼 폭이 정해진
 * 상자에서 벽 그림(창문 등)이 잘리지 않게 높이를 정할 때 쓴다.
 */
export function getRoomHeightShowingWall(width: number, wallDots: number): number {
  const cellWidth = width / ROOM_GRID_COLS;
  const rowHeight = cellWidth * ROW_HEIGHT_RATIO;
  const floorHeight = rowHeight * (ROOM_GRID_ROWS + GRID_BOTTOM_MARGIN_ROWS);
  return floorHeight + wallDots * (cellWidth / DOTS_PER_CELL_WIDTH);
}

/** row·col(0부터 시작) 칸의 바닥 기준 좌상단 좌표와 크기. 가구 배치 좌표 변환에 쓴다. */
export function getRoomGridCellRect(row: number, col: number, layout: RoomGridLayout) {
  return {
    left: col * layout.cellWidth,
    top: row * layout.rowHeight,
    width: layout.cellWidth,
    height: layout.rowHeight,
  };
}
