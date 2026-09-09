import { Image, StyleSheet, View } from "react-native";

import { AVATAR_STAGE_HEIGHT, AvatarStage } from "./AvatarStage";
import { LOCAL_CATALOG_BY_KEY } from "../localCatalog";
import {
  FURNITURE_ITEM_STEM,
  FURNITURE_SETS,
  FURNITURE_TYPES,
  type FurnitureType,
} from "../roomFurniture";
import { ROOM_THEME_BY_KEY, ROOM_THEME_ITEM_STEM, type RoomTheme, type RoomThemeKey } from "../roomThemes";

// 서버 image_url stem → 종류 판별용 역매핑.
const STEM_TO_FURNITURE = new Map<string, FurnitureType>(
  FURNITURE_TYPES.map((type) => [FURNITURE_ITEM_STEM[type], type]),
);
const STEM_TO_THEME = new Map<string, RoomThemeKey>(
  (Object.entries(ROOM_THEME_ITEM_STEM) as [RoomThemeKey, string][]).map(([key, stem]) => [stem, key]),
);

function stemFromImageUrl(imageUrl: string | null): string | null {
  if (!imageUrl) {
    return null;
  }
  const fileName = imageUrl.split("/").pop();
  return fileName ? fileName.replace(/\.[^.]+$/, "") : null;
}

/** 서버 image_url 이 로컬에서 그릴 수 있는 아이템인지. 리빌에서 폴백 여부 판단용. */
export function canRenderRewardVisual(imageUrl: string | null): boolean {
  const stem = stemFromImageUrl(imageUrl);
  if (!stem) {
    return false;
  }
  return LOCAL_CATALOG_BY_KEY.has(stem) || STEM_TO_FURNITURE.has(stem) || STEM_TO_THEME.has(stem);
}

// 가구 세트를 조각들의 상대 오프셋대로 겹쳐 단독으로 그린다(방 격자와 무관).
function FurnitureSprite({ type, size }: { type: FurnitureType; size: number }) {
  const set = FURNITURE_SETS[type];
  const scale = size / Math.max(set.widthDots, set.heightDots);
  return (
    <View style={{ width: set.widthDots * scale, height: set.heightDots * scale }}>
      {set.pieces.map((piece, index) => (
        <Image
          key={index}
          source={piece.source}
          resizeMode="contain"
          style={{
            position: "absolute",
            left: (piece.offsetXDots - set.minXDots) * scale,
            top: (piece.offsetYDots - set.minYDots) * scale,
            width: piece.widthDots * scale,
            height: piece.heightDots * scale,
          }}
        />
      ))}
    </View>
  );
}

// 벽지는 벽+바닥 한 쌍이라 네모난 방 미리보기로 보여준다.
function WallpaperSquare({ theme, size }: { theme: RoomTheme; size: number }) {
  return (
    <View style={[styles.wallpaper, { width: size, height: size }]}>
      <Image source={theme.wallSource} resizeMode="cover" style={styles.wallpaperWall} />
      <Image source={theme.floorSource} resizeMode="cover" style={styles.wallpaperFloor} />
    </View>
  );
}

/**
 * 랜덤박스 보상 아이템을 종류별로 그린다.
 *   - 착용 아바타 아이템: 기본 아바타에 덧입혀서 (AvatarStage)
 *   - 펫: 스프라이트 단독
 *   - 가구: 세트 조합 단독
 *   - 벽지: 네모난 방 미리보기
 * 매핑되는 로컬 에셋이 없으면 null(호출부에서 폴백).
 */
export function RewardItemVisual({ imageUrl, size }: { imageUrl: string | null; size: number }) {
  const stem = stemFromImageUrl(imageUrl);
  if (!stem) {
    return null;
  }

  const avatarItem = LOCAL_CATALOG_BY_KEY.get(stem);
  if (avatarItem) {
    if (avatarItem.slot === "PET") {
      return <Image source={avatarItem.source} resizeMode="contain" style={{ width: size, height: size }} />;
    }
    // 기본 아바타 마네킹에 이 아이템만 착용시켜 보여준다.
    return <AvatarStage equipped={{ [avatarItem.slot]: avatarItem.key }} scale={size / AVATAR_STAGE_HEIGHT} />;
  }

  const furnitureType = STEM_TO_FURNITURE.get(stem);
  if (furnitureType) {
    return <FurnitureSprite type={furnitureType} size={size} />;
  }

  const themeKey = STEM_TO_THEME.get(stem);
  if (themeKey) {
    const theme = ROOM_THEME_BY_KEY.get(themeKey);
    if (theme) {
      return <WallpaperSquare theme={theme} size={size} />;
    }
  }

  return null;
}

const styles = StyleSheet.create({
  wallpaper: {
    borderRadius: 14,
    overflow: "hidden",
  },
  wallpaperWall: {
    width: "100%",
    height: "55%",
  },
  wallpaperFloor: {
    width: "100%",
    height: "45%",
  },
});
