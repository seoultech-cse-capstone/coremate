import type { ImageSourcePropType } from "react-native";
import { Image, StyleSheet, View } from "react-native";

import {
  DEFAULT_LOCAL_KEY_BY_SLOT,
  LOCAL_CATALOG_BY_KEY,
  type LocalAvatarSlot,
} from "../localCatalog";

// 스프라이트 시트 규격(픽셀 아바타 스펙북 08/26 기준): 코디 레이어 캔버스는
// 328x528 이고, 캐릭터 베이스는 328x432 로 더 작게 잘려 있다. 두 캔버스는
// 하단(발끝)을 기준으로 정렬되도록 만들어졌기 때문에, 베이스를 528 캔버스
// 안에 배치할 때 위쪽으로 (528-432)=96px 를 띄워야 코디 아이템과 맞는다.
const RAW_WIDTH = 328;
const RAW_HEIGHT = 528;
const RAW_BASE_HEIGHT = 432;
const RAW_BASE_TOP_OFFSET = RAW_HEIGHT - RAW_BASE_HEIGHT;

// 정면 서있는 포즈 에셋(베이스 + 모든 코디 아이템)의 실제 내용이 차지하는 영역을
// 픽셀 단위로 측정해서 구한 공통 여백(빨간 배경으로 확인). 좌우 48px, 위 64px,
// 아래 32px가 어떤 레이어도 쓰지 않는 빈 공간이라, 이 영역만 보여주면 레이어 간
// 정렬은 그대로 유지하면서 불필요한 여백만 균일하게 잘라낼 수 있다.
const CONTENT_CROP = { x: 48, y: 64, width: 232, height: 432 };

export const AVATAR_STAGE_WIDTH = CONTENT_CROP.width;
export const AVATAR_STAGE_HEIGHT = CONTENT_CROP.height;

const AVATAR_BASE_STAND: ImageSourcePropType = require("../../../../assets/avatar/base/avatar_base_stand_1.png");

// 뒤(발밑)부터 앞(얼굴 위)까지 겹쳐 그리는 순서.
const LAYER_DRAW_ORDER: LocalAvatarSlot[] = [
  "SHOES",
  "BOTTOM",
  "TOP",
  "BAG",
  "EYES",
  "MOUTH",
  "HAIR",
  "EYEWEAR",
  "HAT",
];

type AvatarStageProps = {
  scale?: number;
  /** 부위별로 입힐 로컬 카탈로그 아이템 key. 생략한 부위는 기본값(필수 부위) 또는 미착용(선택 부위). */
  equipped?: Partial<Record<LocalAvatarSlot, string | null>>;
};

/** 여백을 잘라낸 캔버스에 서있는 포즈 아바타를 합성해 보여준다. */
export function AvatarStage({ scale = 1, equipped }: AvatarStageProps) {
  const visibleWidth = AVATAR_STAGE_WIDTH * scale;
  const visibleHeight = AVATAR_STAGE_HEIGHT * scale;
  const rawWidth = RAW_WIDTH * scale;
  const rawHeight = RAW_HEIGHT * scale;

  const keyBySlot: Partial<Record<LocalAvatarSlot, string>> = { ...DEFAULT_LOCAL_KEY_BY_SLOT };
  for (const slot of LAYER_DRAW_ORDER) {
    if (equipped && slot in equipped) {
      const key = equipped[slot];
      if (key) {
        keyBySlot[slot] = key;
      } else {
        delete keyBySlot[slot];
      }
    }
  }

  const layers = LAYER_DRAW_ORDER.map((slot) => keyBySlot[slot])
    .filter((key): key is string => Boolean(key))
    .map((key) => LOCAL_CATALOG_BY_KEY.get(key))
    .filter((item): item is NonNullable<typeof item> => Boolean(item));

  return (
    <View style={{ width: visibleWidth, height: visibleHeight, overflow: "hidden" }}>
      <View
        style={{
          position: "absolute",
          left: -CONTENT_CROP.x * scale,
          top: -CONTENT_CROP.y * scale,
          width: rawWidth,
          height: rawHeight,
        }}
      >
        <Image
          resizeMode="contain"
          source={AVATAR_BASE_STAND}
          style={[
            styles.layer,
            { top: RAW_BASE_TOP_OFFSET * scale, width: rawWidth, height: RAW_BASE_HEIGHT * scale },
          ]}
        />
        {layers.map((item) => (
          <Image
            key={item.key}
            resizeMode="contain"
            source={item.source}
            style={[styles.layer, { top: 0, width: rawWidth, height: rawHeight }]}
          />
        ))}
      </View>
    </View>
  );
}

// 얼굴이 포함된 정사각형 영역(AvatarStage 출력 좌표계 기준). 헤어 상단부터 턱선까지.
const FACE_CROP = { x: 6, y: 26, width: 220, height: 220 };

type AvatarFaceThumbnailProps = {
  size: number;
};

/** 원형 썸네일 자리에 전신 아바타의 얼굴 부분만 잘라서 보여준다. */
export function AvatarFaceThumbnail({ size }: AvatarFaceThumbnailProps) {
  const scale = size / FACE_CROP.width;

  return (
    <View style={[styles.thumbnail, { width: size, height: size, borderRadius: size / 2 }]}>
      <View style={{ position: "absolute", left: -FACE_CROP.x * scale, top: -FACE_CROP.y * scale }}>
        <AvatarStage scale={scale} />
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  layer: {
    position: "absolute",
    left: 0,
  },
  thumbnail: {
    overflow: "hidden",
    backgroundColor: "rgba(255, 255, 255, 0.2)",
  },
});
