# CoreMate

CS 전공지식 반복 학습 앱.

문제 풀이 기록을 기반으로 AI 약점 분석표를 제공하고, 아바타·XP·코인 보상으로 꾸준한 학습을 유도한다.

---

## 프로젝트 구조

```txt
coremate/
  apps/
    backend/              # Spring Boot 백엔드
    mobile/               # React Native + Expo 모바일 앱

  tools/
    ai-pipeline/          # AI 콘텐츠 생성 파이프라인

  docs/                   # 기획, API, DB, AI, 배포 문서
  infra/                  # Docker, AWS 등 인프라 설정
  scripts/                # DB seed, 로컬 실행 스크립트
  .github/                # GitHub Actions, PR/Issue 템플릿
```

## 저장소에 없는 파일

`apps/mobile/google-services.json`은 gitignore 대상이라 저장소에 없다. 담당자에게 받아 `apps/mobile/` 루트에 두어야 안드로이드 네이티브 빌드와 FCM 푸시가 동작한다.

---

## 백엔드를 새로 받았을 때 (2026-09-16 약점 분석표 개편)

백엔드를 `docker compose -f infra/docker-compose.local.yml up -d --build` 로 다시 띄우면 새 컬럼은 자동으로 생긴다(`ddl-auto: update`). 한 가지만 직접 돌리면 된다.

```bash
docker exec -i coremate-mysql sh -c 'MYSQL_PWD="$MYSQL_PASSWORD" mysql -ucoremate coremate' \
  < scripts/db/backfill_mistake_code.sql
```

`common_mistake.mistake_code` 를 채우는 스크립트다. 오개념 코드는 AI 파이프라인 산출물(`tools/ai-pipeline/outputs/`, gitignore 대상)에만 있어서 DB 에는 비어 있다. 이걸 돌리지 않으면 약점 분석표의 "반복된 오개념"이 제목 없이 횟수만 나온다. 여러 번 돌려도 결과는 같다.

---

## 모바일 앱 실행

앱을 여는 방법이 두 가지고, **플랫폼마다 쓸 수 있는 방법이 다르다.**

| | iOS | 안드로이드 |
|---|---|---|
| Expo Go | 앱은 뜬다. 소셜 로그인·푸시는 동작하지 않는다 | **앱이 뜨지 않는다** |
| 개발 빌드 | 전부 동작 | 전부 동작 |

### iOS — Expo Go

```bash
cd apps/mobile && npx expo start --ios
```

### 안드로이드 — 개발 빌드가 필요하다

**Expo Go 로 열면 스플래시에서 멈춘다.** 앱 버그가 아니다. `expo-notifications` 는 SDK 53 부터 Expo Go 안드로이드에서 푸시 기능이 제거됐는데, 이 패키지는 import 되는 순간 내부 모듈이 푸시 토큰 리스너를 등록하고 그 지점에서 예외를 던진다. iOS Expo Go 는 같은 자리에서 경고만 남기고 넘어가 문제가 드러나지 않는다. 라이브러리 동작이라 앱 코드로는 우회할 수 없다.

안드로이드는 개발 빌드로 연다. 우리 네이티브 모듈이 전부 포함된 진짜 CoreMate 앱이 설치된다.

```bash
export ANDROID_HOME=$HOME/Library/Android/sdk
cd apps/mobile
npx expo prebuild -p android --clean
npx expo run:android
```

- `ANDROID_HOME` 이 없으면 `SDK location not found` 로 실패한다. `~/.zshrc` 에 넣어 두면 편하다.
- `prebuild` 는 `app.config.js` 의 설정(카카오 SDK maven 저장소 등)을 네이티브 프로젝트에 주입한다. **이걸 건너뛰면 `com.kakao.sdk:v2-*` 를 찾지 못해 빌드가 깨진다.** `android/` 는 gitignore 대상이라 재생성해도 안전하다.
- 첫 빌드는 10분 안팎. 이후에는 Metro 만 켜면 되고(`npx expo start`), **네이티브 의존성이 바뀔 때만** 다시 빌드한다.

