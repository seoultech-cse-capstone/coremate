# 픽셀룸 에셋 템플릿 스펙 (통합본)

방 배경 · 캐릭터(마네킹) · 코디 · 가구 전 에셋의 크기, 좌표, 조립 규칙을 담은 단일 기준 문서.
이전에 전달한 "1도트 = 8px 고정" 문구는 이 문서의 반응형 스케일 규칙으로 대체한다.

---

## 1. 공통 스케일 시스템

- 모든 에셋은 **도트(dot)** 단위로 제작되어 있고, `@1x` 파일은 1도트=1px, 그 외 파일은 1도트=8px로 미리 확대된 투명 PNG다.
- **화면 표시 규칙: `dotPx = cellWidth ÷ 16 = 방너비 ÷ 112`** (roomGrid.ts의 `cellWidth = 방너비 ÷ 7` 기준)
  - 모든 스프라이트의 표시 크기 = (도트 수) × dotPx. 배경·캐릭터·가구가 자동으로 같은 세계 스케일이 된다.
- 렌더링: `image-rendering: pixelated` (Canvas는 `imageSmoothingEnabled = false`). 위치 이동은 가능한 dotPx 배수로.
- 공통 팔레트: 스프라이트 외곽선 `#2E2226` 공유. 색 보정·반투명 금지, 알파 합성만 사용.

**예시 (iPhone 14, 방너비 390px):** dotPx ≈ 3.48px → 타일 55.7×41.8px, 아바타(41도트) ≈ 143px, 침대(58도트) ≈ 202px, 책상(49도트) ≈ 171px.

---

## 2. 방 그리드 ↔ 바닥 아트 매핑

프론트 레이아웃(현행 코드 기준):

- 격자: 가로 7칸 × 세로 5줄, `rowHeight = cellWidth × 0.75` → **타일 1칸 = 16×12도트** (아트와 비율 정확히 일치: 12÷16 = 0.75)
- 바닥 레이아웃 구역: **위 여백 없음** — 격자 5줄이 벽 경계 바로 아래부터 시작 + 아래 여백 1줄(`GRID_BOTTOM_MARGIN_ROWS = 1`) = **6줄**
- 추가로 `home.tsx`의 `ROOM_OVERLAP = 32px`(고정 px)가 바닥 아래에 더 붙음 → 도트 환산 32 ÷ dotPx ≈ 6~12도트(기기별 상이)

**바닥 아트는 9줄(108도트)로 제작**되어 있어 6줄(72도트) + 오버랩까지 여유 36도트로 모든 기기에서 커버한다.
격자 r번째 줄(0~4)의 접지 중심 y = `바닥 top + (r + 0.5) × rowHeight`.

배치 방법:

- 바닥 이미지: **위쪽을 벽 경계에 맞춰 정렬**(top-anchor), 화면 아래로 넘치는 부분은 크롭.
- 벽 이미지: **아래쪽을 벽 경계에 맞춰 정렬**(bottom-anchor), 위로 모자라는 부분은 `background-color: #E9DEC6`(벽 상단과 동일한 단색)로 연장.
- 격자·여백의 정확한 시작 줄은 프론트 로직을 따르면 됨 — 바닥 아트는 전 영역이 이어지는 타일 패턴이라 어떤 매핑에도 어긋나지 않는다.

---

## 3. 배경 에셋 목록

| 파일                          | 크기(도트) | 내용                                                                      |
| ----------------------------- | ---------- | ------------------------------------------------------------------------- |
| room_wall@1x.png              | 112×96     | 민무늬 벽 + 걸레받이. 상단 연장색 `#E9DEC6`                               |
| room_wall_window@1x.png       | 112×96     | 창문 달린 벽. 창문은 벽 하단 기준 26~70도트 높이(짧은 벽에서도 항상 보임) |
| room_floor@1x.png             | 112×108    | 격자 타일 바닥 (체커 원목, 9줄)                                           |
| room_floor_herringbone@1x.png | 112×108    | 헤링본 원목 바닥 (9줄, 격자 눈금 없음)                                    |
| room_bg@1x / room_bg.png      | 112×204    | 벽+격자바닥 합본 (@1x / @8x)                                              |
| room_bg_herringbone@1x / .png | 112×204    | 창문벽+헤링본 합본                                                        |
| deco_window@1x.png            | 44×35      | 창문 단독(창턱 포함). 원하는 벽 위치에 자유 배치 가능                     |

---

## 4. 캐릭터(기본 마네킹) 포즈

**아바타 캔버스: 41×54도트 · 중심 x=20 · 발바닥 기준선 y=49**

| 포즈                   | 파일                       | 재생         | duration(ms)    |
| ---------------------- | -------------------------- | ------------ | --------------- |
| 서기(앞) idle          | avatar_base_stand_1~4.png  | 1→2→3→2 루프 | 400/150/400/150 |
| 서기(뒤)               | avatar_base_stand_back.png | 정지 1장     | -               |
| 책상 앉기(타이핑)      | avatar_sit_typing_1~3.png  | 1→2→1→3 루프 | 120 균일        |
| 수면(옆, 48×27 캔버스) | avatar_sleep_1~4.png       | 1→2→3→4 루프 | 400/250/550/250 |

