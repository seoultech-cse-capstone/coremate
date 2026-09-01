import { Image } from "expo-image";
import { useEffect, useRef, useState } from "react";
import { StyleSheet, View, type ViewStyle } from "react-native";

import {
  CHEST_CONTENT_OFFSET_RATIO,
  CHEST_FRAME_RATIO,
  CHEST_SEQUENCE,
  CLOSED_FRAME_INDEX,
  OPEN_FRAME_INDEX,
  RANDOM_BOX_FRAMES,
} from "./randomBoxFrames";

type ChestAnimationProps = {
  /** 스프라이트 표시 폭(px). 높이는 프레임 비율로 자동 계산된다. */
  size: number;
  /** true 가 되는 순간 sequence 를 처음부터 재생한다. false 로 돌리면 닫힌 프레임으로 리셋. */
  playing: boolean;
  /** 마지막(open) 프레임까지 재생을 마치면 호출된다. */
  onComplete?: () => void;
  style?: ViewStyle;
};

// 경과 시간 기준으로 sequence 를 따라간다. setTimeout 체이닝 대신 고정 간격(33ms) 인터벌에서
// "지금 몇 ms 지났나 → 몇 번째 스텝인가"를 계산해 프레임을 정한다. duration 값이 비정상(0/누락)
// 이어도 최소 80ms 로 바닥을 깔아, 프레임이 순식간에 지나가 버리는 일이 없다.
export function ChestAnimation({ size, playing, onComplete, style }: ChestAnimationProps) {
  const [frameIndex, setFrameIndex] = useState(CLOSED_FRAME_INDEX);
  const onCompleteRef = useRef(onComplete);
  onCompleteRef.current = onComplete;

  useEffect(() => {
    if (!playing) {
      setFrameIndex(CLOSED_FRAME_INDEX);
      return;
    }

    const steps = CHEST_SEQUENCE.map((s) => ({
      frame: s.frame,
      dur: typeof s.durationMs === "number" && s.durationMs > 0 ? s.durationMs : 80,
    }));
    const cumulative: number[] = [];
    let total = 0;
    for (const s of steps) {
      total += s.dur;
      cumulative.push(total);
    }

    const start = Date.now();
    setFrameIndex(steps[0].frame);
    const id = setInterval(() => {
      const elapsed = Date.now() - start;
      if (elapsed >= total) {
        setFrameIndex(steps[steps.length - 1].frame);
        clearInterval(id);
        onCompleteRef.current?.();
        return;
      }
      let idx = 0;
      while (idx < cumulative.length - 1 && elapsed >= cumulative[idx]) {
        idx += 1;
      }
      setFrameIndex(steps[idx].frame);
    }, 33);

    return () => clearInterval(id);
  }, [playing]);

  const height = size / CHEST_FRAME_RATIO;
  const offsetTransform = [{ translateX: size * CHEST_CONTENT_OFFSET_RATIO }];

  // 18프레임을 전부 겹쳐 마운트해두고 현재 프레임만 보이게 한다(opacity 토글).
  // 재생 중 source 를 갈아끼우면 expo-image 가 프레임마다 비동기 디코드를 하느라
  // 60ms 짜리 움찔 구간을 놓친다 — 미리 다 올려두면 교체가 즉시 일어나 부드럽다.
  return (
    <View style={[{ width: size, height }, style]}>
      {RANDOM_BOX_FRAMES.map((source, index) => (
        <Image
          key={index}
          source={source}
          style={[
            styles.frame,
            { opacity: index === frameIndex ? 1 : 0, transform: offsetTransform },
          ]}
          contentFit="contain"
          // 픽셀아트가 뭉개지지 않도록 확대 시 보간을 끈다.
          allowDownscaling={false}
          // 프레임 교체가 즉시 보이도록 페이드 전환을 끈다.
          transition={0}
        />
      ))}
    </View>
  );
}

export const OPEN_FRAME = OPEN_FRAME_INDEX;

const styles = StyleSheet.create({
  frame: {
    position: "absolute",
    top: 0,
    left: 0,
    width: "100%",
    height: "100%",
  },
});
