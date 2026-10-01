import { useState, type ReactNode } from "react";
import { Image, StyleSheet, View } from "react-native";

import {
  AVATAR_FOOT_X_RATIO,
  AVATAR_FOOT_Y_RATIO,
  AVATAR_SLEEP_STAGE_HEIGHT,
  AVATAR_SLEEP_STAGE_WIDTH,
  AVATAR_STAGE_HEIGHT,
  AVATAR_STAGE_WIDTH,
  AVATAR_WIDTH_IN_CELLS,
  AvatarSleepStage,
  AvatarStage,
  getAvatarDotPx,
} from "./AvatarStage";
import { RoomBackdrop, type RoomLayoutInfo } from "./RoomBackdrop";
import {
  PET_BOX_HEIGHT_DOTS,
  PET_BOX_WIDTH_DOTS,
  PET_FOLLOW_OFFSET_CELLS,
  SLEEP_AVATAR_OFFSET_DOTS,
  SLEEP_RAW_PX_PER_DOT,
  STAND_START_RATIO,
} from "./RoomScene";
import { toEquippedMap } from "../itemStem";
import { DEFAULT_LOCAL_KEY_BY_SLOT, LOCAL_CATALOG_BY_KEY } from "../localCatalog";
import {
  getFurnitureInteractionAnchor,
  getFurnitureLayout,
  getPosedFurniture,
  placementsFromRoom,
} from "../roomFurniture";
import { ROOM_THEME_BY_KEY, ROOM_THEMES, themeKeyFromEquipped } from "../roomThemes";
import type { RoomResponse } from "../roomTypes";

type RoomSnapshotProps = {
  room: RoomResponse;
  height: number;
};

/**
 * 남의 방(친구 방)을 읽기 전용으로 그린다. 벽지·가구·옷은 서버 방 응답 그대로이고, 아바타는
 * 마지막에 취한 포즈(서 있기·책상·침대)로 멈춰 있다. 내 방(RoomScene)과 달리 걷기·탭이 없고
 * 포즈를 저장하지 않는다 — 여기서 저장하면 친구가 아니라 내 계정의 포즈가 바뀐다.
 */
export function RoomSnapshot({ room, height }: RoomSnapshotProps) {
  const [layout, setLayout] = useState<RoomLayoutInfo | null>(null);

  const theme = ROOM_THEME_BY_KEY.get(themeKeyFromEquipped(room.equippedItems)) ?? ROOM_THEMES[0];
  const placements = placementsFromRoom(room.placements);
  const equipped = toEquippedMap(room.equippedItems);
  const posedFurniture = getPosedFurniture(room);
  const posedPosition = posedFurniture ? placements[posedFurniture] : undefined;
  const petItem = equipped.PET ? LOCAL_CATALOG_BY_KEY.get(equipped.PET) : undefined;

  let overlay: ReactNode = null;
  if (layout) {
    const { cellWidth, rowHeight, width, floorHeight } = layout;
    const avatarDotPx = getAvatarDotPx(cellWidth);
    const avatarScale = (AVATAR_WIDTH_IN_CELLS * cellWidth) / AVATAR_STAGE_WIDTH;
    const avatarWidth = AVATAR_STAGE_WIDTH * avatarScale;
    const avatarHeight = AVATAR_STAGE_HEIGHT * avatarScale;
    const shadowWidth = avatarWidth * 0.6;
    const shadowHeight = shadowWidth * 0.35;

    // 발 접지점. 앉거나 누웠으면 그 가구의 상호작용 지점, 서 있으면 내 방의 처음 자리와 같은 곳이다.
    const anchor =
      posedFurniture && posedPosition
        ? getFurnitureInteractionAnchor(posedFurniture, posedPosition, cellWidth, rowHeight, avatarDotPx)
        : { x: width * STAND_START_RATIO.x, y: floorHeight * STAND_START_RATIO.y };
    const footX = Math.round(anchor.x);
    const footY = Math.round(anchor.y);

    let avatar: ReactNode;
    if (posedFurniture === "bed" && posedPosition) {
      const bedLayout = getFurnitureLayout("bed", posedPosition, cellWidth, rowHeight, avatarDotPx);
      const sleepScale = bedLayout.scale / SLEEP_RAW_PX_PER_DOT;
      avatar = (
        <View
          style={[
            styles.layer,
            {
              left: bedLayout.containerLeft + SLEEP_AVATAR_OFFSET_DOTS.x * bedLayout.scale,
              top: bedLayout.containerTop + SLEEP_AVATAR_OFFSET_DOTS.y * bedLayout.scale,
              width: AVATAR_SLEEP_STAGE_WIDTH * sleepScale,
              height: AVATAR_SLEEP_STAGE_HEIGHT * sleepScale,
            },
          ]}
        >
          <AvatarSleepStage hairKey={equipped.HAIR ?? DEFAULT_LOCAL_KEY_BY_SLOT.HAIR} scale={sleepScale} />
        </View>
      );
    } else {
      avatar = (
        <>
          <View
            style={[
              styles.layer,
              styles.shadow,
              {
                left: footX - shadowWidth / 2,
                top: footY - shadowHeight / 2,
                width: shadowWidth,
                height: shadowHeight,
              },
            ]}
          />
          <View
            style={[
              styles.layer,
              { left: footX - avatarWidth * AVATAR_FOOT_X_RATIO, top: footY - avatarHeight * AVATAR_FOOT_Y_RATIO },
            ]}
          >
            <AvatarStage
              activity={posedFurniture === "desk" ? "sit" : "stand"}
              equipped={equipped}
              scale={avatarScale}
            />
          </View>
        </>
      );
    }

    const petWidth = PET_BOX_WIDTH_DOTS * avatarDotPx;
    const petHeight = PET_BOX_HEIGHT_DOTS * avatarDotPx;
    const petX = Math.min(Math.max(footX + PET_FOLLOW_OFFSET_CELLS * cellWidth, 0), width);

    overlay = (
      <>
        {avatar}
        {petItem ? (
          <Image
            resizeMode="contain"
            source={petItem.source}
            style={[styles.layer, { left: petX - petWidth / 2, top: footY - petHeight, width: petWidth, height: petHeight }]}
          />
        ) : null}
      </>
    );
  }

  return (
    <RoomBackdrop
      height={height}
      occupiedFurniture={posedFurniture}
      onLayoutChange={setLayout}
      overlay={overlay}
      placements={placements}
      showGridLines={false}
      theme={theme}
    />
  );
}

const styles = StyleSheet.create({
  layer: {
    position: "absolute",
  },
  shadow: {
    borderRadius: 999,
    backgroundColor: "rgba(0, 0, 0, 0.25)",
  },
});
