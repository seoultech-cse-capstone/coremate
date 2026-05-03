import _bootstrap

import copy
import json
import sys
from pathlib import Path

import yaml
from tqdm import tqdm

from schemas.question_schema import QUESTION_SCHEMA
from src.hash_utils import make_question_hash
from src.openai_client import call_openai_json
from src.validators import validate_question, validate_questions


if len(sys.argv) < 2:
    raise RuntimeError("사용법: python scripts/06_generate_questions.py {SUBJECT_CODE}")

subject_code = sys.argv[1]

config_path = Path(f"configs/{subject_code}.yaml")

if not config_path.exists():
    raise RuntimeError(f"과목 설정 파일이 없습니다: {config_path}")

config = yaml.safe_load(config_path.read_text(encoding="utf-8"))

question_rules = config.get("question_rules", {})
question_count = question_rules.get("questions_per_concept", 2)

difficulty_source_default = question_rules.get("difficulty_source_default", "ai_initial")
source_type_default = question_rules.get("source_type_default", "ai_generated")
review_status_default = question_rules.get("review_status_default", "draft")
is_active_default = question_rules.get("is_active_default", False)
version_no_default = question_rules.get("version_no_default", 1)

eligibility_defaults = question_rules.get(
    "eligibility_defaults",
    {
        "is_daily_quiz_eligible": True,
        "is_mock_eligible": True,
        "is_concept_practice_eligible": True,
    },
)

concept_cards_path = Path(f"outputs/{subject_code}/concept_cards.json")
relations_path = Path(f"outputs/{subject_code}/concept_relations.json")
common_mistakes_path = Path(f"outputs/{subject_code}/common_mistakes.json")

if not concept_cards_path.exists():
    raise RuntimeError(f"concept_cards.json이 없습니다: {concept_cards_path}")

if not relations_path.exists():
    raise RuntimeError(f"concept_relations.json이 없습니다: {relations_path}")

if not common_mistakes_path.exists():
    raise RuntimeError(f"common_mistakes.json이 없습니다: {common_mistakes_path}")

concept_cards = json.loads(concept_cards_path.read_text(encoding="utf-8"))
relations = json.loads(relations_path.read_text(encoding="utf-8"))
mistake_groups = json.loads(common_mistakes_path.read_text(encoding="utf-8"))

mistake_map = {
    item["concept_code"]: item
    for item in mistake_groups
}

prompt_template = Path("prompts/generate_questions.md").read_text(encoding="utf-8")


def find_relations_for_concept(concept_code: str) -> list[dict]:
    return [
        relation
        for relation in relations
        if relation["source_concept_code"] == concept_code
        or relation["target_concept_code"] == concept_code
    ]


def build_mistake_code_map(mistake_groups: list[dict]) -> dict[str, dict]:
    mistake_code_map = {}

    for group in mistake_groups:
        for mistake in group.get("mistakes", []):
            mistake_code = mistake.get("mistake_code")

            if mistake_code:
                mistake_code_map[mistake_code] = mistake

    return mistake_code_map


def build_question_schema_for_mistakes(mistakes: list[dict]) -> dict:
    """
    현재 concept에서 사용할 수 있는 mistake_code만
    hint_mistake_code enum으로 허용한다.

    이렇게 해야 OpenAI API가 mistake_title, 임의 코드, 한국어 문장 등을
    hint_mistake_code에 넣는 문제를 막을 수 있다.
    """

    schema = copy.deepcopy(QUESTION_SCHEMA)

    allowed_mistake_codes = sorted(
        {
            mistake["mistake_code"]
            for mistake in mistakes
            if mistake.get("mistake_code")
        }
    )

    hint_schema = schema["properties"]["questions"]["items"]["properties"]["hint_mistake_code"]

    hint_schema.clear()
    hint_schema.update(
        {
            "type": ["string", "null"],
            "enum": allowed_mistake_codes + [None],
        }
    )

    return schema


mistake_code_map = build_mistake_code_map(mistake_groups)


