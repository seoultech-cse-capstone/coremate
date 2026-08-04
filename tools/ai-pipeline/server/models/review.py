from datetime import datetime
from enum import Enum
from typing import Any

from pydantic import BaseModel, Field


class ReviewArtifactType(str, Enum):
    TAXONOMY = "taxonomy"
    CONCEPT_CARDS = "concept_cards"
    CONCEPT_RELATIONS = "concept_relations"
    COMMON_MISTAKES = "common_mistakes"
    QUESTIONS = "questions"
    QUESTION_REVIEWS = "question_reviews"
    CONCEPT_DIAGNOSTIC_PROFILES = "concept_diagnostic_profiles"
    QUESTION_DIAGNOSTIC_PROFILES = "question_diagnostic_profiles"


class ReviewStatus(str, Enum):
    PENDING = "pending"
    APPROVED = "approved"
    REJECTED = "rejected"


class ReviewDecisionRequest(BaseModel):
    status: ReviewStatus = Field(
        ...,
        description="Manual review decision for the generated item.",
    )
    reviewer: str | None = Field(
        default=None,
        description="Name or identifier of the reviewer.",
    )
    review_note: str | None = Field(
        default=None,
        description="Reviewer note explaining approval, edits, or rejection.",
    )
    edited_content: Any | None = Field(
        default=None,
        description="Human-edited replacement content. If omitted, original content is used.",
    )


class BulkReviewApproveRequest(BaseModel):
    reviewer: str | None = Field(
        default=None,
        description="Name or identifier of the reviewer applying the bulk approval.",
    )
    review_note: str | None = Field(
        default="bulk approval",
        description="Reviewer note stored on every item approved by this request.",
    )
    overwrite_existing: bool = Field(
        default=False,
        description=(
            "If true, rejected or already-reviewed items are changed to approved. "
            "If false, only pending items are changed while already-approved items are kept."
        ),
    )
    export_approved: bool = Field(
        default=True,
        description="If true, write the approved artifact export after approval.",
    )


class BulkSubjectReviewApproveRequest(BulkReviewApproveRequest):
    artifact_types: list[ReviewArtifactType] | None = Field(
        default=None,
        description=(
            "Artifacts to bulk approve. If omitted, the five DB import artifacts "
            "are approved: taxonomy, concept_cards, concept_relations, "
            "common_mistakes, questions."
        ),
    )


class ReviewArtifactSummary(BaseModel):
    subject_code: str
    artifact_type: ReviewArtifactType
    source_path: str
    source_exists: bool
    review_path: str
    review_exists: bool
    approved_export_path: str
    approved_export_exists: bool
    total_count: int
    pending_count: int
    approved_count: int
    rejected_count: int
    edited_count: int
    stale_review_count: int
    error_message: str | None = None


class ReviewItemResponse(BaseModel):
    subject_code: str
    artifact_type: ReviewArtifactType
    item_key: str
    status: ReviewStatus
    reviewer: str | None = None
    review_note: str | None = None
    has_edits: bool
    updated_at: datetime | None = None
    original_content: Any
    effective_content: Any


class ReviewItemListResponse(BaseModel):
    subject_code: str
    artifact_type: ReviewArtifactType
    source_path: str
    review_path: str
    total_count: int
    pending_count: int
    approved_count: int
    rejected_count: int
    edited_count: int
    items: list[ReviewItemResponse]


class ApprovedArtifactExportResponse(BaseModel):
    subject_code: str
    artifact_type: ReviewArtifactType
    approved_count: int
    exported_path: str
    exported_content: Any


class BulkReviewApproveArtifactResponse(BaseModel):
    subject_code: str
    artifact_type: ReviewArtifactType
    total_count: int
    changed_count: int
    already_approved_count: int
    skipped_count: int
    approved_count: int
    exported_path: str | None = None
    exported_count: int | None = None


class BulkReviewApproveSubjectResponse(BaseModel):
    subject_code: str
    artifacts: list[BulkReviewApproveArtifactResponse]
