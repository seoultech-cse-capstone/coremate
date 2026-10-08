import { useMemo, useRef, useState } from "react";
import { Image, StyleSheet, View } from "react-native";
import { Gesture, GestureDetector } from "react-native-gesture-handler";

import { useRoomFurniture, type FurniturePosition } from "../RoomFurnitureContext";
import {
  doFootprintsOverlap,
  FURNITURE_SETS,
  FURNITURE_TYPES,
  getFurnitureFootprintRect,
  getFurnitureLayout,
  type FurnitureType,
} from "../roomFurniture";
import { ROOM_GRID_COLS, ROOM_GRID_ROWS } from "../roomGrid";
import { getAvatarDotPx } from "./AvatarStage";
import { RoomBackdrop, type RoomLayoutInfo } from "./RoomBackdrop";

/** 꾹 누른 뒤 이 시간이 지나야 끌기가 시작된다. 그보다 짧게 떼면 지금처럼 탭(놓기·치우기)이다. */
const LONG_PRESS_MS = 350;

type Drag = {
  type: FurnitureType;
  /** 잡은 칸이 가구 원점(왼쪽 칸)에서 얼마나 떨어져 있는지. 손가락 칸에서 빼면 놓을 자리다. */
  grabOffset: { row: number; col: number };
  target: FurniturePosition;
  canDrop: boolean;
};

type RoomFurnitureEditorProps = {
  height: number;
  /** 바닥을 짧게 탭했을 때(바닥 기준 좌표). 놓기·치우기 규칙은 호출부가 정한다. */
  onFloorTap: (floorX: number, floorY: number, layout: RoomLayoutInfo) => void;
  /** 끌어서 놓으려던 자리가 다른 가구와 겹쳐 원래 자리로 돌아갔을 때. */
  onDropBlocked: () => void;
};

function clampToGrid(type: FurnitureType, position: FurniturePosition): FurniturePosition {
  const footprintCols = FURNITURE_SETS[type].footprintCols;
  return {
    row: Math.min(Math.max(position.row, 0), ROOM_GRID_ROWS - 1),
    col: Math.min(Math.max(position.col, 0), ROOM_GRID_COLS - footprintCols),
  };
}

/**
 * 인벤토리 마이룸 탭의 방 편집기. 짧게 탭하면 지금처럼 놓기·치우기, 놓인 가구를 꾹 누르면
 * 끌어서 옮긴다(#268). 끄는 동안 원래 가구는 숨기고, 놓일 칸에 반투명 고스트를 그린다 —
 * 다른 가구와 겹치는 칸이면 빨갛게 표시하고, 거기서 손을 떼면 원래 자리로 돌아간다.
 */
