import type { ImageSourcePropType } from "react-native";
import { Image, StyleSheet, View } from "react-native";

import {
  DEFAULT_LOCAL_KEY_BY_SLOT,
  LOCAL_CATALOG_BY_KEY,
  type LocalAvatarSlot,
  type LocalCatalogItem,
} from "../localCatalog";

// AVATAR_FE.md 스펙 기준. 1도트=8px, 좌표는 전부 px(도트x8)로 환산해서 쓴다.
// 코디 캔버스가 아바타 캔버스보다 위로 96px(12도트) 커서, 두 캔버스는 하단(발끝)
// 기준으로 정렬된다 — 베이스를 528 캔버스 안에 배치할 때 96px를 띄운다.
const RAW_WIDTH = 328;
const RAW_HEIGHT = 528;
const RAW_BASE_HEIGHT = 432;
const RAW_BASE_TOP_OFFSET = RAW_HEIGHT - RAW_BASE_HEIGHT;

// 정면 서있는 포즈 에셋(베이스+모든 코디)의 실제 내용 영역만 남기고 균일하게 잘라낸
// 여백(측정값: 좌우 48px, 위 64px, 아래 32px). 레이어 간 상대 위치는 그대로 두고
// 바깥쪽 뷰포트만 이 영역으로 좁힌다.
const CONTENT_CROP = { x: 48, y: 64, width: 232, height: 432 };

export const AVATAR_STAGE_WIDTH = CONTENT_CROP.width;
export const AVATAR_STAGE_HEIGHT = CONTENT_CROP.height;

// assets_template.md 4절: 아바타(베이스) 캔버스는 41x54도트, 중심 x=20도트,
// 발바닥 기준선 y=49도트(베이스 캔버스 자체 기준). 스테이지 좌표로 옮기면
// RAW_BASE_TOP_OFFSET(96px)만큼 아래로 내려가 있다. AvatarStage가 실제로
// 그리는 건 CONTENT_CROP만큼 잘라낸 영역이라, 배치 쪽(RoomScene)에서 "발이
// 어디 있는지" 알 수 있도록 그 잘라낸 좌표계 기준 비율로 내보낸다.
const RAW_FOOT_CENTER_X = 20 * 8;
const RAW_FOOT_Y = RAW_BASE_TOP_OFFSET + 49 * 8;
export const AVATAR_FOOT_X_RATIO = (RAW_FOOT_CENTER_X - CONTENT_CROP.x) / AVATAR_STAGE_WIDTH;
export const AVATAR_FOOT_Y_RATIO = (RAW_FOOT_Y - CONTENT_CROP.y) / AVATAR_STAGE_HEIGHT;

const AVATAR_BASE_STAND_FRAMES: ImageSourcePropType[] = [
  require("../../../../assets/avatar/base/avatar_base_stand_1.png"),
  require("../../../../assets/avatar/base/avatar_base_stand_2.png"),
  require("../../../../assets/avatar/base/avatar_base_stand_3.png"),
];
const AVATAR_BASE_STAND_BACK: ImageSourcePropType = require("../../../../assets/avatar/base/avatar_base_stand_back.png");
// 책상 씬 전용. avatar_sit.png는 서있는 포즈와 같은 41x54 캔버스를 쓴다.
const AVATAR_SIT_BASE: ImageSourcePropType = require("../../../../assets/avatar/base/avatar_sit.png");
// 타이핑 손 오버레이. 코디(528 캔버스)가 아니라 베이스(432)와 같은 캔버스라
// top 오프셋도 베이스와 동일하게 RAW_BASE_TOP_OFFSET을 쓴다.
const AVATAR_SIT_HANDS: ImageSourcePropType[] = [
  require("../../../../assets/avatar/base/avatar_sit_hands_1.png"),
  require("../../../../assets/avatar/base/avatar_sit_hands_2.png"),
  require("../../../../assets/avatar/base/avatar_sit_hands_3.png"),
];

// 머리에 붙는 코디(모자·머리·눈·입·안경)는 idle 프레임마다 미세하게 위아래로
// 흔들린다(head_dy, 도트 단위 → x8 = px). 몸에 붙는 코디(상의·하의·신발·가방)는 항상 0.
const HEAD_ATTACH_SLOTS: LocalAvatarSlot[] = ["HAT", "HAIR", "EYES", "MOUTH", "EYEWEAR"];
const HEAD_DY_PX_BY_FRAME: Record<1 | 2 | 3, number> = { 1: 8, 2: 0, 3: -8 };
// 뒷모습에서 전용(_back) 파일이 없어 정면 파일을 재사용하는 머리 코디에 주는 보정.
const HEAD_DY_PX_BACK_FALLBACK = 8;
// 앉기 중엔 프레임 변화가 없어 머리 부착 코디는 고정 +1도트만 준다.
const HEAD_DY_PX_SIT = 8;
// 앉기 중 신발은 (0, -3도트) 시프트해서 재사용한다(다리 각도가 바뀌어서).
const SHOES_DY_PX_SIT = -24;

