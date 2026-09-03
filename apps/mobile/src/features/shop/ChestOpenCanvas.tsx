import {
  Canvas,
  FilterMode,
  Group,
  Image as SkImage,
  LinearGradient,
  MipmapMode,
  Path,
  Rect,
  Skia,
  useImage,
  vec,
} from "@shopify/react-native-skia";
import { useEffect, useRef, useState } from "react";
import { View, type ViewStyle } from "react-native";

import {
  CANVAS_H,
  CANVAS_W,
  CHEST_X,
  CHEST_Y,
  DEFAULT_TUNING,
  getPalette,
  rgba,
  S,
  SX,
} from "./chestLightConfig";
import { ChestLightEngine } from "./chestLightEngine";

// 1x 프레임(76×54)을 nearest 로 ×S 확대 → 픽셀이 뭉개지지 않는다.
const FRAME_SOURCES_1X = [
  require("../../../assets/shop/random-box/00_closed_1x.png"),
  require("../../../assets/shop/random-box/01_hop1_a_1x.png"),
  require("../../../assets/shop/random-box/02_hop1_b_1x.png"),
  require("../../../assets/shop/random-box/03_hop1_c_1x.png"),
  require("../../../assets/shop/random-box/04_hop2_a_1x.png"),
  require("../../../assets/shop/random-box/05_hop2_b_1x.png"),
  require("../../../assets/shop/random-box/06_hop2_c_1x.png"),
  require("../../../assets/shop/random-box/07_hop3_a_1x.png"),
  require("../../../assets/shop/random-box/08_hop3_b_1x.png"),
  require("../../../assets/shop/random-box/09_hop3_c_1x.png"),
  require("../../../assets/shop/random-box/10_swing1_1x.png"),
  require("../../../assets/shop/random-box/11_swing2_1x.png"),
  require("../../../assets/shop/random-box/12_swing3_1x.png"),
  require("../../../assets/shop/random-box/13_swing4_1x.png"),
  require("../../../assets/shop/random-box/14_swing5_1x.png"),
  require("../../../assets/shop/random-box/15_swing6_1x.png"),
  require("../../../assets/shop/random-box/16_swing7_1x.png"),
  require("../../../assets/shop/random-box/17_open_1x.png"),
];

const NEAREST = { filter: FilterMode.Nearest, mipmap: MipmapMode.None };

type Tuning = { intensity: number; coneAngleDeg: number; maxParticles: number };

type ChestOpenCanvasProps = {
  playing: boolean;
  rarity: string | null;
  onComplete?: () => void;
  tuning?: Partial<Tuning>;
  style?: ViewStyle;
};

