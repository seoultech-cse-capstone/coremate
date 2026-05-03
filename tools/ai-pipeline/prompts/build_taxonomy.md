너는 컴퓨터공학 전공 교육과정과 학습 앱 콘텐츠 구조를 설계하는 역할이다.

아래 과목에 대해 CoreMate 앱에서 사용할 개념 트리 초안을 생성하라.

입력 과목:
subject_code: {subject_code}
subject_name: {subject_name}
subject_name_en: {subject_name_en}

과목별 범위 설정:
taxonomy_rules:
{taxonomy_rules_json}

taxonomy_scope:
{taxonomy_scope_json}

생성 구조:
subject
→ category_lv1
→ category_lv2
→ concept_candidates

중요 원칙:
- 이 taxonomy는 최종 DB 저장 전 사람이 검수할 초안이다.
- subject는 입력된 과목 하나만 사용한다.
- category_lv1은 과목의 큰 학습 단위다.
- category_lv2는 앱에서 챕터 또는 문제집 묶음으로 사용할 수 있는 중간 단위다.
- concept_candidates는 실제 문제 생성 기준이 되는 하위 개념이다.
- concept는 너무 넓으면 안 된다.
- concept는 가능하면 하나의 문제에서 평가 가능한 단위여야 한다.

과목 범위 규칙:
- taxonomy_scope.include_topics에 가까운 개념을 우선 포함한다.
- taxonomy_scope.exclude_topics에 있는 개념은 생성하지 않는다.
- overlap_policy를 따른다.
- 다른 과목에서 더 중심적으로 다루는 개념은 제외한다.
- 예를 들어 알고리즘 과목에서 중심적으로 다룰 정렬 알고리즘, 최단 경로 알고리즘, 최소 신장 트리 알고리즘 등은 제외한다.

중복 방지 규칙:
- 같은 concept_name은 taxonomy 전체에서 한 번만 등장해야 한다.
- 같은 의미의 개념을 다른 category_lv2 아래에 중복 배치하지 않는다.
- 어떤 개념이 다른 개념의 선수 개념으로 필요하면 중복 배치하지 말고 나중에 concept_relation에서 prerequisite으로 연결할 수 있게 한다.
- 예를 들어 어떤 개념이 힙을 이해하는 데 필요하더라도 이미 이진 트리 쪽에 존재한다면 힙 쪽에 다시 만들지 않는다.

개념명 규칙:
- concept_name은 한국어 공식명으로 작성한다.
- concept_name_en은 영어 공식명으로 작성한다.
- aliases에는 한국어 표기 변형, 띄어쓰기 변형, 영어 표현을 넣는다.
- 서로 다른 개념을 같은 alias 안에 섞지 않는다.
- 헷갈리는 개념은 하나의 concept에 섞지 말고 별도 concept로 두거나 나중에 concept_relation의 confusing으로 연결한다.

세분화 규칙:
- concept_granularity_policy를 따른다.
- 단순 연산 하나를 모두 concept로 쪼개기보다, 문제 생성에 의미 있는 학습 단위를 우선한다.
- 단, 삽입/삭제/탐색처럼 자료구조의 핵심 성질을 구분하는 데 중요한 연산은 concept로 둘 수 있다.

생성 금지:
- common_mistake는 생성하지 않는다.
- concept_relation은 생성하지 않는다.
- question은 생성하지 않는다.
- concept 설명문을 길게 작성하지 않는다.
- 출력은 반드시 JSON Schema를 따른다.