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
→ Python + PyMuPDF + OpenAI API 기반 내부 콘텐츠 생성 도구
→ PDF 텍스트 추출, 개념 생성, 공통 오해 생성, 문제 생성, 문제 검수

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

## GitHub에 올라오지 않는 파일

아래 파일들은 보안, 저작권, 용량, 중간 산출물 문제 때문에 GitHub에 올리지 않는다.

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
→ OpenAI API Key 포함
→ 직접 생성해야 함

.venv
→ 개인 로컬 Python 가상환경
→ 직접 생성해야 함

data/raw
→ 전공서적 PDF 원본 자료
→ GitHub 업로드 금지
→ 직접 폴더를 만들고 PDF를 넣어야 함

data/extracted
→ PyMuPDF로 PDF에서 추출한 chunks.json
→ 원문 텍스트 일부가 포함될 수 있음
→ 직접 실행해서 생성해야 함

outputs
→ AI 생성 결과물
→ taxonomy, concept_cards, questions, final_report 등
→ 직접 실행해서 생성해야 함
```

---

## 클론 후 필수 세팅

처음 클론한 팀원은 아래 순서대로 세팅한다.

```bash
git clone https://github.com/yongzooda/coremate.git
cd tools/ai-pipeline
```

가상환경 생성 및 실행.

```bash
python3 -m venv .venv
source .venv/bin/activate
```

패키지 설치.

```bash
pip install -r requirements.txt
```

환경변수 파일 생성.

```bash
cp .env.example .env
```

`.env`에 실제 OpenAI API Key 입력.

```txt
OPENAI_API_KEY=your_api_key
OPENAI_MODEL=gpt-5.4-mini
OPENAI_REVIEW_MODEL=gpt-5.4-mini
OPENAI_EMBEDDING_MODEL=text-embedding-3-small
```

설치 확인.

```bash
python -c "import openai; print('openai import 성공')"
python -c "import fitz; print('PyMuPDF import 성공')"
```

`fitz` import가 성공하면 PyMuPDF가 정상 설치된 것이다.

---

## PDF 원본 자료 준비

GitHub에는 `data/raw/`가 올라오지 않는다.

따라서 파이프라인 실행 전에 직접 폴더를 만들고 PDF를 넣어야 한다.

자료구조 테스트 기준:

```bash
mkdir -p data/raw/DATA_STRUCTURE
```

아래 위치에 자료구조 PDF를 넣는다.

```txt
tools/ai-pipeline/data/raw/DATA_STRUCTURE/open_data_structures.pdf
```

파일명은 예시다.

현재 스크립트는 `data/raw/{SUBJECT_CODE}/` 안의 `.pdf` 파일을 읽는다.

```txt
data/raw/DATA_STRUCTURE/
  open_data_structures.pdf
```

주의 사항:

```txt
PDF 원본은 GitHub에 올리지 않는다.
팀 내부 공유 드라이브나 개인 로컬에서만 관리한다.
PDF가 없으면 01_extract_pdf.py 실행 시 오류가 난다.
```

---
