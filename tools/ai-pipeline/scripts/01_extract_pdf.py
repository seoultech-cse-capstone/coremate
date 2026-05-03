import _bootstrap

import json
import sys
from pathlib import Path

from src.pdf_loader import extract_pdf_chunks


if len(sys.argv) < 2:
    raise RuntimeError("사용법: python scripts/01_extract_pdf.py {SUBJECT_CODE}")

subject_code = sys.argv[1]

raw_dir = Path(f"data/raw/{subject_code}")
out_dir = Path(f"data/extracted/{subject_code}")
out_dir.mkdir(parents=True, exist_ok=True)

if not raw_dir.exists():
    raise RuntimeError(f"PDF 폴더가 없습니다: {raw_dir}")

all_chunks = []

for pdf_path in raw_dir.glob("*.pdf"):
    print(f"PDF 추출 중: {pdf_path}")
    chunks = extract_pdf_chunks(str(pdf_path), subject_code)
    all_chunks.extend(chunks)

if not all_chunks:
    raise RuntimeError(f"추출된 PDF 텍스트가 없습니다. PDF 위치를 확인하세요: {raw_dir}")

output_path = out_dir / "chunks.json"

with open(output_path, "w", encoding="utf-8") as f:
    json.dump(all_chunks, f, ensure_ascii=False, indent=2)

print(f"chunks 저장 완료: {output_path}")
print(f"chunk 수: {len(all_chunks)}")