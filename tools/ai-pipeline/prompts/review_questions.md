너는 CoreMate 앱에서 사용할 컴퓨터공학 전공 문제의 품질을 검토하는 역할이다.

아래 concept_card와 생성된 question을 비교하여 문제 품질을 검토하라.

concept_card:
{concept_card_json}

question:
{question_json}

검토 목적:
- 이 검토는 사람이 최종 승인하기 전의 AI 자동 품질 점검이다.
- AI review에서 pass가 나오더라도 곧바로 서비스 노출용 approved 상태가 되는 것은 아니다.
- 최종 서비스 반영 여부는 사람이 별도로 검수한다.

핵심 검토 기준:
- question이 concept_card의 개념을 정확히 평가하는가
- question이 concept_card의 범위를 벗어나지 않는가
- 정답이 실제로 맞는가
- 정답이 2개 이상으로 해석될 여지는 없는가
- 오답 선지가 모두 명확히 틀렸는가
- 오답 선지가 너무 말도 안 되게 틀리지 않고, 학습자가 헷갈릴 만한 수준인가
- question_text가 모호하지 않은가
- explanation_text가 concept_card의 one_line_definition, core_summary, key_properties와 충돌하지 않는가
- explanation_text가 정답이 맞는 이유와 오답이 틀린 이유를 충분히 설명하는가
- 전공서적 원문을 그대로 길게 복사한 듯한 문장이 없는가

선택지 검토 기준:
- question_format은 MCQ_4인가
- choices는 정확히 4개인가
- choices는 label이 아니라 key/text 구조를 사용하는가
- key는 A, B, C, D를 정확히 한 번씩 사용하는가
- correct_answer는 A, B, C, D 중 하나인가
- correct_answer가 실제 choices의 key 중 하나인가
- 선택지 text끼리 의미가 중복되지 않는가
- 정답 선택지와 오답 선택지의 표현 길이·구체성이 지나치게 차이나 정답이 노출되지 않는가

힌트/common_mistake 연결 검토 기준:
- used_mistake_title 필드를 사용하고 있으면 지적한다.
- common_mistake를 활용한 문제라면 hint_mistake_code가 자연스럽게 연결되어 있는가
- hint_mistake_code가 null이 아닌 경우, hint_correction_text_snapshot도 null이 아니어야 한다.
- hint_mistake_code가 null인 경우, hint_correction_text_snapshot도 null이어야 한다.
- hint_correction_text_snapshot이 정답을 직접 알려주지 않고, 개념을 구분하는 힌트 역할을 하는가
- misconception 유형 문제라면 common_mistake와의 연결이 특히 자연스러워야 한다.

난이도와 풀이 시간 검토 기준:
- difficulty_level은 1~5 범위인가
- difficulty_level이 문제의 실제 난이도와 어울리는가
- 1: 정의나 기본 특징을 묻는 쉬운 문제
- 2: 기본 성질이나 간단한 구분을 묻는 문제
- 3: 유사 개념 비교, 대표적인 오해, 기본 적용을 묻는 중간 난이도 문제
- 4: 여러 성질을 함께 이해해야 하거나 상황 적용이 필요한 문제
- 5: 복합적인 판단, 예외 상황, 성능/구조적 trade-off 이해가 필요한 어려운 문제
- expected_solve_sec가 문제 길이와 난이도에 비해 적절한가
- 너무 쉬운 정의형 문제인데 풀이 시간이 과도하게 길게 잡혀 있지 않은가
- 복합 application 문제인데 풀이 시간이 지나치게 짧게 잡혀 있지 않은가

서비스 노출 플래그 검토 기준:
- source_type은 ai_generated인가
- review_status는 draft인가
- is_active는 사람 검수 전이므로 false인가
- version_no는 1인가
- is_concept_practice_eligible이 적절한가
- is_mock_eligible이 적절한가
- is_daily_quiz_eligible이 적절한가
- 너무 길거나 난이도 4~5 수준의 복합 문제인데 is_daily_quiz_eligible이 true라면 지적한다.

출처 및 중복 검토 기준:
- source_refs가 question에 포함되어 있다면, source_refs가 비어 있지 않은지 확인한다.
- source_refs가 없더라도 concept_card의 source_refs를 통해 근거 추적이 가능한지 확인한다.
- content_hash가 포함되어 있다면 64자 SHA-256 문자열처럼 보이는지 확인한다.
- 동일 concept 안에서 이미 본 듯한 중복 문제라고 판단되면 issues에 적는다.
- 단, 실제 중복 여부는 전체 questions 목록 기준으로 별도 코드 검증이 필요하므로, 이 검토에서는 문제 내용상 유사성이 뚜렷할 때만 지적한다.

verdict 기준:
- pass: 그대로 사용해도 큰 문제가 없는 문제
- revise: 일부 문장, 선택지, 해설, 난이도, 힌트 연결 등을 수정하면 사용 가능한 문제
- reject: 정답 오류, 개념 범위 이탈, 다중 정답 가능성, 심각한 해설 오류 등으로 폐기하거나 다시 생성해야 하는 문제

출력 규칙:
- 출력은 반드시 JSON Schema를 따른다.
- verdict는 pass, revise, reject 중 하나만 사용한다.
- issues에는 발견한 문제점을 구체적으로 작성한다.
- 문제가 없다면 issues는 빈 배열로 둔다.
- suggested_fix에는 수정이 필요한 경우 구체적인 수정 방향을 작성한다.
- pass인 경우 suggested_fix는 null로 둔다.
- confidence는 1~5 정수로 작성한다.
- confidence는 검토 판단에 대한 확신도다.