export function RoomFurnitureEditor({ height, onFloorTap, onDropBlocked }: RoomFurnitureEditorProps) {
  const { placements, setPlacement } = useRoomFurniture();
  const [layout, setLayout] = useState<RoomLayoutInfo | null>(null);
  const [drag, setDrag] = useState<Drag | null>(null);
  // 제스처 콜백은 만들어질 때의 값을 잡으므로, 끄는 중 상태는 ref 로도 들고 있는다.
  const dragRef = useRef<Drag | null>(null);
  // 놓은 직후의 새 자리. setPlacement 는 캐시를 한 박자 늦게(비동기) 바꾸므로, 그 사이 끌기 상태만
  // 지우면 원래 자리가 한 번 그려졌다가 새 자리로 튄다. 캐시(placements)가 바뀔 때까지 새 자리를 대신 보여 준다.
  const [pendingDrop, setPendingDrop] = useState<{
    type: FurnitureType;
    target: FurniturePosition;
    basePlacements: typeof placements;
  } | null>(null);

  const updateDrag = (next: Drag | null) => {
    dragRef.current = next;
    setDrag(next);
  };

  const toCell = (floorX: number, floorY: number, info: RoomLayoutInfo) => ({
    row: Math.floor(floorY / info.rowHeight),
    col: Math.floor(floorX / info.cellWidth),
  });

  /** 손가락 아래 놓인 가구. 발자국 칸이나 그림(벽 쪽으로 솟은 부분 포함) 안이면 잡힌다. 겹치면 앞(아래 줄) 가구. */
  const findFurnitureAt = (floorX: number, floorY: number, info: RoomLayoutInfo): FurnitureType | null => {
    const avatarDotPx = getAvatarDotPx(info.cellWidth);
    const hits = FURNITURE_TYPES.filter((type) => {
      const position = placements[type];
      if (!position) {
        return false;
      }
      const footprint = getFurnitureFootprintRect(type, position, info.cellWidth, info.rowHeight);
      const set = FURNITURE_SETS[type];
      const sprite = getFurnitureLayout(type, position, info.cellWidth, info.rowHeight, avatarDotPx);
      const inFootprint =
        floorX >= footprint.left && floorX < footprint.right && floorY >= footprint.top && floorY < footprint.bottom;
      const inSprite =
        floorX >= sprite.containerLeft &&
        floorX < sprite.containerLeft + set.widthDots * sprite.scale &&
        floorY >= sprite.containerTop &&
        floorY < footprint.bottom;
      return inFootprint || inSprite;
    });
    hits.sort((a, b) => placements[b]!.row - placements[a]!.row);
    return hits[0] ?? null;
  };

  const evaluate = (type: FurnitureType, target: FurniturePosition) =>
    !FURNITURE_TYPES.some((otherType) => {
      const otherPosition = placements[otherType];
      return otherType !== type && otherPosition && doFootprintsOverlap(type, target, otherType, otherPosition);
    });

  // 아래 콜백들은 렌더 중이 아니라 터치 이벤트 때 불린다. react-hooks/refs 는 제스처 빌더에 넘긴
  // 함수를 렌더 중 호출로 오인해 dragRef 접근을 막으므로, 이 블록에서만 끈다.
  /* eslint-disable react-hooks/refs */
  const gesture = useMemo(() => {
    const pan = Gesture.Pan()
      .runOnJS(true)
      .activateAfterLongPress(LONG_PRESS_MS)
      .onStart((event) => {
        if (!layout) {
          return;
        }
        const floorY = event.y - layout.wallHeight;
        const type = findFurnitureAt(event.x, floorY, layout);
        if (!type) {
          return;
        }
        const origin = placements[type]!;
        const cell = toCell(event.x, floorY, layout);
        const grabOffset = {
          row: cell.row - origin.row,
          col: Math.min(Math.max(cell.col - origin.col, 0), FURNITURE_SETS[type].footprintCols - 1),
        };
        updateDrag({ type, grabOffset, target: origin, canDrop: true });
      })
      .onUpdate((event) => {
        const current = dragRef.current;
        if (!current || !layout) {
          return;
        }
        const cell = toCell(event.x, event.y - layout.wallHeight, layout);
        const target = clampToGrid(current.type, {
          row: cell.row - current.grabOffset.row,
          col: cell.col - current.grabOffset.col,
        });
        // 칸이 바뀔 때만 다시 그린다. 손가락이 같은 칸 안에서 움직이는 동안은 그대로다.
        if (target.row !== current.target.row || target.col !== current.target.col) {
          updateDrag({ ...current, target, canDrop: evaluate(current.type, target) });
        }
      })
      .onEnd(() => {
        const current = dragRef.current;
        if (!current) {
          return;
        }
        const origin = placements[current.type];
        const moved = !origin || origin.row !== current.target.row || origin.col !== current.target.col;
        if (moved) {
          if (current.canDrop) {
            setPlacement(current.type, current.target);
            setPendingDrop({ type: current.type, target: current.target, basePlacements: placements });
          } else {
            onDropBlocked();
          }
        }
      })
      .onFinalize(() => {
        updateDrag(null);
      });

    const tap = Gesture.Tap()
      .runOnJS(true)
      .onEnd((event, success) => {
        if (success && layout) {
          setPendingDrop(null);
          onFloorTap(event.x, event.y - layout.wallHeight, layout);
        }
      });

    // 꾹 누르기가 실패(일찍 손을 뗌)해야 탭으로 본다 — 끌기와 탭이 한 번에 둘 다 일어나지 않는다.
    return Gesture.Exclusive(pan, tap);
    // 콜백이 쓰는 값(layout·placements·핸들러)이 바뀔 때마다 새로 만든다.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [layout, placements, setPlacement, onFloorTap, onDropBlocked]);
  /* eslint-enable react-hooks/refs */

  // 끄는 동안 원래 자리의 가구는 숨긴다. 고스트가 새 자리를 보여 준다.
  const visiblePlacements = useMemo(() => {
    if (drag) {
      const next = { ...placements };
      delete next[drag.type];
      return next;
    }
    // 캐시가 아직 놓기 전 그대로면(같은 객체) 새 자리를 덧씌운다. 캐시가 바뀌면 그대로 쓴다.
    if (pendingDrop && pendingDrop.basePlacements === placements) {
      return { ...placements, [pendingDrop.type]: pendingDrop.target };
    }
    return undefined;
  }, [drag, pendingDrop, placements]);

  const ghost = drag && layout ? renderGhost(drag, layout) : null;

  return (
    <GestureDetector gesture={gesture}>
      <View collapsable={false}>
        <RoomBackdrop height={height} onLayoutChange={setLayout} placements={visiblePlacements} />
        {ghost}
      </View>
    </GestureDetector>
  );
}

function renderGhost(drag: Drag, layout: RoomLayoutInfo) {
  const footprint = getFurnitureFootprintRect(drag.type, drag.target, layout.cellWidth, layout.rowHeight);
  const sprite = getFurnitureLayout(
    drag.type,
    drag.target,
    layout.cellWidth,
    layout.rowHeight,
    getAvatarDotPx(layout.cellWidth),
  );

  return (
    <View pointerEvents="none" style={[StyleSheet.absoluteFill, { top: layout.wallHeight }]}>
      <View
        style={[
          styles.ghostCell,
          drag.canDrop ? styles.ghostCellOk : styles.ghostCellBlocked,
          {
            left: footprint.left,
            top: footprint.top,
            width: footprint.right - footprint.left,
            height: footprint.bottom - footprint.top,
          },
        ]}
      />
      {sprite.pieces.map((piece, index) => (
        <Image
          key={index}
          resizeMode="stretch"
          source={piece.source}
          style={{
            position: "absolute",
            left: piece.left,
            top: piece.top,
            width: piece.width,
            height: piece.height,
            opacity: drag.canDrop ? 0.6 : 0.35,
          }}
        />
      ))}
    </View>
  );
}

const styles = StyleSheet.create({
  ghostCell: {
    position: "absolute",
    borderRadius: 4,
    borderWidth: 2,
  },
  ghostCellOk: {
    borderColor: "#6D5DFB",
    backgroundColor: "rgba(109, 93, 251, 0.25)",
  },
  ghostCellBlocked: {
    borderColor: "#EF4444",
    backgroundColor: "rgba(239, 68, 68, 0.3)",
  },
});