// 서있는 정면: 아바타 → 신발 → 하의 → 상의 → 가방 → 머리 → 눈 → 입 → 안경 → 모자.
const FRONT_LAYER_ORDER: LocalAvatarSlot[] = [
  "SHOES",
  "BOTTOM",
  "TOP",
  "BAG",
  "HAIR",
  "EYES",
  "MOUTH",
  "EYEWEAR",
  "HAT",
];
// 서있는 뒷모습: 눈·입·안경은 그리지 않는다. 가방이 머리카락 위로 오는 순서.
const BACK_LAYER_ORDER: LocalAvatarSlot[] = ["SHOES", "BOTTOM", "TOP", "HAIR", "BAG", "HAT"];
// 책상 앉기: 가방은 앉는 동안 숨긴다.
const SIT_LAYER_ORDER: LocalAvatarSlot[] = [
  "SHOES",
  "BOTTOM",
  "TOP",
  "HAIR",
  "EYES",
  "MOUTH",
  "EYEWEAR",
  "HAT",
];

type AvatarMode = "front" | "back" | "sit";

type AvatarStageProps = {
  scale?: number;
  /** 부위별로 입힐 로컬 카탈로그 아이템 key. 생략한 부위는 기본값(필수 부위) 또는 미착용(선택 부위). */
  equipped?: Partial<Record<LocalAvatarSlot, string | null>>;
  /** 서있는 정면 idle 프레임(1~3). facing="back"이거나 activity="sit"일 땐 무시된다. */
  frame?: 1 | 2 | 3;
  facing?: "front" | "back";
  /** "sit"이면 책상 앉기 포즈(항상 정면)로 그린다 — facing보다 우선한다. */
  activity?: "stand" | "sit";
  /** 앉기 중 타이핑 손 오버레이 프레임(1~3). activity="sit"일 때만 쓴다. */
  handFrame?: 1 | 2 | 3;
};

type ResolvedLayer = {
  key: string;
  source: ImageSourcePropType;
  dyPx: number;
};

function resolveLayers(
  order: LocalAvatarSlot[],
  keyBySlot: Partial<Record<LocalAvatarSlot, string>>,
  mode: AvatarMode,
  frame: 1 | 2 | 3,
): ResolvedLayer[] {
  return order
    .map((slot) => keyBySlot[slot])
    .filter((key): key is string => Boolean(key))
    .map((key) => LOCAL_CATALOG_BY_KEY.get(key))
    .filter((item): item is LocalCatalogItem => Boolean(item))
    .map((item) => {
      const isHeadAttach = HEAD_ATTACH_SLOTS.includes(item.slot);

      if (mode === "back") {
        if (item.backSource) {
          return { key: item.key, source: item.backSource, dyPx: 0 };
        }
        // 뒷모습 전용 파일이 없으면 정면 파일을 그대로 재사용한다(폴백).
        return { key: item.key, source: item.source, dyPx: isHeadAttach ? HEAD_DY_PX_BACK_FALLBACK : 0 };
      }

      if (mode === "sit") {
        const source = item.sitSource ?? item.source;
        const dyPx = isHeadAttach ? HEAD_DY_PX_SIT : item.slot === "SHOES" ? SHOES_DY_PX_SIT : 0;
        return { key: item.key, source, dyPx };
      }

      return {
        key: item.key,
        source: item.source,
        dyPx: isHeadAttach ? HEAD_DY_PX_BY_FRAME[frame] : 0,
      };
    });
}

