import { FURNITURE_SETS, FURNITURE_TYPES, type FurniturePosition, type FurnitureType } from "./roomFurniture";
import { ROOM_GRID_COLS } from "./roomGrid";

// 방 바닥을 걸을 때 가구를 피해 돌아가는 길. 가구가 차지한 칸(발자국)을 막힌 칸으로 보고 칸 단위로
// 가장 짧은 길을 찾은 뒤, 가구에 닿지 않고 곧게 갈 수 있는 구간은 하나로 펴서 꺾임을 줄인다.
// 가는 길에 가구가 없으면 예전처럼 목표까지 곧게 걷는다.

/** 바닥 좌표(발 접지점). 바닥 왼쪽 위가 (0, 0)이다. */
export type FloorPoint = { x: number; y: number };

export type WalkGrid = {
  cols: number;
  rows: number;
  cellWidth: number;
  rowHeight: number;
  /** row * cols + col 칸이 가구로 막혔는지 */
  blocked: boolean[];
};

// 곧게 펼 때 가구 가장자리를 스치지 않도록 두는 여유(칸 크기 대비). 칸 가운데는 이 여유 밖이다.
const CLEARANCE_RATIO = 0.2;
// 대각선은 양옆 두 칸이 모두 비어 있을 때만 간다 — 가구 모서리를 가로지르지 않게.
const NEIGHBORS: [number, number][] = [
  [-1, 0], [1, 0], [0, -1], [0, 1],
  [-1, -1], [-1, 1], [1, -1], [1, 1],
];

/** 배치된 가구로 막힌 칸을 표시한 바닥 격자. 줄 수는 바닥 높이를 덮을 만큼(가구를 못 놓는 맨 아래 여백 포함). */
export function buildWalkGrid(
  placements: Partial<Record<FurnitureType, FurniturePosition>>,
  cellWidth: number,
  rowHeight: number,
  floorHeight: number,
): WalkGrid {
  const cols = ROOM_GRID_COLS;
  const rows = Math.max(1, Math.ceil(floorHeight / rowHeight));
  const blocked = new Array<boolean>(rows * cols).fill(false);
  for (const type of FURNITURE_TYPES) {
    const position = placements[type];
    if (!position || position.row < 0 || position.row >= rows) {
      continue;
    }
    const lastCol = Math.min(position.col + FURNITURE_SETS[type].footprintCols, cols);
    for (let col = Math.max(position.col, 0); col < lastCol; col++) {
      blocked[position.row * cols + col] = true;
    }
  }
  return { cols, rows, cellWidth, rowHeight, blocked };
}

function clamp(value: number, min: number, max: number): number {
  return Math.min(Math.max(value, min), max);
}

/** 점이 들어 있는 칸. 바닥 밖이면 가장 가까운 가장자리 칸으로 본다. */
function cellIndexAt(grid: WalkGrid, point: FloorPoint): number {
  const col = clamp(Math.floor(point.x / grid.cellWidth), 0, grid.cols - 1);
  const row = clamp(Math.floor(point.y / grid.rowHeight), 0, grid.rows - 1);
  return row * grid.cols + col;
}

function cellRect(grid: WalkGrid, index: number) {
  const row = Math.floor(index / grid.cols);
  const col = index % grid.cols;
  return {
    left: col * grid.cellWidth,
    top: row * grid.rowHeight,
    right: (col + 1) * grid.cellWidth,
    bottom: (row + 1) * grid.rowHeight,
  };
}

function cellCenter(grid: WalkGrid, index: number): FloorPoint {
  const rect = cellRect(grid, index);
  return { x: (rect.left + rect.right) / 2, y: (rect.top + rect.bottom) / 2 };
}

/** 칸 사이의 가장 짧은 길(칸 번호 목록, 출발·도착 포함). 출발·도착 칸은 막혀 있어도 지나갈 수 있다. */
function findCellPath(grid: WalkGrid, startIndex: number, goalIndex: number): number[] | null {
  const { cols, rows } = grid;
  const total = cols * rows;
  const passable = (index: number) => index === startIndex || index === goalIndex || !grid.blocked[index];
  const distance = new Array<number>(total).fill(Infinity);
  const previous = new Array<number>(total).fill(-1);
  const visited = new Array<boolean>(total).fill(false);
  distance[startIndex] = 0;

  // 칸이 50개 안팎이라 우선순위 큐 없이 매번 가장 가까운 칸을 고른다.
  for (;;) {
    let current = -1;
    for (let index = 0; index < total; index++) {
      if (!visited[index] && distance[index] < Infinity && (current === -1 || distance[index] < distance[current])) {
        current = index;
      }
    }
    if (current === -1) {
      return null;
    }
    if (current === goalIndex) {
      break;
    }
    visited[current] = true;
    const row = Math.floor(current / cols);
    const col = current % cols;
    for (const [dRow, dCol] of NEIGHBORS) {
      const nextRow = row + dRow;
      const nextCol = col + dCol;
      if (nextRow < 0 || nextRow >= rows || nextCol < 0 || nextCol >= cols) {
        continue;
      }
      const next = nextRow * cols + nextCol;
      if (visited[next] || !passable(next)) {
        continue;
      }
      if (dRow !== 0 && dCol !== 0 && (!passable(row * cols + nextCol) || !passable(nextRow * cols + col))) {
        continue;
      }
      // 칸이 납작해서(가로 > 세로) 실제 화면 거리로 잰다.
      const cost = Math.hypot(dCol * grid.cellWidth, dRow * grid.rowHeight);
      if (distance[current] + cost < distance[next]) {
        distance[next] = distance[current] + cost;
        previous[next] = current;
      }
    }
  }

  const path: number[] = [];
  for (let index = goalIndex; index !== -1; index = previous[index]) {
    path.unshift(index);
  }
  return path;
}

