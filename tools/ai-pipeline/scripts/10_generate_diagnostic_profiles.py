import _bootstrap

import argparse
import json
from pathlib import Path

from tqdm import tqdm

from schemas.concept_diagnostic_profile_schema import CONCEPT_DIAGNOSTIC_PROFILE_SCHEMA
from schemas.question_diagnostic_profile_schema import (
    build_question_diagnostic_schema_for_mistakes,
)
from src.ai_client import call_ai_json
from src.hash_utils import make_question_hash
from src.resume import SAVE_EVERY, load_existing, report_resume, save_progress

# 진단 프로필 데이터 버전. 백엔드 weakness.analysis.diagnostic-profile-version 와 맞춘다.
DIAGNOSTIC_PROFILE_VERSION = "v1"


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description=(
            "개념/문항 진단 프로필을 생성해 outputs/{SUBJECT}/ 에 "
            "concept_diagnostic_profiles.json, question_diagnostic_profiles.json 로 저장한다."
        ),
    )
    parser.add_argument("subject_code", help="예: DATA_STRUCTURE, NETWORK")
    parser.add_argument(
        "--limit",
        type=int,
        default=None,
        help="생성할 개념 수 상한(스모크용). 지정 시 앞에서부터 N개 개념만 처리.",
    )
    parser.add_argument(
        "--skip-questions",
        action="store_true",
        help="문항 진단 프로필 생성을 건너뛰고 개념 프로필만 생성.",
    )
    parser.add_argument(
        "--force",
        action="store_true",
        help="이미 만들어 둔 프로필도 무시하고 전부 다시 생성한다(형식이 바뀐 경우).",
    )
    return parser.parse_args()


def load_json(path: Path):
    if not path.exists():
        raise RuntimeError(f"필요한 산출물이 없습니다: {path}")
    return json.loads(path.read_text(encoding="utf-8"))


def main() -> None:
    args = parse_args()
    subject_code = args.subject_code
    base = Path(f"outputs/{subject_code}")

    concept_cards = load_json(base / "concept_cards.json")
    questions = load_json(base / "questions.json")
    common_mistakes = load_json(base / "common_mistakes.json")
    relations = load_json(base / "concept_relations.json")

    mistakes_by_concept: dict[str, list] = {}
    for group in common_mistakes:
        mistakes_by_concept[group["concept_code"]] = group.get("mistakes", [])

    def relations_for(concept_code: str) -> list:
        return [
            r
            for r in relations
            if r.get("source_concept_code") == concept_code
            or r.get("target_concept_code") == concept_code
        ]

    if args.limit is not None:
        concept_cards = concept_cards[: args.limit]
    selected_codes = {card["concept_code"] for card in concept_cards}

    concept_prompt = Path("prompts/generate_concept_diagnostic_profile.md").read_text(encoding="utf-8")
    question_prompt = Path("prompts/generate_question_diagnostic_profile.md").read_text(encoding="utf-8")

    # 검수 워크플로우(review_service)는 "아티팩트 1개 = 파일 1개, JSON 배열" 을 전제로 하므로
    # 개념/문항 프로필을 각각의 파일로 내보낸다(concept_cards.json / questions.json 과 동일 관례).
    concept_path = base / "concept_diagnostic_profiles.json"
    question_path = base / "question_diagnostic_profiles.json"

    # 이어하기: 이전 실행에서 만들어 둔 것은 건너뛴다(중단돼도 이미 쓴 비용을 버리지 않는다).
    # 형식이 바뀐(버전이 다른) 항목은 완료로 보지 않고 다시 만든다.
    current_version = lambda item: item.get("diagnostic_profile_version") == DIAGNOSTIC_PROFILE_VERSION
    concept_profiles, done_concepts = load_existing(
        concept_path, lambda p: p.get("concept_code"), force=args.force, keep=current_version
    )
    question_profiles, done_questions = load_existing(
        question_path, lambda p: p.get("content_hash"), force=args.force, keep=current_version
    )

    pending_cards = [c for c in concept_cards if c["concept_code"] not in done_concepts]
    report_resume("개념 프로필", len(done_concepts), len(pending_cards))

    for index, card in enumerate(tqdm(pending_cards, desc="concept profiles"), start=1):
        concept_code = card["concept_code"]
        user_prompt = concept_prompt.format(
            concept_card_json=json.dumps(card, ensure_ascii=False, indent=2),
            common_mistakes_json=json.dumps(
                mistakes_by_concept.get(concept_code, []), ensure_ascii=False, indent=2
            ),
            relations_json=json.dumps(relations_for(concept_code), ensure_ascii=False, indent=2),
        )
        result = call_ai_json(
            system_prompt="너는 CS 전공 학습자의 약점을 진단하는 학습 콘텐츠 전문가다.",
            user_prompt=user_prompt,
            schema_name="concept_diagnostic_profile_schema",
            schema=CONCEPT_DIAGNOSTIC_PROFILE_SCHEMA,
            reasoning_effort="low",
        )
        result["concept_code"] = concept_code
        result["concept_name"] = card.get("concept_name")
        result["diagnostic_profile_version"] = DIAGNOSTIC_PROFILE_VERSION
        concept_profiles.append(result)
        if index % SAVE_EVERY == 0:
            save_progress(concept_path, concept_profiles)
    save_progress(concept_path, concept_profiles)

    if not args.skip_questions:
        target_questions = [
            q for q in questions
            if q.get("concept_code") in selected_codes
            and str(q.get("content_hash") or make_question_hash(q)) not in done_questions
        ]
        report_resume("문항 프로필", len(done_questions), len(target_questions))
        cards_by_code = {card["concept_code"]: card for card in concept_cards}
        for index, question in enumerate(tqdm(target_questions, desc="question profiles"), start=1):
            concept_code = question["concept_code"]
            user_prompt = question_prompt.format(
                question_json=json.dumps(question, ensure_ascii=False, indent=2),
                concept_card_json=json.dumps(
                    cards_by_code.get(concept_code, {}), ensure_ascii=False, indent=2
                ),
                common_mistakes_json=json.dumps(
                    mistakes_by_concept.get(concept_code, []), ensure_ascii=False, indent=2
                ),
            )
            # 이 개념의 mistake_code 만 misconception_code 로 허용(임의 코드 방지).
            result = call_ai_json(
                system_prompt="너는 CS 전공 시험 문항을 진단 관점에서 분석하는 전문가다.",
                user_prompt=user_prompt,
                schema_name="question_diagnostic_profile_schema",
                schema=build_question_diagnostic_schema_for_mistakes(
                    mistakes_by_concept.get(concept_code, [])
                ),
                reasoning_effort="low",
            )
            # 문항 식별·비생성 필드는 원본에서 그대로 채운다(import 가 content_hash 로 question_id 해석).
            result["content_hash"] = str(question.get("content_hash") or make_question_hash(question))
            result["concept_code"] = concept_code
            result["expected_solve_sec"] = question.get("expected_solve_sec")
            result["difficulty"] = question.get("difficulty_level")
            result["diagnostic_profile_version"] = DIAGNOSTIC_PROFILE_VERSION
            question_profiles.append(result)
            if index % SAVE_EVERY == 0:
                save_progress(question_path, question_profiles)
    save_progress(question_path, question_profiles)

    print(f"진단 프로필 생성 완료: {concept_path} ({len(concept_profiles)}개)")
    print(f"                      {question_path} ({len(question_profiles)}개)")
    print("다음 단계: 검수(리뷰 API) → 승인 내보내기 → scripts/11_import_diagnostic_profiles.py")


if __name__ == "__main__":
    main()
