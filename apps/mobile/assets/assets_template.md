# 픽셀룸 에셋 템플릿 스펙 (통합본)

방 배경 · 캐릭터(마네킹) · 코디 · 가구 · 펫 전 에셋의 크기, 좌표, 조립 규칙을 담은 단일 기준 문서.
초안 이후 실제 구현·에셋 교체 과정에서 달라진 부분을 실물 코드/파일 기준으로 갱신했다(아래 각 절 참고).

---

## 1. 공통 스케일 시스템

- 모든 에셋은 **도트(dot)** 단위로 제작되어 있고, **1도트 = 8px로 미리 확대된 투명 PNG**다. 초안에 있던 `@1x`(1도트=1px) 변형은 더 이상 쓰지 않는다 — 배경 포함 전체 에셋이 이 8px/dot 하나로 통일돼 있다.
- **화면 표시 규칙: `dotPx = cellWidth ÷ 16 = 방너비 ÷ 112`** (roomGrid.ts의 `cellWidth = 방너비 ÷ 7` 기준)
  - 모든 스프라이트의 표시 크기 = (도트 수) × dotPx. 배경·캐릭터·가구가 자동으로 같은 세계 스케일이 된다.
  - 예외: 가구 중 일부는 이 공통 dotPx 대신 세트 전용 배율을 쓴다 — 6절 참고.
- 렌더링: `image-rendering: pixelated` (Canvas는 `imageSmoothingEnabled = false`). 위치 이동은 가능한 dotPx 배수로.
- 공통 팔레트: 스프라이트 외곽선 `#2E2226` 공유. 색 보정·반투명 금지, 알파 합성만 사용.

**예시 (iPhone 14, 방너비 390px):** dotPx ≈ 3.48px → 타일 55.7×41.8px, 아바타(41도트) ≈ 143px, 침대(58도트) ≈ 202px, 책상(49도트) ≈ 171px.

### 1-1. 파일명 규칙

- 코디/캐릭터 아이템: `{부위}_{이름}.png` (예: `top_tshirt_graphic`, `hat_cap`, `hair_basic`). 포즈 전용 변형이 있는 아이템만 접미사를 붙인다 — `_back`(뒷모습) / `_sit`(앉기) / `_sleep`(수면). 예: `top_hoodie_back`, `bottom_jeans_sit`, `hair_basic_sleep`.
- 장착 아이템과 무관하게 수면 중 강제로 쓰는 공용 파일: `eyes_basic_closed_sleep.png`, `mouth_smile_open_sleep.png`.
- **주의**: 안경류는 슬롯 이름은 `EYEWEAR`지만 파일명 접두어는 `glasses_`를 쓴다(`glasses_horn`, `glasses_sun`) — 슬롯 이름과 파일 접두어가 다른 유일한 부위.
- 가구: 전부 `furn_{이름}.png` 접두어(예: `furn_desk_basic`, `furn_bed_basic`, `furn_bed_blanket_basic`, `furn_pc_tower`, `furn_washer`).
- 배경: `room_{테마이름}_{wall|floor}.png`(예: `room_basic_wall`, `room_herringbone_floor`).
- 아바타 베이스 포즈: `avatar_{포즈}_{번호}.png`(예: `avatar_base_stand_1`, `avatar_sleep_1`). 앉기 손 오버레이만 예외로 `avatar_sit_hands_{번호}.png`.
- 펫: `pet_{이름}.png`(예: `pet_dog`, `pet_turtle`).
- 배율 접미사(`@1x`)는 쓰지 않는다 — 전체 에셋이 1도트=8px로 통일.

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

