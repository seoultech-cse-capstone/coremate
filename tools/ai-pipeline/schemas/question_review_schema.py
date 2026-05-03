QUESTION_REVIEW_SCHEMA = {
    "type": "object",
    "additionalProperties": False,
    "required": ["review"],
    "properties": {
        "review": {
            "type": "object",
            "additionalProperties": False,
            "required": [
                "verdict",
                "issues",
                "suggested_fix",
                "confidence",
            ],
            "properties": {
                "verdict": {"type": "string", "enum": ["pass", "revise", "reject"]},
                "issues": {"type": "array", "items": {"type": "string"}},
                "suggested_fix": {"type": ["string", "null"]},
                "confidence": {"type": "integer", "minimum": 1, "maximum": 5},
            },
        }
    },
}