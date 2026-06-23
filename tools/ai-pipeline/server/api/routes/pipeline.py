from fastapi import APIRouter, HTTPException, status

from server.dependencies import pipeline_runner
from server.models.pipeline import (
    ArtifactStatus,
    CreatePipelineJobRequest,
    PipelineJobResponse,
    SubjectSummary,
)
from server.services.artifact_service import get_subject_artifacts
from server.services.subject_service import (
    SubjectNotFoundError,
    get_subject_config,
    list_subject_configs,
)

router = APIRouter()


@router.get("/subjects", response_model=list[SubjectSummary])
def list_subjects() -> list[SubjectSummary]:
    return list_subject_configs()


@router.get("/subjects/{subject_code}", response_model=SubjectSummary)
def get_subject(subject_code: str) -> SubjectSummary:
    try:
        return get_subject_config(subject_code)
    except SubjectNotFoundError as exc:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=str(exc),
        ) from exc


@router.get("/subjects/{subject_code}/artifacts", response_model=list[ArtifactStatus])
def list_subject_artifacts(subject_code: str) -> list[ArtifactStatus]:
    try:
        get_subject_config(subject_code)
    except SubjectNotFoundError as exc:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=str(exc),
        ) from exc

    return get_subject_artifacts(subject_code)


@router.post(
    "/jobs",
    response_model=PipelineJobResponse,
    status_code=status.HTTP_202_ACCEPTED,
)
def create_pipeline_job(request: CreatePipelineJobRequest) -> PipelineJobResponse:
    try:
        get_subject_config(request.subject_code)
        return pipeline_runner.submit(request)
    except SubjectNotFoundError as exc:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=str(exc),
        ) from exc
    except ValueError as exc:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=str(exc),
        ) from exc


@router.get("/jobs", response_model=list[PipelineJobResponse])
def list_pipeline_jobs() -> list[PipelineJobResponse]:
    return pipeline_runner.list_jobs()


@router.get("/jobs/{job_id}", response_model=PipelineJobResponse)
def get_pipeline_job(job_id: str) -> PipelineJobResponse:
    job = pipeline_runner.get_job(job_id)
    if job is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"pipeline job not found: {job_id}",
        )

    return job

