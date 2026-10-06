import type { ImageSourcePropType } from "react-native";

import { stemFromImageUrl } from "./itemStem";
import type { AvatarEquippedItem } from "./types";

// 벽지·바닥은 항상 세트로만 쓴다(따로 조합하지 않음) — 방 하나를 "테마" 단위로 고른다.
export type RoomThemeKey = "basic" | "herringbone";

export type RoomTheme = {
  key: RoomThemeKey;
  label: string;
  wallSource: ImageSourcePropType;
  floorSource: ImageSourcePropType;
};

export const ROOM_THEMES: RoomTheme[] = [
  {
    key: "basic",
    label: "베이직",
    wallSource: require("../../../assets/room/room_basic_wall.png"),
    floorSource: require("../../../assets/room/room_basic_floor.png"),
  },
  {
    key: "herringbone",
    label: "헤링본",
    wallSource: require("../../../assets/room/room_herringbone_wall.png"),
    floorSource: require("../../../assets/room/room_herringbone_floor.png"),
  },
];

export const DEFAULT_ROOM_THEME_KEY: RoomThemeKey = "basic";

// 각 테마가 대응하는 서버 WALLPAPER 아이템 image_url stem(벽지 파일명). 인벤토리
// 보유 목록과 로컬 테마를 잇는 키다 — require() 안의 파일명은 런타임에 못 읽어서
// 여기 명시한다.
export const ROOM_THEME_ITEM_STEM: Record<RoomThemeKey, string> = {
  basic: "room_basic_wall",
  herringbone: "room_herringbone_wall",
};

export const ROOM_THEME_BY_KEY = new Map(ROOM_THEMES.map((theme) => [theme.key, theme]));

// 서버 WALLPAPER image_url stem ↔ 로컬 테마 key.
const STEM_TO_THEME = new Map<string, RoomThemeKey>(
  (Object.entries(ROOM_THEME_ITEM_STEM) as [RoomThemeKey, string][]).map(([key, stem]) => [stem, key]),
);

/** 서버 벽지 아이템 image_url → 로컬 테마 key. 앱에 없는 테마면 undefined. */
export function themeKeyFromImageUrl(imageUrl: string | null): RoomThemeKey | undefined {
  const stem = stemFromImageUrl(imageUrl);
  return stem ? STEM_TO_THEME.get(stem) : undefined;
}

/** 서버 장착 목록의 WALLPAPER 슬롯 → 테마 key. 장착이 없거나 앱에 없는 테마면 기본 테마. */
export function themeKeyFromEquipped(equippedItems: AvatarEquippedItem[]): RoomThemeKey {
  const wallpaper = equippedItems.find((item) => item.slot === "WALLPAPER");
  return themeKeyFromImageUrl(wallpaper?.imageUrl ?? null) ?? DEFAULT_ROOM_THEME_KEY;
}

// assets_template.md 3절: 두 테마 모두 같은 도트 규격(가로 112도트 = 7칸x16도트).
// 바닥은 9줄(108도트, 격자6줄+여유 36도트), 벽은 96도트. dotPx로 표시 크기를 구한다.
export const FLOOR_DOT_HEIGHT = 108;
export const WALL_DOT_HEIGHT = 96;
// 벽 art가 필요한 높이보다 짧을 때(화면이 세로로 아주 긴 기기) 위쪽을 채우는 색 — 벽 상단과 동일.
export const WALL_EXTEND_COLOR = "#E9DEC6";
