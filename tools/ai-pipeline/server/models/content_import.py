from enum import Enum

from pydantic import BaseModel, Field


class ContentImportStatus(str, Enum):
    VALIDATED = "validated"
    IMPORTED = "imported"


class ContentImportRequest(BaseModel):
    dry_run: bool = Field(
        default=True,
        description=(
            "Validate approved JSON exports and return the import plan without "
            "writing to MySQL."
        ),
    )
    activate_on_import: bool = Field(
        default=False,
        description=(
            "If true, imported concepts, common mistakes, questions, and "
            "relations are marked active. If false, each artifact's is_active "
            "value is preserved where present."
        ),
    )


class ContentImportArtifactSummary(BaseModel):
    artifact_type: str
    path: str
    item_count: int


class ContentImportTableSummary(BaseModel):
    table_name: str
    planned_count: int
    imported_count: int = 0


class ContentImportResponse(BaseModel):
    subject_code: str
    db_subject_code: str
    status: ContentImportStatus
    dry_run: bool
    activate_on_import: bool
    artifacts: list[ContentImportArtifactSummary]
    tables: list[ContentImportTableSummary]
    warnings: list[str] = Field(default_factory=list)
