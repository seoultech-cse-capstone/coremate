from pathlib import Path

import fitz


def extract_pdf_chunks(pdf_path: str, subject_code: str) -> list[dict]:
    path = Path(pdf_path)
    doc = fitz.open(path)

    chunks = []

    for page_index, page in enumerate(doc):
        text = page.get_text("text").strip()

        if not text:
            continue

        chunks.append({
            "chunk_id": f"{subject_code}_{path.stem}_p{page_index + 1}",
            "subject_code": subject_code,
            "source_file": path.name,
            "page": page_index + 1,
            "text": text,
        })

    return chunks