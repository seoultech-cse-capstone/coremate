"""빈/부실 concept_card 를 골라 재생성·병합한다.

기본(소스 기반) 모드:
    정의/요약이 빈 카드만 대상으로, 개선된 검색기로 청크를 찾아 원본 03과 동일한
    프롬프트로 재생성한다. 청크가 0개면 LLM 호출을 건너뛴다(소스 부재).

일반지식(--general) 모드:
    소스에 근거가 없어 '빈칸' 또는 '근거부족 안내문'으로 남은 카드를 대상으로,
    일반지식 허용 프롬프트(prompts/generate_concept_card_general.md)로 재생성한다.
    청크가 0개여도 건너뛰지 않으며, 채운 카드는 추적용 사이드카 로그에 기록한다.

공통: 카드별 오류 격리, 저장 전 원본 .bak 백업, 멱등(다시 돌려도 여전히 문제인 것만).

사용법:
    python scripts/03b_backfill_empty_concept_cards.py                    # 소스기반, 6과목
    python scripts/03b_backfill_empty_concept_cards.py DATABASE           # 소스기반, 특정 과목
    python scripts/03b_backfill_empty_concept_cards.py --general          # 일반지식, 6과목
    python scripts/03b_backfill_empty_concept_cards.py --general --dry-run  # 대상만 집계
"""

import _bootstrap

import datetime
import json
import re
import shutil
import sys
from pathlib import Path

from schemas.concept_card_schema import CONCEPT_CARD_SCHEMA
from src.chunk_retriever import retrieve_chunks_by_keywords
from src.ai_client import call_ai_json

ALL_SUBJECTS = [
    "ALGORITHM",
    "COMPUTER_ARCHITECTURE",
    "DATABASE",
    "DATA_STRUCTURE",
    "NETWORK",
    "OPERATING_SYSTEM",
]

args = [a for a in sys.argv[1:] if not a.startswith("--")]
flags = {a for a in sys.argv[1:] if a.startswith("--")}
dry_run = "--dry-run" in flags
general = "--general" in flags
subjects = args if args else ALL_SUBJECTS

GROUNDED_PROMPT = Path("prompts/generate_concept_card.md").read_text(encoding="utf-8")
GENERAL_PROMPT = Path("prompts/generate_concept_card_general.md").read_text(encoding="utf-8")
prompt_template = GENERAL_PROMPT if general else GROUNDED_PROMPT

# one_line_definition 이 '근거/자료 부족' 안내문인 카드 탐지(정의가 실질적으로 없음).
# 소스를 언급(_SOURCE_META)하면서 동시에 부족/없음(_INSUFF)을 말할 때만 안내문으로 본다.
# (소스를 인용해 실제 정의를 서술한 정상 카드가 오탐되지 않도록 둘의 공존을 요구)
_SOURCE_META_RE = re.compile(
    r"(source_chunks|제공된\s*(자료|source)|주어진\s*(자료|source)|출처\s*자료|제공되지\s*않)"
)
_INSUFF_RE = re.compile(r"(없|부족|않아|어렵|수\s*없)")


def is_empty(value) -> bool:
    return value is None or (isinstance(value, str) and value.strip() == "")


def is_apology(card: dict) -> bool:
    text = card.get("one_line_definition") or ""
    return bool(_SOURCE_META_RE.search(text) and _INSUFF_RE.search(text))


def needs_backfill(card: dict) -> bool:
    """소스기반 모드 대상: 정의/요약이 빈 카드."""
    return is_empty(card.get("one_line_definition")) or is_empty(card.get("core_summary"))


def is_target(card: dict) -> bool:
    if needs_backfill(card):
        return True
    # 일반지식 모드에서는 '안내문'으로 채워진 카드도 다시 채운다.
    return general and is_apology(card)


def build_concept_map(taxonomy: dict) -> dict:
    """03의 iter_concepts와 동일한 concept 입력을 concept_code 기준으로 구성."""
    subject = taxonomy["subject"]
    mapping = {}
    for lv1 in taxonomy["category_lv1_list"]:
        for lv2 in lv1["category_lv2_list"]:
            for concept in lv2["concept_candidates"]:
                mapping[concept["concept_code"]] = {
                    "subject_code": subject["subject_code"],
                    "subject_name": subject["subject_name"],
                    "subject_name_en": subject["subject_name_en"],
                    "category_lv1_code": lv1["category_lv1_code"],
                    "category_lv1_name": lv1["category_lv1_name"],
                    "category_lv2_code": lv2["category_lv2_code"],
                    "category_lv2_name": lv2["category_lv2_name"],
                    **concept,
                }
    return mapping


