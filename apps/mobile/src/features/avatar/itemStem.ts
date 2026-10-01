import type { EquippedMap } from "./AvatarEquipContext";
import { LOCAL_CATALOG_BY_KEY } from "./localCatalog";
import type { AvatarEquippedItem } from "./types";

// 서버 아이템 image_url("/assets/avatar/hair_basic.png")에서 파일명 stem("hair_basic")만
// 뽑아낸다. 이 stem이 로컬 카탈로그의 key/가구·테마 매핑과 이어지는 유일한 값이다.
export function stemFromImageUrl(imageUrl: string | null): string | null {
  if (!imageUrl) {
    return null;
  }
  const fileName = imageUrl.split("/").pop();
  return fileName ? fileName.replace(/\.[^.]+$/, "") : null;
}

/**
 * 서버 장착 목록을 AvatarStage가 받는 부위별 로컬 key로 바꾼다. 친구처럼 이 기기에
 * 착용 기록이 없는 사람의 아바타를 그릴 때 쓴다. 앱에 그림이 없는 아이템과 벽지 같은
 * 몸 밖 슬롯은 빠지고, 빠진 부위는 AvatarStage가 기본 착용(필수 부위) 또는 미착용으로 그린다.
 */
export function toEquippedMap(equippedItems: AvatarEquippedItem[]): EquippedMap {
  const equipped: EquippedMap = {};
  for (const equippedItem of equippedItems) {
    const stem = stemFromImageUrl(equippedItem.imageUrl);
    const localItem = stem ? LOCAL_CATALOG_BY_KEY.get(stem) : undefined;
    if (localItem && localItem.slot === equippedItem.slot) {
      equipped[localItem.slot] = localItem.key;
    }
  }
  return equipped;
}
