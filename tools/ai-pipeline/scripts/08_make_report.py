import _bootstrap

import json
import sys
from collections import Counter
from pathlib import Path


if len(sys.argv) < 2:
    raise RuntimeError("사용법: python scripts/08_make_report.py {SUBJECT_CODE}")

subject_code = sys.argv[1]

base_dir = Path(f"outputs/{subject_code}")

concept_cards = json.loads((base_dir / "concept_cards.json").read_text(encoding="utf-8"))
relations = json.loads((base_dir / "concept_relations.json").read_text(encoding="utf-8"))
mistakes = json.loads((base_dir / "common_mistakes.json").read_text(encoding="utf-8"))
questions = json.loads((base_dir / "questions.json").read_text(encoding="utf-8"))
reviews = json.loads((base_dir / "question_reviews.json").read_text(encoding="utf-8"))

taxonomy_warning_path = base_dir / "taxonomy_warnings.txt"
question_validation_error_path = base_dir / "question_validation_errors.txt"

taxonomy_warnings = []
if taxonomy_warning_path.exists():
    taxonomy_warnings = taxonomy_warning_path.read_text(encoding="utf-8").splitlines()

question_validation_errors = []
if question_validation_error_path.exists():
    question_validation_errors = question_validation_error_path.read_text(encoding="utf-8").splitlines()

mistake_map = {
    item["concept_code"]: item
    for item in mistakes
}

review_map = {
    item["question_text"]: item
    for item in reviews
}

question_type_counter = Counter(q.get("question_type", "unknown") for q in questions)
difficulty_counter = Counter(q.get("difficulty_level", "unknown") for q in questions)
relation_type_counter = Counter(r.get("relation_type", "unknown") for r in relations)

pass_count = sum(1 for r in reviews if r["review"]["verdict"] == "pass")
revise_count = sum(1 for r in reviews if r["review"]["verdict"] == "revise")
reject_count = sum(1 for r in reviews if r["review"]["verdict"] == "reject")

insufficient_source_count = sum(1 for c in concept_cards if c.get("insufficient_source") is True)
no_source_ref_concept_count = sum(1 for c in concept_cards if not c.get("source_refs"))

total_mistake_count = sum(len(item.get("mistakes", [])) for item in mistakes)

questions_with_validation_errors = [
    q for q in questions
    if q.get("validation_errors")
]

questions_without_hint = [
    q for q in questions
    if q.get("hint_mistake_code") is None
]

content_hashes = [q.get("content_hash") for q in questions if q.get("content_hash")]
duplicate_hash_count = len(content_hashes) - len(set(content_hashes))

lines = []

lines.append(f"# {subject_code} 개념 조사 및 문제 생성 테스트 결과\n")

lines.append("## 생성 요약\n")
lines.append(f"- taxonomy warning 수: {len(taxonomy_warnings)}")
lines.append(f"- concept card 수: {len(concept_cards)}")
lines.append(f"- insufficient_source=true concept 수: {insufficient_source_count}")
lines.append(f"- source_refs 없는 concept 수: {no_source_ref_concept_count}")
lines.append(f"- relation 수: {len(relations)}")
lines.append(f"- common mistake 수: {total_mistake_count}")
lines.append(f"- question 수: {len(questions)}")
lines.append(f"- validation_errors 있는 question 수: {len(questions_with_validation_errors)}")
lines.append(f"- hint_mistake_code 없는 question 수: {len(questions_without_hint)}")
lines.append(f"- content_hash 중복 수: {duplicate_hash_count}")
lines.append(f"- AI review pass: {pass_count}")
lines.append(f"- AI review revise: {revise_count}")
lines.append(f"- AI review reject: {reject_count}\n")

lines.append("## relation type별 개수\n")
for relation_type, count in relation_type_counter.items():
    lines.append(f"- {relation_type}: {count}")

lines.append("\n## question type별 개수\n")
for question_type, count in question_type_counter.items():
    lines.append(f"- {question_type}: {count}")

lines.append("\n## difficulty level별 개수\n")
for difficulty, count in sorted(difficulty_counter.items(), key=lambda x: str(x[0])):
    lines.append(f"- {difficulty}: {count}")

