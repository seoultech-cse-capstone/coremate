ALLOWED_CHOICE_KEYS = ["A", "B", "C", "D"]

ALLOWED_QUESTION_TYPES = [
    "definition",
    "comparison",
    "misconception",
    "example",
    "application",
]

ALLOWED_DIFFICULTY_LEVELS = [1, 2, 3, 4, 5]

ALLOWED_DIFFICULTY_SOURCES = [
    "ai_initial",
    "manual",
    "user_log_based",
]

ALLOWED_SOURCE_TYPES = [
    "ai_generated",
    "manual_authored",
    "imported",
]

ALLOWED_REVIEW_STATUSES = [
    "draft",
    "reviewed",
    "approved",
    "rejected",
    "archived",
]


def _is_non_empty_string(value) -> bool:
    return isinstance(value, str) and bool(value.strip())


def validate_question(question: dict, *, strict: bool = False) -> list[str]:
    errors = []

    if not _is_non_empty_string(question.get("concept_code")):
        errors.append("concept_code가 비어 있음")

    if question.get("question_format") != "MCQ_4":
        errors.append("question_format이 MCQ_4가 아님")

    if question.get("question_type") not in ALLOWED_QUESTION_TYPES:
        errors.append(
            "question_type이 허용 값이 아님 "
            f"허용값: {ALLOWED_QUESTION_TYPES}"
        )

    if not _is_non_empty_string(question.get("question_text")):
        errors.append("question_text가 비어 있음")

    choices = question.get("choices", [])

    if not isinstance(choices, list):
        errors.append("choices가 리스트가 아님")
        choices = []

    if len(choices) != 4:
        errors.append("선택지가 4개가 아님")

    choice_keys = []
    choice_texts = []

    for index, choice in enumerate(choices):
        if not isinstance(choice, dict):
            errors.append(f"choices[{index}]가 객체가 아님")
            continue

        if "label" in choice:
            errors.append(
                f"choices[{index}]에 label 필드가 있음. "
                "DB 명세 기준으로 key 필드를 사용해야 함"
            )

        key = choice.get("key")
        text = choice.get("text")

        choice_keys.append(key)
        choice_texts.append(text)

        if key not in ALLOWED_CHOICE_KEYS:
            errors.append(f"choices[{index}].key가 A/B/C/D 중 하나가 아님")

        if not _is_non_empty_string(text):
            errors.append(f"choices[{index}].text가 비어 있음")

    if sorted(choice_keys) != ALLOWED_CHOICE_KEYS:
        errors.append("선택지 key 구성이 A/B/C/D가 아님")

    non_empty_texts = [
        text.strip()
        for text in choice_texts
        if isinstance(text, str) and text.strip()
    ]

    if len(non_empty_texts) != len(set(non_empty_texts)):
        errors.append("선택지 text에 중복이 있음")

    correct_answer = question.get("correct_answer")

    if correct_answer not in ALLOWED_CHOICE_KEYS:
        errors.append("correct_answer가 A/B/C/D 중 하나가 아님")
    elif choice_keys and correct_answer not in choice_keys:
        errors.append("correct_answer가 choices의 key 목록에 없음")

    if not _is_non_empty_string(question.get("explanation_text")):
        errors.append("explanation_text가 비어 있음")

    if question.get("difficulty_level") not in ALLOWED_DIFFICULTY_LEVELS:
        errors.append("difficulty_level이 1~5 범위가 아님")

    expected_solve_sec = question.get("expected_solve_sec")

    if not isinstance(expected_solve_sec, int):
        errors.append("expected_solve_sec가 정수가 아님")
    elif expected_solve_sec <= 0:
        errors.append("expected_solve_sec가 0 이하임")

    if "used_mistake_title" in question:
        errors.append(
            "used_mistake_title 필드가 있음. "
            "DB 연결을 위해 hint_mistake_code 사용을 권장"
        )

    if "hint_mistake_code" in question:
        hint_mistake_code = question.get("hint_mistake_code")

        if hint_mistake_code is not None and not _is_non_empty_string(hint_mistake_code):
            errors.append("hint_mistake_code가 null 또는 비어 있지 않은 문자열이어야 함")
    elif strict:
        errors.append("hint_mistake_code가 없음")

    if "source_refs" in question:
        source_refs = question.get("source_refs")

        if not isinstance(source_refs, list):
            errors.append("source_refs가 리스트가 아님")
        else:
            for index, ref in enumerate(source_refs):
                if not isinstance(ref, dict):
                    errors.append(f"source_refs[{index}]가 객체가 아님")
                    continue

                if not _is_non_empty_string(ref.get("chunk_id")):
                    errors.append(f"source_refs[{index}].chunk_id가 비어 있음")

                if not _is_non_empty_string(ref.get("source_file")):
                    errors.append(f"source_refs[{index}].source_file이 비어 있음")

                if not isinstance(ref.get("page"), int):
                    errors.append(f"source_refs[{index}].page가 정수가 아님")
    elif strict:
        errors.append("source_refs가 없음")

    if "content_hash" in question:
        content_hash = question.get("content_hash")

        if not isinstance(content_hash, str) or len(content_hash) != 64:
            errors.append("content_hash가 64자 SHA-256 문자열 형식이 아님")
    elif strict:
        errors.append("content_hash가 없음")

    if "difficulty_source" in question:
        if question.get("difficulty_source") not in ALLOWED_DIFFICULTY_SOURCES:
            errors.append(
                "difficulty_source가 허용 값이 아님 "
                f"허용값: {ALLOWED_DIFFICULTY_SOURCES}"
            )
    elif strict:
        errors.append("difficulty_source가 없음")

    if "source_type" in question:
        if question.get("source_type") not in ALLOWED_SOURCE_TYPES:
            errors.append(
                "source_type이 허용 값이 아님 "
                f"허용값: {ALLOWED_SOURCE_TYPES}"
            )
    elif strict:
        errors.append("source_type이 없음")

    if "review_status" in question:
        if question.get("review_status") not in ALLOWED_REVIEW_STATUSES:
            errors.append(
                "review_status가 허용 값이 아님 "
                f"허용값: {ALLOWED_REVIEW_STATUSES}"
            )
    elif strict:
        errors.append("review_status가 없음")

    eligibility_fields = [
        "is_daily_quiz_eligible",
        "is_mock_eligible",
        "is_concept_practice_eligible",
    ]

    for field in eligibility_fields:
        if field in question:
            if not isinstance(question.get(field), bool):
                errors.append(f"{field}가 boolean이 아님")
        elif strict:
            errors.append(f"{field}가 없음")

    if "is_active" in question:
        if not isinstance(question.get("is_active"), bool):
            errors.append("is_active가 boolean이 아님")
    elif strict:
        errors.append("is_active가 없음")

    if "version_no" in question:
        version_no = question.get("version_no")

        if not isinstance(version_no, int):
            errors.append("version_no가 정수가 아님")
        elif version_no <= 0:
            errors.append("version_no가 1 이상이어야 함")
    elif strict:
        errors.append("version_no가 없음")

    return errors


def validate_questions(questions: list[dict], *, strict: bool = False) -> list[str]:
    errors = []

    if not isinstance(questions, list):
        return ["questions가 리스트가 아님"]

    seen_hashes = {}
    seen_question_texts = {}

    for index, question in enumerate(questions):
        question_errors = validate_question(question, strict=strict)

        for error in question_errors:
            errors.append(f"[question index {index}] {error}")

        content_hash = question.get("content_hash")

        if content_hash:
            if content_hash in seen_hashes:
                errors.append(
                    f"[question index {index}] content_hash 중복: "
                    f"이전 index {seen_hashes[content_hash]}"
                )
            else:
                seen_hashes[content_hash] = index

        question_text = question.get("question_text")

        if isinstance(question_text, str) and question_text.strip():
            normalized_text = question_text.strip()

            if normalized_text in seen_question_texts:
                errors.append(
                    f"[question index {index}] question_text 중복: "
                    f"이전 index {seen_question_texts[normalized_text]}"
                )
            else:
                seen_question_texts[normalized_text] = index

    return errors