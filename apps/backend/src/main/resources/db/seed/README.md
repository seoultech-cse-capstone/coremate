# 정책 시드 데이터

서비스 동작에 **필요한** 정책 데이터다. `scripts/db/*.sql`(약점분석 손검증용 폐기 더미)과 성격이 다르다.

## 적용 방식

`application.yaml`의 `spring.sql.init`이 **백엔드 기동 시 자동 실행**한다.
`spring.jpa.defer-datasource-initialization: true`로 두어 Hibernate가 테이블을 만든 **뒤에** 실행된다.

모든 파일은 **멱등**하게 작성한다(`INSERT ... ON DUPLICATE KEY UPDATE`).
매 기동마다 실행되므로 멱등하지 않으면 중복 행이 쌓인다.

## 파일

| 파일 | 내용 | 상태 |
| --- | --- | --- |
| `01_reward_policies.sql` | 보상 정책 17행 (XP 3 / 코인 2 / 퀴즈·출석 2 / 레벨업 6 / 스타터팩·광고·프리미엄 4) | 적용 중 |

로컬에서 확인된 동작: 백엔드 기동 1회 → 17행 적용, 재기동 → 그대로 17행(중복 없음).
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
