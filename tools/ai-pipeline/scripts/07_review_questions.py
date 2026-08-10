import _bootstrap

import json
import os
import sys
from concurrent.futures import ThreadPoolExecutor, as_completed
from pathlib import Path

from tqdm import tqdm

from schemas.question_review_schema import QUESTION_REVIEW_SCHEMA
from src.ai_client import REVIEW_MODEL, call_ai_json
from src.resume import SAVE_EVERY, is_forced, load_existing, report_resume, save_progress


if len(sys.argv) < 2:
    raise RuntimeError("사용법: python scripts/07_review_questions.py {SUBJECT_CODE}")

subject_code = sys.argv[1]

# 동시 호출 수. 레이트리밋에 걸리면 낮춰서 다시 실행하면 된다(이어하기로 이어서 검토한다).
REVIEW_WORKERS = int(os.getenv("REVIEW_WORKERS", "8"))

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

def review_question(question: dict) -> dict:
    """문항 하나를 검토한다. 워커 스레드에서 돈다(공유 상태를 건드리지 않는다)."""
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

    return {
        "concept_code": question["concept_code"],
        "concept_name": question["concept_name"],
        "question_text": question["question_text"],
        "correct_answer": question["correct_answer"],
        "validation_errors": question.get("validation_errors", []),
        "review": result["review"],
    }


# 문항 수천 개를 순차로 돌면 몇 시간이 걸려서 동시에 호출한다.
# 저장은 메인 스레드에서만 하므로 워커끼리 공유 상태를 건드리지 않는다.
failed_count = 0

with ThreadPoolExecutor(max_workers=REVIEW_WORKERS) as executor:
    futures = {executor.submit(review_question, q): q for q in pending_questions}

    for index, future in enumerate(tqdm(as_completed(futures), total=len(futures)), start=1):
        try:
            reviews.append(future.result())
        except Exception as error:
            failed_count += 1
            print(f"\n검토 실패(건너뜀): {futures[future]['concept_code']} - {error}")
            continue

        if index % SAVE_EVERY == 0:
            save_progress(out_path, reviews)

save_progress(out_path, reviews)

print(f"question_reviews 생성 완료: {out_path}")
print(f"검토된 문제 수: {len(reviews)}")

if failed_count:
    print(f"검토 실패 {failed_count}건. 같은 명령을 다시 실행하면 실패한 문항만 이어서 검토한다.")