def normalize_question(
    *,
    question: dict,
    concept_card: dict,
) -> dict:
    normalized = dict(question)

    # concept 기준값은 concept_card 기준으로 고정
    normalized["concept_code"] = concept_card["concept_code"]
    normalized["concept_name"] = concept_card["concept_name"]

    # DB 명세 기준 기본값 보정
    normalized["difficulty_source"] = normalized.get("difficulty_source") or difficulty_source_default
    normalized["source_type"] = normalized.get("source_type") or source_type_default
    normalized["review_status"] = normalized.get("review_status") or review_status_default
    normalized["is_active"] = normalized.get("is_active", is_active_default)
    normalized["version_no"] = normalized.get("version_no", version_no_default)

    # 출제 가능 여부 기본값 보정
    normalized["is_daily_quiz_eligible"] = normalized.get(
        "is_daily_quiz_eligible",
        eligibility_defaults.get("is_daily_quiz_eligible", True),
    )
    normalized["is_mock_eligible"] = normalized.get(
        "is_mock_eligible",
        eligibility_defaults.get("is_mock_eligible", True),
    )
    normalized["is_concept_practice_eligible"] = normalized.get(
        "is_concept_practice_eligible",
        eligibility_defaults.get("is_concept_practice_eligible", True),
    )

    # source_refs는 concept_card의 근거 출처를 question에 스냅샷으로 복사
    normalized["source_refs"] = concept_card.get("source_refs", [])

    # hint_mistake_code 연결 보정
    hint_mistake_code = normalized.get("hint_mistake_code")

    if hint_mistake_code is None:
        normalized["hint_correction_text_snapshot"] = None
    elif hint_mistake_code in mistake_code_map:
        normalized["hint_correction_text_snapshot"] = mistake_code_map[hint_mistake_code].get(
            "correction_text"
        )

    # content_hash는 최종 question 구조 기준으로 생성
    normalized["content_hash"] = make_question_hash(normalized)

    return normalized


all_questions = []
hint_reference_errors = []

for concept_card in tqdm(concept_cards):
    concept_code = concept_card["concept_code"]

    current_mistake_group = mistake_map.get(concept_code, {"mistakes": []})
    current_mistakes = current_mistake_group.get("mistakes", [])

    question_schema = build_question_schema_for_mistakes(current_mistakes)

    user_prompt = prompt_template.format(
        concept_card_json=json.dumps(concept_card, ensure_ascii=False, indent=2),
        relations_json=json.dumps(
            find_relations_for_concept(concept_code),
            ensure_ascii=False,
            indent=2,
        ),
        common_mistakes_json=json.dumps(
            current_mistake_group,
            ensure_ascii=False,
            indent=2,
        ),
        question_count=question_count,
    )

    result = call_openai_json(
        system_prompt="너는 CS 전공 지식 문제를 정확하게 생성하는 전문가다.",
        user_prompt=user_prompt,
        schema_name="question_schema",
        schema=question_schema,
        reasoning_effort="medium",
    )

    for question in result["questions"]:
        normalized_question = normalize_question(
            question=question,
            concept_card=concept_card,
        )

        validation_errors = validate_question(normalized_question)

        hint_mistake_code = normalized_question.get("hint_mistake_code")

        if hint_mistake_code is not None and hint_mistake_code not in mistake_code_map:
            error_message = (
                f"hint_mistake_code가 common_mistakes.json에 존재하지 않음: "
                f"{hint_mistake_code}"
            )

            validation_errors.append(error_message)
            hint_reference_errors.append(
                f"[concept_code={concept_code}] {error_message}"
            )

        normalized_question["validation_errors"] = validation_errors

        all_questions.append(normalized_question)


batch_validation_errors = validate_questions(all_questions)
batch_validation_errors.extend(hint_reference_errors)

out_dir = Path(f"outputs/{subject_code}")
out_dir.mkdir(parents=True, exist_ok=True)

out_path = out_dir / "questions.json"

with open(out_path, "w", encoding="utf-8") as f:
    json.dump(all_questions, f, ensure_ascii=False, indent=2)

validation_error_path = out_dir / "question_validation_errors.txt"

if batch_validation_errors:
    with open(validation_error_path, "w", encoding="utf-8") as f:
        for error in batch_validation_errors:
            f.write(f"{error}\n")

    print("\n전체 question 검증 경고:")
    for error in batch_validation_errors:
        print(f"- {error}")

    print(f"\nquestion 검증 경고 저장 완료: {validation_error_path}")
else:
    print("전체 question 검증 경고 없음")

print(f"questions 생성 완료: {out_path}")
print(f"생성된 문제 수: {len(all_questions)}")