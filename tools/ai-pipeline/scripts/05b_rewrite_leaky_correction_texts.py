"""정답을 노출하는 힌트(correction_text)를 방향 제시형으로 다시 쓴다.

힌트는 common_mistakes.json 의 correction_text 를 문항에 그대로 스냅샷으로 복사한 값이다.
문항 생성기가 같은 correction_text 로 정답 선택지를 만들다 보니, 힌트를 누르면 정답이
그대로 보이는 문항이 생겼다. 문항 본문/선택지/정답은 그대로 두고 힌트만 고친다.

content_hash 는 concept_code/question_text/choices/correct_answer 로만 만들므로
이 작업으로 해시가 바뀌지 않는다(= 진단 프로필/검수 결과가 그대로 유효하다).

사용법:
    python scripts/05b_rewrite_leaky_correction_texts.py {SUBJECT_CODE} [--threshold 0.5] [--dry-run]
"""

import _bootstrap

import argparse
import json
import os
import re
from concurrent.futures import ThreadPoolExecutor, as_completed
from difflib import SequenceMatcher
from pathlib import Path

from tqdm import tqdm

from schemas.correction_text_rewrite_schema import CORRECTION_TEXT_REWRITE_SCHEMA
from src.ai_client import call_ai_json
from src.resume import SAVE_EVERY, save_progress

REWRITE_WORKERS = int(os.getenv("REWRITE_WORKERS", "6"))


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("subject_code", help="예: ALGORITHM, DATABASE")
    parser.add_argument(
        "--threshold",
        type=float,
        default=0.5,
        help="힌트-정답 유사도가 이 값 이상이면 다시 쓴다(기본 0.5).",
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="AI 호출 없이 대상만 집계해서 보여준다.",
    )
    return parser.parse_args()


def normalize(text: str | None) -> str:
    return re.sub(r"\s+", "", text or "")


def similarity(left: str | None, right: str | None) -> float:
    return SequenceMatcher(None, normalize(left), normalize(right)).ratio()


def correct_choice_text(question: dict) -> str:
    return next(
        (c["text"] for c in question.get("choices", []) if c.get("key") == question.get("correct_answer")),
        "",
    )


def main() -> None:
    args = parse_args()
    base = Path(f"outputs/{args.subject_code}")

    questions_path = base / "questions.json"
    mistakes_path = base / "common_mistakes.json"

    questions = json.loads(questions_path.read_text(encoding="utf-8"))
    mistake_groups = json.loads(mistakes_path.read_text(encoding="utf-8"))
    concept_cards = json.loads((base / "concept_cards.json").read_text(encoding="utf-8"))

    cards_by_code = {c["concept_code"]: c for c in concept_cards}
    mistakes_by_code: dict[str, dict] = {}
    concept_of_mistake: dict[str, str] = {}
    for group in mistake_groups:
        for mistake in group.get("mistakes", []):
            mistakes_by_code[mistake["mistake_code"]] = mistake
            concept_of_mistake[mistake["mistake_code"]] = group["concept_code"]

    # 정답을 노출하는 mistake_code 를 모으고, 노출된 정답 원문도 함께 담는다.
    leaked_answers: dict[str, list[str]] = {}
    for question in questions:
        code = question.get("hint_mistake_code")
        if not code or code not in mistakes_by_code:
            continue
        answer = correct_choice_text(question)
        if similarity(question.get("hint_correction_text_snapshot"), answer) >= args.threshold:
            leaked_answers.setdefault(code, []).append(answer)

    # 개념 단위로 묶어 호출 수를 줄인다(개념 하나에 여러 mistake 가 걸릴 수 있다).
    targets_by_concept: dict[str, list[str]] = {}
    for code in leaked_answers:
        targets_by_concept.setdefault(concept_of_mistake[code], []).append(code)

    print(
        f"{args.subject_code}: 정답 노출 힌트 {len(leaked_answers)}개 "
        f"(개념 {len(targets_by_concept)}개, 유사도 {args.threshold} 이상)"
    )

    if args.dry_run or not targets_by_concept:
        return

    prompt_template = Path("prompts/rewrite_correction_text.md").read_text(encoding="utf-8")

    def rewrite_concept(concept_code: str) -> list[dict]:
        codes = targets_by_concept[concept_code]
        targets = [
            {
                "mistake_code": code,
                "mistake_title": mistakes_by_code[code].get("mistake_title"),
                "mistake_text": mistakes_by_code[code].get("mistake_text"),
                "current_correction_text": mistakes_by_code[code].get("correction_text"),
                "leaked_answers": sorted(set(leaked_answers[code])),
            }
            for code in codes
        ]
        result = call_ai_json(
            system_prompt="너는 학습자가 스스로 오해를 알아차리도록 돕는 힌트 문구 작성 전문가다.",
            user_prompt=prompt_template.format(
                concept_card_json=json.dumps(
                    cards_by_code.get(concept_code, {}), ensure_ascii=False, indent=2
                ),
                targets_json=json.dumps(targets, ensure_ascii=False, indent=2),
            ),
            schema_name="correction_text_rewrite_schema",
            schema=CORRECTION_TEXT_REWRITE_SCHEMA,
            reasoning_effort="low",
        )
        return result["rewrites"]

    rewritten: dict[str, str] = {}
    failed: list[str] = []

    with ThreadPoolExecutor(max_workers=REWRITE_WORKERS) as executor:
        futures = {executor.submit(rewrite_concept, code): code for code in targets_by_concept}
        for index, future in enumerate(tqdm(as_completed(futures), total=len(futures)), start=1):
            try:
                for item in future.result():
                    code = item["mistake_code"]
                    if code in leaked_answers:
                        rewritten[code] = item["correction_text"]
            except Exception as error:
                failed.append(futures[future])
                print(f"\n재작성 실패(건너뜀): {futures[future]} - {error}")
            if index % SAVE_EVERY == 0:
                apply_and_save(
                    mistake_groups, questions, rewritten, mistakes_path, questions_path
                )

    apply_and_save(mistake_groups, questions, rewritten, mistakes_path, questions_path)

    print(f"correction_text 재작성: {len(rewritten)}개")
    print(f"힌트 스냅샷 갱신 문항: {count_snapshot_updates(questions, rewritten)}건")
    if failed:
        print(f"실패한 개념 {len(failed)}개: {failed}. 다시 실행하면 남은 것만 처리한다.")


def count_snapshot_updates(questions: list[dict], rewritten: dict[str, str]) -> int:
    return sum(1 for q in questions if q.get("hint_mistake_code") in rewritten)


def apply_and_save(
    mistake_groups: list[dict],
    questions: list[dict],
    rewritten: dict[str, str],
    mistakes_path: Path,
    questions_path: Path,
) -> None:
    """새 correction_text 를 common_mistakes 와 문항 스냅샷에 함께 반영한다."""
    if not rewritten:
        return

    for group in mistake_groups:
        for mistake in group.get("mistakes", []):
            new_text = rewritten.get(mistake["mistake_code"])
            if new_text:
                mistake["correction_text"] = new_text

    # 힌트는 문항에 스냅샷으로 복사돼 있으므로 같은 mistake 를 쓰는 문항 전부 갱신한다.
    for question in questions:
        new_text = rewritten.get(question.get("hint_mistake_code"))
        if new_text:
            question["hint_correction_text_snapshot"] = new_text

    save_progress(mistakes_path, mistake_groups)
    save_progress(questions_path, questions)


if __name__ == "__main__":
    main()
