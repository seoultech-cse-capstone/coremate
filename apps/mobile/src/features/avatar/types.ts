// com.coremate.backend.avatar 도메인 계약과 1:1로 대응하는 타입.

/** 아이템 마스터 종류 13종. 장착형 9종은 같은 이름의 AvatarSlot을 갖고, PET/WALLPAPER/FLOOR/FURNITURE는 스프라이트 스택에 안 속한다. */
export type AvatarItemType =
  | "HAT"
  | "HAIR"
  | "EYES"
  | "EYEWEAR"
  | "MOUTH"
  | "TOP"
  | "BOTTOM"
  | "SHOES"
  | "BAG"
  | "PET"
  | "WALLPAPER"
  | "FLOOR"
  | "FURNITURE";

/** 장착 슬롯 12종(FURNITURE만 슬롯 없음). HAIR/EYES/MOUTH/WALLPAPER/FLOOR는 필수라 해제 불가, 교체만 가능. */
export type AvatarSlot =
  | "HAT"
  | "HAIR"
  | "EYES"
  | "EYEWEAR"
  | "MOUTH"
  | "TOP"
  | "BOTTOM"
  | "SHOES"
  | "BAG"
  | "PET"
  | "WALLPAPER"
  | "FLOOR";

export const REQUIRED_AVATAR_SLOTS: readonly AvatarSlot[] = [
  "HAIR",
  "EYES",
  "MOUTH",
  "WALLPAPER",
  "FLOOR",
];

export type ItemRarity = "COMMON" | "RARE" | "EPIC" | "LEGENDARY";

/** 마지막에 취한 방 포즈. null은 STAND와 같다. */
export type RoomPose = "STAND" | "SIT_DESK" | "SLEEP_BED";

export type AvatarItem = {
  itemId: number;
  itemName: string;
  itemType: AvatarItemType;
  slot: string | null;
  rarity: string;
  imageUrl: string | null;
  description: string | null;
  isActive: boolean;
};

export type AvatarEquippedItem = {
  slot: AvatarSlot;
  userItemId: number;
  itemId: number;
  itemName: string;
  itemType: AvatarItemType;
  rarity: string;
  imageUrl: string | null;
  /** 정면 기준 겹쳐 그리는 순서. 스프라이트 스택이 아니면 null(PET/WALLPAPER/FLOOR). */
  renderOrder: number | null;
};

export type AvatarProfile = {
  userId: number;
  /** 베이스 마네킹. 성별 구분 없이 BASIC_01 고정 */
  baseAvatarType: string;
  equippedItems: AvatarEquippedItem[];
  roomPose: RoomPose | null;
  roomPoseUpdatedAt: string | null;
  /** 친구 방에 붙이는 경과 시간 라벨(예: "3일 전부터"). STAND거나 시각이 없으면 null. 접속 상태가 아니라 마지막 포즈 시각이다. */
  roomPoseElapsedText: string | null;
  updatedAt: string;
};

export type UserAvatar = {
  userId: number;
  profile: AvatarProfile;
  items: AvatarItem[];
};

export type UpdateEquippedItemsRequest = {
  equippedItems: {
    slot: AvatarSlot;
    /** null이면 해당 슬롯 장착 해제(필수 슬롯은 서버가 거부) */
    userItemId: number | null;
  }[];
};