def regenerate_card(concept: dict, chunks: list[dict]) -> dict:
    related_chunks = retrieve_chunks_by_keywords(
        chunks=chunks,
        concept_name=concept["concept_name"],
        concept_name_en=concept["concept_name_en"],
        aliases=concept.get("aliases", []),
        top_k=5,
    )

    # 소스기반 모드에서는 근거가 없으면 건너뛴다. 일반지식 모드는 그대로 진행.
    if not related_chunks and not general:
        return {"status": "skipped_no_source"}

    user_prompt = prompt_template.format(
        concept_json=json.dumps(concept, ensure_ascii=False, indent=2),
        source_chunks_json=json.dumps(related_chunks, ensure_ascii=False, indent=2),
    )

    result = call_ai_json(
        system_prompt="너는 CS 전공 개념을 공통 조사 템플릿에 맞게 구조화하는 전문가다.",
        user_prompt=user_prompt,
        schema_name="concept_card_schema",
        schema=CONCEPT_CARD_SCHEMA,
        reasoning_effort="low",
    )
    return {"status": "generated", "card": result["concept_card"], "used_chunks": len(related_chunks)}


def record_provenance(subject_code: str, entries: list[dict]) -> None:
    """일반지식으로 채운 카드를 추적용 사이드카에 누적 기록."""
    log_path = Path(f"outputs/{subject_code}/general_backfill_log.json")
    existing = {}
    if log_path.exists():
        for e in json.loads(log_path.read_text(encoding="utf-8")):
            existing[e["concept_code"]] = e
    for e in entries:
        existing[e["concept_code"]] = e
    log_path.write_text(
        json.dumps(list(existing.values()), ensure_ascii=False, indent=2),
        encoding="utf-8",
    )


mode_label = "일반지식(--general)" if general else "소스기반"
print(f"모드: {mode_label}\n")

totals = {"targets": 0, "filled": 0, "still_problem": 0, "skipped_no_source": 0, "errors": 0}

for subject_code in subjects:
    cards_path = Path(f"outputs/{subject_code}/concept_cards.json")
    tax_path = Path(f"outputs/{subject_code}/taxonomy.json")
    chunks_path = Path(f"data/extracted/{subject_code}/chunks.json")

    if not cards_path.exists():
        print(f"[{subject_code}] concept_cards.json 없음 → 건너뜀")
        continue

    cards = json.loads(cards_path.read_text(encoding="utf-8"))
    targets = [(i, c) for i, c in enumerate(cards) if is_target(c)]
    totals["targets"] += len(targets)

    print(f"\n[{subject_code}] 대상 {len(targets)}개")
    if not targets:
        continue

    if dry_run:
        for _, card in targets:
            kind = "빈칸" if needs_backfill(card) else "안내문"
            print(f"   - [{kind}] {card.get('concept_code')} {card.get('concept_name')}")
        continue

    concept_map = build_concept_map(json.loads(tax_path.read_text(encoding="utf-8")))
    chunks = json.loads(chunks_path.read_text(encoding="utf-8"))

    changed = False
    provenance = []
    stamp = datetime.datetime.now().isoformat(timespec="seconds")
    for idx, card in targets:
        code = card.get("concept_code")
        concept = concept_map.get(code)
        if concept is None:
            print(f"   ! {code}: taxonomy에 없음 → 건너뜀")
            totals["errors"] += 1
            continue
        try:
            outcome = regenerate_card(concept, chunks)
        except Exception as exc:  # 카드별 오류 격리
            print(f"   ! {code} {card.get('concept_name')}: 생성 오류 → 원본 유지 ({exc})")
            totals["errors"] += 1
            continue

        if outcome["status"] == "skipped_no_source":
            print(f"   · {code} {card.get('concept_name')}: 청크 0개(소스 부재) → 스킵, 원본 유지")
            totals["skipped_no_source"] += 1
            continue

        new_card = outcome["card"]
        new_card["concept_code"] = code  # 병합 키 고정(모델 드리프트 방지)
        cards[idx] = new_card
        changed = True

        if needs_backfill(new_card) or is_apology(new_card):
            print(f"   △ {code} {card.get('concept_name')}: 재생성했으나 여전히 부실")
            totals["still_problem"] += 1
        else:
            src = "일반지식" if general and outcome.get("used_chunks", 0) == 0 else f"청크 {outcome.get('used_chunks')}개"
            print(f"   ✓ {code} {card.get('concept_name')}: 채움 ({src})")
            totals["filled"] += 1
            if general:
                provenance.append({
                    "concept_code": code,
                    "concept_name": card.get("concept_name"),
                    "used_chunks": outcome.get("used_chunks", 0),
                    "filled_at": stamp,
                    "source": "general_knowledge" if outcome.get("used_chunks", 0) == 0 else "source_chunks",
                })

    if changed:
        backup = cards_path.with_suffix(".json.bak")
        shutil.copy2(cards_path, backup)
        cards_path.write_text(
            json.dumps(cards, ensure_ascii=False, indent=2), encoding="utf-8"
        )
        print(f"   → 저장 완료 (백업: {backup})")
        if general and provenance:
            record_provenance(subject_code, provenance)
            print(f"   → 추적 로그: outputs/{subject_code}/general_backfill_log.json (+{len(provenance)})")
    else:
        print("   → 변경 없음, 저장 생략")

print("\n===== 백필 요약 =====")
print(f"모드               : {mode_label}")
print(f"대상               : {totals['targets']}")
if not dry_run:
    print(f"채움               : {totals['filled']}")
    print(f"재생성했으나 부실  : {totals['still_problem']}")
    print(f"소스부재로 스킵    : {totals['skipped_no_source']}")
    print(f"오류               : {totals['errors']}")
