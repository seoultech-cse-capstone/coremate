import { useEffect, useState } from "react";

type FrameStep<T> = { value: T; duration: number };

/** sequence를 반복 재생하며 현재 값을 돌려준다. active가 false면 첫 값에 멈춘다. */
export function useFrameSequence<T>(sequence: FrameStep<T>[], active: boolean): T {
  const [current, setCurrent] = useState<T>(sequence[0].value);

  useEffect(() => {
    if (!active) {
      setCurrent(sequence[0].value);
      return;
    }

    let cancelled = false;
    let index = 0;
    let timeoutId: ReturnType<typeof setTimeout>;

    const tick = () => {
      if (cancelled) {
        return;
      }
      const step = sequence[index % sequence.length];
      setCurrent(step.value);
      index += 1;
      timeoutId = setTimeout(tick, step.duration);
    };

    tick();
    return () => {
      cancelled = true;
      clearTimeout(timeoutId);
    };
    // sequence는 호출부에서 매번 새 배열 리터럴로 넘기는 경우가 많아 참조가 안정적이지
    // 않다 — active가 바뀔 때만 재시작하면 충분하다.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [active]);

  return current;
}
