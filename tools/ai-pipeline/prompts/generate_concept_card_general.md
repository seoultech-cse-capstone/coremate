너는 컴퓨터공학 전공 개념을 CoreMate의 공통 조사 템플릿에 맞게 정리하는 역할이다.

아래 concept 후보와 (있다면) source_chunks를 바탕으로 concept_card를 생성하라.
source_chunks가 비어 있거나 해당 개념과 무관하면, 널리 인정되는 표준 컴퓨터공학 일반지식을 근거로 작성한다.

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
- source_chunks에 관련 근거가 있으면 그 내용을 우선 활용한다.
- source_chunks가 비었거나 무관하면, 학계·교재에서 표준으로 통용되는 정확한 정의를 일반지식으로 작성한다. 이 경우에도 절대 필드를 비워두지 않는다.
- 불확실하거나 논쟁적인 세부는 넣지 말고, 널리 합의된 핵심만 기술한다.
- one_line_definition은 한 문장으로 짧게, 반드시 실제 정의를 작성한다. "자료가 부족하다", "source_chunks에 근거가 없다" 같은 안내 문구는 절대 쓰지 않는다.
- core_summary는 2~4문장으로 작성한다.
- core_keywords는 3~5개로 작성한다.
- key_properties는 문제 생성에 활용할 수 있는 핵심 성질 중심으로 작성한다.
- source_refs에는 실제로 참고한 source_chunks의 chunk_id, source_file, page만 넣는다. 참고한 chunk가 없으면 빈 배열로 둔다.
- insufficient_source 플래그는 근거 출처를 정직하게 반영한다: source_chunks의 관련 근거로 작성했으면 false, 관련 근거 없이 일반지식으로 작성했으면 true로 설정한다. (플래그 값과 무관하게 정의·요약 필드는 어느 경우든 반드시 실제 내용으로 채운다.)
- common_mistake는 생성하지 않는다.
- concept_relation은 생성하지 않는다.
- question은 생성하지 않는다.
- 출력은 반드시 JSON Schema를 따른다.
