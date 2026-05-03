CONCEPT_CARD_SCHEMA = {
    "type": "object",
    "additionalProperties": False,
    "required": ["concept_card"],
    "properties": {
        "concept_card": {
            "type": "object",
            "additionalProperties": False,
            "required": [
                "subject_name",
                "category_lv1",
                "category_lv2",
                "concept_code",
                "concept_name",
                "concept_name_en",
                "aliases",
                "one_line_definition",
                "core_summary",
                "core_keywords",
                "key_properties",
                "source_refs",
                "insufficient_source",
            ],
            "properties": {
                "subject_name": {"type": "string"},
                "category_lv1": {"type": "string"},
                "category_lv2": {"type": "string"},
                "concept_code": {"type": "string"},
                "concept_name": {"type": "string"},
                "concept_name_en": {"type": "string"},
                "aliases": {"type": "array", "items": {"type": "string"}},
                "one_line_definition": {"type": "string"},
                "core_summary": {"type": "string"},
                "core_keywords": {"type": "array", "items": {"type": "string"}},
                "key_properties": {"type": "array", "items": {"type": "string"}},
                "source_refs": {
                    "type": "array",
                    "items": {
                        "type": "object",
                        "additionalProperties": False,
                        "required": ["chunk_id", "source_file", "page"],
                        "properties": {
                            "chunk_id": {"type": "string"},
                            "source_file": {"type": "string"},
                            "page": {"type": "integer"},
                        },
                    },
                },
                "insufficient_source": {"type": "boolean"},
            },
        }
    },
}