/** 여백을 잘라낸 캔버스에 아바타를 합성해 보여준다. */
export function AvatarStage({
  scale = 1,
  equipped,
  frame = 1,
  facing = "front",
  activity = "stand",
  handFrame = 1,
}: AvatarStageProps) {
  const visibleWidth = AVATAR_STAGE_WIDTH * scale;
  const visibleHeight = AVATAR_STAGE_HEIGHT * scale;
  const rawWidth = RAW_WIDTH * scale;
  const rawHeight = RAW_HEIGHT * scale;

  const mode: AvatarMode = activity === "sit" ? "sit" : facing;
  const layerOrder = mode === "back" ? BACK_LAYER_ORDER : mode === "sit" ? SIT_LAYER_ORDER : FRONT_LAYER_ORDER;

  const keyBySlot: Partial<Record<LocalAvatarSlot, string>> = { ...DEFAULT_LOCAL_KEY_BY_SLOT };
  for (const slot of layerOrder) {
    if (equipped && slot in equipped) {
      const key = equipped[slot];
      if (key) {
        keyBySlot[slot] = key;
      } else {
        delete keyBySlot[slot];
      }
    }
  }

  const layers = resolveLayers(layerOrder, keyBySlot, mode, frame);
  const baseSource = mode === "back" ? AVATAR_BASE_STAND_BACK : AVATAR_SIT_BASE;

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
        {/* 프레임 애니메이션은 source를 바꿔치기하지 않고, 모든 프레임을 겹쳐 그려둔 채
            opacity만 바꾼다 — 이미지 소스를 매번 바꾸면 네이티브에서 다시 디코드하며
            한 프레임씩 깜빡이는 게 눈에 띈다(특히 손 오버레이처럼 120ms로 빠를 때). */}
        {mode === "front" ? (
          AVATAR_BASE_STAND_FRAMES.map((source, index) => (
            <Image
              key={`base-${index}`}
              resizeMode="contain"
              source={source}
              style={[
                styles.layer,
                {
                  top: RAW_BASE_TOP_OFFSET * scale,
                  width: rawWidth,
                  height: RAW_BASE_HEIGHT * scale,
                  opacity: index + 1 === frame ? 1 : 0,
                },
              ]}
            />
          ))
        ) : (
          <Image
            resizeMode="contain"
            source={baseSource}
            style={[
              styles.layer,
              { top: RAW_BASE_TOP_OFFSET * scale, width: rawWidth, height: RAW_BASE_HEIGHT * scale },
            ]}
          />
        )}
        {layers.map((layer) => (
          <Image
            key={layer.key}
            resizeMode="contain"
            source={layer.source}
            style={[styles.layer, { top: layer.dyPx * scale, width: rawWidth, height: rawHeight }]}
          />
        ))}
        {mode === "sit"
          ? AVATAR_SIT_HANDS.map((source, index) => (
              <Image
                key={`hand-${index}`}
                resizeMode="contain"
                source={source}
                style={[
                  styles.layer,
                  {
                    top: RAW_BASE_TOP_OFFSET * scale,
                    width: rawWidth,
                    height: RAW_BASE_HEIGHT * scale,
                    opacity: index + 1 === handFrame ? 1 : 0,
                  },
                ]}
              />
            ))
          : null}
      </View>
    </View>
  );
}

// 수면 캔버스는 서있기·앉기(41x54도트)와 전혀 다른 48x27도트(384x216px) 캔버스라
// 별도로 다룬다. 모든 레이어가 같은 원점(0,0)에 겹치므로 오프셋 계산이 필요 없다.
export const AVATAR_SLEEP_STAGE_WIDTH = 384;
export const AVATAR_SLEEP_STAGE_HEIGHT = 216;

const AVATAR_SLEEP_BASE_FRAMES: ImageSourcePropType[] = [
  require("../../../../assets/avatar/base/avatar_sleep_1.png"),
  require("../../../../assets/avatar/base/avatar_sleep_2.png"),
  require("../../../../assets/avatar/base/avatar_sleep_3.png"),
];
// 눈·입은 장착 아이템과 무관하게 공용 파일로 대체한다(자는 동안은 표정이 고정).
const AVATAR_SLEEP_EYES: ImageSourcePropType = require("../../../../assets/avatar/items/eyes_basic_closed_sleep.png");
const AVATAR_SLEEP_MOUTH: ImageSourcePropType = require("../../../../assets/avatar/items/mouth_smile_open_sleep.png");

type AvatarSleepStageProps = {
  scale?: number;
  frame?: 1 | 2 | 3;
  /** 착용 중인 머리 key. _sleep 전용 파일이 있는 머리만 그린다(없으면 생략). */
  hairKey?: string;
};

/** 침대에 누운 모습. 모자·안경·상의·하의·신발·가방은 이불이 덮으므로 그리지 않는다. */
export function AvatarSleepStage({ scale = 1, frame = 1, hairKey }: AvatarSleepStageProps) {
  const width = AVATAR_SLEEP_STAGE_WIDTH * scale;
  const height = AVATAR_SLEEP_STAGE_HEIGHT * scale;
  const hairSleepSource = hairKey ? LOCAL_CATALOG_BY_KEY.get(hairKey)?.sleepSource : undefined;

  return (
    <View style={{ width, height }}>
      {/* source를 바꿔치기하지 않고 세 프레임을 다 겹쳐 그린 뒤 opacity만 바꾼다 —
          AvatarStage의 서있기 프레임과 같은 이유(이미지 재디코드로 인한 깜빡임 방지). */}
      {AVATAR_SLEEP_BASE_FRAMES.map((source, index) => (
        <Image
          key={index}
          resizeMode="contain"
          source={source}
          style={[styles.layer, { top: 0, width, height, opacity: index + 1 === frame ? 1 : 0 }]}
        />
      ))}
      {hairSleepSource ? (
        <Image resizeMode="contain" source={hairSleepSource} style={[styles.layer, { top: 0, width, height }]} />
      ) : null}
      <Image resizeMode="contain" source={AVATAR_SLEEP_EYES} style={[styles.layer, { top: 0, width, height }]} />
      <Image resizeMode="contain" source={AVATAR_SLEEP_MOUTH} style={[styles.layer, { top: 0, width, height }]} />
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
