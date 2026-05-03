# CoreMate

CS 전공지식 반복 학습 앱.

문제 풀이 기록을 기반으로 AI 약점 분석표를 제공하고, 아바타·XP·코인 보상으로 꾸준한 학습을 유도한다.

---

## 구조

```txt
coremate/
  README.md
  .gitignore
  .env.example

  apps/
    backend/              # Spring Boot 백엔드 서버
    mobile/               # React Native + Expo 모바일 앱

  tools/
    ai-pipeline/          # Python 기반 CS 콘텐츠/문제 생성 파이프라인

  docs/
    product/              # 기획서, MVP, 화면 흐름
    api/                  # API 명세
    db/                   # DB 명세, ERD
    ai/                   # AI 파이프라인 설명, 프롬프트 규칙
    deploy/               # 배포 방법 정리

  infra/
    docker-compose.local.yml
    aws/                  # ECS, RDS, S3, CloudWatch 관련 정리

  scripts/
    seed/                 # 초기 데이터 insert SQL
    local/                # 로컬 실행/DB 초기화 스크립트

  .github/
    workflows/            # GitHub Actions CI/CD
    ISSUE_TEMPLATE/
    pull_request_template.md
```

---

## 폴더 역할

```txt
apps/backend
→ Java 21 + Spring Boot 서버
→ 로그인, 문제집, 풀이 기록, 약점 분석, 보상, 아바타, 친구, 상점 API

apps/mobile
→ React Native + Expo 앱
→ 시작화면, 홈, 문제집, 약점 분석, 마이페이지/아바타, 친구, 상점 화면

tools/ai-pipeline
→ Python + OpenAI API 기반 내부 콘텐츠 생성 도구
→ 개념 생성, 공통 오해 생성, 문제 생성, 문제 검수

docs
→ 팀원들이 같이 보는 문서
→ 기획서, DB 명세, API 명세, 화면 흐름, AI 파이프라인 정리

infra
→ 배포/로컬 실행 관련 설정
→ Docker, AWS ECS/Fargate, RDS, S3 등

scripts
→ DB 초기 데이터, 로컬 개발용 스크립트

.github
→ PR 템플릿, 이슈 템플릿, 자동 테스트/배포 설정
```

---

## AI 파이프라인 실행

```bash
cd tools/ai-pipeline

python3 -m venv .venv
source .venv/bin/activate

pip install -r requirements.txt

cp .env.example .env
```

`.env`에 실제 값 입력.

```txt
OPENAI_API_KEY=your_api_key
OPENAI_MODEL=gpt-5.4-mini
OPENAI_REVIEW_MODEL=gpt-5.4-mini
OPENAI_EMBEDDING_MODEL=text-embedding-3-small
```

---

## GitHub에 올리지 않는 것

```txt
.venv/
.env
__pycache__/
data/raw/
data/extracted/
outputs/
.DS_Store
```

```txt
.env
→ API Key 포함

.venv
→ 개인 로컬 가상환경

data/raw
→ 원본 자료

data/extracted
→ 추출 중간 결과

outputs
→ AI 생성 결과물
```

---

## 클론 후 세팅

```bash
git clone https://github.com/yongzooda/coremate.git
cd coremate/tools/ai-pipeline

python3 -m venv .venv
source .venv/bin/activate

pip install -r requirements.txt

cp .env.example .env
```

`.env`에 API Key 입력 후 사용.

---
