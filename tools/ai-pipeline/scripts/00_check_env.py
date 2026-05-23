import _bootstrap

from src.ai_client import AI_PROVIDER, check_ai_connection

message = check_ai_connection()

print(f"{AI_PROVIDER} API 연결 성공")
print(message)