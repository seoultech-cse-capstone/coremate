import math
import re


def normalize(text: str) -> str:
    return text.lower().replace(" ", "")


# 검색어를 토큰으로 쪼갤 때 걸러낼 기능어(변별력이 없는 단어)
_STOPWORDS = {
    # 영어 기능어
    "a", "an", "the", "of", "for", "and", "or", "to", "in", "on", "with",
    "by", "is", "are", "be", "as", "at", "from", "into", "that", "this",
    # 한국어 일반어
    "및", "그리고", "또는", "등", "관련", "대한", "위한", "통한", "따른",
}

# 라틴/숫자 덩어리 또는 한글 덩어리를 각각 하나의 토큰으로 추출
# 예) "Kruskal's Algorithm" -> ["kruskal", "s", "algorithm"], "cap정리" -> ["cap", "정리"]
_TOKEN_RE = re.compile(r"[a-z0-9]+|[가-힣]+")


def _tokenize(text: str) -> list[str]:
    tokens = _TOKEN_RE.findall(text.lower())
    return [t for t in tokens if len(t) >= 2 and t not in _STOPWORDS]


def retrieve_chunks_by_keywords(
    *,
    chunks: list[dict],
    concept_name: str,
    concept_name_en: str,
    aliases: list[str],
    top_k: int = 5,
) -> list[dict]:
    terms = [t for t in [concept_name, concept_name_en, *aliases] if t]

    # 1) 검색어에서 뽑은 고유 토큰. 다단어 이름도 단어 단위로 매칭된다.
    #    예) "Huffman Coding" -> {"huffman", "coding"} → 소스에 "huffman"만 있어도 매칭
    query_tokens = set()
    for term in terms:
        query_tokens.update(_tokenize(term))

    # 2) 공백 제거한 전체 구문. 정확히 일치하면 강한 신호로 가산(기존 동작 보존)
    phrase_forms = {normalize(term) for term in terms if normalize(term)}

    # 청크 텍스트를 소문자로 한 번만 전처리. 토큰은 부분문자열로 매칭(한국어 조사 굴절 흡수)
    chunk_texts = [chunk["text"].lower() for chunk in chunks]
    norm_texts = [text.replace(" ", "") for text in chunk_texts]
    n_docs = len(chunks) or 1

    # 3) 토큰별 문서빈도(df) → idf. 희소 토큰일수록 변별력이 높다.
    #    "kruskal"(28개 청크)은 높게, "algorithm"(수천 청크)은 0에 가깝게 가중.
    idf = {}
    for token in query_tokens:
        df = sum(1 for text in chunk_texts if token in text)
        idf[token] = math.log(1 + n_docs / (1 + df))

    scored = []
    for idx, chunk in enumerate(chunks):
        text = chunk_texts[idx]
        score = 0.0

        # 토큰 매칭: 변별력(idf) 가중 합산
        for token in query_tokens:
            if token in text:
                score += idf[token]

        # 전체 구문 정확 일치 보너스(기존 substring 매칭을 부분집합으로 포함)
        norm_text = norm_texts[idx]
        for phrase in phrase_forms:
            if phrase in norm_text:
                score += 10.0

        if score > 0:
            scored.append((score, chunk))

    scored.sort(key=lambda x: x[0], reverse=True)

    return [chunk for _, chunk in scored[:top_k]]
