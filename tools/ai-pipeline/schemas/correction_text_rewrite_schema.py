CORRECTION_TEXT_REWRITE_SCHEMA = {
    "type": "object",
    "additionalProperties": False,
    "required": ["rewrites"],
    "properties": {
        "rewrites": {
            "type": "array",
            "items": {
                "type": "object",
                "additionalProperties": False,
                "required": ["mistake_code", "correction_text"],
                "properties": {
                    "mistake_code": {"type": "string"},
                    "correction_text": {"type": "string"},
                },
            },
        },
    },
}
