import os
import subprocess
import sys
from concurrent.futures import ThreadPoolExecutor
from dataclasses import dataclass
from pathlib import Path

from server.core.paths import AI_PIPELINE_ROOT, SCRIPTS_DIR
from server.models.pipeline import (
    CreatePipelineJobRequest,
    PipelineJobResponse,
    PipelineStep,
    StepStatus,
)
from server.services.job_store import JobStore


@dataclass(frozen=True)
class StepCommand:
    step: PipelineStep
    script_name: str


DEFAULT_STEP_COMMANDS = [
    StepCommand(PipelineStep.EXTRACT_PDF, "01_extract_pdf.py"),
    StepCommand(PipelineStep.BUILD_TAXONOMY, "02_build_taxonomy.py"),
    StepCommand(PipelineStep.GENERATE_CONCEPT_CARDS, "03_generate_concept_cards.py"),
    StepCommand(PipelineStep.GENERATE_CONCEPT_RELATIONS, "04_generate_concept_relations.py"),
    StepCommand(PipelineStep.GENERATE_COMMON_MISTAKES, "05_generate_common_mistakes.py"),
    StepCommand(PipelineStep.GENERATE_QUESTIONS, "06_generate_questions.py"),
    StepCommand(PipelineStep.REVIEW_QUESTIONS, "07_review_questions.py"),
    StepCommand(PipelineStep.MAKE_REPORT, "08_make_report.py"),
]

STEP_TO_SCRIPT = {command.step: command.script_name for command in DEFAULT_STEP_COMMANDS}
DEFAULT_STEPS = [command.step for command in DEFAULT_STEP_COMMANDS]
OUTPUT_TAIL_LENGTH = 4000


class PipelineRunner:
    def __init__(self, *, job_store: JobStore, max_workers: int = 2) -> None:
        self._job_store = job_store
        self._executor = ThreadPoolExecutor(max_workers=max_workers)

    def submit(self, request: CreatePipelineJobRequest) -> PipelineJobResponse:
        subject_code = request.subject_code.upper()
        steps = self._normalize_steps(request.steps)

        job = self._job_store.create_job(
            subject_code=subject_code,
            steps=steps,
            continue_on_error=request.continue_on_error,
        )
        self._executor.submit(self._run_job, job.job_id)
        return job

    def list_jobs(self) -> list[PipelineJobResponse]:
        return self._job_store.list_jobs()

    def get_job(self, job_id: str) -> PipelineJobResponse | None:
        return self._job_store.get_job(job_id)

    def _run_job(self, job_id: str) -> None:
        self._job_store.mark_running(job_id)
        subject_code = self._job_store.get_subject_code(job_id)
        continue_on_error = self._job_store.get_continue_on_error(job_id)
        failed_messages = []

        for step in self._job_store.get_job_steps(job_id):
            step_index = self._job_store.start_step(job_id, step)
            result = self._run_step(step=step, subject_code=subject_code)

            if result.returncode == 0:
                self._job_store.finish_step(
                    job_id=job_id,
                    step_index=step_index,
                    status=StepStatus.SUCCEEDED,
                    return_code=result.returncode,
                    stdout_tail=_tail(result.stdout),
                    stderr_tail=_tail(result.stderr),
                )
                continue

            error_message = (
                f"{step.value} failed with return code {result.returncode}"
            )
            failed_messages.append(error_message)
            self._job_store.finish_step(
                job_id=job_id,
                step_index=step_index,
                status=StepStatus.FAILED,
                return_code=result.returncode,
                stdout_tail=_tail(result.stdout),
                stderr_tail=_tail(result.stderr),
                error_message=error_message,
            )

            if not continue_on_error:
                self._job_store.mark_failed(job_id, error_message)
                return

        if failed_messages:
            self._job_store.mark_failed(job_id, "; ".join(failed_messages))
        else:
            self._job_store.mark_succeeded(job_id)

    def _run_step(self, *, step: PipelineStep, subject_code: str) -> subprocess.CompletedProcess[str]:
        script_name = STEP_TO_SCRIPT[step]
        script_path = SCRIPTS_DIR / script_name

        if not script_path.exists():
            return subprocess.CompletedProcess(
                args=[sys.executable, str(script_path), subject_code],
                returncode=127,
                stdout="",
                stderr=f"script not found: {script_path}",
            )

        env = os.environ.copy()
        env["PYTHONPATH"] = _build_pythonpath(AI_PIPELINE_ROOT, env.get("PYTHONPATH"))

        return subprocess.run(
            [sys.executable, str(script_path), subject_code],
            cwd=AI_PIPELINE_ROOT,
            env=env,
            text=True,
            capture_output=True,
            check=False,
        )

    @staticmethod
    def _normalize_steps(steps: list[PipelineStep] | None) -> list[PipelineStep]:
        if not steps:
            return list(DEFAULT_STEPS)

        unknown_steps = [step for step in steps if step not in STEP_TO_SCRIPT]
        if unknown_steps:
            unknown_values = ", ".join(step.value for step in unknown_steps)
            raise ValueError(f"unsupported pipeline steps: {unknown_values}")

        return list(steps)


def _build_pythonpath(root: Path, existing: str | None) -> str:
    entries = [str(root)]
    if existing:
        entries.append(existing)
    return os.pathsep.join(entries)


def _tail(value: str | None) -> str | None:
    if not value:
        return None

    return value[-OUTPUT_TAIL_LENGTH:]

