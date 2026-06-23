from pathlib import Path

import yaml

from server.core.paths import (
    CONFIG_DIR,
    EXTRACTED_DATA_DIR,
    OUTPUTS_DIR,
    RAW_DATA_DIR,
    to_relative_path,
)
from server.models.pipeline import SubjectSummary


class SubjectNotFoundError(Exception):
    pass


def list_subject_configs() -> list[SubjectSummary]:
    subjects = [
        _read_subject_config(path)
        for path in sorted(CONFIG_DIR.glob("*.yaml"))
    ]
    subjects.sort(key=lambda subject: subject.subject_code)
    return subjects


def get_subject_config(subject_code: str) -> SubjectSummary:
    normalized_subject_code = subject_code.upper()
    path = CONFIG_DIR / f"{normalized_subject_code}.yaml"

    if not path.exists():
        raise SubjectNotFoundError(
            f"subject config not found: {normalized_subject_code}"
        )

    return _read_subject_config(path)


def _read_subject_config(path: Path) -> SubjectSummary:
    data = yaml.safe_load(path.read_text(encoding="utf-8")) or {}
    subject_code = data.get("subject_code") or path.stem
    raw_pdf_dir = RAW_DATA_DIR / subject_code
    extracted_chunks_path = EXTRACTED_DATA_DIR / subject_code / "chunks.json"
    output_dir = OUTPUTS_DIR / subject_code

    return SubjectSummary(
        subject_code=subject_code,
        db_subject_code=data.get("db_subject_code"),
        subject_name=data.get("subject_name", subject_code),
        subject_name_en=data.get("subject_name_en", subject_code),
        config_path=to_relative_path(path),
        raw_pdf_count=_count_raw_pdfs(raw_pdf_dir),
        has_extracted_chunks=extracted_chunks_path.exists(),
        has_output_dir=output_dir.exists(),
    )


def _count_raw_pdfs(path: Path) -> int:
    if not path.exists():
        return 0

    return sum(1 for item in path.glob("*.pdf") if item.is_file())

