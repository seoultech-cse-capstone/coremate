"""생성 스크립트의 이어하기(resume) 지원.

AI 호출 루프는 대부분 수십~수백 회 호출한 뒤 마지막에 한 번 저장하는 구조라,
도중에 실패하면 이미 지불한 호출 비용이 통째로 버려진다. 이 모듈은 두 가지를 준다.

1) 중간 저장 — SAVE_EVERY 건마다 파일에 기록해 실패 지점까지는 남긴다.
2) 이어하기 — 다시 실행할 때 기존 산출물을 읽어 이미 만든 항목은 건너뛴다.

사용 예:
    from src.resume import SAVE_EVERY, is_forced, load_existing, save_progress

    force = is_forced(sys.argv)
    items, done = load_existing(out_path, lambda c: c["concept_code"], force=force)
    pending = [c for c in sources if c["concept_code"] not in done]

    for index, source in enumerate(tqdm(pending), start=1):
        items.append(generate(source))
        if index % SAVE_EVERY == 0:
            save_progress(out_path, items)
    save_progress(out_path, items)
"""

import json
from pathlib import Path
from typing import Any, Callable, Sequence

# 몇 건마다 중간 저장할지.
SAVE_EVERY = 10

FORCE_FLAG = "--force"


def is_forced(argv: Sequence[str]) -> bool:
    """--force 가 있으면 기존 산출물을 무시하고 전부 다시 생성한다."""
    return FORCE_FLAG in argv


def load_existing(
    path: Path,
    key_of: Callable[[dict], Any],
    *,
    force: bool = False,
    keep: Callable[[dict], bool] | None = None,
) -> tuple[list, set]:
    """
    기존 산출물(JSON 배열)을 읽어 (이어서 쓸 목록, 완료된 키 집합) 을 돌려준다.

    keep 을 주면 그 조건을 통과한 항목만 완료로 인정한다(형식이 바뀐 항목 재생성용).
    파일이 없거나 깨져 있으면 빈 상태에서 시작한다.
    """
    if force or not path.exists():
        return [], set()

    try:
        items = json.loads(path.read_text(encoding="utf-8"))
    except json.JSONDecodeError:
        return [], set()

    if not isinstance(items, list):
        return [], set()

    kept = []
    done = set()

    for item in items:
        if not isinstance(item, dict):
            continue
        if keep is not None and not keep(item):
            continue
        try:
            key = key_of(item)
        except (KeyError, TypeError):
            continue
        if key is None:
            continue
        kept.append(item)
        done.add(key)

    return kept, done


def save_progress(path: Path, items: list) -> None:
    """중간 저장. 쓰다가 죽어도 원본이 깨지지 않도록 임시 파일에 쓴 뒤 교체한다."""
    path.parent.mkdir(parents=True, exist_ok=True)
    temp_path = path.with_suffix(f"{path.suffix}.tmp")

    with open(temp_path, "w", encoding="utf-8") as file:
        json.dump(items, file, ensure_ascii=False, indent=2)

    temp_path.replace(path)


def report_resume(label: str, done_count: int, pending_count: int) -> None:
    """이어하기 상황을 한 줄로 알린다(건너뛴 게 없으면 조용히 넘어간다)."""
    if done_count:
        print(f"{label}: 기존 {done_count}개 건너뜀, {pending_count}개 생성 예정")
