import _bootstrap

import json
import sys
from pathlib import Path

from schemas.concept_relation_schema import CONCEPT_RELATION_SCHEMA
from src.ai_client import call_ai_json


if len(sys.argv) < 2:
    raise RuntimeError("사용법: python scripts/04_generate_concept_relations.py {SUBJECT_CODE}")

subject_code = sys.argv[1]

concept_cards = json.loads(
    Path(f"outputs/{subject_code}/concept_cards.json").read_text(encoding="utf-8")
)

prompt_template = Path("prompts/generate_concept_relations.md").read_text(encoding="utf-8")

user_prompt = prompt_template.format(
    concept_cards_json=json.dumps(concept_cards, ensure_ascii=False, indent=2)
)

result = call_ai_json(
    system_prompt="너는 CS 전공 개념 간 관계를 정확히 분류하는 전문가다.",
    user_prompt=user_prompt,
    schema_name="concept_relation_schema",
    schema=CONCEPT_RELATION_SCHEMA,
    reasoning_effort="medium",
)

out_path = Path(f"outputs/{subject_code}/concept_relations.json")

with open(out_path, "w", encoding="utf-8") as f:
    json.dump(result["relations"], f, ensure_ascii=False, indent=2)

print(f"concept_relations 생성 완료: {out_path}")
print(f"생성된 relation 수: {len(result['relations'])}")