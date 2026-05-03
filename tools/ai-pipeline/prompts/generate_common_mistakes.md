너는 컴퓨터공학 학습자가 특정 개념을 공부할 때 자주 하는 오해를 정리하는 역할이다.

아래 concept_card와 관련 concept_relations를 바탕으로 common_mistake를 생성하라.

concept_card:
{concept_card_json}

related_relations:
{relations_json}

규칙:
- common_mistake는 사용자 개인 오답 기록이 아니라 개념 기반 공용 오해다.
- 하나의 mistake는 하나의 오해만 다룬다.
- mistake_title은 짧고 명확하게 작성한다.
- mistake_text는 학생이 잘못 이해하는 내용을 작성한다.
- correction_text는 그 오해를 바로잡는 설명이다.
- correction_text는 문제 풀이 중 물음표 힌트로 재사용될 수 있다.
- correction_text는 정답을 직접 알려주지 않고, 사용자가 어떤 개념을 구분해야 하는지 방향을 잡아주는 1~2문장으로 작성한다.
- concept_card의 key_properties를 적극 활용한다.
- confusing 관계가 있으면 오해 생성에 반영한다.
- 억지스러운 오해를 만들지 않는다.
- 2~4개의 mistake를 생성한다.
- mistake_code는 출력하지 않는다.
- review_status, review_note, is_active는 출력하지 않는다.
- mistake_code, review_status, review_note, is_active는 스크립트에서 후처리로 붙인다.
- 출력은 반드시 JSON Schema를 따른다.