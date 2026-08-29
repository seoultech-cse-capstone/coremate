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
