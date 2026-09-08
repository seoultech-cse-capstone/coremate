import json
import os
import time
from typing import Any

from dotenv import load_dotenv
from openai import OpenAI

load_dotenv()

AI_PROVIDER = os.getenv("AI_PROVIDER", "openai").strip().lower()

OPENAI_API_KEY = os.getenv("OPENAI_API_KEY")
OPENAI_MODEL = os.getenv("OPENAI_MODEL")
OPENAI_REVIEW_MODEL = os.getenv("OPENAI_REVIEW_MODEL")
OPENAI_EMBEDDING_MODEL = os.getenv("OPENAI_EMBEDDING_MODEL", "text-embedding-3-small")

GEMINI_API_KEY = os.getenv("GEMINI_API_KEY")
GEMINI_MODEL = os.getenv("GEMINI_MODEL")
GEMINI_REVIEW_MODEL = os.getenv("GEMINI_REVIEW_MODEL")
GEMINI_EMBEDDING_MODEL = os.getenv("GEMINI_EMBEDDING_MODEL", "gemini-embedding-001")

if AI_PROVIDER == "openai":
    DEFAULT_MODEL = OPENAI_MODEL
    REVIEW_MODEL = OPENAI_REVIEW_MODEL or OPENAI_MODEL
    EMBEDDING_MODEL = OPENAI_EMBEDDING_MODEL
elif AI_PROVIDER == "gemini":
    DEFAULT_MODEL = GEMINI_MODEL
    REVIEW_MODEL = GEMINI_REVIEW_MODEL or GEMINI_MODEL
    EMBEDDING_MODEL = GEMINI_EMBEDDING_MODEL
else:
    raise RuntimeError("AI_PROVIDER는 openai 또는 gemini여야 합니다.")

MAX_RETRIES = 4
RETRY_BASE_SLEEP_SEC = 2

_openai_client: OpenAI | None = None
_gemini_client: Any | None = None


def _get_openai_client() -> OpenAI:
    global _openai_client

    if not OPENAI_API_KEY:
        raise RuntimeError("OPENAI_API_KEY가 .env에 없습니다.")

    if _openai_client is None:
        _openai_client = OpenAI(api_key=OPENAI_API_KEY)

    return _openai_client


def _get_gemini_client() -> Any:
    global _gemini_client

    if not GEMINI_API_KEY:
        raise RuntimeError("GEMINI_API_KEY가 .env에 없습니다.")

    if _gemini_client is None:
        from google import genai

        _gemini_client = genai.Client(api_key=GEMINI_API_KEY)

    return _gemini_client


def _require_model(model: str | None, env_name: str) -> str:
    if not model:
        raise RuntimeError(f"{env_name}이 .env에 없습니다.")

    return model


def _call_openai_json(
    *,
    system_prompt: str,
    user_prompt: str,
    schema_name: str,
    schema: dict[str, Any],
    model: str | None,
    reasoning_effort: str,
) -> tuple[dict[str, Any], dict[str, Any]]:
    selected_model = _require_model(model or DEFAULT_MODEL, "OPENAI_MODEL")

    response = _get_openai_client().responses.create(
        model=selected_model,
        reasoning={"effort": reasoning_effort},
        input=[
            {"role": "system", "content": system_prompt},
            {"role": "user", "content": user_prompt},
        ],
        text={
            "format": {
                "type": "json_schema",
                "name": schema_name,
                "strict": True,
                "schema": schema,
            }
        },
    )

    usage = getattr(response, "usage", None)
    input_details = getattr(usage, "input_tokens_details", None)
    output_details = getattr(usage, "output_tokens_details", None)
    meta = {
        "model": getattr(response, "model", None) or selected_model,
        "input_tokens": getattr(usage, "input_tokens", None),
        "cached_tokens": getattr(input_details, "cached_tokens", None),
        "output_tokens": getattr(usage, "output_tokens", None),
        "reasoning_tokens": getattr(output_details, "reasoning_tokens", None),
    }

    return json.loads(response.output_text), meta


def _call_gemini_json(
    *,
    system_prompt: str,
    user_prompt: str,
    schema: dict[str, Any],
    model: str | None,
) -> tuple[dict[str, Any], dict[str, Any]]:
    selected_model = _require_model(model or DEFAULT_MODEL, "GEMINI_MODEL")

    response = _get_gemini_client().models.generate_content(
        model=selected_model,
        contents=user_prompt,
        config={
            "system_instruction": system_prompt,
            "response_mime_type": "application/json",
            "response_json_schema": schema,
        },
    )

    if not response.text:
        raise RuntimeError("Gemini 응답이 비어 있습니다.")

    usage = getattr(response, "usage_metadata", None)
    meta = {
        "model": selected_model,
        "input_tokens": getattr(usage, "prompt_token_count", None),
        "cached_tokens": getattr(usage, "cached_content_token_count", None),
        "output_tokens": getattr(usage, "candidates_token_count", None),
        "reasoning_tokens": getattr(usage, "thoughts_token_count", None),
    }

    return json.loads(response.text), meta


