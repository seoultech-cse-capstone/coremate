# 픽셀 아바타 렌더러 구현 스펙 (프론트엔드 전달용)

방(룸) 화면에서 움직이는 레이어드 픽셀 아바타를 구현한다. 기본 마네킹 스프라이트 위에 코디 아이템 PNG를 순서대로 겹쳐 그리는 방식(메이플스토리식)이며, 모든 수치·파일명·순서는 아래 스펙을 그대로 따른다.

## 0. 공통 규칙

- 모든 에셋은 **1도트 = 8px**로 미리 확대된 투명 배경 PNG다. 아래 좌표·크기 단위는 전부 **도트(dot)** 기준이므로, 화면 px로 쓸 때는 ×8 한다.
- 렌더링 시 `image-rendering: pixelated` (Canvas면 `imageSmoothingEnabled=false`) 필수. 위치 이동은 항상 도트 단위(8px 배수)로만 — 소수점/서브픽셀 이동 금지.
- 겹치기는 전부 단순 알파 합성이다. 마스크·회전·스케일 연산 없음.

## 1. 캔버스와 앵커

| 에셋                            | 캔버스(도트) | px(@8x) | 앵커                                                                 |
| ------------------------------- | ------------ | ------- | -------------------------------------------------------------------- |
| 아바타 포즈 프레임              | 41×54        | 328×432 | 중심 x=20, 발바닥 y=49                                               |
| 코디 아이템 (서기·앉기용)       | 41×66        | 328×528 | 아바타 캔버스 기준 **(0, −12)** 에 겹침 (위 12도트는 모자 여유 공간) |
| 수면 아바타 프레임              | 48×27        | 384×216 | 침대 원점 기준 (4, −12)                                              |
| 수면용 코디                     | 48×27        | 384×216 | 수면 아바타와 **같은 캔버스 → (0, 0)** 겹침                          |
| 책상 desk_basic                 | 49×17        | 392×136 | 아바타 기준 (−5, 33)                                                 |
| 의자 chair_basic                | 31×26        | 248×208 | 아바타 기준 (4, 24)                                                  |
| 침대 bed*basic / bed_blanket*\* | 58×28        | 464×224 | 셋 다 같은 캔버스·같은 원점 → (0,0) 겹침                             |

## 2. 포즈 프레임과 재생

| 포즈              | 파일                       | 재생                                 | 프레임별 duration        |
| ----------------- | -------------------------- | ------------------------------------ | ------------------------ |
| 서기(앞) idle     | avatar_base_stand_1~4.png  | 1→2→3→2 루프 (4번 파일 = 2번과 동일) | 400 / 150 / 400 / 150 ms |
| 서기(뒤)          | avatar_base_stand_back.png | 정지 1장                             | -                        |
| 책상 앉기(타이핑) | avatar_sit_typing_1~3.png  | 1→2→1→3 루프                         | 120ms 균일               |
| 수면              | avatar_sleep_1~4.png       | 1→2→3→4 루프                         | 400 / 250 / 550 / 250 ms |

**head_dy (머리 부착 코디의 프레임별 y 시프트, 도트):**

- stand 파일 1: **+1**, 파일 2: **0**, 파일 3: **−1**, 파일 4: **0**
- stand_back: +1 (단, `_back` 코디 파일은 이 보정이 이미 그려져 있으므로 0으로 취급. 앞면 파일을 뒷모습에 재사용할 때만 +1 적용)
- sit: 모든 프레임 **+1** 고정 (머리가 안 움직이는 포즈)
- sleep: 0 (수면용 코디는 좌표가 이미 맞춰져 있음)

머리 부착(part가 hat/hair/eyes/mouth/glasses) 코디만 head_dy를 적용하고, 몸 부착(top/bottom/shoes/bag)은 항상 0이다.

## 3. 코디 아이템과 파일 규칙

부위(part) 9종: `hat, hair, eyes, glasses, mouth, top, bottom, shoes, bag` — 부위당 동시 착용 1개.

기본(정면) 파일 17종: `hat_cap, hat_wizard, hair_brown, hair_long_black, eyes_black, eyes_blue, glasses_horn, glasses_sun, mouth_smile, mouth_smile_open, top_tshirt, top_hoodie, bottom_jeans, bottom_shorts, shoes_sneakers, shoes_boots, bag_backpack`

포즈 전용 파일(있는 것만): 접미사 `_back`, `_sit`, `_sleep`.
`hat_cap_back, hair_brown_back, hair_long_black_back, top_hoodie_back, bag_backpack_back, top_tshirt_sit, top_hoodie_sit, bottom_shorts_sit, bottom_jeans_sit, hair_brown_sleep, hair_long_black_sleep, eyes_closed_sleep, mouth_sleep`

**포즈별 해석 규칙(폴백):**

