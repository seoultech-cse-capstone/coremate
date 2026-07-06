from datetime import datetime
from enum import Enum

from pydantic import BaseModel, Field


class PipelineStep(str, Enum):
    EXTRACT_PDF = "extract_pdf"
    BUILD_TAXONOMY = "build_taxonomy"
    GENERATE_CONCEPT_CARDS = "generate_concept_cards"
    GENERATE_CONCEPT_RELATIONS = "generate_concept_relations"
    GENERATE_COMMON_MISTAKES = "generate_common_mistakes"
    GENERATE_QUESTIONS = "generate_questions"
    REVIEW_QUESTIONS = "review_questions"
    MAKE_REPORT = "make_report"


class JobStatus(str, Enum):
    QUEUED = "queued"
    RUNNING = "running"
    SUCCEEDED = "succeeded"
    FAILED = "failed"


class StepStatus(str, Enum):
    PENDING = "pending"
    RUNNING = "running"
    SUCCEEDED = "succeeded"
    FAILED = "failed"
    SKIPPED = "skipped"


class CreatePipelineJobRequest(BaseModel):
    subject_code: str = Field(
        ...,
        min_length=1,
        examples=["OPERATING_SYSTEM"],
        description="Subject code matching a YAML file under configs.",
    )
    steps: list[PipelineStep] | None = Field(
        default=None,
        description="Pipeline steps to run. If omitted, all steps run in default order.",
    )
    continue_on_error: bool = Field(
        default=False,
        description="If true, later steps continue after a failed step.",
    )


class SubjectSummary(BaseModel):
    subject_code: str
    db_subject_code: str | None = None
    subject_name: str
    subject_name_en: str
    config_path: str
    raw_pdf_count: int
    has_extracted_chunks: bool
    has_output_dir: bool


class ArtifactStatus(BaseModel):
    name: str
    kind: str
    path: str
    exists: bool
    size_bytes: int | None = None
    updated_at: datetime | None = None


class PipelineStepRun(BaseModel):
    step: PipelineStep
    status: StepStatus
    started_at: datetime | None = None
    finished_at: datetime | None = None
    return_code: int | None = None
    stdout_tail: str | None = None
    stderr_tail: str | None = None
    error_message: str | None = None


class PipelineJobResponse(BaseModel):
    job_id: str
    subject_code: str
    status: JobStatus
    steps: list[PipelineStep]
    continue_on_error: bool
    created_at: datetime
    started_at: datetime | None = None
    finished_at: datetime | None = None
    current_step: PipelineStep | None = None
    step_runs: list[PipelineStepRun]
    error_message: str | None = None

