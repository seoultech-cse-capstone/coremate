// 방 바닥 격자 지오메트리 — 이 위치·크기 규칙은 고정이다.
// 지금은 RoomScene이 안내선으로만 그리지만, 나중에 가구가 이 칸 단위로
// 배치되므로 좌표 계산은 반드시 이 파일 하나만 거치게 한다. 안내선을 지우게
// 되더라도(#165 실제 벽지/바닥 에셋 적용 시) 이 지오메트리 자체는 그대로 둔다.

// 확정된 방 격자: 가로 10 × 세로 7(#165).
export const ROOM_GRID_COLS = 10;
export const ROOM_GRID_ROWS = 7;
// 칸을 정사각형이 아니라 납작한 직사각형으로 그려서 내려다본 듯한 입체감을 준다.
const ROW_HEIGHT_RATIO = 0.75;
// 격자 마지막 줄 아래로 가구를 놓을 수 없는 빈 바닥을 두 칸만큼 남겨둔다.
const GRID_BOTTOM_MARGIN_ROWS = 2;

export type RoomGridLayout = {
  cellWidth: number;
  rowHeight: number;
  gridHeight: number;
  gridBottomMargin: number;
  floorHeight: number;
  wallHeight: number;
};

/**
 * 방 너비·전체 높이가 주어졌을 때 격자/바닥/벽 치수를 계산한다.
 * cols·rows를 넘기면 그 칸수로 계산한다 — 화면에서 임시로 다른 칸수를 미리 보고
 * 싶을 때만 쓰고, 실제 가구 배치 기준은 항상 기본값(ROOM_GRID_COLS/ROWS)이다.
 */
export function getRoomGridLayout(
  width: number,
  roomHeight: number,
  cols: number = ROOM_GRID_COLS,
  rows: number = ROOM_GRID_ROWS,
): RoomGridLayout {
  const cellWidth = width / cols;
  const rowHeight = cellWidth * ROW_HEIGHT_RATIO;
  const gridHeight = rowHeight * rows;
  const gridBottomMargin = rowHeight * GRID_BOTTOM_MARGIN_ROWS;
  const floorHeight = Math.min(roomHeight, gridHeight + gridBottomMargin);
  const wallHeight = roomHeight - floorHeight;

  return { cellWidth, rowHeight, gridHeight, gridBottomMargin, floorHeight, wallHeight };
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