에뮬레이터는 Android Studio 에서 띄워 두면 `run:android` 가 알아서 잡는다. 실기기는 USB 디버깅을 켜고 연결한 뒤 `npx expo run:android --device` 를 쓴다. 빌드 결과 APK 는 `apps/mobile/android/app/build/outputs/apk/debug/app-debug.apk` 에 남으므로 `adb install -r` 로 직접 넣어도 된다.

⚠️ **실기기로 볼 때는 폰과 PC 가 같은 와이파이에 있어야 한다.** 앱이 `apps/mobile/.env` 의 `EXPO_PUBLIC_API_BASE_URL`(PC 의 LAN IP)로 백엔드를 찾아가기 때문이다. 이 값은 `scripts/local/start-backend.sh` 가 갱신하며, **Metro 가 이미 떠 있으면 완전히 껐다 켜야** 새 주소가 반영된다.

---

# AI 콘텐츠 파이프라인 빠른 실행 가이드

`tools/ai-pipeline`은 전공서적 PDF를 기반으로 CoreMate 서비스에 사용할 CS 학습 콘텐츠 초안을 생성하는 내부 도구다.

생성 결과는 바로 DB에 저장하지 않는다.  
사람이 JSON 파일과 `final_report.md`를 검수한 뒤 최종 승인된 데이터만 DB에 반영한다.

---

## 생성되는 파일

한 과목을 실행하면 아래 파일들이 생성된다.

```txt
outputs/{SUBJECT_CODE}/taxonomy.json
outputs/{SUBJECT_CODE}/concept_cards.json
outputs/{SUBJECT_CODE}/concept_relations.json
outputs/{SUBJECT_CODE}/common_mistakes.json
outputs/{SUBJECT_CODE}/questions.json
outputs/{SUBJECT_CODE}/question_reviews.json
outputs/{SUBJECT_CODE}/final_report.md
```

PDF 추출 결과는 아래에 생성된다.

```txt
data/extracted/{SUBJECT_CODE}/chunks.json
```

---

## 지원 AI Provider

`.env`의 `AI_PROVIDER` 값으로 사용할 AI를 선택한다.

```txt
AI_PROVIDER=openai
AI_PROVIDER=gemini
```

| 값 | 의미 |
| --- | --- |
| `openai` | OpenAI GPT 모델 사용 |
| `gemini` | Google Gemini 모델 사용 |

AI Provider를 바꿔도 실행 명령어와 결과 파일 구조는 동일하다.

---

## 과목 코드

| 과목 | SUBJECT_CODE |
| --- | --- |
| 자료구조 | `DATA_STRUCTURE` |
| 알고리즘 | `ALGORITHM` |
| 운영체제 | `OPERATING_SYSTEM` |
| 데이터베이스 | `DATABASE` |
| 컴퓨터구조 | `COMPUTER_ARCHITECTURE` |
| 네트워크 | `NETWORK` |

---

# 처음 세팅

## 1. 저장소 클론

```bash
git clone https://github.com/yongzooda/coremate.git
```

GitHub 저장소를 내 컴퓨터로 받는다.

```bash
cd coremate/tools/ai-pipeline
```

AI 파이프라인 폴더로 이동한다.

---

## 2. 가상환경 생성

```bash
python3 -m venv .venv
```

Python 패키지를 프로젝트별로 분리하기 위한 가상환경을 만든다.

```bash
source .venv/bin/activate
```

가상환경을 활성화한다.

---

## 3. 패키지 설치

```bash
pip install -r requirements.txt
```

AI 파이프라인 실행에 필요한 패키지를 설치한다.

설치 확인:

```bash
python -c "import openai; print('openai import 성공')"
python -c "from google import genai; print('google-genai import 성공')"
python -c "import fitz; print('PyMuPDF import 성공')"
```

---

## 4. .env 파일 생성

```bash
cp -n .env.example .env
```

개인 API 키를 넣을 `.env` 파일을 만든다.

`.env` 예시:

