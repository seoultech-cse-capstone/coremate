TAXONOMY_SCHEMA = {
    "type": "object",
    "additionalProperties": False,
    "required": ["subject", "category_lv1_list"],
    "properties": {
        "subject": {
            "type": "object",
            "additionalProperties": False,
            "required": ["subject_code", "subject_name", "subject_name_en"],
            "properties": {
                "subject_code": {"type": "string"},
                "subject_name": {"type": "string"},
                "subject_name_en": {"type": "string"},
            },
        },
        "category_lv1_list": {
            "type": "array",
            "items": {
                "type": "object",
                "additionalProperties": False,
                "required": [
                    "category_lv1_code",
                    "category_lv1_name",
                    "category_lv1_name_en",
                    "description",
                    "category_lv2_list",
                ],
                "properties": {
                    "category_lv1_code": {"type": "string"},
                    "category_lv1_name": {"type": "string"},
                    "category_lv1_name_en": {"type": "string"},
                    "description": {"type": "string"},
                    "category_lv2_list": {
                        "type": "array",
                        "items": {
                            "type": "object",
                            "additionalProperties": False,
                            "required": [
                                "category_lv2_code",
                                "category_lv2_name",
                                "category_lv2_name_en",
                                "description",
                                "concept_candidates",
                            ],
                            "properties": {
                                "category_lv2_code": {"type": "string"},
                                "category_lv2_name": {"type": "string"},
                                "category_lv2_name_en": {"type": "string"},
                                "description": {"type": "string"},
                                "concept_candidates": {
                                    "type": "array",
                                    "items": {
                                        "type": "object",
                                        "additionalProperties": False,
                                        "required": [
                                            "concept_code",
                                            "concept_name",
                                            "concept_name_en",
                                            "aliases",
                                        ],
                                        "properties": {
                                            "concept_code": {"type": "string"},
                                            "concept_name": {"type": "string"},
                                            "concept_name_en": {"type": "string"},
                                            "aliases": {
                                                "type": "array",
                                                "items": {"type": "string"},
                                            },
                                        },
                                    },
                                },
                            },
                        },
                    },
                },
            },
        },
    },
}