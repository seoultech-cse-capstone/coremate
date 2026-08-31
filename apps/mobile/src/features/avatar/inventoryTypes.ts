import type { AvatarItemType, AvatarSlot, ItemRarity } from "./types";

export type InventoryItem = {
  userItemId: number;
  itemId: number;
  itemName: string;
  itemType: AvatarItemType;
  /** 장착 슬롯. FURNITURE는 null */
  slot: AvatarSlot | null;
  rarity: ItemRarity;
  imageUrl: string | null;
  /** 기본 지급 비매품 여부. 상점에도 랜덤박스에도 나오지 않는다 */
  defaultItem: boolean;
  /** 지금 장착 중인지. 가구는 항상 false이며 배치 개수로 판단한다 */
  equipped: boolean;
  quantity: number;
  /** 방에 놓인 개수. 가구만 값 있음 */
  placedCount: number | null;
  /** 아직 놓을 수 있는 개수(보유 - 배치). 가구만 값 있음 */
  remainingCount: number | null;
  metadata: unknown;
  acquiredAt: string;
};

export type InventoryGroup = {
  itemType: AvatarItemType;
  /** 이 부위의 장착 슬롯. FURNITURE는 슬롯이 없어 null */
  slot: AvatarSlot | null;
  count: number;
  items: InventoryItem[];
};

export type Inventory = {
  userId: number;
  totalCount: number;
  /** 13종이 항상 모두 내려온다(비어 있어도 빈 목록으로) */
  groups: InventoryGroup[];
};
