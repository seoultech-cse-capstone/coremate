import _bootstrap

import json
import sys
from pathlib import Path

import yaml
from tqdm import tqdm

from schemas.common_mistake_schema import COMMON_MISTAKE_SCHEMA
from src.ai_client import call_ai_json


if len(sys.argv) < 2:
    raise RuntimeError("사용법: python scripts/05_generate_common_mistakes.py {SUBJECT_CODE}")

subject_code = sys.argv[1]

config_path = Path(f"configs/{subject_code}.yaml")

if config_path.exists():
    config = yaml.safe_load(config_path.read_text(encoding="utf-8"))
else:
    config = {}

common_mistake_rules = config.get("common_mistake_rules", {})

review_status_default = common_mistake_rules.get("review_status_default", "draft")
is_active_default = common_mistake_rules.get("is_active_default", False)

concept_cards_path = Path(f"outputs/{subject_code}/concept_cards.json")
relations_path = Path(f"outputs/{subject_code}/concept_relations.json")

if not concept_cards_path.exists():
    raise RuntimeError(f"concept_cards.json이 없습니다: {concept_cards_path}")

if not relations_path.exists():
    raise RuntimeError(f"concept_relations.json이 없습니다: {relations_path}")

concept_cards = json.loads(concept_cards_path.read_text(encoding="utf-8"))
relations = json.loads(relations_path.read_text(encoding="utf-8"))

prompt_template = Path("prompts/generate_common_mistakes.md").read_text(encoding="utf-8")


def find_relations_for_concept(concept_code: str) -> list[dict]:
    return [
        relation
        for relation in relations
        if relation["source_concept_code"] == concept_code
        or relation["target_concept_code"] == concept_code
    ]


def attach_mistake_codes(
    *,
    concept_code: str,
    mistakes: list[dict],
) -> list[dict]:
    normalized_mistakes = []

    for index, mistake in enumerate(mistakes, start=1):
        mistake_code = f"{concept_code}_M{index:02d}"

        normalized_mistakes.append(
            {
                "mistake_code": mistake_code,
                "mistake_title": mistake["mistake_title"],
                "mistake_text": mistake["mistake_text"],
                "correction_text": mistake["correction_text"],
                "review_status": review_status_default,
                "review_note": None,
                "is_active": is_active_default,
            }
        )

    return normalized_mistakes


results = []

for concept_card in tqdm(concept_cards):
    concept_code = concept_card["concept_code"]
    related_relations = find_relations_for_concept(concept_code)

    user_prompt = prompt_template.format(
        concept_card_json=json.dumps(concept_card, ensure_ascii=False, indent=2),
        relations_json=json.dumps(related_relations, ensure_ascii=False, indent=2),
    )

    result = call_ai_json(
        system_prompt="너는 CS 전공 학습자의 공통 오개념을 정리하는 전문가다.",
        user_prompt=user_prompt,
        schema_name="common_mistake_schema",
        schema=COMMON_MISTAKE_SCHEMA,
        reasoning_effort="low",
    )

    mistakes = result.get("mistakes", [])

    results.append(
        {
            "concept_code": concept_code,
            "concept_name": concept_card["concept_name"],
            "mistakes": attach_mistake_codes(
                concept_code=concept_code,
                mistakes=mistakes,
            ),
        }
    )

out_dir = Path(f"outputs/{subject_code}")
out_dir.mkdir(parents=True, exist_ok=True)

out_path = out_dir / "common_mistakes.json"

with open(out_path, "w", encoding="utf-8") as f:
    json.dump(results, f, ensure_ascii=False, indent=2)

total_mistake_count = sum(len(item["mistakes"]) for item in results)

print(f"common_mistakes 생성 완료: {out_path}")
print(f"생성된 concept별 mistake 묶음 수: {len(results)}")
print(f"생성된 mistake 총 개수: {total_mistake_count}")