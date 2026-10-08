import { Image, View } from "react-native";

import { FURNITURE_SETS, type FurnitureType } from "../roomFurniture";

/**
 * 가구 세트 전체(침대 프레임+이불, 의자+책상처럼 조각이 여럿인 것 포함)를 size×size 상자 안에
 * 비율을 지켜 그린다. 첫 조각 하나만 그리면 침대는 이불이, 책상은 상판이 빠져 보인다(#245).
 * 조각 순서가 곧 그리는 순서다(roomFurniture.ts 의 세트 정의 참고).
 */
export function FurnitureSetPreview({ type, size }: { type: FurnitureType; size: number }) {
  const set = FURNITURE_SETS[type];
  const scale = Math.min(size / set.widthDots, size / set.heightDots);

  return (
    <View style={{ width: set.widthDots * scale, height: set.heightDots * scale }}>
      {set.pieces.map((piece, index) => (
        <Image
          key={index}
          resizeMode="stretch"
          source={piece.source}
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
