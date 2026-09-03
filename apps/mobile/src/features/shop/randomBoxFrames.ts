import type { ImageSourcePropType } from "react-native";

import chestAnim from "../../../assets/shop/random-box/chest_anim.json";

// 랜덤박스 열기 스프라이트. chest_anim.json 의 sequence 가 인덱스로 프레임을 가리키므로,
// require 는 인덱스 순서(0=closed ... 17=open)를 그대로 지켜야 한다.
export const RANDOM_BOX_FRAMES: ImageSourcePropType[] = [
  require("../../../assets/shop/random-box/00_closed.png"),
  require("../../../assets/shop/random-box/01_hop1_a.png"),
  require("../../../assets/shop/random-box/02_hop1_b.png"),
  require("../../../assets/shop/random-box/03_hop1_c.png"),
  require("../../../assets/shop/random-box/04_hop2_a.png"),
  require("../../../assets/shop/random-box/05_hop2_b.png"),
  require("../../../assets/shop/random-box/06_hop2_c.png"),
  require("../../../assets/shop/random-box/07_hop3_a.png"),
  require("../../../assets/shop/random-box/08_hop3_b.png"),
  require("../../../assets/shop/random-box/09_hop3_c.png"),
  require("../../../assets/shop/random-box/10_swing1.png"),
  require("../../../assets/shop/random-box/11_swing2.png"),
  require("../../../assets/shop/random-box/12_swing3.png"),
  require("../../../assets/shop/random-box/13_swing4.png"),
  require("../../../assets/shop/random-box/14_swing5.png"),
  require("../../../assets/shop/random-box/15_swing6.png"),
  require("../../../assets/shop/random-box/16_swing7.png"),
  require("../../../assets/shop/random-box/17_open.png"),
];

export const CLOSED_FRAME_INDEX = 0;
export const OPEN_FRAME_INDEX = RANDOM_BOX_FRAMES.length - 1;

/** 닫힌 상자 정지 이미지. 상점 카드 썸네일 등 애니메이션이 필요 없는 곳에서 재사용한다. */
export const CLOSED_CHEST_IMAGE = RANDOM_BOX_FRAMES[CLOSED_FRAME_INDEX];

// 스프라이트가 456px 캔버스 안에서 왼쪽에 고정(닫힌 상자 중심 x≈146, 캔버스 중심 228)돼 있어,
// contentFit 만으로는 화면에서 왼쪽으로 치우쳐 보인다. 표시 폭에 이 비율을 곱한 만큼
// 오른쪽으로 밀어 상자를 가운데로 맞춘다. (228-146)/456 ≈ 0.18
export const CHEST_CONTENT_OFFSET_RATIO = 0.18;

export type ChestSequenceStep = {
  frame: number;
  name: string;
  durationMs: number;
};

export const CHEST_SEQUENCE: ChestSequenceStep[] = chestAnim.sequence;
export const CHEST_FRAME_RATIO = chestAnim.frameWidth / chestAnim.frameHeight;
export const CHEST_TOTAL_MS: number = chestAnim.totalMs;
