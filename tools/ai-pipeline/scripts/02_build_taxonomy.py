import _bootstrap

import json
import sys
from pathlib import Path

import yaml

from schemas.taxonomy_schema import TAXONOMY_SCHEMA
from src.openai_client import call_openai_json
from src.taxonomy_validator import validate_taxonomy


if len(sys.argv) < 2:
    raise RuntimeError("사용법: python scripts/02_build_taxonomy.py {SUBJECT_CODE}")

subject_code = sys.argv[1]

config_path = Path(f"configs/{subject_code}.yaml")

if not config_path.exists():
    raise RuntimeError(f"과목 설정 파일이 없습니다: {config_path}")

config = yaml.safe_load(config_path.read_text(encoding="utf-8"))

prompt_template = Path("prompts/build_taxonomy.md").read_text(encoding="utf-8")

taxonomy_rules = config.get("taxonomy_rules", {})
taxonomy_scope = config.get("taxonomy_scope", {})

user_prompt = prompt_template.format(
    subject_code=config["subject_code"],
    subject_name=config["subject_name"],
    subject_name_en=config["subject_name_en"],
    taxonomy_rules_json=json.dumps(taxonomy_rules, ensure_ascii=False, indent=2),
    taxonomy_scope_json=json.dumps(taxonomy_scope, ensure_ascii=False, indent=2),
)

result = call_openai_json(
    system_prompt="너는 CS 전공 지식을 학습 앱용 계층 구조로 정리하는 전문가다.",
    user_prompt=user_prompt,
    schema_name="taxonomy_schema",
    schema=TAXONOMY_SCHEMA,
    reasoning_effort="medium",
)

out_dir = Path(f"outputs/{subject_code}")
out_dir.mkdir(parents=True, exist_ok=True)

out_path = out_dir / "taxonomy.json"

with open(out_path, "w", encoding="utf-8") as f:
    json.dump(result, f, ensure_ascii=False, indent=2)

print(f"taxonomy 생성 완료: {out_path}")

warnings = validate_taxonomy(
    result,
    exclude_topics=taxonomy_scope.get("exclude_topics", []),
)

warning_path = out_dir / "taxonomy_warnings.txt"

if warnings:
    print("\ntaxonomy 검토 경고:")
    for warning in warnings:
        print(f"- {warning}")

    with open(warning_path, "w", encoding="utf-8") as f:
        for warning in warnings:
            f.write(f"{warning}\n")

    print(f"\ntaxonomy 경고 저장 완료: {warning_path}")
else:
    print("taxonomy 자동 검토 경고 없음")