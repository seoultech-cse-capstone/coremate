import type { ImageSourcePropType } from "react-native";

// 백엔드 아이템 카탈로그가 아직 비어있어(#165 0단계 미확정) 실제 itemId/itemName과
// 매칭할 방법이 없다. 그동안 프론트에서 코디 조합을 테스트해볼 수 있도록, 받아둔
// 로컬 스프라이트를 임시로 나열한 목록이다 — 실제 카탈로그가 정해지면 통째로 교체한다.
export type LocalAvatarSlot =
  | "HAT"
  | "HAIR"
  | "EYES"
  | "EYEWEAR"
  | "MOUTH"
  | "TOP"
  | "BOTTOM"
  | "SHOES"
  | "BAG";

export type LocalCatalogItem = {
  key: string;
  slot: LocalAvatarSlot;
  label: string;
  source: ImageSourcePropType;
  /** 뒷모습 전용 파일(있는 아이템만). 없으면 뒷모습에서도 정면 파일을 그대로 재사용한다. */
  backSource?: ImageSourcePropType;
  /** 책상 앉기 전용 파일(top/bottom만 있음). 없으면 앉기에서도 정면 파일을 그대로 재사용한다. */
  sitSource?: ImageSourcePropType;
  /** 수면 전용 파일(hair만 있음). 없으면 수면 중엔 그리지 않는다. */
  sleepSource?: ImageSourcePropType;
};

// 항상 무언가 착용돼 있어야 하는 부위. 나머지(HAT/EYEWEAR/SHOES/BAG)는 "없음"이 가능하다.
export const REQUIRED_LOCAL_SLOTS: LocalAvatarSlot[] = ["HAIR", "EYES", "MOUTH", "TOP", "BOTTOM"];

export const LOCAL_SLOT_LABEL: Record<LocalAvatarSlot, string> = {
  HAT: "모자",
  HAIR: "머리",
  EYES: "눈",
  EYEWEAR: "안경",
  MOUTH: "입",
  TOP: "상의",
  BOTTOM: "하의",
  SHOES: "신발",
  BAG: "가방",
};

export const LOCAL_SLOT_ORDER: LocalAvatarSlot[] = [
  "HAIR",
  "EYES",
  "MOUTH",
  "TOP",
  "BOTTOM",
  "HAT",
  "EYEWEAR",
  "SHOES",
  "BAG",
];

