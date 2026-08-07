# 정책 시드 데이터

서비스 동작에 **필요한** 정책 데이터다. `scripts/db/*.sql`(약점분석 손검증용 폐기 더미)과 성격이 다르다.

## 적용 방식

`application.yaml`의 `spring.sql.init`이 **백엔드 기동 시 자동 실행**한다.
`spring.jpa.defer-datasource-initialization: true`로 두어 Hibernate가 테이블을 만든 **뒤에** 실행된다.

모든 파일은 **멱등**하게 작성한다(`INSERT ... ON DUPLICATE KEY UPDATE`).
매 기동마다 실행되므로 멱등하지 않으면 중복 행이 쌓인다.

## 시드 파일이 곧 현재 정책이다

`01_reward_policies.sql`은 맨 앞 `[0]` 단계에서 **파일에 선언되지 않은 정책을 `is_active = FALSE`로 끈다.**
따라서 정책을 끄고 싶으면 파일에서 그 행을 지우고 재기동하면 된다.

행을 `DELETE` 하지 않는 이유는 `reward_logs`·`xp_logs`의 `reward_policy_id`가
"이 지급이 어떤 정책 때문이었는지"를 가리키기 때문이다. 지우면 과거 지급 근거가 끊긴다.

⚠️ **이 `[0]` 단계는 로컬·개발 전용이다.** 시드 목록에 없는 정책은 예외 없이 꺼지므로
DB에서 직접 만든 정책도 다음 기동에 비활성화된다.
AR-ADMIN-04(관리자가 `reward_policies` 관리) 기능이 생기거나 운영에 배포할 때는
관리자가 화면에서 만든 정책이 조용히 꺼지므로 **반드시 제거**해야 한다.
배포 준비 시 Flyway로 옮기면서 함께 정리할 항목이다.

## 파일

| 파일 | 내용 | 상태 |
| --- | --- | --- |
| `01_reward_policies.sql` | 보상 정책 19행 (XP 3 / 코인 4 = 최초 통과 2 + 재응시 2 / 퀴즈·출석 2 / 레벨업 6 / 스타터팩·광고·프리미엄 4) | 적용 중 |

로컬에서 확인된 동작: 백엔드 기동 1회 → 19행 적용, 재기동 → 그대로 19행(중복 없음).
테스트에서는 `src/test/resources/application.yaml`이 main 설정을 대체하므로 시드가 실행되지 않는다.
H2에는 `ON DUPLICATE KEY UPDATE`가 없어 오히려 실행되면 실패하므로, 이 분리가 맞다.

## 아직 만들지 못한 시드

수치는 확정됐지만 **선행 조건이 없어 보류**한 것들이다.

| 시드 | 막힌 이유 |
| --- | --- |
| `feature_access_products` (AI 분석표 100/50코인, 월 5/30회) | **JPA 엔티티가 없다.** 엔티티가 없으면 `ddl-auto`가 테이블을 만들지 않아 INSERT가 실패한다 |
| `subscription_plans` (프리미엄 혜택) | **JPA 엔티티가 없다.** 구독 원화 금액도 미정 |
| `shop_products` (상점 5개 상품) | 엔티티는 있으나 `item_id`·`random_box_id` FK가 `avatar_items`·`random_boxes`를 참조. 아바타 아이템 품목이 미정 |
| `random_boxes` / `random_box_items` (확률표) | `random_box_items.item_id`가 `avatar_items`를 참조. 아이템 마스터가 먼저 확정돼야 한다 |

## 주의

`01_reward_policies.sql`의 레벨업·스타터팩 행은 **코인 부분만** 넣었다.
랜덤박스·특별 아이템 지급은 아이템 마스터가 확정된 뒤 별도 처리가 필요하다.
