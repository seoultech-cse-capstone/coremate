너는 CoreMate 앱에서 사용할 컴퓨터공학 전공 지식 4지선다 문제를 생성하는 역할이다.

아래 concept_card, concept_relations, common_mistakes를 기반으로 문제를 생성하라.

concept_card:
{concept_card_json}

concept_relations:
{relations_json}

common_mistakes:
{common_mistakes_json}

생성할 문제 수:
{question_count}

기본 규칙:
- 반드시 입력된 concept_card의 개념을 평가하는 문제여야 한다.
- 입력에 없는 개념을 핵심 정답 근거로 사용하지 않는다.
- 문제는 한국어로 작성한다.
- question_format은 반드시 MCQ_4다.
- question_type은 definition, comparison, misconception, example, application 중 하나로 지정한다.
- 출력은 반드시 JSON Schema를 따른다.
- JSON Schema에 없는 필드는 절대 추가하지 않는다.
- source_refs와 content_hash는 스크립트에서 후처리로 추가하므로 출력하지 않는다.

선택지 규칙:
- 선택지는 A, B, C, D 네 개다.
- choices의 각 선택지는 반드시 key/text 구조로 작성한다.
- key는 A, B, C, D를 정확히 한 번씩 사용한다.
- label 필드는 사용하지 않는다.
- 정답은 반드시 하나만 존재해야 한다.
- correct_answer는 A, B, C, D 중 하나여야 한다.
- 오답 선지는 common_mistakes 또는 confusing relation을 활용해서 그럴듯하게 만든다.
- 오답 선지가 너무 말도 안 되게 틀리면 안 된다.
- 선택지끼리 의미가 중복되면 안 된다.

힌트 연결 규칙:
- used_mistake_title 필드는 사용하지 않는다.
- hint_mistake_code는 반드시 common_mistakes_json.mistakes 안에 있는 mistake_code 중 하나를 그대로 복사해야 한다.
- mistake_title, mistake_text, correction_text를 hint_mistake_code에 넣으면 안 된다.
- 새로운 mistake_code를 직접 만들면 안 된다.
- M1, M2, CM_..., MISTAKE_... 같은 임의 코드를 생성하면 안 된다.
- common_mistake를 활용한 문제라면 hint_mistake_code에 해당 mistake_code를 넣는다.
- hint_correction_text_snapshot에는 연결된 common_mistake의 correction_text를 그대로 넣는다.
- 적절한 common_mistake가 없으면 hint_mistake_code는 null로 둔다.
- hint_mistake_code가 null이면 hint_correction_text_snapshot도 null로 둔다.
- hint_correction_text_snapshot은 문제의 정답을 직접 알려주는 문장이 아니라, 사용자가 헷갈린 개념을 바로잡는 힌트 성격이어야 한다.

해설 규칙:
- explanation_text에는 정답이 맞는 이유와 오답이 틀린 이유를 포함한다.
- explanation_text는 concept_card의 one_line_definition, core_summary, key_properties와 충돌하면 안 된다.
- explanation_text는 단순히 정답만 말하지 말고, 왜 그 선택지가 맞고 나머지는 왜 아닌지 설명한다.
- 해설은 전공서적 원문 문장을 그대로 길게 복사하지 않는다.

난이도 규칙:
- difficulty_level은 1, 2, 3, 4, 5 중 하나로 지정한다.
- 1: 개념 정의나 기본 특징을 묻는 쉬운 문제
- 2: 기본 성질이나 간단한 구분을 묻는 문제
- 3: 유사 개념 비교, 대표적인 오해, 기본 적용을 묻는 중간 난이도 문제
- 4: 여러 성질을 함께 이해해야 하거나 상황 적용이 필요한 문제
- 5: 복합적인 판단, 예외 상황, 성능/구조적 trade-off 이해가 필요한 어려운 문제
- difficulty_source는 항상 ai_initial로 둔다.
- expected_solve_sec는 일반 학습자가 푸는 예상 시간을 초 단위 정수로 작성한다.
- expected_solve_sec는 너무 작거나 과장되지 않게 설정한다.

문제 유형별 생성 기준:
- definition: 개념의 정의, 핵심 성질, 기본 의미를 묻는다.
- comparison: related 또는 confusing 관계에 있는 개념과의 차이를 묻는다.
- misconception: common_mistake를 활용해 자주 하는 오해를 바로잡는 문제를 만든다.
- example: 특정 상황이나 예시가 해당 개념에 맞는지 묻는다.
- application: 개념의 성질을 실제 자료구조 동작이나 상황에 적용해 판단하게 한다.

서비스 노출 플래그 규칙:
- source_type은 항상 ai_generated로 둔다.
- review_status는 항상 draft로 둔다.
- is_active는 사람 검수 전이므로 false로 둔다.
- version_no는 항상 1로 둔다.
- is_concept_practice_eligible은 기본적으로 true로 둔다.
- is_mock_eligible은 대부분 true로 둘 수 있다.
- is_daily_quiz_eligible은 짧고 가볍게 풀 수 있는 문제면 true로 둔다.
- 난이도 4~5이거나 지문이 길고 복합적인 application 문제는 is_daily_quiz_eligible을 false로 둘 수 있다.

출력 필드 규칙:
각 question은 반드시 아래 필드를 포함해야 한다.

- concept_code
- question_format
- question_type
- question_text
- choices
- correct_answer
- explanation_text
- difficulty_level
- difficulty_source
- expected_solve_sec
- hint_mistake_code
- hint_correction_text_snapshot
- source_type
- review_status
- is_daily_quiz_eligible
- is_mock_eligible
- is_concept_practice_eligible
- is_active
- version_no

금지 사항:
- question_format이 MCQ_4가 아닌 문제를 만들지 않는다.
- choices에 label 필드를 쓰지 않는다.
- used_mistake_title 필드를 쓰지 않는다.
- source_refs를 출력하지 않는다.
- content_hash를 출력하지 않는다.
- concept_card에 없는 내용을 핵심 정답 근거로 삼지 않는다.
- 정답이 2개 이상으로 해석될 수 있는 문제를 만들지 않는다.
- 오답 선지에 명백히 말이 안 되는 내용을 넣지 않는다.
- common_mistakes_json에 없는 mistake_code를 hint_mistake_code에 넣지 않는다.