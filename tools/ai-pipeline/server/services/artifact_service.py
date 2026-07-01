from pathlib import Path
from datetime import datetime, timezone

from server.core.paths import EXTRACTED_DATA_DIR, OUTPUTS_DIR, to_relative_path
from server.models.pipeline import ArtifactStatus

ARTIFACTS = [
    ("chunks", "extracted", "chunks.json"),
    ("taxonomy", "output", "taxonomy.json"),
    ("taxonomy_warnings", "output", "taxonomy_warnings.txt"),
    ("concept_cards", "output", "concept_cards.json"),
    ("concept_relations", "output", "concept_relations.json"),
    ("common_mistakes", "output", "common_mistakes.json"),
    ("questions", "output", "questions.json"),
    ("question_validation_errors", "output", "question_validation_errors.txt"),
    ("question_reviews", "output", "question_reviews.json"),
    ("final_report", "output", "final_report.md"),
]


def _artifact_status(name: str, kind: str, path: Path) -> ArtifactStatus:
    if not path.exists():
        return ArtifactStatus(
            name=name,
            kind=kind,
            path=to_relative_path(path),
            exists=False,
        )

    stat = path.stat()
    return ArtifactStatus(
        name=name,
        kind=kind,
        path=to_relative_path(path),
        exists=True,
        size_bytes=stat.st_size,
        updated_at=datetime.fromtimestamp(stat.st_mtime, tz=timezone.utc),
    )


def get_subject_artifacts(subject_code: str) -> list[ArtifactStatus]:
    normalized_subject_code = subject_code.upper()
    artifacts = []

    for name, kind, file_name in ARTIFACTS:
        if kind == "extracted":
            absolute_path = EXTRACTED_DATA_DIR / normalized_subject_code / file_name
        else:
            absolute_path = OUTPUTS_DIR / normalized_subject_code / file_name

        artifacts.append(_artifact_status(name, kind, absolute_path))

    return artifacts
