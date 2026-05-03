너는 컴퓨터공학 전공 개념을 CoreMate의 공통 조사 템플릿에 맞게 정리하는 역할이다.

아래 concept 후보와 source_chunks를 바탕으로 concept_card를 생성하라.

입력 concept:
{concept_json}

source_chunks:
{source_chunks_json}

공통 조사 템플릿 필드:
- subject_name
- category_lv1
- category_lv2
- concept_name
- concept_name_en
- aliases
- one_line_definition
- core_summary
- core_keywords
- key_properties
- source_refs

규칙:
- concept_name은 입력된 공식명을 유지한다.
- aliases는 입력 alias를 유지하되, 필요한 표기 변형이 있으면 추가할 수 있다.
- source_chunks에 근거가 있는 내용 중심으로 작성한다.
- 전공서적이나 강의자료 문장을 그대로 길게 복사하지 않는다.
- one_line_definition은 한 문장으로 짧게 작성한다.
- core_summary는 2~4문장으로 작성한다.
- core_keywords는 3~5개로 작성한다.
- key_properties는 문제 생성에 활용할 수 있는 핵심 성질 중심으로 작성한다.
- source_refs에는 실제로 참고한 chunk_id, source_file, page만 넣는다.
- source_chunks가 부족하면 억지로 내용을 만들지 말고 insufficient_source를 true로 표시한다.
- common_mistake는 생성하지 않는다.
- concept_relation은 생성하지 않는다.
- question은 생성하지 않는다.
- 출력은 반드시 JSON Schema를 따른다.