export const LOCAL_AVATAR_CATALOG: LocalCatalogItem[] = [
  {
    key: "hair_basic",
    slot: "HAIR",
    label: "기본 머리",
    source: require("../../../assets/avatar/items/hair_basic.png"),
    backSource: require("../../../assets/avatar/items/hair_basic_back.png"),
    sleepSource: require("../../../assets/avatar/items/hair_basic_sleep.png"),
  },
  {
    key: "hair_long_black",
    slot: "HAIR",
    label: "긴 검은 머리",
    source: require("../../../assets/avatar/items/hair_long_black.png"),
    backSource: require("../../../assets/avatar/items/hair_long_black_back.png"),
    sleepSource: require("../../../assets/avatar/items/hair_long_black_sleep.png"),
  },
  {
    key: "eyes_basic",
    slot: "EYES",
    label: "기본 눈",
    source: require("../../../assets/avatar/items/eyes_basic.png"),
  },
  {
    key: "eyes_blue",
    slot: "EYES",
    label: "파란 눈",
    source: require("../../../assets/avatar/items/eyes_blue.png"),
  },
  {
    key: "mouth_basic",
    slot: "MOUTH",
    label: "기본 입",
    source: require("../../../assets/avatar/items/mouth_basic.png"),
  },
  {
    key: "mouth_smile_open",
    slot: "MOUTH",
    label: "웃는 입",
    source: require("../../../assets/avatar/items/mouth_smile_open.png"),
  },
  {
    key: "top_basic",
    slot: "TOP",
    label: "기본 상의",
    source: require("../../../assets/avatar/items/top_basic.png"),
    sitSource: require("../../../assets/avatar/items/top_basic_sit.png"),
  },
  {
    key: "top_hoodie",
    slot: "TOP",
    label: "후드티",
    source: require("../../../assets/avatar/items/top_hoodie.png"),
    backSource: require("../../../assets/avatar/items/top_hoodie_back.png"),
    sitSource: require("../../../assets/avatar/items/top_hoodie_sit.png"),
  },
  {
    key: "top_hoodie_dev",
    slot: "TOP",
    label: "개발자 후드티",
    source: require("../../../assets/avatar/items/top_hoodie_dev.png"),
    backSource: require("../../../assets/avatar/items/top_hoodie_dev_back.png"),
  },
  {
    key: "top_flannel",
    slot: "TOP",
    label: "플란넬 셔츠",
    source: require("../../../assets/avatar/items/top_flannel.png"),
    backSource: require("../../../assets/avatar/items/top_flannel_back.png"),
  },
  {
    key: "top_tshirt_graphic",
    slot: "TOP",
    label: "그래픽 티셔츠",
    source: require("../../../assets/avatar/items/top_tshirt_graphic.png"),
  },
  {
    key: "bottom_basic",
    slot: "BOTTOM",
    label: "기본 하의",
    source: require("../../../assets/avatar/items/bottom_basic.png"),
    sitSource: require("../../../assets/avatar/items/bottom_basic_sit.png"),
  },
  {
    key: "bottom_jeans",
    slot: "BOTTOM",
    label: "청바지",
    source: require("../../../assets/avatar/items/bottom_jeans.png"),
    sitSource: require("../../../assets/avatar/items/bottom_jeans_sit.png"),
  },
  {
    key: "bottom_joggers",
    slot: "BOTTOM",
    label: "조거팬츠",
    source: require("../../../assets/avatar/items/bottom_joggers.png"),
    backSource: require("../../../assets/avatar/items/bottom_joggers_back.png"),
  },
  {
    key: "bottom_slacks",
    slot: "BOTTOM",
    label: "슬랙스",
    source: require("../../../assets/avatar/items/bottom_slacks.png"),
    backSource: require("../../../assets/avatar/items/bottom_slacks_back.png"),
  },
  {
    key: "bottom_shorts_socks",
    slot: "BOTTOM",
    label: "반바지",
    source: require("../../../assets/avatar/items/bottom_shorts_socks.png"),
    backSource: require("../../../assets/avatar/items/bottom_shorts_socks_back.png"),
  },
  {
    key: "hat_cap",
    slot: "HAT",
    label: "야구모자",
    source: require("../../../assets/avatar/items/hat_cap.png"),
    backSource: require("../../../assets/avatar/items/hat_cap_back.png"),
  },
  {
    key: "hat_wizard",
    slot: "HAT",
    label: "마법사 모자",
    source: require("../../../assets/avatar/items/hat_wizard.png"),
  },
  {
    key: "hat_beanie",
    slot: "HAT",
    label: "비니",
    source: require("../../../assets/avatar/items/hat_beanie.png"),
    backSource: require("../../../assets/avatar/items/hat_beanie_back.png"),
  },
  {
    key: "hat_headset",
    slot: "HAT",
    label: "헤드셋",
    source: require("../../../assets/avatar/items/hat_headset.png"),
    backSource: require("../../../assets/avatar/items/hat_headset_back.png"),
  },
  {
    key: "glasses_horn",
    slot: "EYEWEAR",
    label: "뿔테 안경",
    source: require("../../../assets/avatar/items/glasses_horn.png"),
  },
  {
    key: "glasses_sun",
    slot: "EYEWEAR",
    label: "선글라스",
    source: require("../../../assets/avatar/items/glasses_sun.png"),
  },
  {
    key: "shoes_sneakers",
    slot: "SHOES",
    label: "운동화",
    source: require("../../../assets/avatar/items/shoes_sneakers.png"),
  },
  {
    key: "shoes_boots",
    slot: "SHOES",
    label: "부츠",
    source: require("../../../assets/avatar/items/shoes_boots.png"),
  },
  {
    key: "bag_backpack",
    slot: "BAG",
    label: "백팩",
    source: require("../../../assets/avatar/items/bag_backpack.png"),
    backSource: require("../../../assets/avatar/items/bag_backpack_back.png"),
  },
];

export const LOCAL_CATALOG_BY_KEY = new Map(LOCAL_AVATAR_CATALOG.map((item) => [item.key, item]));

export const DEFAULT_LOCAL_KEY_BY_SLOT: Partial<Record<LocalAvatarSlot, string>> = {
  HAIR: "hair_basic",
  EYES: "eyes_basic",
  MOUTH: "mouth_basic",
  TOP: "top_basic",
  BOTTOM: "bottom_basic",
};
