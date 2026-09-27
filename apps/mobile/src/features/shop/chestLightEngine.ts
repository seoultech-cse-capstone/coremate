// 랜덤박스 오픈 연출의 순수 시뮬레이션(프레임워크 무관).
// chest_light_preview.html 의 tick()/stepParticles()/seamPaths() 를 그대로 옮겼다.
// 렌더링은 하지 않고, 매 프레임 상태(inten/open/flash/particles)와 기하만 계산한다.

import chestAnim from "../../../assets/shop/random-box/chest_anim.json";
import {
  CHEST_X,
  CHEST_Y,
  FRAME_NAMES,
  HOP,
  NSTEP,
  OPEN_X,
  OPEN_Y,
  OX,
  OY,
  PW,
  RESIDUAL,
  S,
  SX,
  TARGET,
} from "./chestLightConfig";

type SeqStep = { frame: number; name: string; durationMs: number };
const SEQ: SeqStep[] = chestAnim.sequence;

export type Particle = {
  x: number;
  y: number;
  vx: number;
  vy: number;
  life: number;
  decay: number;
  size: number;
  coin: boolean;
};

export type SeamPaths = {
  front: [number, number][];
  end: [number, number][];
  gap: number;
};

export class ChestLightEngine {
  frameIdx = 0;
  private seqPos = -1;
  private seqT = 0;
  tGlobal = 0;
  inten = 0;
  open = 0;
  flash = 0;
  particles: Particle[] = [];
  playing = false;
  done = false;

  // 디버그/튜닝 계수
  intensity = 1.0;
  coneAngleDeg = 32;
  maxParticles = 28;

  reset() {
    this.frameIdx = 0;
    this.seqPos = -1;
    this.seqT = 0;
    this.inten = 0;
    this.open = 0;
    this.flash = 0;
    this.particles = [];
    this.playing = false;
    this.done = false;
  }

  start() {
    this.reset();
    this.playing = true;
    this.seqPos = 0;
    this.seqT = 0;
    this.frameIdx = SEQ[0].frame;
  }

  private currentName(): string {
    return FRAME_NAMES[this.frameIdx] ?? "closed";
  }

  private hopInfo() {
    return HOP[this.currentName()] ?? { dx: 0, dy: 0, lift: 0 };
  }

  chestOffsetY(): number {
    return this.hopInfo().dy * S;
  }

  seamPaths(): SeamPaths {
    const h = this.hopInfo();
    const gy = 15 - h.lift / 2;
    const P = (x: number, y: number): [number, number] => [
      CHEST_X + (x + h.dx) * S,
      CHEST_Y + (y + h.dy) * S,
    ];
    const front: [number, number][] = [];
    for (let t = NSTEP - 1; t >= 0; t--) front.push(P(OX - SX * t + 1, OY - t + gy));
    const end: [number, number][] = [P(OX + 2, OY + gy), P(OX + PW - 2, OY + gy)];
    return { front, end, gap: h.lift };
  }

  private spawnParticle(strength: number) {
    const spread = 10 + 60 * this.open;
    let x = OPEN_X + (Math.random() - 0.5) * spread * 2;
    let y = OPEN_Y + this.chestOffsetY() + (Math.random() - 0.5) * 8;
    if (this.open < 0.3) {
      const sp = this.seamPaths();
      const pts = Math.random() < 0.6 ? sp.front : sp.end;
      const u = Math.random() * (pts.length - 1);
      const a = Math.floor(u);
      const f = u - a;
      const b = Math.min(a + 1, pts.length - 1);
      x = pts[a][0] + (pts[b][0] - pts[a][0]) * f;
      y = pts[a][1] + (pts[b][1] - pts[a][1]) * f;
    }
    this.particles.push({
      x,
      y,
      vx: (Math.random() - 0.5) * 0.6 * (0.3 + this.open),
      vy: -(0.6 + Math.random() * 1.6) * (0.4 + strength),
      life: 1,
      decay: 0.006 + Math.random() * 0.012,
      size: (Math.random() < 0.25 ? 3 : 2) * (S / 3),
      coin: this.open > 0.6 && Math.random() < 0.18,
    });
  }

  private stepParticles(dt: number) {
    const want = this.maxParticles * this.inten * (0.3 + this.open);
    if (this.particles.length < want && Math.random() < 0.5 + this.inten) {
      this.spawnParticle(this.inten);
    }
    for (const p of this.particles) {
      p.x += p.vx * dt;
      p.y += p.vy * dt;
      p.vy += 0.004 * dt;
      p.life -= p.decay * dt;
    }
    this.particles = this.particles.filter((p) => p.life > 0 && p.y > -20);
  }

  // dt: 프레임 단위(1 = 16.67ms)
  step(dt: number) {
    this.tGlobal += dt * 16.67;
    if (this.playing) {
      this.seqT += dt * 16.67;
      while (this.seqPos < SEQ.length && this.seqT >= SEQ[this.seqPos].durationMs) {
        this.seqT -= SEQ[this.seqPos].durationMs;
        this.seqPos++;
        if (this.seqPos >= SEQ.length) {
          this.playing = false;
          this.seqPos = SEQ.length - 1;
          this.done = true;
          break;
        }
        this.frameIdx = SEQ[this.seqPos].frame;
        if (SEQ[this.seqPos].name === "open") this.flash = 1;
      }
    }
    const name = this.currentName();
    const t = TARGET[name] ?? TARGET.closed;
    let ti = t.i;
    const to = t.o;
    if (name === "closed" && this.seqPos > 0) {
      ti = Math.max(ti, RESIDUAL * Math.min(1, this.seqPos / 4));
    }
    if (name === "open") ti = 0.7 + 0.08 * Math.sin(this.tGlobal / 260);
    this.inten += (ti - this.inten) * (ti > this.inten ? 0.45 : 0.1) * dt;
    this.open += (to - this.open) * (to > this.open ? 0.35 : 0.15) * dt;
    this.flash = Math.max(0, this.flash - 0.06 * dt);
    this.stepParticles(dt);
  }
}
