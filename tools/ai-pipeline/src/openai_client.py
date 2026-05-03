import json
import os
from typing import Any

from dotenv import load_dotenv
from openai import OpenAI

load_dotenv()

api_key = os.getenv("OPENAI_API_KEY")

if not api_key:
    raise RuntimeError("OPENAI_API_KEY가 .env에 없습니다.")

client = OpenAI(api_key=api_key)

DEFAULT_MODEL = os.getenv("OPENAI_MODEL")
REVIEW_MODEL = os.getenv("OPENAI_REVIEW_MODEL")
EMBEDDING_MODEL = os.getenv("OPENAI_EMBEDDING_MODEL", "text-embedding-3-small")


def call_openai_json(
    *,
    system_prompt: str,
    user_prompt: str,
    schema_name: str,
    schema: dict[str, Any],
    model: str | None = None,
    reasoning_effort: str = "low",
) -> dict[str, Any]:
    selected_model = model or DEFAULT_MODEL

    if not selected_model:
        raise RuntimeError("OPENAI_MODEL이 .env에 없습니다.")

    response = client.responses.create(
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


def create_embedding(text: str) -> list[float]:
    response = client.embeddings.create(
        model=EMBEDDING_MODEL,
        input=text,
    )

    return response.data[0].embedding