COMMON_MISTAKE_SCHEMA = {
    "type": "object",
    "additionalProperties": False,
    "required": ["concept_code", "mistakes"],
    "properties": {
        "concept_code": {"type": "string"},
        "mistakes": {
            "type": "array",
            "items": {
                "type": "object",
                "additionalProperties": False,
                "required": [
                    "mistake_title",
                    "mistake_text",
                    "correction_text",
                ],
                "properties": {
                    "mistake_title": {"type": "string"},
                    "mistake_text": {"type": "string"},
                    "correction_text": {"type": "string"},
                },
            },
        },
    },
}