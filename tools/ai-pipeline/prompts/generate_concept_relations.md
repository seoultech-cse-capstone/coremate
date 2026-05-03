너는 컴퓨터공학 개념 간 관계를 정리하는 역할이다.

아래 concept_cards 목록만 사용해서 concept_relation 후보를 생성하라.

concept_cards:
{concept_cards_json}

허용 relation_type:
- related
- confusing
- prerequisite

규칙:
- 입력 목록에 없는 concept는 절대 만들지 않는다.
- source_concept_code와 target_concept_code는 반드시 입력 concept_cards 안에 있는 값만 사용한다.
- source_concept_code와 target_concept_code가 같으면 안 된다.
- relation_type은 related, confusing, prerequisite 중 하나만 사용한다.
- confusing은 학생들이 실제로 자주 헷갈릴 만한 개념에만 사용한다.
- prerequisite은 source 개념이 target 개념을 이해하기 위한 선수 개념일 때만 사용한다.
- relation_note에는 왜 연결되는지 짧게 작성한다.
- 출력은 반드시 JSON Schema를 따른다.