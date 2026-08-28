export { avatarApi } from "./avatarApi";
export { useMyAvatar, useUpdateEquippedItemsMutation } from "./useAvatarQueries";
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
