"""검수가 지적한 적정 난이도를 문항의 difficulty_level 에 반영한다.

개념당 난이도 1~5를 강제로 배분(difficulty_mix)하다 보니, 난이도 5짜리 소재가 없는
개념에서도 모델이 라벨만 5로 붙이고 실제로는 2~3 수준인 문항을 만들었다. 검수는 이걸
"difficulty_level 이 실제 난이도보다 과하다, N이 더 적절하다" 형태로 지적했다.

문항 내용은 정상이므로 재생성하지 않고 라벨만 고친다. AI 호출이 없다.
difficulty_level 은 content_hash 에 들어가지 않으므로 검수/진단 프로필이 그대로 유효하다.
문항 진단 프로필은 difficulty 를 복사해 두므로 함께 갱신한다.

사용법:
    python scripts/07b_apply_review_difficulty.py {SUBJECT_CODE} [--dry-run]
"""

import _bootstrap

import argparse
import collections
import json
import re
from pathlib import Path

from src.resume import save_progress

# 검수 문구에서 권장 난이도를 뽑는다. 위쪽 패턴이 더 구체적이라 먼저 본다.
RECOMMEND = r"(?:더\s*)?(?:적절|자연스럽|타당|낫|좋겠|권장|바람직|어울|합당)"
PATTERNS = [
    re.compile(r"([1-5])\s*[~\-–]\s*([1-5])\s*(?:정도|수준|사이)?\s*(?:가|이|로|으로|도|는|은)?\s*" + RECOMMEND),
    re.compile(r"([1-5])\s*[~\-–]\s*([1-5])\s*(?:정도|수준|사이)?\s*(?:에|가|이)?\s*(?:더\s*)?가까"),
    re.compile(r"([1-5])\s*(?:정도|수준)?\s*(?:가|이|로|으로|도|는|은)?\s*" + RECOMMEND),
    re.compile(r"([1-5])\s*(?:정도|수준)?\s*(?:에|가|이)?\s*(?:더\s*)?가까"),
    re.compile(r"[1-5]\s*보다는?\s*.{0,12}?([1-5])"),
    re.compile(r"([1-5])\s*(?:로|으로)\s*(?:조정|낮추|내리|하향|올리|상향|수정|변경)"),
]

MENTIONS_DIFFICULTY = re.compile(r"난이도|difficulty")


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("subject_code", help="예: ALGORITHM, DATABASE")
    parser.add_argument("--dry-run", action="store_true", help="반영하지 않고 집계만 한다.")
    return parser.parse_args()


def review_text(review: dict) -> str:
    issues = " ".join(str(i) for i in (review.get("issues") or []))
    return f"{issues} {review.get('suggested_fix') or ''}"


def suggested_difficulty(text: str) -> int | None:
    """권장 난이도를 뽑는다. 범위로 말했으면 가운데 값(반올림)을 쓴다."""
    for pattern in PATTERNS:
        match = pattern.search(text)
        if not match:
            continue
        found = [int(g) for g in match.groups() if g]
        if found:
            return int(sum(found) / len(found) + 0.5)
    return None


def main() -> None:
    args = parse_args()
    base = Path(f"outputs/{args.subject_code}")

    questions_path = base / "questions.json"
    profiles_path = base / "question_diagnostic_profiles.json"

    questions = json.loads(questions_path.read_text(encoding="utf-8"))
    reviews = json.loads((base / "question_reviews.json").read_text(encoding="utf-8"))
    profiles = json.loads(profiles_path.read_text(encoding="utf-8")) if profiles_path.exists() else []

    questions_by_key = {(q["concept_code"], q["question_text"]): q for q in questions}

    moves = collections.Counter()
    unparsed = 0
    targets: dict[str, int] = {}

    for review_item in reviews:
        review = review_item["review"]
        if review.get("verdict") == "pass":
            continue
        text = review_text(review)
        if not MENTIONS_DIFFICULTY.search(text):
            continue

        question = questions_by_key.get((review_item["concept_code"], review_item["question_text"]))
        if question is None:
            continue

        target = suggested_difficulty(text)
        if target is None:
            unparsed += 1
            continue
        if target == question["difficulty_level"]:
            continue

        moves[(question["difficulty_level"], target)] += 1
        targets[question["content_hash"]] = target

    print(f"{args.subject_code}: 난이도 조정 {len(targets)}건 (권장 난이도 파싱 실패 {unparsed}건은 그대로 둠)")
    for (before, after), count in sorted(moves.items()):
        print(f"    {before} -> {after}: {count}건")

    if args.dry_run or not targets:
        return

    for question in questions:
        target = targets.get(question["content_hash"])
        if target:
            question["difficulty_level"] = target

    # 진단 프로필은 difficulty 를 문항에서 복사해 두므로 같이 맞춘다.
    updated_profiles = 0
    for profile in profiles:
        target = targets.get(profile.get("content_hash"))
        if target and profile.get("difficulty") != target:
            profile["difficulty"] = target
            updated_profiles += 1

    save_progress(questions_path, questions)
    if profiles:
        save_progress(profiles_path, profiles)

    print(f"    문항 {len(targets)}건, 진단 프로필 {updated_profiles}건 갱신")


if __name__ == "__main__":
    main()
