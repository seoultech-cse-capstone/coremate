from server.services.job_store import JobStore
from server.services.pipeline_runner import PipelineRunner

job_store = JobStore()
pipeline_runner = PipelineRunner(job_store=job_store)

