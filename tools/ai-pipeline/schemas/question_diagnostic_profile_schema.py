# 문항별 진단 프로필 생성용 strict JSON 스키마.
# question_diagnostic_profile 테이블의 AI 생성분(진단 요약/스킬 유형/오답 오개념 맵)만 규정한다.
# expected_solve_sec·difficulty 는 이미 question 데이터에 있으므로 AI가 생성하지 않고
# 스크립트가 문항 원본에서 그대로 채운다.
# answer_misconception_map: 각 선택지가 드러내는 오개념/해석(오답의 진단 가치 핵심).
#
# misconception_code 는 백엔드가 "같은 오개념이 반복되는지"를 세는 식별자다
# (약점분석 AI 입력의 misconception_code / repeated_misconceptions).
# 생성 시 개념별 common_mistakes 의 mistake_code 목록을 enum 으로 주입하고
# (build_question_diagnostic_schema_for_mistakes), 해당 없으면 OTHER,
# 정답 선택지는 NONE 을 쓴다.
NO_MISCONCEPTION_CODE = "NONE"
OTHER_MISCONCEPTION_CODE = "OTHER"

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
                "required": ["option_key", "is_correct", "interpretation", "misconception_code"],
                "properties": {
                    "option_key": {"type": "string"},
                    "is_correct": {"type": "boolean"},
                    "interpretation": {"type": "string"},
                    "misconception_code": {"type": "string"},
                },
            },
        },
    },
}


def build_question_diagnostic_schema_for_mistakes(mistakes: list[dict]) -> dict:
    """
    현재 개념에서 쓸 수 있는 mistake_code 만 misconception_code enum 으로 허용한다.
    (06_generate_questions.py 의 hint_mistake_code 처리와 같은 방식)

    이렇게 해야 모델이 임의 코드나 한국어 문장을 코드 자리에 넣는 것을 막을 수 있다.
    """
    import copy

    schema = copy.deepcopy(QUESTION_DIAGNOSTIC_PROFILE_SCHEMA)

    allowed_codes = sorted(
        {
            mistake["mistake_code"]
            for mistake in mistakes
            if mistake.get("mistake_code")
        }
    )
    allowed_codes += [OTHER_MISCONCEPTION_CODE, NO_MISCONCEPTION_CODE]

    schema["properties"]["answer_misconception_map"]["items"]["properties"][
        "misconception_code"
    ]["enum"] = allowed_codes

    return schema