/** from→to 직선이 (여유를 두고) 막힌 칸에 닿지 않는지. exempt 칸(출발·도착 가구)은 지나가도 된다. */
function isClearLine(grid: WalkGrid, from: FloorPoint, to: FloorPoint, exempt: number[]): boolean {
  const marginX = grid.cellWidth * CLEARANCE_RATIO;
  const marginY = grid.rowHeight * CLEARANCE_RATIO;
  const length = Math.hypot(to.x - from.x, to.y - from.y);
  const steps = Math.max(1, Math.ceil(length / (Math.min(grid.cellWidth, grid.rowHeight) / 4)));
  for (let step = 0; step <= steps; step++) {
    const t = step / steps;
    const x = from.x + (to.x - from.x) * t;
    const y = from.y + (to.y - from.y) * t;
    for (const [offsetX, offsetY] of [
      [-marginX, -marginY],
      [marginX, -marginY],
      [-marginX, marginY],
      [marginX, marginY],
    ]) {
      const index = cellIndexAt(grid, { x: x + offsetX, y: y + offsetY });
      if (grid.blocked[index] && !exempt.includes(index)) {
        return false;
      }
    }
  }
  return true;
}

/**
 * from에서 to까지 걸어갈 경유점(from 제외, 마지막이 to). 가구를 돌아가야 하면 경유점이 생기고,
 * 곧게 갈 수 있으면 [to] 하나다. 갈 길이 아예 없으면(가구에 갇힘) 곧게 간다.
 * 출발·도착 칸은 가구 위여도 된다 — 책상 의자에서 일어나거나 침대·책상으로 걸어갈 때다.
 */
export function findWalkPath(grid: WalkGrid, from: FloorPoint, to: FloorPoint): FloorPoint[] {
  const startIndex = cellIndexAt(grid, from);
  const goalIndex = cellIndexAt(grid, to);
  const cellPath = findCellPath(grid, startIndex, goalIndex);
  if (!cellPath) {
    return [to];
  }

  const points = [from, ...cellPath.slice(1, -1).map((index) => cellCenter(grid, index)), to];
  const exempt = [startIndex, goalIndex];
  const waypoints: FloorPoint[] = [];
  let currentIndex = 0;
  while (currentIndex < points.length - 1) {
    let nextIndex = points.length - 1;
    while (nextIndex > currentIndex + 1 && !isClearLine(grid, points[currentIndex], points[nextIndex], exempt)) {
      nextIndex--;
    }
    waypoints.push(points[nextIndex]);
    currentIndex = nextIndex;
  }
  return waypoints;
}

/** 점이 가구 칸 위면 가장 가까운 빈 칸 안의 점으로 옮긴다. 빈 칸이거나 빈 칸이 하나도 없으면 그대로다. */
export function nearestFreePoint(grid: WalkGrid, point: FloorPoint): FloorPoint {
  if (!grid.blocked[cellIndexAt(grid, point)]) {
    return point;
  }
  let best = -1;
  let bestDistance = Infinity;
  grid.blocked.forEach((isBlocked, index) => {
    if (isBlocked) {
      return;
    }
    const rect = cellRect(grid, index);
    const distance = Math.hypot(
      Math.max(rect.left - point.x, 0, point.x - rect.right),
      Math.max(rect.top - point.y, 0, point.y - rect.bottom),
    );
    if (distance < bestDistance) {
      best = index;
      bestDistance = distance;
    }
  });
  if (best === -1) {
    return point;
  }
  const rect = cellRect(grid, best);
  const marginX = grid.cellWidth * CLEARANCE_RATIO;
  const marginY = grid.rowHeight * CLEARANCE_RATIO;
  return {
    x: clamp(point.x, rect.left + marginX, rect.right - marginX),
    y: clamp(point.y, rect.top + marginY, rect.bottom - marginY),
  };
}

/**
 * 가만히 있을 때 산책할 곳. 지금 칸이 아니고 가는 길이 있는 빈 칸 중, 가까운(가로·세로 maxStepCells칸 안)
 * 칸 하나를 골라 그 안의 아무 점이나 돌려준다. maxRows 줄 아래(화면에 가려진 바닥)는 고르지 않는다.
 */
export function pickWanderPoint(
  grid: WalkGrid,
  from: FloorPoint,
  options: { maxRows: number; maxStepCells: number; random?: () => number },
): FloorPoint | null {
  const random = options.random ?? Math.random;
  const startIndex = cellIndexAt(grid, from);
  const startRow = Math.floor(startIndex / grid.cols);
  const startCol = startIndex % grid.cols;
  const candidates = grid.blocked.flatMap((isBlocked, index) => {
    const row = Math.floor(index / grid.cols);
    const col = index % grid.cols;
    const isNear = Math.abs(row - startRow) <= options.maxStepCells && Math.abs(col - startCol) <= options.maxStepCells;
    return !isBlocked && index !== startIndex && row < options.maxRows && isNear ? [index] : [];
  });

  // 무작위 순서로 보다가 갈 수 있는 첫 칸을 고른다.
  for (let remaining = candidates.length; remaining > 0; remaining--) {
    const pick = Math.floor(random() * remaining);
    const index = candidates[pick];
    candidates[pick] = candidates[remaining - 1];
    if (!findCellPath(grid, startIndex, index)) {
      continue;
    }
    const rect = cellRect(grid, index);
    const marginX = grid.cellWidth * CLEARANCE_RATIO;
    const marginY = grid.rowHeight * CLEARANCE_RATIO;
    return {
      x: rect.left + marginX + random() * (grid.cellWidth - 2 * marginX),
      y: rect.top + marginY + random() * (grid.rowHeight - 2 * marginY),
    };
  }
  return null;
}