- **head_dy** (머리 부착 코디의 프레임별 y시프트, 도트): stand 파일1 = +1, 파일2 = 0, 파일3 = −1, 파일4 = 0 · sit = 항상 +1 · back = +1(단 `_back` 코디는 보정이 이미 반영돼 있어 0으로 취급) · sleep = 0
- 이동: 걷기 프레임 없음. idle 스택 전체를 통통 바운스(`offsetY = -|sin| × 2~3도트`, 주기 ~250ms). 아래/좌우 이동·정지 = 앞모습, 위 이동 = 뒷모습.
- 발바닥을 칸에 놓는 공식: `footScreenY = 격자칸의 접지 y`, `avatarTop = footScreenY − 49 × dotPx`, `avatarLeft = 칸중심X − 20 × dotPx`

---

## 5. 코디 에셋

**코디 캔버스: 41×66도트** (아바타 캔버스 위로 12도트 여유 — 모자 공간). **아바타 원점 기준 (0, −12)에 오버레이.** 정렬 기준 프레임은 stand 2번(중간).
수면용 코디만 **48×27 캔버스**(수면 프레임과 동일)라 (0, 0)으로 겹침.

부위 9종(동시 착용 각 1개): hat, hair, eyes, glasses, mouth, top, bottom, shoes, bag

**기본(정면) 17종:** hat_cap · hat_wizard · hair_brown · hair_long_black · eyes_black · eyes_blue · glasses_horn · glasses_sun · mouth_smile · mouth_smile_open · top_tshirt · top_hoodie · bottom_jeans · bottom_shorts · shoes_sneakers · shoes_boots · bag_backpack(어깨끈만)

**포즈 전용(접미사 규칙 `_back` `_sit` `_sleep`):** hat_cap_back · hair_brown_back · hair_long_black_back · top_hoodie_back · bag_backpack_back(가방 본체) · top_tshirt_sit · top_hoodie_sit · bottom_shorts_sit · bottom_jeans_sit · hair_brown_sleep · hair_long_black_sleep · eyes_closed_sleep(공용) · mouth_sleep(공용)

**포즈별 해석(폴백) 규칙:**

- 서기(앞): 기본 파일.
- 서기(뒤): `_back` 있으면 사용(시프트 0). 없으면 기본 재사용 — 몸 부착 그대로, 머리 부착은 +1. eyes/glasses/mouth는 미표시.
- 앉기: top/bottom은 `_sit`. shoes는 기본 파일 **(0, −3)** 시프트 재사용. 머리 부착 5부위는 기본 파일 +1. bag은 숨김.
- 수면: hair는 `_sleep`. 눈·입은 장착 아이템과 무관하게 공용 `eyes_closed_sleep`/`mouth_sleep`으로 대체. 나머지 부위 미표시(이불이 덮음).

**레이어 순서(아래→위):**

- 서기(앞)·이동: 아바타 → shoes → bottom → top → bag → hair → eyes → mouth → glasses → hat
- 서기(뒤): 아바타(back) → shoes → bottom → top → hair → bag → hat
- 앉기: chair → 아바타 → shoes → bottom → top → hair → eyes → mouth → glasses → hat → **avatar_sit_hands_N** → desk
- 수면: bed_basic → 수면 아바타 → hair → eyes_closed → mouth_sleep → bed_blanket_N

---

## 6. 가구·씬 조립

| 파일                 | 크기(도트) | 배치                                                  |
| -------------------- | ---------- | ----------------------------------------------------- |
| desk_basic.png       | 49×17      | 아바타 원점 기준 (−5, 33)                             |
| chair_basic.png      | 31×26      | 아바타 원점 기준 (4, 24)                              |
| bed_basic.png        | 58×28      | 침대 원점                                             |
| bed_blanket_1/2.png  | 58×28      | 침대와 같은 원점 (0,0) 겹침. 1=몸 내려감, 2=몸 올라감 |
| bed_blanket_flat.png | 58×28      | 빈 침대용 이불                                        |

- **책상 씬**: chair → 아바타+코디 → sit_hands → desk 순. 항상 정면.
- **침대 씬**: 침대 원점 기준 수면 아바타 (4, −12). 이불 동기화: 수면 프레임 1·4 ↔ bed_blanket_2, 프레임 2·3 ↔ bed_blanket_1.
- **앉기 손 오버레이**: 상의가 베이스의 타이핑 손을 덮으므로, 아바타 프레임과 같은 번호의 `avatar_sit_hands_1~3.png`(41×54, (0,0) 정렬)를 코디 위에 그린다.

---

## 7. 구현 체크리스트

1. 컨테이너에 위 여유 12도트(코디 캔버스) 확보 또는 코디를 음수 top으로 배치.
2. 프레임 전환은 이미지 스왑만 — 트랜지션/페이드 금지.
3. 배경: 바닥 top-anchor + 벽 bottom-anchor + 벽색 연장. `ROOM_OVERLAP` 구간은 바닥 아트 여유분(2줄)이 흡수.
4. 모든 코디는 부위별 z만 지키면 어떤 조합도 안전. 갈색머리+검정눈+티셔츠+반바지+스니커즈 = 원본 캐릭터 재현(정합성 테스트용으로 사용 가능).
