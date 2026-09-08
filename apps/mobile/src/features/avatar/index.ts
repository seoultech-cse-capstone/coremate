export { avatarApi } from "./avatarApi";
export { useMyAvatar, useUpdateEquippedItemsMutation } from "./useAvatarQueries";
export { AvatarStage, AvatarFaceThumbnail } from "./components/AvatarStage";
export { RoomScene } from "./components/RoomScene";
export { RoomBackdrop } from "./components/RoomBackdrop";
export type { RoomLayoutInfo } from "./components/RoomBackdrop";
export { RoomThemeProvider, useRoomTheme } from "./RoomThemeContext";
export { AvatarEquipProvider, useAvatarEquip } from "./AvatarEquipContext";
export type { EquippedMap } from "./AvatarEquipContext";
export { ROOM_THEMES, ROOM_THEME_ITEM_STEM } from "./roomThemes";
export type { RoomTheme, RoomThemeKey } from "./roomThemes";
export { ROOM_GRID_COLS, ROOM_GRID_ROWS, getRoomGridLayout, getRoomGridCellRect } from "./roomGrid";
export type { RoomGridLayout } from "./roomGrid";
export { RoomFurnitureProvider, useRoomFurniture } from "./RoomFurnitureContext";
export type { FurniturePosition } from "./RoomFurnitureContext";
export { FURNITURE_SETS, FURNITURE_TYPES, FURNITURE_ITEM_STEM } from "./roomFurniture";
export type { FurnitureSet, FurnitureType } from "./roomFurniture";
export {
  DEFAULT_LOCAL_KEY_BY_SLOT,
  LOCAL_AVATAR_CATALOG,
  LOCAL_CATALOG_BY_KEY,
  LOCAL_SLOT_LABEL,
  LOCAL_SLOT_ORDER,
  REQUIRED_LOCAL_SLOTS,
} from "./localCatalog";
export type { LocalAvatarSlot, LocalCatalogItem } from "./localCatalog";
export { inventoryApi } from "./inventoryApi";
export { useInventory } from "./useInventoryQueries";
export { roomApi } from "./roomApi";
export { useMyRoom, useSavePlacementsMutation, useSavePoseMutation } from "./useRoomQueries";
export type { RoomPlacement, RoomResponse, SaveRoomPlacementsRequest, SaveRoomPoseRequest } from "./roomTypes";
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
