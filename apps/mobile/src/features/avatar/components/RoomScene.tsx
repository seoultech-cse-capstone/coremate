import { useRef, useState } from "react";
import { Animated, Pressable, StyleSheet, View, type LayoutChangeEvent } from "react-native";

import { AVATAR_STAGE_HEIGHT, AVATAR_STAGE_WIDTH, AvatarStage } from "./AvatarStage";

// 실제 벽지·바닥 에셋(#165)이 아직 없어서, 방이라는 걸 알아볼 수 있는 최소한의
// 자리만 잡아둔다. 이미지가 정해지면 이 두 영역을 이미지로 바꾸면 된다.
const WALL_COLOR = "#DCD6FB";
const FLOOR_COLOR = "#C9BFE8";
const WALL_RATIO = 0.6;

type RoomSceneProps = {
  height: number;
};

/** 방(벽+바닥) 위에 아바타를 세워 보여준다. 아바타를 탭하면 살짝 튀어오른다. */
export function RoomScene({ height }: RoomSceneProps) {
  const [width, setWidth] = useState(0);
  const bounce = useRef(new Animated.Value(0)).current;

  const onLayout = (event: LayoutChangeEvent) => {
    setWidth(event.nativeEvent.layout.width);
  };

  const handlePressAvatar = () => {
    Animated.sequence([
      Animated.timing(bounce, { toValue: -16, duration: 120, useNativeDriver: true }),
      Animated.spring(bounce, { toValue: 0, useNativeDriver: true, friction: 4, tension: 80 }),
    ]).start();
  };

  const wallHeight = height * WALL_RATIO;
  const floorHeight = height - wallHeight;
  const avatarHeight = Math.min(height * 0.78, width * 0.78 * (AVATAR_STAGE_HEIGHT / AVATAR_STAGE_WIDTH));
  const avatarScale = avatarHeight / AVATAR_STAGE_HEIGHT;
  const avatarWidth = AVATAR_STAGE_WIDTH * avatarScale;

  return (
    <View onLayout={onLayout} style={[styles.card, { height }]}>
      <View style={{ height: wallHeight, backgroundColor: WALL_COLOR }} />
      <View style={{ height: floorHeight, backgroundColor: FLOOR_COLOR }} />

      {width > 0 ? (
        <Pressable
          accessibilityLabel="내 아바타"
          accessibilityRole="button"
          onPress={handlePressAvatar}
          style={[
            styles.avatarWrap,
            { bottom: floorHeight * 0.35, transform: [{ translateX: -avatarWidth / 2 }] },
          ]}
        >
          <Animated.View style={{ transform: [{ translateY: bounce }] }}>
            <AvatarStage scale={avatarScale} />
          </Animated.View>
        </Pressable>
      ) : null}
    </View>
  );
}

const styles = StyleSheet.create({
  card: {
    width: "100%",
    borderRadius: 24,
    overflow: "hidden",
  },
  avatarWrap: {
    position: "absolute",
    left: "50%",
  },
});
