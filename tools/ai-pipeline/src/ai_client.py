import json
import os
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
) -> dict[str, Any]:
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

    return json.loads(response.output_text)


def _call_gemini_json(
    *,
    system_prompt: str,
    user_prompt: str,
    schema: dict[str, Any],
    model: str | None,
) -> dict[str, Any]:
    selected_model = _require_model(model or DEFAULT_MODEL, "GEMINI_MODEL")

    response = _get_gemini_client().models.generate_content(
        model=selected_model,
        contents=user_prompt,
        config={
            "system_instruction": system_prompt,
            "response_format": {
                "text": {
                    "mime_type": "application/json",
                    "schema": schema,
                }
            },
        },
    )

    if not response.text:
        raise RuntimeError("Gemini 응답이 비어 있습니다.")

    return json.loads(response.text)


def call_ai_json(
    *,
    system_prompt: str,
    user_prompt: str,
    schema_name: str,
    schema: dict[str, Any],
    model: str | None = None,
    reasoning_effort: str = "low",
) -> dict[str, Any]:
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