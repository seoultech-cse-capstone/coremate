from copy import deepcopy
from datetime import datetime, timezone
from threading import Lock
from uuid import uuid4

from server.models.pipeline import (
    JobStatus,
    PipelineJobResponse,
    PipelineStep,
    PipelineStepRun,
    StepStatus,
)


class JobStore:
    def __init__(self) -> None:
        self._jobs: dict[str, dict] = {}
        self._lock = Lock()

    def create_job(
        self,
        *,
        subject_code: str,
        steps: list[PipelineStep],
        continue_on_error: bool,
    ) -> PipelineJobResponse:
        job_id = str(uuid4())
        now = datetime.now(timezone.utc)

        job = {
            "job_id": job_id,
            "subject_code": subject_code,
            "status": JobStatus.QUEUED,
            "steps": steps,
            "continue_on_error": continue_on_error,
            "created_at": now,
            "started_at": None,
            "finished_at": None,
            "current_step": None,
            "step_runs": [],
            "error_message": None,
        }

        with self._lock:
            self._jobs[job_id] = job

        return self._to_response(job)

    def list_jobs(self) -> list[PipelineJobResponse]:
        with self._lock:
            jobs = [deepcopy(job) for job in self._jobs.values()]

        jobs.sort(key=lambda job: job["created_at"], reverse=True)
        return [self._to_response(job) for job in jobs]

    def get_job(self, job_id: str) -> PipelineJobResponse | None:
        with self._lock:
            job = self._jobs.get(job_id)
            if job is None:
                return None
            snapshot = deepcopy(job)

        return self._to_response(snapshot)

    def get_job_steps(self, job_id: str) -> list[PipelineStep]:
        with self._lock:
            return list(self._jobs[job_id]["steps"])

    def get_continue_on_error(self, job_id: str) -> bool:
        with self._lock:
            return bool(self._jobs[job_id]["continue_on_error"])

    def get_subject_code(self, job_id: str) -> str:
        with self._lock:
            return str(self._jobs[job_id]["subject_code"])

    def mark_running(self, job_id: str) -> None:
        with self._lock:
            job = self._jobs[job_id]
            job["status"] = JobStatus.RUNNING
            job["started_at"] = datetime.now(timezone.utc)

    def mark_succeeded(self, job_id: str) -> None:
        with self._lock:
            job = self._jobs[job_id]
            job["status"] = JobStatus.SUCCEEDED
            job["current_step"] = None
            job["finished_at"] = datetime.now(timezone.utc)

    def mark_failed(self, job_id: str, error_message: str) -> None:
        with self._lock:
            job = self._jobs[job_id]
            job["status"] = JobStatus.FAILED
            job["current_step"] = None
            job["finished_at"] = datetime.now(timezone.utc)
            job["error_message"] = error_message

    def start_step(self, job_id: str, step: PipelineStep) -> int:
        with self._lock:
            job = self._jobs[job_id]
            job["current_step"] = step
            job["step_runs"].append(
                {
                    "step": step,
                    "status": StepStatus.RUNNING,
                    "started_at": datetime.now(timezone.utc),
                    "finished_at": None,
                    "return_code": None,
                    "stdout_tail": None,
                    "stderr_tail": None,
                    "error_message": None,
                }
            )
            return len(job["step_runs"]) - 1

    def finish_step(
        self,
        *,
        job_id: str,
        step_index: int,
        status: StepStatus,
        return_code: int | None = None,
        stdout_tail: str | None = None,
        stderr_tail: str | None = None,
        error_message: str | None = None,
    ) -> None:
        with self._lock:
            run = self._jobs[job_id]["step_runs"][step_index]
            run["status"] = status
            run["finished_at"] = datetime.now(timezone.utc)
            run["return_code"] = return_code
            run["stdout_tail"] = stdout_tail
            run["stderr_tail"] = stderr_tail
            run["error_message"] = error_message

    @staticmethod
    def _to_response(job: dict) -> PipelineJobResponse:
        return PipelineJobResponse(
            **{
                **job,
                "step_runs": [PipelineStepRun(**run) for run in job["step_runs"]],
            }
        )
