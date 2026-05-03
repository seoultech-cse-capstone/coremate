import _bootstrap

import json
import sys
from pathlib import Path

from tqdm import tqdm

from schemas.concept_card_schema import CONCEPT_CARD_SCHEMA
from src.chunk_retriever import retrieve_chunks_by_keywords
from src.openai_client import call_openai_json


if len(sys.argv) < 2:
    raise RuntimeError("사용법: python scripts/03_generate_concept_cards.py {SUBJECT_CODE}")

subject_code = sys.argv[1]

taxonomy = json.loads(
    Path(f"outputs/{subject_code}/taxonomy.json").read_text(encoding="utf-8")
)

chunks = json.loads(
    Path(f"data/extracted/{subject_code}/chunks.json").read_text(encoding="utf-8")
)

prompt_template = Path("prompts/generate_concept_card.md").read_text(encoding="utf-8")


def iter_concepts(taxonomy_data: dict):
    subject = taxonomy_data["subject"]

    for lv1 in taxonomy_data["category_lv1_list"]:
        for lv2 in lv1["category_lv2_list"]:
            for concept in lv2["concept_candidates"]:
                yield {
                    "subject_code": subject["subject_code"],
                    "subject_name": subject["subject_name"],
                    "subject_name_en": subject["subject_name_en"],
                    "category_lv1_code": lv1["category_lv1_code"],
                    "category_lv1_name": lv1["category_lv1_name"],
                    "category_lv2_code": lv2["category_lv2_code"],
                    "category_lv2_name": lv2["category_lv2_name"],
                    **concept,
                }


concept_cards = []

for concept in tqdm(list(iter_concepts(taxonomy))):
    related_chunks = retrieve_chunks_by_keywords(
        chunks=chunks,
        concept_name=concept["concept_name"],
        concept_name_en=concept["concept_name_en"],
        aliases=concept.get("aliases", []),
        top_k=5,
    )

    user_prompt = prompt_template.format(
        concept_json=json.dumps(concept, ensure_ascii=False, indent=2),
        source_chunks_json=json.dumps(related_chunks, ensure_ascii=False, indent=2),
    )

    result = call_openai_json(
        system_prompt="너는 CS 전공 개념을 공통 조사 템플릿에 맞게 구조화하는 전문가다.",
        user_prompt=user_prompt,
        schema_name="concept_card_schema",
        schema=CONCEPT_CARD_SCHEMA,
        reasoning_effort="low",
    )

    concept_cards.append(result["concept_card"])

out_path = Path(f"outputs/{subject_code}/concept_cards.json")

with open(out_path, "w", encoding="utf-8") as f:
    json.dump(concept_cards, f, ensure_ascii=False, indent=2)

print(f"concept_cards 생성 완료: {out_path}")
print(f"생성된 concept card 수: {len(concept_cards)}")