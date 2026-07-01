from pathlib import Path

AI_PIPELINE_ROOT = Path(__file__).resolve().parents[2]
CONFIG_DIR = AI_PIPELINE_ROOT / "configs"
RAW_DATA_DIR = AI_PIPELINE_ROOT / "data" / "raw"
EXTRACTED_DATA_DIR = AI_PIPELINE_ROOT / "data" / "extracted"
OUTPUTS_DIR = AI_PIPELINE_ROOT / "outputs"
SCRIPTS_DIR = AI_PIPELINE_ROOT / "scripts"


def to_relative_path(path: Path) -> str:
    try:
        return str(path.relative_to(AI_PIPELINE_ROOT))
    except ValueError:
        return str(path)

