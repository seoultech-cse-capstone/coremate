import type { ImageSourcePropType } from "react-native";
import { Image, StyleSheet, View } from "react-native";

// 스프라이트 시트 규격(픽셀 아바타 스펙북 08/26 기준): 코디 레이어 캔버스는
// 328x528 이고, 캐릭터 베이스는 328x432 로 더 작게 잘려 있다. 두 캔버스는
// 하단(발끝)을 기준으로 정렬되도록 만들어졌기 때문에, 베이스를 528 캔버스
// 안에 배치할 때 위쪽으로 (528-432)=96px 를 띄워야 코디 아이템과 맞는다.
export const AVATAR_STAGE_WIDTH = 328;
export const AVATAR_STAGE_HEIGHT = 528;
const AVATAR_BASE_HEIGHT = 432;
const AVATAR_BASE_TOP_OFFSET = AVATAR_STAGE_HEIGHT - AVATAR_BASE_HEIGHT;

// 정면 서있는 포즈의 이목구비/의상 기본값(is_default 아이템에 대응).
// 머리·눈·입·상의·하의는 항상 무언가 착용돼 있어야 하므로 기본 에셋을 둔다.
const AVATAR_BASE_STAND: ImageSourcePropType = require("../../../../assets/avatar/base/avatar_base_stand_1.png");

const AVATAR_BASIC_LAYERS: ImageSourcePropType[] = [
  require("../../../../assets/avatar/items/bottom_basic.png"),
  require("../../../../assets/avatar/items/top_basic.png"),
  require("../../../../assets/avatar/items/eyes_basic.png"),
  require("../../../../assets/avatar/items/mouth_basic.png"),
  require("../../../../assets/avatar/items/hair_basic.png"),
];

type AvatarStageProps = {
  scale?: number;
};

/** 328x528 캔버스에 기본 아바타(서있는 포즈)를 합성해 보여준다. */
export function AvatarStage({ scale = 1 }: AvatarStageProps) {
  const width = AVATAR_STAGE_WIDTH * scale;
  const height = AVATAR_STAGE_HEIGHT * scale;

  return (
    <View style={{ width, height }}>
      <Image
        resizeMode="contain"
        source={AVATAR_BASE_STAND}
        style={[
          styles.layer,
          { top: AVATAR_BASE_TOP_OFFSET * scale, width, height: AVATAR_BASE_HEIGHT * scale },
        ]}
      />
      {AVATAR_BASIC_LAYERS.map((source, index) => (
        <Image
          key={index}
          resizeMode="contain"
          source={source}
          style={[styles.layer, { top: 0, width, height }]}
        />
      ))}
    </View>
  );
}

// 얼굴이 포함된 정사각형 영역(스테이지 좌표계 기준). 헤어 상단부터 턱선까지.
const FACE_CROP = { x: 54, y: 90, width: 220, height: 220 };

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