def call_ai_json(
    *,
    system_prompt: str,
    user_prompt: str,
    schema_name: str,
    schema: dict[str, Any],
    model: str | None = None,
    reasoning_effort: str = "low",
) -> dict[str, Any]:
    """호출 결과 JSON만 필요한 배치 스크립트용. 호출 메타데이터는 버린다."""
    payload, _ = call_ai_json_with_meta(
        system_prompt=system_prompt,
        user_prompt=user_prompt,
        schema_name=schema_name,
        schema=schema,
        model=model,
        reasoning_effort=reasoning_effort,
    )
    return payload


def call_ai_json_with_meta(
    *,
    system_prompt: str,
    user_prompt: str,
    schema_name: str,
    schema: dict[str, Any],
    model: str | None = None,
    reasoning_effort: str = "low",
) -> tuple[dict[str, Any], dict[str, Any]]:
    """결과 JSON과 함께 실제 사용된 모델명·토큰 사용량을 돌려준다.

    런타임 호출(약점분석)은 어느 모델로 얼마를 썼는지 로그에 남겨야 해서 이쪽을 쓴다.
    meta 의 토큰 값은 프로바이더가 안 주면 None 이다.
    """
    # 수백~수천 회를 도는 루프라 일시적인 429/5xx 한 번에 실행 전체가 죽지 않도록 재시도한다.
    last_error: Exception | None = None

    for attempt in range(MAX_RETRIES):
        try:
            if AI_PROVIDER == "openai":
                return _call_openai_json(
                    system_prompt=system_prompt,
                    user_prompt=user_prompt,
                    schema_name=schema_name,
                    schema=schema,
                    model=model,
                    reasoning_effort=reasoning_effort,
                )

            if AI_PROVIDER == "gemini":
                return _call_gemini_json(
                    system_prompt=system_prompt,
                    user_prompt=user_prompt,
                    schema=schema,
                    model=model,
                )

            raise RuntimeError("AI_PROVIDER는 openai 또는 gemini여야 합니다.")
        except (RuntimeError, json.JSONDecodeError) as error:
            raise
        except Exception as error:  # API 호출 실패(네트워크/레이트리밋/일시 오류)
            last_error = error
            if attempt == MAX_RETRIES - 1:
                break
            time.sleep(RETRY_BASE_SLEEP_SEC * (2 ** attempt))

    raise RuntimeError(f"AI 호출이 {MAX_RETRIES}회 모두 실패했습니다: {last_error}") from last_error


def create_embedding(text: str) -> list[float]:
    if AI_PROVIDER == "openai":
        response = _get_openai_client().embeddings.create(
            model=EMBEDDING_MODEL,
            input=text,
        )

        return response.data[0].embedding

    if AI_PROVIDER == "gemini":
        response = _get_gemini_client().models.embed_content(
            model=EMBEDDING_MODEL,
            contents=text,
        )

        embedding = response.embeddings[0]
        values = getattr(embedding, "values", embedding)

        return list(values)

    raise RuntimeError("AI_PROVIDER는 openai 또는 gemini여야 합니다.")


def check_ai_connection() -> str:
    selected_model = _require_model(DEFAULT_MODEL, f"{AI_PROVIDER.upper()}_MODEL")

    if AI_PROVIDER == "openai":
        response = _get_openai_client().responses.create(
            model=selected_model,
            input="한국어로 'AI API 연결 성공'이라고만 답해줘.",
        )

        return response.output_text

    if AI_PROVIDER == "gemini":
        response = _get_gemini_client().models.generate_content(
            model=selected_model,
            contents="한국어로 'AI API 연결 성공'이라고만 답해줘.",
        )

        return response.text or ""

    raise RuntimeError("AI_PROVIDER는 openai 또는 gemini여야 합니다.")


def list_ai_models() -> list[str]:
    if AI_PROVIDER == "openai":
        models = _get_openai_client().models.list()
        return sorted(model.id for model in models.data)

    if AI_PROVIDER == "gemini":
        models = _get_gemini_client().models.list()
        model_names = []

        for model in models:
            name = getattr(model, "name", None) or getattr(model, "id", None) or str(model)
            model_names.append(name.replace("models/", ""))

        return sorted(model_names)

    raise RuntimeError("AI_PROVIDER는 openai 또는 gemini여야 합니다.")