```txt
AI_PROVIDER=openai

OPENAI_API_KEY=본인_OPENAI_API_KEY
OPENAI_MODEL=gpt-5.4-mini
OPENAI_REVIEW_MODEL=gpt-5.4-mini
OPENAI_EMBEDDING_MODEL=text-embedding-3-small

GEMINI_API_KEY=본인_GEMINI_API_KEY
GEMINI_MODEL=gemini-3.5-flash
GEMINI_REVIEW_MODEL=gemini-3.5-flash
GEMINI_EMBEDDING_MODEL=gemini-embedding-001
```

OpenAI로 실행하려면:

```txt
AI_PROVIDER=openai
```

Gemini로 실행하려면:

```txt
AI_PROVIDER=gemini
```

`.env`는 API 키가 들어가는 개인 파일이므로 GitHub에 올리면 안 된다.

---

## 5. API 연결 확인

```bash
python scripts/00_check_env.py
```

현재 `.env`에 설정된 AI Provider와 모델로 API 연결이 되는지 확인한다.

```bash
python scripts/00_list_models.py
```

현재 AI Provider에서 사용 가능한 모델 목록을 확인한다.

---

# PDF 준비

## 1. 과목별 PDF 폴더 생성

```bash
mkdir -p data/raw/{SUBJECT_CODE}
```

과목별 PDF를 넣을 폴더를 만든다.

운영체제 예시:

```bash
mkdir -p data/raw/OPERATING_SYSTEM
```

---

## 2. PDF 넣기

과목별 PDF는 아래 위치에 넣는다.

```txt
data/raw/{SUBJECT_CODE}/
```

운영체제 예시:

```txt
data/raw/OPERATING_SYSTEM/
  operating_systems_and_middleware.pdf
  xv6_riscv_book_rev5.pdf
  intro_computer_architecture_and_operating_systems.pdf
```

기본 원칙은 과목당 PDF 3개다.

```txt
메인 전공서 1개
+ 보조 전공서 1개
+ 구현/응용/심화 자료 1개
```

PDF 원문은 저작권과 용량 문제 때문에 GitHub에 올리지 않는다.

---

# 파이프라인 실행

아래 명령어에서 `{SUBJECT_CODE}`를 실제 과목 코드로 바꿔 실행한다.

예시:

```txt
{SUBJECT_CODE} → OPERATING_SYSTEM
```

---

## 1. PDF 텍스트 추출

```bash
python scripts/01_extract_pdf.py {SUBJECT_CODE}
```

`data/raw/{SUBJECT_CODE}/` 안의 PDF를 읽고 텍스트를 추출한다.

결과:

```txt
data/extracted/{SUBJECT_CODE}/chunks.json
```

---

## 2. taxonomy 생성

```bash
python scripts/02_build_taxonomy.py {SUBJECT_CODE}
```

과목의 학습 구조를 생성한다.

구조:

```txt
subject
→ category_lv1
→ category_lv2
→ concept
```

결과:

```txt
outputs/{SUBJECT_CODE}/taxonomy.json
```

경고가 있으면 추가 생성:

```txt
outputs/{SUBJECT_CODE}/taxonomy_warnings.txt
```

---

## 3. taxonomy 검수

```bash
code outputs/{SUBJECT_CODE}/taxonomy.json
```

생성된 taxonomy를 사람이 직접 검수한다.

이 단계에서는 반드시 멈추고 확인한다.  
taxonomy가 잘못되면 이후 생성 결과도 같이 흔들린다.

---

## 4. concept card 생성

```bash
python scripts/03_generate_concept_cards.py {SUBJECT_CODE}
```

각 concept의 설명, 핵심 키워드, 근거 source를 생성한다.

결과:

```txt
outputs/{SUBJECT_CODE}/concept_cards.json
```

---

## 5. concept relation 생성

```bash
python scripts/04_generate_concept_relations.py {SUBJECT_CODE}
```

개념 간 관계를 생성한다.

관계 유형:

```txt
related
confusing
prerequisite
```

결과:

```txt
outputs/{SUBJECT_CODE}/concept_relations.json
```

---

## 6. common mistake 생성

```bash
python scripts/05_generate_common_mistakes.py {SUBJECT_CODE}
```

