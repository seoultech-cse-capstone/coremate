CONCEPT_RELATION_SCHEMA = {
    "type": "object",
    "additionalProperties": False,
    "required": ["relations"],
    "properties": {
        "relations": {
            "type": "array",
            "items": {
                "type": "object",
                "additionalProperties": False,
                "required": [
                    "source_concept_code",
                    "target_concept_code",
                    "relation_type",
                    "relation_note",
                ],
                "properties": {
                    "source_concept_code": {"type": "string"},
                    "target_concept_code": {"type": "string"},
                    "relation_type": {
                        "type": "string",
                        "enum": ["related", "confusing", "prerequisite"],
                    },
                    "relation_note": {"type": "string"},
                },
            },
        }
    },
}