export function ChestOpenCanvas({ playing, rarity, onComplete, tuning, style }: ChestOpenCanvasProps) {
  // eslint-disable-next-line react-hooks/rules-of-hooks
  const images = FRAME_SOURCES_1X.map((s) => useImage(s));
  const engineRef = useRef<ChestLightEngine>(new ChestLightEngine());
  const rafRef = useRef<number | null>(null);
  const lastTsRef = useRef(0);
  const onCompleteRef = useRef(onComplete);
  onCompleteRef.current = onComplete;
  const [, forceRender] = useState(0);

  const t: Tuning = { ...DEFAULT_TUNING, ...tuning };
  const eng = engineRef.current;
  eng.intensity = t.intensity;
  eng.coneAngleDeg = t.coneAngleDeg;
  eng.maxParticles = t.maxParticles;

  useEffect(() => {
    if (!playing) {
      eng.reset();
      if (rafRef.current !== null) cancelAnimationFrame(rafRef.current);
      rafRef.current = null;
      forceRender((n) => n + 1);
      return;
    }

    eng.start();
    lastTsRef.current = 0;
    let completed = false;
    const loop = (ts: number) => {
      const dt = lastTsRef.current === 0 ? 1 : Math.min(3, (ts - lastTsRef.current) / 16.67);
      lastTsRef.current = ts;
      eng.step(dt);
      forceRender((n) => n + 1);
      if (eng.done && !completed) {
        completed = true;
        onCompleteRef.current?.();
      }
      // done 이후에도 open 상태의 은은한 펄스를 계속 그린다.
      rafRef.current = requestAnimationFrame(loop);
    };
    rafRef.current = requestAnimationFrame(loop);
    return () => {
      if (rafRef.current !== null) cancelAnimationFrame(rafRef.current);
      rafRef.current = null;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [playing]);

  const pal = getPalette(rarity);
  const img = images[eng.frameIdx] ?? images[0];

  const inten = eng.inten;
  const open = eng.open;
  const flash = eng.flash;
  const tG = eng.tGlobal;
  const k = t.intensity * inten;

  // ── 빛기둥 + 라디얼 글로우 (상자 뒤) ──
  const coneElements: React.ReactNode[] = [];
  const coneW = Math.max(0, (open - 0.15) / 0.85);
  const coneK = k * coneW * coneW;
  if (coneK > 0.001) {
    const oy = eng.chestOffsetY();
    const ax = CANVAS_W / 2; // OPEN_X == CANVAS_W/2
    const ay = CHEST_Y + 30 * S + oy;
    const ang = (t.coneAngleDeg * Math.PI) / 180;
    const h = CANVAS_H * (0.35 + 0.75 * open);
    const halfTop = Math.tan(ang) * h * (0.15 + 0.85 * open);
    const halfBase = 6 * S * (0.15 + 0.85 * open);
    const bands: [number, number][] = [
      [1.0, 0.55],
      [0.62, 0.45],
      [0.3, 0.55],
    ];
    bands.forEach(([w, a], idx) => {
      const p = Skia.Path.Make();
      p.moveTo(ax - halfBase * w, ay);
      p.lineTo(ax - halfTop * w, ay - h);
      p.lineTo(ax + halfTop * w, ay - h);
      p.lineTo(ax + halfBase * w, ay);
      p.close();
      coneElements.push(
        <Path key={`cone${idx}`} path={p}>
          <LinearGradient
            start={vec(0, ay)}
            end={vec(0, ay - h)}
            positions={[0, 0.45, 1]}
            colors={[rgba(pal.core, a * coneK), rgba(pal.mid, a * 0.55 * coneK), rgba(pal.edge, 0)]}
          />
        </Path>,
      );
    });
    const r = (14 + 26 * open) * S * (0.8 + 0.2 * Math.sin(tG / 180));
    const glow = Skia.Path.Make();
    glow.addOval(Skia.XYWHRect(ax - r, ay - r, r * 2, r * 2));
    coneElements.push(
      <Path key="glow" path={glow}>
        <LinearGradient
          start={vec(ax, ay - r)}
          end={vec(ax, ay + r)}
          positions={[0, 0.35, 1]}
          colors={[rgba(pal.core, 0.75 * coneK), rgba(pal.mid, 0.35 * coneK), rgba(pal.edge, 0)]}
        />
      </Path>,
    );
  }

  // ── 틈새 빛 (상자 위) ──
  const seamElements: React.ReactNode[] = [];
  const seamK = t.intensity * inten * Math.pow(1 - Math.min(1, open / 0.6), 1.5);
  if (seamK > 0.001) {
    const { front, end, gap } = eng.seamPaths();
    const g = 0.5 + gap * 0.35;
    const flick = 0.85 + 0.15 * Math.sin(tG / 37) * Math.sin(tG / 61);
    const makeLine = (pts: [number, number][]) => {
      const p = Skia.Path.Make();
      p.moveTo(pts[0][0], pts[0][1]);
      for (let i = 1; i < pts.length; i++) p.lineTo(pts[i][0], pts[i][1]);
      return p;
    };
    const seams = [front, end];
    seams.forEach((pts, si) => {
      const linePath = makeLine(pts);
      const halo: [number, number][] = [
        [16, 0.06],
        [10, 0.1],
        [6, 0.16],
        [3.2, 0.3],
      ];
      halo.forEach(([w, a], hi) => {
        seamElements.push(
          <Path
            key={`halo${si}_${hi}`}
            path={linePath}
            style="stroke"
            strokeWidth={w * S * g * 0.5}
            strokeCap="round"
            strokeJoin="round"
            color={rgba(pal.mid, a * seamK * flick)}
          />,
        );
      });
      seamElements.push(
        <Path
          key={`core${si}`}
          path={linePath}
          style="stroke"
          strokeWidth={(0.6 + gap * 0.25) * S}
          strokeCap="round"
          strokeJoin="round"
          color={rgba(pal.core, 0.9 * seamK)}
        />,
      );
    });
    // 빛살
    const nl = Math.hypot(1, SX);
    const addRays = (pts: [number, number][], nx: number, ny: number, count: number, seed: number, tag: string) => {
      for (let i = 0; i < count; i++) {
        const u = (i + 0.5) / count;
        const a = Math.floor(u * (pts.length - 1));
        const f = u * (pts.length - 1) - a;
        const b = Math.min(a + 1, pts.length - 1);
        const x = pts[a][0] + (pts[b][0] - pts[a][0]) * f;
        const y = pts[a][1] + (pts[b][1] - pts[a][1]) * f;
        const ph = Math.sin(tG / 90 + i * 1.7 + seed) * 0.5 + 0.5;
        const len = (8 + 26 * g * seamK) * S * (0.45 + 0.55 * ph);
        for (const dir of [1, -1]) {
          const L = len * (dir > 0 ? 1 : 0.7);
          const ex = x + nx * L * dir;
          const ey = y + ny * L * dir;
          const rp = Skia.Path.Make();
          rp.moveTo(x, y);
          rp.lineTo(ex, ey);
          seamElements.push(
            <Path
              key={`ray${tag}_${i}_${dir}`}
              path={rp}
              style="stroke"
              strokeWidth={(1.2 + 1.4 * ph) * S * 0.5}
              strokeCap="round"
            >
              <LinearGradient
                start={vec(x, y)}
                end={vec(ex, ey)}
                colors={[rgba(pal.core, 0.55 * seamK * (0.6 + 0.4 * ph)), rgba(pal.mid, 0)]}
              />
            </Path>,
          );
        }
      }
    };
    addRays(front, -1 / nl, SX / nl, 7, 0, "F");
    addRays(end, 0, 1, 6, 3, "E");
  }

  // ── 입자 (앞/뒤로 나눠 배치, 색 버킷별 Path 로 묶어 노드 수를 줄임) ──
  const baseY = CHEST_Y + 30 * S + eng.chestOffsetY();
  const buildParticlePaths = (behind: boolean) => {
    const bright = Skia.Path.Make();
    const dim = Skia.Path.Make();
    const coins: React.ReactNode[] = [];
    for (let i = 0; i < eng.particles.length; i++) {
      const p = eng.particles[i];
      if (p.y > baseY !== behind) continue;
      if (p.coin) {
        const cx = Math.round(p.x);
        const cy = Math.round(p.y);
        coins.push(
          <Group key={`coin${behind ? "b" : "f"}_${i}`}>
            <Rect x={cx - 4} y={cy - 3} width={8} height={6} color="#f0b856" />
            <Rect x={cx - 3} y={cy - 2} width={4} height={2} color="#ffe48a" />
            <Rect x={cx - 4} y={cy + 2} width={8} height={1} color="#c07e34" />
          </Group>,
        );
        continue;
      }
      const s = p.size;
      const rect = Skia.XYWHRect(Math.round(p.x - s / 2), Math.round(p.y - s / 2), s, s);
      if (p.life > 0.5) bright.addRect(rect);
      else dim.addRect(rect);
    }
    return { bright, dim, coins };
  };
  const back = buildParticlePaths(true);
  const front = buildParticlePaths(false);

  return (
    <View style={[{ width: CANVAS_W, height: CANVAS_H }, style]}>
      <Canvas style={{ width: CANVAS_W, height: CANVAS_H }}>
        {/* 빛기둥 + 글로우 (뒤) */}
        <Group blendMode="plus">{coneElements}</Group>
        {/* 뒤 입자 */}
        <Group blendMode="plus">
          <Path path={back.bright} color={rgba(pal.core, 0.9)} />
          <Path path={back.dim} color={rgba(pal.mid, 0.9)} />
          {back.coins}
        </Group>
        {/* 상자 스프라이트 */}
        {img ? (
          <SkImage
            image={img}
            x={CHEST_X}
            y={CHEST_Y}
            width={76 * S}
            height={54 * S}
            fit="fill"
            sampling={NEAREST}
          />
        ) : null}
        {/* 틈새 빛 (위) */}
        <Group blendMode="plus">{seamElements}</Group>
        {/* 앞 입자 */}
        <Group blendMode="plus">
          <Path path={front.bright} color={rgba(pal.core, 0.9)} />
          <Path path={front.dim} color={rgba(pal.mid, 0.9)} />
          {front.coins}
        </Group>
        {/* 열림 플래시 (최상단) */}
        {flash > 0.001 ? (
          <Group blendMode="plus">
            <Rect x={0} y={0} width={CANVAS_W} height={CANVAS_H} color={rgba(pal.core, flash * 0.55)} />
          </Group>
        ) : null}
      </Canvas>
    </View>
  );
}
