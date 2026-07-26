# 문항별 진단 프로필 생성용 strict JSON 스키마.
# question_diagnostic_profile 테이블의 AI 생성분(진단 요약/스킬 유형/오답 오개념 맵)만 규정한다.
# expected_solve_sec·difficulty 는 이미 question 데이터에 있으므로 AI가 생성하지 않고
# 스크립트가 문항 원본에서 그대로 채운다.
# answer_misconception_map: 각 선택지가 드러내는 오개념/해석(오답의 진단 가치 핵심).
QUESTION_DIAGNOSTIC_PROFILE_SCHEMA = {
    "type": "object",
    "additionalProperties": False,
    "required": [
        "diagnostic_summary",
        "question_skill_type",
        "answer_misconception_map",
    ],
    "properties": {
        "diagnostic_summary": {"type": "string"},
        "question_skill_type": {
            "type": "string",
            "enum": ["RECALL", "COMPREHENSION", "APPLICATION", "ANALYSIS"],
        },
        "answer_misconception_map": {
            "type": "array",
            "items": {
                "type": "object",
                "additionalProperties": False,
                "required": ["option_key", "is_correct", "interpretation"],
                "properties": {
                    "option_key": {"type": "string"},
                    "is_correct": {"type": "boolean"},
                    "interpretation": {"type": "string"},
                },
            },
        },
    },
}
