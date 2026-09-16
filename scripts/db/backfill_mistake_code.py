"""common_mistake.mistake_code 백필.

DB의 common_mistake 에는 오개념 코드가 없고, 코드(예: ALG-C-01-01-01_M02)는 AI 파이프라인
산출물(outputs/{SUBJECT}/common_mistakes.json)에만 있다. 약점 분석표가 "반복된 오개념"을
제목으로 보여 주려면 문항 진단 프로필의 오개념 코드를 제목으로 되돌릴 수 있어야 해서,
(개념 코드, 오해 제목)으로 짝지어 mistake_code 를 채운다.

실행 (파이프라인 venv 사용, DB 접속 정보는 infra/.env 를 그대로 씀):
    tools/ai-pipeline/.venv/bin/python scripts/db/backfill_mistake_code.py            # 미리보기
    tools/ai-pipeline/.venv/bin/python scripts/db/backfill_mistake_code.py --execute  # 실제 반영
"""

import argparse
import json
import os
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]
OUTPUTS_DIR = REPO_ROOT / "tools" / "ai-pipeline" / "outputs"
INFRA_ENV = REPO_ROOT / "infra" / ".env"
PIPELINE_ENV = REPO_ROOT / "tools" / "ai-pipeline" / ".env"


def load_pairs() -> dict[tuple[str, str], str]:
    """(concept_code, mistake_title) → mistake_code."""
    pairs: dict[tuple[str, str], str] = {}
    files = sorted(OUTPUTS_DIR.glob("*/common_mistakes.json"))
    if not files:
        sys.exit(f"산출물이 없습니다: {OUTPUTS_DIR}/*/common_mistakes.json")
    for path in files:
        for group in json.loads(path.read_text(encoding="utf-8")):
            concept_code = group["concept_code"]
            for mistake in group.get("mistakes", []):
                key = (concept_code, mistake["mistake_title"])
                code = mistake["mistake_code"]
                if pairs.get(key, code) != code:
                    sys.exit(f"같은 (개념, 제목)에 다른 코드가 있습니다: {key}")
                pairs[key] = code
    return pairs


def connect():
    from dotenv import load_dotenv
    import mysql.connector

    load_dotenv(PIPELINE_ENV)
    load_dotenv(INFRA_ENV)

    def env(*names: str, default: str = "") -> str:
        for name in names:
            value = os.environ.get(name)
            if value:
                return value
        return default

    return mysql.connector.connect(
        host=env("COREMATE_MYSQL_HOST", "MYSQL_HOST", default="127.0.0.1"),
        port=int(env("COREMATE_MYSQL_PORT", "MYSQL_PORT", default="3306")),
        user=env("COREMATE_MYSQL_USER", "MYSQL_USER", default="coremate"),
        password=env("COREMATE_MYSQL_PASSWORD", "MYSQL_PASSWORD"),
        database=env("COREMATE_MYSQL_DATABASE", "MYSQL_DATABASE", default="coremate"),
        charset="utf8mb4",
        autocommit=False,
    )


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--execute", action="store_true", help="실제로 UPDATE 한다(기본은 미리보기)")
    args = parser.parse_args()

    pairs = load_pairs()
    connection = connect()
    cursor = connection.cursor()
    try:
        cursor.execute(
            "SELECT cm.common_mistake_id, c.concept_code, cm.mistake_title, cm.mistake_code"
            " FROM common_mistake cm JOIN concept c ON c.concept_id = cm.concept_id"
        )
        rows = cursor.fetchall()

        updates: list[tuple[str, int]] = []
        already = 0
        unmatched: list[tuple[str, str]] = []
        for mistake_id, concept_code, title, current_code in rows:
            code = pairs.get((concept_code, title))
            if code is None:
                unmatched.append((concept_code, title))
                continue
            if current_code == code:
                already += 1
                continue
            updates.append((code, mistake_id))

        print(f"DB 행 {len(rows)}개 / 산출물 쌍 {len(pairs)}개")
        print(f"채울 행 {len(updates)}개 · 이미 같은 값 {already}개 · 산출물에 없어 건너뛴 행 {len(unmatched)}개")
        for concept_code, title in unmatched[:5]:
            print(f"  건너뜀: {concept_code} / {title}")
        if len(unmatched) > 5:
            print(f"  … 외 {len(unmatched) - 5}개")

        if not args.execute:
            print("\n미리보기입니다. 반영하려면 --execute 를 붙이세요.")
            return

        cursor.executemany(
            "UPDATE common_mistake SET mistake_code = %s WHERE common_mistake_id = %s", updates
        )
        connection.commit()
        print(f"\n반영 완료: {cursor.rowcount}행")
    finally:
        cursor.close()
        connection.close()


if __name__ == "__main__":
    main()
