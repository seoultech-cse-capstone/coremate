import { createContext, useContext, type ReactNode } from "react";
import { View, type StyleProp, type ViewStyle } from "react-native";

/** 더미 화면 안의 강조 대상들을 id 로 모아 두는 보관소. 온보딩 화면이 여기서 꺼내 위치를 잰다. */
export type SpotlightRegistry = Map<string, View>;

const SpotlightRegistryContext = createContext<SpotlightRegistry | null>(null);

export const SpotlightRegistryProvider = SpotlightRegistryContext.Provider;

type SpotlightTargetProps = {
  id: string;
  style?: StyleProp<ViewStyle>;
  children: ReactNode;
};

/** 감싼 영역을 강조 대상으로 등록한다. 모양은 바꾸지 않는다. */
export function SpotlightTarget({ id, style, children }: SpotlightTargetProps) {
  const registry = useContext(SpotlightRegistryContext);

  return (
    <View
      collapsable={false}
      style={style}
      ref={(view) => {
        if (!registry) {
          return;
        }
        if (view) {
          registry.set(id, view);
        } else {
          registry.delete(id);
        }
      }}
    >
      {children}
    </View>
  );
}
