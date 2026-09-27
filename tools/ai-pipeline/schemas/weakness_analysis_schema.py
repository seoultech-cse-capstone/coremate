# OpenAI Structured Outputs(strict json_schema)용 약점분석 출력 스키마.
# 백엔드 apps/backend/src/main/resources/ai/weakness-analysis-output.schema.json 를 이식.
# 주의: strict 모드 호환을 위해 maxLength 는 제외하고 길이 제약은 프롬프트로 유도한다
#       (maxItems/enum 은 유지). 전 필드 required + additionalProperties:false.

WEAKNESS_ANALYSIS_SCHEMA = {
    "type": "object",
    "additionalProperties": False,
    "required": ["overview", "concept_analyses"],
    "properties": {
        "overview": {"type": "string"},
        "concept_analyses": {
            "type": "array",
            "items": {
                "type": "object",
                "additionalProperties": False,
                "required": [
                    "concept_id",
                    "analysis_status",
                    "primary_type",
                    "secondary_signals",
                    "cause_code",
                    "confused_relation",
                    "diagnosis",
                    "likely_cause",
                    "concept_tip",
                    "evidence_refs",
                ],
                "properties": {
                    "concept_id": {"type": "integer"},
                    "analysis_status": {
                        "type": "string",
                        "enum": ["SUPPORTED", "AMBIGUOUS", "INSUFFICIENT"],
                    },
                    "primary_type": {
                        "type": "string",
                        "enum": [
                            "CONCEPT_CONFUSION",
                            "APPLICATION_GAP",
                            "SPEED_FLUENCY",
                            "EXECUTION_ERROR",
                        ],
                    },
                    "secondary_signals": {
                        "type": "array",
                        "maxItems": 2,
                        "items": {
                            "type": "string",
                            "enum": [
                                "REPEATED_MISCONCEPTION",
                                "CONCEPT_RELATION_CONFUSION",
                                "HINT_DEPENDENCY",
                                "SLOW_FLUENCY",
                                "INCONSISTENT_EXECUTION",
                            ],
                        },
                    },
                    "cause_code": {
                        "type": "string",
                        "enum": [
                            "TERM_CONFUSION",
                            "RELATIONSHIP_MAPPING_GAP",
                            "APPLICATION_GAP",
                            "PROCEDURE_FLUENCY_GAP",
                            "REPEATED_MISCONCEPTION",
                            "CARELESS_EXECUTION",
                        ],
                    },
                    "confused_relation": {"type": "string"},
                    "diagnosis": {"type": "string"},
                    "likely_cause": {"type": "string"},
                    "concept_tip": {"type": "string"},
                    "evidence_refs": {
                        "type": "array",
                        "maxItems": 2,
                        "items": {"type": "string"},
                    },
                },
            },
        },
    },
}
