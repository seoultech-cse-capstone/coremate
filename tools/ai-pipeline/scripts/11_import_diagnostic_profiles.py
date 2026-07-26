import _bootstrap

import argparse
import json
from pathlib import Path
from typing import Any

# 콘텐츠 임포터와 동일한 MySQL 연결·설정을 재사용한다(관례 일치).
from server.services.content_import_service import (
    MySQLConnectionSettings,
    _connect_mysql,
)

REVIEW_STATUS_DRAFT = "DRAFT"  # DiagnosticReviewStatus.DRAFT (AI 생성 직후)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="diagnostic_profiles.json 을 concept/question_diagnostic_profile 테이블에 적재한다.",
    )
    parser.add_argument("subject_code", help="예: DATA_STRUCTURE, NETWORK")
    parser.add_argument(
        "--execute",
        action="store_true",
        help="MySQL 에 실제로 기록한다. 없으면 해석 결과만 검증(dry-run).",
    )
    return parser.parse_args()


def _json_text(value: Any) -> str | None:
    if value is None:
        return None
    if isinstance(value, str):
        return value
    return json.dumps(value, ensure_ascii=False)


def _resolve_concept_id(cursor: Any, concept_code: str) -> int | None:
    cursor.execute(
        "SELECT concept_id FROM concept WHERE concept_code = %s",
        (concept_code,),
    )
    row = cursor.fetchone()
    return int(row["concept_id"]) if row else None


def _resolve_question(cursor: Any, content_hash: str) -> dict | None:
    cursor.execute(
        "SELECT question_id, concept_id FROM question WHERE content_hash = %s",
        (content_hash,),
    )
    return cursor.fetchone()


def _upsert_concept_profile(cursor: Any, concept_id: int, p: dict) -> None:
    cursor.execute(
        """
        INSERT INTO concept_diagnostic_profile (
            concept_id, concept_core, concept_explanation, key_points,
            confusion_comparisons, prerequisite_concepts, learning_example,
            misconception_notes, basic_review_points, misconception_mapping_coverage,
            diagnostic_profile_version, review_status, created_at, updated_at
        ) VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, NOW(), NOW())
        ON DUPLICATE KEY UPDATE
            concept_core = VALUES(concept_core),
            concept_explanation = VALUES(concept_explanation),
            key_points = VALUES(key_points),
            confusion_comparisons = VALUES(confusion_comparisons),
            prerequisite_concepts = VALUES(prerequisite_concepts),
            learning_example = VALUES(learning_example),
            misconception_notes = VALUES(misconception_notes),
            basic_review_points = VALUES(basic_review_points),
            misconception_mapping_coverage = VALUES(misconception_mapping_coverage),
            diagnostic_profile_version = VALUES(diagnostic_profile_version),
            review_status = VALUES(review_status),
            updated_at = NOW()
        """,
        (
            concept_id,
            _json_text(p.get("concept_core")),
            _json_text(p.get("concept_explanation")),
            _json_text(p.get("key_points")),
            _json_text(p.get("confusion_comparisons")),
            _json_text(p.get("prerequisite_concepts")),
            _json_text(p.get("learning_example")),
            _json_text(p.get("misconception_notes")),
            _json_text(p.get("basic_review_points")),
            round(float(p.get("misconception_mapping_coverage") or 0.0), 3),
            p.get("diagnostic_profile_version"),
            REVIEW_STATUS_DRAFT,
        ),
    )


def _upsert_question_profile(cursor: Any, question_id: int, concept_id: int, p: dict) -> None:
    cursor.execute(
        """
        INSERT INTO question_diagnostic_profile (
            question_id, concept_id, diagnostic_summary, question_skill_type,
            expected_solve_sec, difficulty, answer_misconception_map,
            diagnostic_profile_version, review_status, created_at, updated_at
        ) VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, NOW(), NOW())
        ON DUPLICATE KEY UPDATE
            concept_id = VALUES(concept_id),
            diagnostic_summary = VALUES(diagnostic_summary),
            question_skill_type = VALUES(question_skill_type),
            expected_solve_sec = VALUES(expected_solve_sec),
            difficulty = VALUES(difficulty),
            answer_misconception_map = VALUES(answer_misconception_map),
            diagnostic_profile_version = VALUES(diagnostic_profile_version),
            review_status = VALUES(review_status),
            updated_at = NOW()
        """,
        (
            question_id,
            concept_id,
            _json_text(p.get("diagnostic_summary")),
            p.get("question_skill_type"),
            p.get("expected_solve_sec"),
            p.get("difficulty"),
            _json_text(p.get("answer_misconception_map")),
            p.get("diagnostic_profile_version"),
            REVIEW_STATUS_DRAFT,
        ),
    )


def main() -> None:
    args = parse_args()
    path = Path(f"outputs/{args.subject_code}/diagnostic_profiles.json")
    if not path.exists():
        raise RuntimeError(f"diagnostic_profiles.json 이 없습니다: {path}")
    data = json.loads(path.read_text(encoding="utf-8"))

    concept_profiles = data.get("concept_profiles", [])
    question_profiles = data.get("question_profiles", [])

    settings = MySQLConnectionSettings.from_env()
    connection = _connect_mysql(settings)
    concept_ok = concept_skip = question_ok = question_skip = 0
    skipped_concepts: list[str] = []
    skipped_questions: list[str] = []

    try:
        cursor = connection.cursor(dictionary=True)
        try:
            for p in concept_profiles:
                concept_id = _resolve_concept_id(cursor, p["concept_code"])
                if concept_id is None:
                    concept_skip += 1
                    skipped_concepts.append(p["concept_code"])
                    continue
                if args.execute:
                    _upsert_concept_profile(cursor, concept_id, p)
                concept_ok += 1

            for p in question_profiles:
                question = _resolve_question(cursor, p["content_hash"])
                if question is None:
                    question_skip += 1
                    skipped_questions.append(p["content_hash"])
                    continue
                if args.execute:
                    _upsert_question_profile(
                        cursor, int(question["question_id"]), int(question["concept_id"]), p
                    )
                question_ok += 1

            if args.execute:
                connection.commit()
        except Exception:
            connection.rollback()
            raise
        finally:
            cursor.close()
    finally:
        connection.close()

    mode = "적재 완료" if args.execute else "DRY-RUN(해석만, 미기록)"
    print(f"[{mode}] subject={args.subject_code}")
    print(f"  개념 프로필: 해석성공 {concept_ok} / 미매칭 {concept_skip}")
    print(f"  문항 프로필: 해석성공 {question_ok} / 미매칭 {question_skip}")
    if skipped_concepts:
        print(f"  미매칭 concept_code(앞 5개): {skipped_concepts[:5]}")
    if skipped_questions:
        print(f"  미매칭 content_hash(앞 3개): {skipped_questions[:3]}")


if __name__ == "__main__":
    main()
