# 개념별 진단 프로필 생성용 strict JSON 스키마.
# concept_diagnostic_profile 테이블(개념 핵심/설명/혼동 비교/선수 개념 등)을 채우는
# AI 출력 규격이다. OpenAI Structured Outputs(strict) 제약에 맞춰
# additionalProperties=False + 모든 필드 required 로 구성한다.
# (개수/길이 제한은 strict 호환을 위해 스키마에서 빼고 프롬프트로 유도한다.)
CONCEPT_DIAGNOSTIC_PROFILE_SCHEMA = {
    "type": "object",
    "additionalProperties": False,
    "required": [
        "concept_core",
        "concept_explanation",
        "key_points",
        "confusion_comparisons",
        "prerequisite_concepts",
        "learning_example",
        "misconception_notes",
        "basic_review_points",
        "misconception_mapping_coverage",
    ],
    "properties": {
        "concept_core": {"type": "string"},
        "concept_explanation": {"type": "string"},
        "key_points": {
            "type": "array",
            "items": {"type": "string"},
        },
        "confusion_comparisons": {
            "type": "array",
            "items": {
                "type": "object",
                "additionalProperties": False,
                "required": ["compared_with", "difference"],
                "properties": {
                    "compared_with": {"type": "string"},
                    "difference": {"type": "string"},
                },
            },
        },
        "prerequisite_concepts": {
            "type": "array",
            "items": {"type": "string"},
        },
        "learning_example": {"type": "string"},
        "misconception_notes": {"type": "string"},
        "basic_review_points": {
            "type": "array",
            "items": {"type": "string"},
        },
        "misconception_mapping_coverage": {"type": "number"},
    },
}
