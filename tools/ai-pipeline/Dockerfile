# CoreMate AI 서버(FastAPI). 배포 환경에서는 백엔드와 같은 ECS 태스크 안에서 사이드카로 돈다.
# 호출자는 같은 태스크의 백엔드뿐이라(localhost:8000) 127.0.0.1 에만 바인딩해 외부에 열지 않는다.
FROM python:3.14-slim

# server/core/paths.py 가 ai-pipeline 루트의 두 단계 위(레포 루트)를 기준으로 경로를 계산한다.
# /app 처럼 루트에 가까운 곳에 두면 parents[1] 이 없어 import 단계에서 IndexError 로 죽으므로
# 레포와 같은 깊이로 둔다.
WORKDIR /srv/coremate/tools/ai-pipeline

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY . .

EXPOSE 8000
CMD ["uvicorn", "server.main:app", "--host", "127.0.0.1", "--port", "8000"]
