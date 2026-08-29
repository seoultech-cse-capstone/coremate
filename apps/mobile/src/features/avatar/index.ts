export { avatarApi } from "./avatarApi";
export { useMyAvatar, useUpdateEquippedItemsMutation } from "./useAvatarQueries";
export { AvatarStage, AvatarFaceThumbnail } from "./components/AvatarStage";
export { RoomScene } from "./components/RoomScene";
export { RoomThemeProvider, useRoomTheme } from "./RoomThemeContext";
export { ROOM_THEMES } from "./roomThemes";
export type { RoomTheme, RoomThemeKey } from "./roomThemes";
export { ROOM_GRID_COLS, ROOM_GRID_ROWS, getRoomGridLayout, getRoomGridCellRect } from "./roomGrid";
export type { RoomGridLayout } from "./roomGrid";
export {
  DEFAULT_LOCAL_KEY_BY_SLOT,
  LOCAL_AVATAR_CATALOG,
  LOCAL_SLOT_LABEL,
  LOCAL_SLOT_ORDER,
  REQUIRED_LOCAL_SLOTS,
} from "./localCatalog";
export type { LocalAvatarSlot, LocalCatalogItem } from "./localCatalog";
export { inventoryApi } from "./inventoryApi";
export { useInventory } from "./useInventoryQueries";
export { REQUIRED_AVATAR_SLOTS } from "./types";
export type {
  AvatarEquippedItem,
  AvatarItem,
  AvatarItemType,
  AvatarProfile,
  AvatarSlot,
  ItemRarity,
  RoomPose,
  UpdateEquippedItemsRequest,
  UserAvatar,
} from "./types";
export type { Inventory, InventoryGroup, InventoryItem } from "./inventoryTypes";
