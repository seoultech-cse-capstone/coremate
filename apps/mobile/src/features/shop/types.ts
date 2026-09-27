// com.coremate.backend.shop 도메인 계약과 1:1로 대응하는 타입.

/** 상점에 노출되는 상품 3종(랜덤박스/닉네임변경권/성별변경권-폐기 예정) 중 서버가 실제로 돌려주는 값. */
export type ShopProductType =
  | "RANDOM_BOX"
  | "GENDER_CHANGE"
  | "NICKNAME_CHANGE"
  | "COIN_PACKAGE"
  | "UNKNOWN";

export type ShopProduct = {
  shopProductId: number;
  productType: ShopProductType;
  itemId: number | null;
  randomBoxId: number | null;
  productName: string;
  priceCoin: number;
  requiredLevel: number;
  isActive: boolean;
  createdAt: string;
};

export type PurchaseShopProductRequest = {
  shopProductId: number;
  quantity?: number;
  /** 닉네임 변경권 구매 시 변경할 새 닉네임(최대 6글자). 서버가 필수로 요구한다. */
  newNickname?: string;
};

export type ShopPurchaseResult = {
  shopPurchaseId: number;
  userId: number;
  shopProductId: number;
  remainingCoin: number;
  purchasedAt: string;
};

export type RandomBoxGrade = string;

export type RandomBox = {
  randomBoxId: number;
  boxName: string;
  boxGrade: RandomBoxGrade;
  requiredLevel: number;
  priceCoin: number;
  isActive: boolean;
};

export type RandomBoxItem = {
  itemId: number | null;
  itemName: string | null;
  /** 아이템 스프라이트 매핑용 image_url. 코인 보상은 null */
  imageUrl: string | null;
  /** "ITEM" | "COIN" */
  rewardType: string;
  coinAmount: number | null;
  /** 0~1 사이 소수를 문자열로 준다(예: "0.250000") */
  probability: string;
};

export type RandomBoxOdds = {
  /** 아이템 등급(COMMON/RARE/EPIC/LEGENDARY) 또는 코인 보상(COIN) */
  grade: string;
  probability: string;
};

export type OpenRandomBoxRequest = {
  randomBoxId: number;
};

export type RandomBoxOpenResult = {
  randomBoxOpenLogId: number;
  userId: number;
  randomBoxId: number;
  /** "ITEM" | "COIN" */
  rewardType: string;
  rewardItemId: number | null;
  rewardCoinAmount: number | null;
  /** 보상 등급. 아이템은 COMMON/RARE/EPIC/LEGENDARY, 코인 보상은 "COIN". 상자 들썩임 연출의 빛 색에 쓴다. */
  rewardRarity: string | null;
  openedAt: string;
};