개념별 공통 오해와 교정 설명을 생성한다.

결과:

```txt
outputs/{SUBJECT_CODE}/common_mistakes.json
```

---

## 7. question 생성

```bash
python scripts/06_generate_questions.py {SUBJECT_CODE}
```

concept card, concept relation, common mistake를 기반으로 4지선다 문제를 생성한다.

결과:

```txt
outputs/{SUBJECT_CODE}/questions.json
```

경고가 있으면 추가 생성:

```txt
outputs/{SUBJECT_CODE}/question_validation_errors.txt
```

---

## 8. question review 생성

```bash
python scripts/07_review_questions.py {SUBJECT_CODE}
```

생성된 문제를 AI가 다시 검토한다.

검토 verdict:

```txt
pass
revise
reject
```

결과:

```txt
outputs/{SUBJECT_CODE}/question_reviews.json
```

---

## 9. final report 생성

```bash
python scripts/08_make_report.py {SUBJECT_CODE}
```

생성된 JSON 결과를 사람이 보기 쉬운 Markdown 보고서로 만든다.

결과:

```txt
outputs/{SUBJECT_CODE}/final_report.md
```

확인:

```bash
code outputs/{SUBJECT_CODE}/final_report.md
```

---

# 한 과목 전체 실행 예시

운영체제 실행 예시:

```bash
cd ~/coremate/tools/ai-pipeline
source .venv/bin/activate

python scripts/00_check_env.py

python scripts/01_extract_pdf.py OPERATING_SYSTEM
python scripts/02_build_taxonomy.py OPERATING_SYSTEM
```

여기서 반드시 taxonomy를 검수한다.

```bash
code outputs/OPERATING_SYSTEM/taxonomy.json
```

검수가 끝나면 이어서 실행한다.

```bash
python scripts/03_generate_concept_cards.py OPERATING_SYSTEM
python scripts/04_generate_concept_relations.py OPERATING_SYSTEM
python scripts/05_generate_common_mistakes.py OPERATING_SYSTEM
python scripts/06_generate_questions.py OPERATING_SYSTEM
python scripts/07_review_questions.py OPERATING_SYSTEM
python scripts/08_make_report.py OPERATING_SYSTEM
```

최종 결과를 확인한다.

```bash
code outputs/OPERATING_SYSTEM/final_report.md
```

---


# 다시 실행해야 하는 경우

| 수정한 것 | 다시 실행할 단계 |
| --- | --- |
| PDF 파일 변경 | `01_extract_pdf`부터 |
| yaml의 taxonomy 범위 변경 | `02_build_taxonomy`부터 |
| taxonomy.json 직접 수정 | `03_generate_concept_cards`부터 |
| concept_cards.json 수정 | `04_generate_concept_relations`부터 |
| common_mistakes.json 수정 | `06_generate_questions`부터 |
| questions.json 수정 | `07_review_questions`부터 |
| report만 다시 만들기 | `08_make_report`만 |
| AI_PROVIDER 또는 모델 변경 | 원하는 생성 단계부터 다시 실행 |

---

# 자주 쓰는 확인 명령어


PDF가 들어갔는지 확인:

```bash
ls data/raw/{SUBJECT_CODE}
```

추출된 chunk 개수 확인:

```bash
python -c "import json; data=json.load(open('data/extracted/{SUBJECT_CODE}/chunks.json')); print(len(data)); print(data[0].keys())"
```

최종 결과 폴더 확인:

```bash
ls outputs/{SUBJECT_CODE}
```

---

# GitHub에 올리면 안 되는 것

아래 파일과 폴더는 GitHub에 올리지 않는다.

```txt
.env
.venv/
data/raw/
data/extracted/
outputs/
__pycache__/
```

이유:

| 항목 | 이유 |
| --- | --- |
| `.env` | API 키 포함 |
| `.venv/` | 개인 로컬 가상환경 |
| `data/raw/` | PDF 원문 포함 |
| `data/extracted/` | PDF 추출 텍스트 포함 |
| `outputs/` | 검수 전 AI 생성 결과 |
| `__pycache__/` | Python 캐시 |