if taxonomy_warnings:
    lines.append("\n## taxonomy 경고\n")
    for warning in taxonomy_warnings:
        lines.append(f"- {warning}")

if question_validation_errors:
    lines.append("\n## question 검증 경고\n")
    for error in question_validation_errors:
        lines.append(f"- {error}")

for concept in concept_cards:
    concept_code = concept["concept_code"]

    lines.append("\n---\n")
    lines.append(f"## {concept['concept_name']} ({concept['concept_name_en']})\n")
    lines.append(f"**concept_code**: {concept_code}")
    lines.append(f"**category_lv1**: {concept['category_lv1']}")
    lines.append(f"**category_lv2**: {concept['category_lv2']}")
    lines.append(f"**insufficient_source**: {concept.get('insufficient_source')}\n")

    lines.append(f"**한 줄 정의**: {concept['one_line_definition']}\n")
    lines.append(f"**핵심 요약**: {concept['core_summary']}\n")

    lines.append("**핵심 키워드**")
    for keyword in concept["core_keywords"]:
        lines.append(f"- {keyword}")

    lines.append("\n**핵심 성질**")
    for prop in concept["key_properties"]:
        lines.append(f"- {prop}")

    source_refs = concept.get("source_refs", [])
    lines.append("\n**출처 chunk**")
    if source_refs:
        for ref in source_refs:
            lines.append(f"- {ref['source_file']} p.{ref['page']} / {ref['chunk_id']}")
    else:
        lines.append("- source_refs 없음")

    concept_mistakes = mistake_map.get(concept_code, {"mistakes": []})

    lines.append("\n**공용 오해**")
    for mistake in concept_mistakes["mistakes"]:
        lines.append(f"- {mistake['mistake_title']}")
        lines.append(f"  - mistake_code: {mistake.get('mistake_code')}")
        lines.append(f"  - 오해: {mistake['mistake_text']}")
        lines.append(f"  - 교정: {mistake['correction_text']}")
        lines.append(f"  - review_status: {mistake.get('review_status')}")
        lines.append(f"  - is_active: {mistake.get('is_active')}")

    concept_questions = [
        q for q in questions
        if q["concept_code"] == concept_code
    ]

    lines.append("\n**생성 문제**")

    for question in concept_questions:
        review_item = review_map.get(question["question_text"])
        review = review_item["review"] if review_item else {}

        lines.append(f"\n- 문제: {question['question_text']}")

        for choice in question["choices"]:
            lines.append(f"  - {choice['key']}. {choice['text']}")

        lines.append(f"  - 정답: {question['correct_answer']}")
        lines.append(f"  - 문제 유형: {question.get('question_type')}")
        lines.append(f"  - 해설: {question['explanation_text']}")
        lines.append(f"  - 난이도: {question['difficulty_level']}")
        lines.append(f"  - 난이도 출처: {question.get('difficulty_source')}")
        lines.append(f"  - 예상 풀이 시간: {question['expected_solve_sec']}초")
        lines.append(f"  - 힌트 연결: {question.get('hint_mistake_code')}")
        lines.append(f"  - 힌트 스냅샷: {question.get('hint_correction_text_snapshot')}")
        lines.append(f"  - source_type: {question.get('source_type')}")
        lines.append(f"  - review_status: {question.get('review_status')}")
        lines.append(f"  - is_active: {question.get('is_active')}")
        lines.append(
            "  - 출제 가능: "
            f"concept={question.get('is_concept_practice_eligible')}, "
            f"mock={question.get('is_mock_eligible')}, "
            f"daily={question.get('is_daily_quiz_eligible')}"
        )
        lines.append(f"  - content_hash: {question.get('content_hash')}")
        lines.append(f"  - 코드 검증 오류: {question.get('validation_errors', [])}")
        lines.append(f"  - AI 검토: {review.get('verdict', 'unknown')}")

        if review.get("issues"):
            lines.append(f"  - 이슈: {', '.join(review['issues'])}")

        if review.get("suggested_fix"):
            lines.append(f"  - 수정 제안: {review['suggested_fix']}")

out_path = base_dir / "final_report.md"
out_path.write_text("\n".join(lines), encoding="utf-8")

print(f"final_report 생성 완료: {out_path}")