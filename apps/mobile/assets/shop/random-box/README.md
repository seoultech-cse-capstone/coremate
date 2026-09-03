# 랜덤박스 에셋

이 폴더에 랜덤박스 관련 이미지와 애니메이션 JSON을 넣어주세요.
코드에서는 `require("../../../assets/shop/random-box/<파일명>")` 로 참조합니다.

## 넣을 파일 (권장 명명)

| 용도 | 파일명(예시) | 형식 |
|---|---|---|
| 닫힌 박스 | `box_closed.png` | PNG |
| 열린 박스 | `box_opened.png` | PNG |
| 열리는 과정(스프라이트/프레임) | `box_opening.png` 또는 `box_opening_1.png`, `box_opening_2.png` ... | PNG |
| 애니메이션 데이터 | `box_opening.json` | JSON (Lottie 또는 프레임 메타) |

- 프레임을 개별 PNG로 주면 파일명 끝에 `_1`, `_2` 순번을 붙여주세요.
- JSON이 Lottie면 그대로 두고, 커스텀 프레임 메타면 어떤 키가 무슨 뜻인지 한 줄 적어주세요.
- 실제 파일을 넣은 뒤 이 README는 지워도 됩니다.
