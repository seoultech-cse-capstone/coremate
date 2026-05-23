import _bootstrap

from src.ai_client import AI_PROVIDER, list_ai_models

print(f"provider: {AI_PROVIDER}")

for model_id in list_ai_models():
    print(model_id)