**참고**: 프론트 격자는 7×5로 확정했다. 기획 문서(#165)에 적힌 10×7과 다른데, 화면에서 실제로 맞춰본 결과 7×5로 통일한 것 — 백엔드 가구 배치 API가 나올 때 이 수치로 맞춰야 한다.

---

## 3. 배경 에셋 목록

**벽지+바닥은 항상 같은 테마 세트로만 쓴다(따로 조합하지 않음)** — 방 하나를 "테마" 단위로 고른다. 현재 2개 테마가 있고, 각각 벽+바닥 한 쌍이다.

| 파일                         | 크기(도트) | 내용                  |
| ---------------------------- | ---------- | --------------------- |
| room_basic_wall.png          | 112×96     | 베이직 테마 벽         |
| room_basic_floor.png         | 112×108    | 베이직 테마 바닥(9줄)  |
| room_herringbone_wall.png    | 112×96     | 헤링본 테마 벽         |
| room_herringbone_floor.png   | 112×108    | 헤링본 테마 바닥(9줄)  |

창문 변형, 벽+바닥 합본 파일, 창문 단독 데코 파일은 초안에 있었지만 **현재 에셋엔 없다.**

---

## 4. 캐릭터(기본 마네킹) 포즈

**아바타 캔버스: 41×54도트 · 중심 x=20 · 발바닥 기준선 y=49**

| 포즈                   | 파일                        | 재생                              | duration(ms)    |
| ---------------------- | --------------------------- | --------------------------------- | ---------------- |
| 서기(앞) idle          | avatar_base_stand_1~3.png   | 1→2→3→2 루프                      | 400/150/400/150 |
| 서기(뒤)               | avatar_base_stand_back.png  | 정지 1장                          | -                |
| 책상 앉기(베이스)      | avatar_sit.png               | 정지 1장(서기와 같은 41×54 캔버스) | -                |
| 책상 앉기 손 오버레이  | avatar_sit_hands_1~3.png    | 1→2→1→3 루프                      | 120 균일         |
| 수면(옆, 48×27 캔버스) | avatar_sleep_1~3.png        | 1→2→3→2 루프(3장, 4번째는 2번 재사용) | 400/250/550/250 |

**주의**: 서기 idle·수면 모두 초안엔 4개 파일(`_1~4`)로 적혀 있었지만, 실제 파일은 3장(`_1~3`)뿐이다 — 루프의 마지막 단계에서 2번 파일을 재사용해 4단계처럼 보이게 한다.

- **head_dy** (머리 부착 코디의 프레임별 y시프트, 도트): stand 파일1 = +1, 파일2 = 0, 파일3 = −1 · sit = 항상 +1 · back = +1(단 `_back` 코디는 보정이 이미 반영돼 있어 0으로 취급) · sleep = 0
- **이동**: 걷기 프레임 없음. idle 스택 전체를 통통 바운스한다. 초안은 도트 비례 진폭(2~3도트, 화면 크기에 따라 스케일)이었지만, 실제 구현은 **화면 크기와 무관한 고정 진폭 12px, 주기 250ms**로 최종 조정됐다(손 튜닝 확정값). 이동 속도는 220px/s, 최소 이동 시간 300ms.
- 아래/좌우 이동·정지 = 앞모습, 위 이동 = 뒷모습.
- 발바닥을 칸에 놓는 공식: `footScreenY = 격자칸의 접지 y`, `avatarTop = footScreenY − 49 × dotPx`, `avatarLeft = 칸중심X − 20 × dotPx`

---

## 5. 코디 에셋

**코디 캔버스: 41×66도트** (아바타 캔버스 위로 12도트 여유 — 모자 공간). **아바타 원점 기준 (0, −12)에 오버레이.** 정렬 기준 프레임은 stand 2번(중간).
수면용 코디만 **48×27 캔버스**(수면 프레임과 동일)라 (0, 0)으로 겹침.

부위 10종(동시 착용 각 1개): `HAIR, EYES, MOUTH, TOP, BOTTOM`(필수 — 항상 하나는 장착) · `HAT, EYEWEAR, SHOES, BAG`(선택 — "없음" 가능) · `PET`(선택 — 옷처럼 아바타 캔버스에 겹쳐 그리지 않고 방에서 아바타를 뒤따라 다니는 별도 스프라이트, 8절 참고).

**등록된 아이템(로컬 테스트 카탈로그 기준, `localCatalog.ts`):**

| 부위    | key                 | 라벨          | 포즈 변형 파일 |
| ------- | ------------------- | ------------- | -------------- |
| HAIR    | hair_basic          | 기본 머리      | back, sleep    |
| HAIR    | hair_long_black     | 긴 검은 머리    | back, sleep    |
| EYES    | eyes_basic          | 기본 눈        | -              |
| EYES    | eyes_blue           | 파란 눈        | -              |
| MOUTH   | mouth_basic         | 기본 입        | -              |
| MOUTH   | mouth_smile_open    | 웃는 입        | -              |
| TOP     | top_basic           | 기본 상의      | sit            |
| TOP     | top_hoodie          | 후드티         | back, sit      |
| TOP     | top_hoodie_dev      | 개발자 후드티   | back           |
| TOP     | top_flannel         | 플란넬 셔츠     | back           |
| TOP     | top_tshirt_graphic  | 그래픽 티셔츠   | -              |
| BOTTOM  | bottom_basic        | 기본 하의      | sit            |
| BOTTOM  | bottom_jeans        | 청바지         | sit            |
| BOTTOM  | bottom_joggers      | 조거팬츠       | back           |
| BOTTOM  | bottom_slacks       | 슬랙스         | back           |
| BOTTOM  | bottom_shorts_socks | 반바지         | back           |
| HAT     | hat_cap             | 야구모자       | back           |
| HAT     | hat_wizard          | 마법사 모자     | -              |
| HAT     | hat_beanie          | 비니           | back           |
| HAT     | hat_headset         | 헤드셋         | back           |
| HAT     | hat_backcap         | 백캡           | back           |
| EYEWEAR | glasses_horn        | 뿔테 안경      | -              |
| EYEWEAR | glasses_sun         | 선글라스       | -              |
| SHOES   | shoes_sneakers      | 운동화         | -              |
| SHOES   | shoes_boots         | 부츠           | -              |
| BAG     | bag_backpack        | 백팩           | back           |
| PET     | pet_dog             | 강아지         | (8절 참고)      |
| PET     | pet_turtle          | 거북이         | (8절 참고)      |

기본 장착값: `HAIR=hair_basic, EYES=eyes_basic, MOUTH=mouth_basic, TOP=top_basic, BOTTOM=bottom_basic`. 나머지 부위는 기본값 없음("없음" 상태로 시작).

**수면 전용 공용 파일**(장착 아이템과 무관하게 강제 사용): `eyes_basic_closed_sleep.png`, `mouth_smile_open_sleep.png`.

**포즈별 해석(폴백) 규칙:**

- 서기(앞): 기본 파일.
- 서기(뒤): `_back` 있으면 사용(시프트 0). 없으면 기본 재사용 — 몸 부착 그대로, 머리 부착은 +1. eyes/glasses/mouth는 미표시.
- 앉기: top/bottom은 `_sit`. shoes는 기본 파일 **(0, −3)** 시프트 재사용. 머리 부착 5부위는 기본 파일 +1. bag은 숨김.
- 수면: hair는 `_sleep`. 눈·입은 장착 아이템과 무관하게 공용 `eyes_basic_closed_sleep`/`mouth_smile_open_sleep`으로 대체. 나머지 부위 미표시(이불이 덮음).

**레이어 순서(아래→위):**

- 서기(앞)·이동: 아바타 → shoes → bottom → top → bag → hair → eyes → mouth → glasses → hat
- 서기(뒤): 아바타(back) → shoes → bottom → top → hair → bag → hat
- 앉기: chair → 아바타 → shoes → bottom → top → hair → eyes → mouth → glasses → hat → **avatar_sit_hands_N** → desk
- 수면: furn_bed_basic → 수면 아바타 → hair → eyes_basic_closed_sleep → mouth_smile_open_sleep → furn_bed_blanket_basic

---

## 6. 가구·씬 조립

가구는 격자에서 차지하는 가로 칸 수(`footprintCols`)만큼 폭이 정확히 맞도록, dotPx 대신 **세트 전용 배율**을 쓴다: `scale = footprintCols × cellWidth ÷ 세트 폭(도트)`.

**상호작용 가구(침대·책상)** — 탭하면 아바타가 걸어가서 앉거나 눕는다. 가로 2칸.

| 파일                       | 크기(도트) | 배치(아바타 원점 기준) | role                        |
| -------------------------- | ---------- | ----------------------- | ---------------------------- |
| furn_desk_basic.png        | 49×17      | (−5, 33)                 | front(책상 상판, 아바타 위)   |
| furn_chair_basic.png       | 31×26      | (4, 24)                  | back(항상 아바타 뒤)         |
| furn_bed_basic.png         | 58×28      | (0, 0)                   | back                         |
| furn_bed_blanket_basic.png | 58×28      | (0, 0)                   | front(이불, 자는 아바타 위)   |

- 책상 씬 레이어: chair → 아바타+코디 → sit_hands → desk. 항상 정면.
- 침대 씬 레이어: furn_bed_basic → 수면 아바타(침대 원점 기준 (4, −12)) → furn_bed_blanket_basic.
- 이불은 프레임별 변형 없이 `furn_bed_blanket_basic.png` 한 장만 쓴다 — 수면 프레임에 따라 이불이 바뀌는 동기화는 하지 않기로 확정.
- **앉기 손 오버레이**: 상의가 베이스의 타이핑 손을 덮으므로, 아바타 프레임과 같은 번호의 `avatar_sit_hands_1~3.png`(41×54, (0,0) 정렬)를 코디 위에 그린다.

**장식 가구(상호작용 없음)** — 전부 조각 1개, 가로 1칸.

| 파일                       | 크기(도트) | 비고                                                                     |
| -------------------------- | ---------- | ------------------------------------------------------------------------- |
| furn_drying_rack.png       | 17×13      | 빨래 건조대                                                                |
| furn_shelf.png             | 16×20      | 선반                                                                       |
| furn_fridge.png            | 16×22      | 냉장고                                                                     |
| furn_pc_tower.png          | 10×17      | PC 본체 — 실제 폭의 2배(가상 20도트)로 계산해서 칸의 절반 크기로 왼쪽 정렬(축소 표시) |
| furn_pet_cushion.png       | 15×7       | 반려동물 방석                                                              |
| furn_washer.png            | 16×20      | 세탁기                                                                     |

### 6-1. 펫(동행 스프라이트)

옷처럼 아바타 캔버스에 겹쳐 그리지 않고, 방에서 아바타를 따라다니는 별도 스프라이트다. 애니메이션 프레임 없음(정지 이미지 1장).

| 파일          | 원본 크기(도트) | 표시 상자(도트)                          |
| ------------- | ---------------- | ----------------------------------------- |
| pet_dog.png   | 18×13             | 9×6.5(원본의 절반 — contain으로 비율 유지) |
| pet_turtle.png| 17×10             | 9×6.5(같은 상자, 원본이 더 납작해 살짝 작게 보임) |

- 발 접지 앵커: 표시 상자의 가로 중앙 하단.
- 아바타가 걸을 때 목적지 **오른쪽으로 0.55칸** 떨어진 자리를 향해, 아바타보다 **220ms 늦게** 출발해서 따라온다.
- 이동 중 통통 튐은 아바타와 완전히 같은 값(주기 250ms, 진폭 12px)을 쓴다.

---

## 7. 구현 체크리스트

1. 컨테이너에 위 여유 12도트(코디 캔버스) 확보 또는 코디를 음수 top으로 배치.
2. 프레임 전환은 이미지 스왑만 — 트랜지션/페이드 금지. (프레임마다 별도 `<Image>`를 항상 마운트해두고 opacity만 바꾸는 방식으로 구현 — source를 갈아끼우면 재디코드로 깜빡인다.)
3. 배경: 바닥 top-anchor + 벽 bottom-anchor + 벽색 연장. `ROOM_OVERLAP` 구간은 바닥 아트 여유분(2줄)이 흡수.
4. 모든 코디는 부위별 z만 지키면 어떤 조합도 안전.
5. 새 코디/가구/펫 에셋을 추가하거나 크기·배치를 조정하면 이 문서(특히 5·6절 표)도 같이 갱신한다.
