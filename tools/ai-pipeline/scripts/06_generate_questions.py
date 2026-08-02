import _bootstrap

import copy
import json
import sys
from pathlib import Path

import yaml
from tqdm import tqdm

from schemas.question_schema import QUESTION_SCHEMA
from src.hash_utils import make_question_hash
from src.ai_client import call_ai_json
from src.resume import SAVE_EVERY, is_forced, load_existing, report_resume, save_progress
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
difficulty_mix = question_rules.get("difficulty_mix", [])

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


out_dir = Path(f"outputs/{subject_code}")
out_path = out_dir / "questions.json"


def summarize_existing(questions: list[dict]) -> list[dict]:
    """중복 방지용으로 기존 문항의 논점만 추린다(전문을 넣으면 토큰이 과하게 든다)."""
    return [
        {
            "question_text": question.get("question_text"),
            "question_type": question.get("question_type"),
            "difficulty_level": question.get("difficulty_level"),
        }
        for question in questions
    ]


def request_difficulties(existing: list[dict], need: int) -> list[int]:
    """difficulty_mix에서 기존 문항이 이미 채운 난이도를 뺀 나머지를 이번 회차로 요청한다."""
    if not difficulty_mix:
        return []

    remaining = list(difficulty_mix)

    for question in existing:
        level = question.get("difficulty_level")
        if level in remaining:
            remaining.remove(level)

    # mix보다 더 많이 만들어야 하면 어려운 난이도부터 반복해 채운다.
    while len(remaining) < need:
        remaining.append(max(difficulty_mix))

    return sorted(remaining[:need])


# 이어하기: 개념별로 이미 만든 문항 수를 세어 부족한 만큼만 추가 생성한다.
all_questions, _ = load_existing(
    out_path, lambda q: q.get("concept_code"), force=is_forced(sys.argv)
)

existing_by_concept: dict[str, list[dict]] = {}
existing_hashes = set()
for question in all_questions:
    existing_by_concept.setdefault(question.get("concept_code"), []).append(question)
    if question.get("content_hash"):
        existing_hashes.add(question["content_hash"])

pending_cards = []
for concept_card in concept_cards:
    existing = existing_by_concept.get(concept_card["concept_code"], [])
    shortfall = question_count - len(existing)
    if shortfall > 0:
        pending_cards.append((concept_card, existing, shortfall))

done_count = len(concept_cards) - len(pending_cards)
total_to_generate = sum(shortfall for _, _, shortfall in pending_cards)
report_resume("questions", done_count, len(pending_cards))
print(
    f"questions: 기존 {len(all_questions)}문항, "
    f"개념 {len(pending_cards)}개에 {total_to_generate}문항 추가 생성 예정 "
    f"(목표 개념당 {question_count}문항)"
)

for index, (concept_card, existing_questions, shortfall) in enumerate(tqdm(pending_cards), start=1):
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
        question_count=shortfall,
        existing_questions_json=json.dumps(
            summarize_existing(existing_questions),
            ensure_ascii=False,
            indent=2,
        ),
        requested_difficulties=json.dumps(
            request_difficulties(existing_questions, shortfall),
            ensure_ascii=False,
        ),
    )

    result = call_ai_json(
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

        # 추가 생성이라 기존 문항과 같은 내용이 다시 나올 수 있다. content_hash로 걸러낸다.
        content_hash = normalized_question.get("content_hash")
        if content_hash in existing_hashes:
            continue
        existing_hashes.add(content_hash)

        validation_errors = validate_question(normalized_question)

        hint_mistake_code = normalized_question.get("hint_mistake_code")

        if hint_mistake_code is not None and hint_mistake_code not in mistake_code_map:
            error_message = (
                f"hint_mistake_code가 common_mistakes.json에 존재하지 않음: "
                f"{hint_mistake_code}"
            )

            validation_errors.append(error_message)

        normalized_question["validation_errors"] = validation_errors

        all_questions.append(normalized_question)

    if index % SAVE_EVERY == 0:
        save_progress(out_path, all_questions)

save_progress(out_path, all_questions)

# hint 참조 오류는 이어하기로 건너뛴 문항까지 포함해야 하므로 전체 기준으로 다시 모은다.
hint_reference_errors = [
    f"[concept_code={question.get('concept_code')}] "
    f"hint_mistake_code가 common_mistakes.json에 존재하지 않음: {question.get('hint_mistake_code')}"
    for question in all_questions
    if question.get("hint_mistake_code") is not None
    and question.get("hint_mistake_code") not in mistake_code_map
]

batch_validation_errors = validate_questions(all_questions)
batch_validation_errors.extend(hint_reference_errors)

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