import _bootstrap

import json
import sys
from pathlib import Path

from tqdm import tqdm

from schemas.question_review_schema import QUESTION_REVIEW_SCHEMA
from src.ai_client import REVIEW_MODEL, call_ai_json
from src.resume import SAVE_EVERY, is_forced, load_existing, report_resume, save_progress


if len(sys.argv) < 2:
    raise RuntimeError("사용법: python scripts/07_review_questions.py {SUBJECT_CODE}")

subject_code = sys.argv[1]

concept_cards = json.loads(
    Path(f"outputs/{subject_code}/concept_cards.json").read_text(encoding="utf-8")
)

questions = json.loads(
    Path(f"outputs/{subject_code}/questions.json").read_text(encoding="utf-8")
)

concept_map = {
    item["concept_code"]: item
    for item in concept_cards
}

prompt_template = Path("prompts/review_questions.md").read_text(encoding="utf-8")

out_path = Path(f"outputs/{subject_code}/question_reviews.json")

# 이어하기: 이미 검토한 문항은 건너뛴다(--force 로 전체 재검토).
# 리뷰 결과에는 content_hash 가 없으므로 (개념코드, 문항 본문) 으로 식별한다.
def review_key(item: dict):
    return (item.get("concept_code"), item.get("question_text"))

reviews, done_keys = load_existing(out_path, review_key, force=is_forced(sys.argv))
pending_questions = [
    q for q in questions if (q["concept_code"], q["question_text"]) not in done_keys
]
report_resume("question_reviews", len(done_keys), len(pending_questions))

for index, question in enumerate(tqdm(pending_questions), start=1):
    concept_card = concept_map[question["concept_code"]]

    user_prompt = prompt_template.format(
        concept_card_json=json.dumps(concept_card, ensure_ascii=False, indent=2),
        question_json=json.dumps(question, ensure_ascii=False, indent=2),
    )

    result = call_ai_json(
        system_prompt="너는 CS 문제의 정답성, 명확성, 해설 품질을 검토하는 전문가다.",
        user_prompt=user_prompt,
        schema_name="question_review_schema",
        schema=QUESTION_REVIEW_SCHEMA,
        model=REVIEW_MODEL,
        reasoning_effort="medium",
    )

    reviews.append({
        "concept_code": question["concept_code"],
        "concept_name": question["concept_name"],
        "question_text": question["question_text"],
        "correct_answer": question["correct_answer"],
        "validation_errors": question.get("validation_errors", []),
        "review": result["review"],
    })

    if index % SAVE_EVERY == 0:
        save_progress(out_path, reviews)

save_progress(out_path, reviews)

print(f"question_reviews 생성 완료: {out_path}")
print(f"검토된 문제 수: {len(reviews)}")