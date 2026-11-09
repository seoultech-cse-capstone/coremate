/**
 * 위치를 잴 수 있는 네이티브 요소. react-native 버전마다 View ref 타입이 달라(0.86 View, 0.88 ReactNativeElement)
 * 필요한 메서드만 요구한다.
 */
export type MeasurableNode = {
  measureInWindow: (callback: (x: number, y: number, width: number, height: number) => void) => void;
};

// 실제 화면의 버튼을 온보딩이 강조할 수 있게 id 로 모아 두는 보관소.
// 화면 모양을 바꾸지 않도록 감싸는 View 없이 기존 요소에 ref 만 단다.
const targets = new Map<string, MeasurableNode>();
const refCallbacks = new Map<string, (view: MeasurableNode | null) => void>();
const scrollers = new Map<string, { current: unknown }>();

/** 강조 대상으로 등록하는 ref. 같은 id 에는 같은 함수를 돌려줘 렌더마다 다시 붙지 않게 한다. */
export function spotlightRef(id: string) {
  let callback = refCallbacks.get(id);
  if (!callback) {
    callback = (view: MeasurableNode | null) => {
      if (view) {
        targets.set(id, view);
      } else {
        targets.delete(id);
      }
    };
    refCallbacks.set(id, callback);
  }
  return callback;
}

export function getSpotlightTarget(id: string): MeasurableNode | undefined {
  return targets.get(id);
}

type LayoutNode = MeasurableNode & {
  measureLayout: (
    relativeTo: unknown,
    onSuccess: (x: number, y: number, width: number, height: number) => void,
    onFail?: () => void,
  ) => void;
};

type ScrollHandle = MeasurableNode & {
  scrollTo: (options: { x?: number; y: number; animated?: boolean }) => void;
  getInnerViewRef: () => MeasurableNode | null;
};

/**
 * 강조 대상이 들어 있는 ScrollView 를 화면 이름으로 등록한다. 해제 함수를 돌려준다.
 * ScrollView ref 타입이 react-native 버전마다 달라 current 만 받는다.
 */
export function registerSpotlightScroll(id: string, scrollRef: { current: unknown }) {
  scrollers.set(id, scrollRef);
  return () => {
    if (scrollers.get(id) === scrollRef) {
      scrollers.delete(id);
    }
  };
}

/** 대상이 스크롤 영역 가운데 오도록 스크롤한다. 끝을 넘지 않게 맞춘다. */
export function centerSpotlightTarget(scrollId: string, targetId: string) {
  const scrollView = scrollers.get(scrollId)?.current as ScrollHandle | null | undefined;
  const target = targets.get(targetId) as LayoutNode | undefined;
  const inner = scrollView?.getInnerViewRef();
  if (!scrollView || !target || !inner) {
    return;
  }
  scrollView.measureInWindow((_x, _y, _width, viewportHeight) => {
    inner.measureInWindow((_ix, _iy, _iw, contentHeight) => {
      target.measureLayout(
        inner,
        (_tx, targetY, _tw, targetHeight) => {
          const maxOffset = Math.max(0, contentHeight - viewportHeight);
          const centered = targetY + targetHeight / 2 - viewportHeight / 2;
          scrollView.scrollTo({ y: Math.min(Math.max(0, centered), maxOffset), animated: true });
        },
        () => console.error("[onboarding] 강조 대상 스크롤 위치를 재지 못함", targetId),
      );
    });
  });
}
