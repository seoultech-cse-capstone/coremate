import hashlib
import json


def make_question_hash(question: dict) -> str:
    payload = {
        "concept_code": question.get("concept_code"),
        "question_text": question.get("question_text"),
        "choices": question.get("choices"),
        "correct_answer": question.get("correct_answer"),
    }

    raw = json.dumps(payload, ensure_ascii=False, sort_keys=True)
    return hashlib.sha256(raw.encode("utf-8")).hexdigest()