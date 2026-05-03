import _bootstrap

from src.openai_client import client, DEFAULT_MODEL

if not DEFAULT_MODEL:
    raise RuntimeError(".env에 OPENAI_MODEL이 없습니다.")

response = client.responses.create(
    model=DEFAULT_MODEL,
    input="한국어로 'OpenAI API 연결 성공'이라고만 답해줘.",
)

print(response.output_text)