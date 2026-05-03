def normalize(text: str) -> str:
    return text.lower().replace(" ", "")


def retrieve_chunks_by_keywords(
    *,
    chunks: list[dict],
    concept_name: str,
    concept_name_en: str,
    aliases: list[str],
    top_k: int = 5,
) -> list[dict]:
    terms = [concept_name, concept_name_en] + aliases
    normalized_terms = [normalize(term) for term in terms if term]

    scored = []

    for chunk in chunks:
        text = normalize(chunk["text"])
        score = 0

        for term in normalized_terms:
            if term in text:
                score += 10

        if score > 0:
            scored.append((score, chunk))

    scored.sort(key=lambda x: x[0], reverse=True)

    return [chunk for _, chunk in scored[:top_k]]