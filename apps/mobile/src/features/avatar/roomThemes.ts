import type { ImageSourcePropType } from "react-native";

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
    wallSource: require("../../../assets/room/room_wall_basic.png"),
    floorSource: require("../../../assets/room/room_floor_basic.png"),
  },
  {
    key: "herringbone",
    label: "헤링본",
    wallSource: require("../../../assets/room/room_wall_herringbone.png"),
    floorSource: require("../../../assets/room/room_floor_herringbone.png"),
  },
];

export const DEFAULT_ROOM_THEME_KEY: RoomThemeKey = "basic";

export const ROOM_THEME_BY_KEY = new Map(ROOM_THEMES.map((theme) => [theme.key, theme]));

// assets_template.md 3절: 두 테마 모두 같은 도트 규격(가로 112도트 = 7칸x16도트).
// 바닥은 9줄(108도트, 격자6줄+여유 36도트), 벽은 96도트. dotPx로 표시 크기를 구한다.
export const FLOOR_DOT_HEIGHT = 108;
export const WALL_DOT_HEIGHT = 96;
// 벽 art가 필요한 높이보다 짧을 때(화면이 세로로 아주 긴 기기) 위쪽을 채우는 색 — 벽 상단과 동일.
export const WALL_EXTEND_COLOR = "#E9DEC6";