- **서기(앞)**: 기본 파일 그대로.
- **서기(뒤)**: `_back` 파일이 있으면 그걸 사용(시프트 0). 없으면 기본 파일 재사용 — 몸 부착은 그대로, 머리 부착(hat_wizard)은 +1 시프트. **eyes/glasses/mouth는 그리지 않는다.**
- **앉기**: top/bottom은 `_sit` 파일 사용. shoes는 기본 파일을 **(0, −3)** 시프트해서 재사용. 머리 부착 5부위는 기본 파일 +1 시프트. **bag은 앉는 동안 숨김.**
- **수면**: hair는 `_sleep` 파일 사용, eyes는 장착 여부와 무관하게 `eyes_closed_sleep`, mouth는 `mouth_sleep` 공용 파일로 대체. **hat/glasses/top/bottom/shoes/bag은 그리지 않는다** (이불이 몸을 덮음).

## 4. 레이어 순서 (아래→위)

- **서기(앞)·이동**: 아바타 프레임 → shoes → bottom → top → bag → hair → eyes → mouth → glasses → hat
- **서기(뒤)**: 아바타(back) → shoes → bottom → top → hair → bag → hat _(가방 본체가 머리카락 위로 오는 게 자연스러움)_
- **앉기(책상 씬)**: chair → 아바타 프레임 → shoes → bottom(\_sit) → top(\_sit) → hair → eyes → mouth → glasses → hat → **avatar_sit_hands_N** → desk
- **수면(침대 씬)**: bed_basic → 수면 아바타 프레임 → hair(\_sleep) → eyes_closed_sleep → mouth_sleep → bed_blanket

## 5. 씬 동기화 규칙

- **앉기 손 오버레이**: 상의를 입히면 베이스 프레임에 그려진 타이핑 손이 옷에 가려진다. 그래서 손만 있는 오버레이 `avatar_sit_hands_1~3.png`(41×54, 아바타와 (0,0) 정렬)를 제공한다. 항상 아바타 프레임 번호와 **같은 번호**의 손 오버레이를 코디 위에 그린다 (상의 미착용이어도 그려도 무방 — 같은 픽셀이라 차이 없음).
- **책상 씬**은 항상 정면 포즈만 사용한다.

## 6. 이동 연출 (걷기 프레임 없음)

- 이동 시 별도 걷기 애니메이션 없이, 서있기 idle 스프라이트 전체(코디 포함 스택)를 **통통 튀는 바운스**로 이동시킨다: 이동 중 `offsetY = -|sin(t)| × 진폭(2~3도트)`, 주기 ~250ms 권장. 착지 타이밍에 이동 스텝을 맞추면 귀엽다.
- 아래 방향 이동·정지 = 정면 프레임, 위 방향 이동 = 뒷모습 프레임. 좌우 이동은 정면 프레임 그대로 사용(좌우 구분 없음).
- 클릭 상호작용: 아바타 클릭 → 정면↔뒷면 토글. 책상 클릭 → 책상 앞으로 이동 후 앉기 씬 전환. 침대 클릭 → 침대로 이동 후 수면 씬 전환.

## 7. 착용 상태 데이터 모델 예시

```json
{
  "equipped": {
    "hat": "hat_cap",
    "hair": "hair_brown",
    "eyes": "eyes_black",
    "glasses": null,
    "mouth": "mouth_smile_open",
    "top": "top_tshirt",
    "bottom": "bottom_shorts",
    "shoes": "shoes_sneakers",
    "bag": "bag_backpack"
  }
}
```

```js
// 부위별 메타
const PARTS = {
  hat: { attach: "head", z: 100 },
  glasses: { attach: "head", z: 90 },
  mouth: { attach: "head", z: 80 },
  eyes: { attach: "head", z: 70 },
  hair: { attach: "head", z: 60 },
  bag: { attach: "body", z: 50 }, // 서기(뒤)에서는 z: 65 (머리카락 위)
  top: { attach: "body", z: 40 },
  bottom: { attach: "body", z: 30 },
  shoes: { attach: "body", z: 20 },
};
// 프레임 틱마다: drawImage(아바타프레임) 후 z 오름차순으로
// drawImage(코디, offsetX*8, (offsetY + (attach==="head" ? headDy : 0) - 12) * 8)
```

## 8. 구현 시 주의

- 코디 캔버스가 아바타보다 위로 12도트 크므로, 아바타 컨테이너를 잡을 때 위 여백 12도트(96px)를 확보하거나 코디를 음수 top으로 배치한다.
- 프레임 교체는 이미지 스왑만으로 처리하고, 트랜지션·페이드 넣지 말 것(도트 감성 유지).
- 모든 PNG는 같은 팔레트·같은 아웃라인 색(#2E2226)을 공유하므로 색 보정 금지.
