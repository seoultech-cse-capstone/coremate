# 부하/스트레스 테스트 (이슈 #273)

읽기 API가 **동시 사용자 몇 명까지 버티고 어디서 무너지는지(무릎 지점)** 를 측정하고,
그 순간 **서버 쪽 병목(HikariCP 풀 대기 등)** 을 Grafana 로 함께 관찰한다.

부하 생성은 **k6**(클라이언트 지표: RPS·지연·에러율), 서버 관찰은 **Prometheus + Grafana**
(actuator 메트릭: 커넥션 풀·JVM·HTTP 지연)로 역할을 나눈다.

## 구성

```
apps/backend/loadtest/k6/read-apis.js         # k6 부하 스크립트 (smoke/load/stress)
infra/loadtest/docker-compose.observability.yml # Prometheus + Grafana 스택
infra/loadtest/prometheus.yml                  # backend:8080/actuator/prometheus 스크레이프
infra/loadtest/grafana/...                     # 데이터소스 + 대시보드 자동 프로비저닝
```

## 사전 준비

1. **k6 설치**: `brew install k6`
2. **테스트 계정**: 로그인 가능한 계정 하나. 없으면 회원가입 API 로 하나 만든다.
   부하 시 같은 계정으로 로그인하므로 시드 계정 하나면 충분하다.
3. **backend 재빌드**: actuator 의존성이 새로 추가됐다. `/actuator/prometheus` 가 뜨려면
   backend 컨테이너를 다시 빌드해야 한다.

## 실행 순서

### 1) 앱 + 관측 스택 기동

```bash
cd infra
# actuator 반영 위해 --build 필수
docker compose -f docker-compose.local.yml up --build -d
# 메트릭 노출 확인 (숫자가 나와야 함)
curl -s http://localhost:8080/actuator/prometheus | head

# 관측 스택
docker compose -f loadtest/docker-compose.observability.yml up -d
```

Grafana: http://localhost:3001 (admin / admin) → 대시보드 **CoreMate Load Test**

### 2) k6 실행 (부하 생성기는 되도록 다른 머신에서)

```bash
cd apps/backend/loadtest/k6

# 스모크 — 파이프라인·계정 확인
k6 run -e SCENARIO=smoke -e BASE_URL=http://localhost:8080 \
  -e EMAIL=loadtest@coremate.dev -e PASSWORD='P@ssw0rd!' read-apis.js

# 부하 — 기준선 (VU 50 유지)
k6 run -e SCENARIO=load  -e BASE_URL=http://localhost:8080 \
  -e EMAIL=loadtest@coremate.dev -e PASSWORD='P@ssw0rd!' read-apis.js

# 스트레스 — 무릎 지점 탐색 (VU 50→800 계단식)
k6 run -e SCENARIO=stress -e BASE_URL=http://localhost:8080 \
  -e EMAIL=loadtest@coremate.dev -e PASSWORD='P@ssw0rd!' read-apis.js
```

> ⚠️ **부하 생성기를 앱과 같은 맥북에서 돌리면** k6·앱·MySQL·Redis 가 같은 CPU 를 나눠 써서
> 클라이언트가 먼저 포화된다. 서버의 진짜 한계를 보려면 k6 를 다른 머신/VM 에서 `BASE_URL`
> 을 이 PC 로 향하게 돌려라.

## 무엇을 보나 (병목 판독)

- **Grafana → HikariCP `pending(대기)`** 가 0 위로 쌓이기 시작 = 풀 고갈 시작 = 무릎 지점.
  같은 시각 k6 에서 에러율이 튀고 p95/p99 가 급증하면 "풀 병목"으로 확정.
- **RPS 패널이 평평해지는 지점** = 처리량 한계.
- **지연 p95/p99(uri별)** 로 어떤 API 가 먼저 느려지는지 구분.
- **CPU/GC** 가 먼저 100%/급증하면 풀이 아니라 연산·GC 병목.

## 유저 시나리오 (부하 설계 지도)

실제 사용자가 앱에서 하는 행동을 **시나리오**로 묶고, 각 시나리오의 부하 특성(읽기/쓰기 비중·트래픽 패턴·병목 핫스팟)과 **어떤 테스트 유형이 맞는지**를 정리한다.
부하는 "엔드포인트 하나"가 아니라 "사용자 흐름"으로 몰리므로, 스크립트도 흐름 단위로 설계해야 현실적이다.

> 현재 `read-apis.js` 가 커버하는 범위는 **S1(홈 진입)의 읽기 일부** 뿐이다. 아래 S2~S8 은 확장 대상(설계 지도)이다.

### 테스트 유형 레퍼런스

| 유형 | 무엇을 보나 | VU 패턴 |
|------|------------|---------|
| **smoke** | 기능·파이프라인 정상 | 아주 낮게(1~5) |
| **load** | 평소 부하에서의 성능 기준선 | 예상 동시접속 유지 |
| **stress** | 한계·무릎 지점 | 계단식 증가 |
| **spike** | 순간 폭주 대응 (이벤트·푸시 직후) | 급등 후 급락 |
| **soak** | 장시간 누수(메모리·커넥션) | 중간 부하 수시간 유지 |

### 시나리오 목록

#### S1. 홈 진입 (콜드 스타트) — 읽기 fan-out
앱을 켜면 홈 화면을 그리려 **여러 조회 API가 동시에 폭발**한다. 동시 접속이 가장 먼저 몰리는 지점.
- 흐름: `login` → `notifications/device-tokens`(등록) → 홈 데이터 병렬 조회
  - `users/me`, `daily-quizzes/today`, `attendance/today`, `avatar/me`, `avatar/room`,
    `currency/me`, `xp/summary`, `rewards/summary`, `notifications/unread-count`, `progress/subjects`, `progress/concepts/summary`
- 비중: **읽기 95%+**
- 패턴: **아침 등교/출근 시간대 동시 접속** → `spike` 후보. 평상시는 `load`.
- 핫스팟: 커넥션 풀(읽기 fan-out), `daily-quizzes/today`(전체 공통 → 캐시 효과 큼, 대조군)

#### S2. 학습 세션 (핵심 플로우) — 읽기+쓰기 혼합
앱의 본질 기능. 탐색부터 풀이 완료까지 **순차 의존 체인**.
- 흐름: `content/subjects` → `.../category-lv1` → `.../category-lv2` → `.../concepts`
  → `POST solve-sessions/concept`(시작) → `POST .../{id}/answers`(문항 수만큼 반복)
  → `GET solve/questions/{id}/hint`(선택) → `POST .../{id}/complete`(완료)
- 비중: 읽기(탐색) + **쓰기(답안·완료)**
- 패턴: 꾸준한 `load`, 세션 길어 `soak` 로 커넥션 누수 확인
- 핫스팟: **`complete`** — 보상 지급 트랜잭션 + `Idempotency-Key`. 쓰기·동시성 집중점.

#### S3. 데일리 퀴즈 — 캐시 읽기 + 쓰기 폭주
- 흐름: `GET daily-quizzes/today`(공통 세트) → `POST .../{setId}/submit`(출석+보상) → `today/reward`, `result`
- 비중: 조회는 캐시 적중, **제출은 쓰기 집중**
- 패턴: **"오늘의 퀴즈" 알림 직후 동시 제출** → 대표적 `spike`. 쓰기(출석+보상)가 한 시각에 몰림.
- 핫스팟: `submit`(출석·보상 트랜잭션), today 캐시 적중률

#### S4. 게이미피케이션 — 구매/뽑기 (쓰기·동시성)
- 흐름: `GET shop/products` / `random-box/items` / `odds` → `POST shop/purchase` / `random-box/open`
  → `GET avatar/inventory` → `PUT avatar/equipped-items` / `room/placements`
- 비중: 조회 후 **재화 차감·아이템 지급 쓰기**
- 패턴: `stress`(동시 구매 경합), 보상 이벤트 시 `spike`
- 핫스팟: `purchase`·`open` — 재화 트랜잭션 + `Idempotency-Key`. **동시성(중복 차감/지급) 검증 핵심.**

#### S5. 복습 / 진도 확인 — 읽기 집계
- 흐름: `progress/concepts/summary`, `progress/problem-sets`, `progress/pass-rates`, `problem-sets/completed`, `bookmarks/questions`
- 비중: **읽기 100%** (사용자별 집계 쿼리 — 캐시 적용 여지)
- 패턴: `load`/`stress` 로 집계 쿼리 부하 측정
- 핫스팟: 사용자별 집계 쿼리(캐시 없음) → DB 부하

#### S6. 소셜 / 친구 — 읽기 + 가벼운 쓰기
- 흐름: `GET friends`, `friends/search`, `friends/{id}/profile`, `friends/{id}/learning-history` → `POST friends/pokes`
- 비중: 읽기 위주 + 찌르기 쓰기
- 패턴: `load`
- 핫스팟: `pokes`(일일 한도 + `Idempotency-Key`), `learning-history`(집계 조회)

#### S7. 인증 수명주기 — 토큰 발급 부하
- 흐름: `POST auth/login` → `POST auth/reissue`(만료 시) → `POST auth/logout`. (회원가입은 `email/send-code → verify-code → signup` 3단계)
- 비중: **쓰기(토큰 발급/검증)**, Redis 세션 집중
- 패턴: 로그인 러시 시 `spike`, 평상시 `load`
- 핫스팟: 비밀번호 해싱(BCrypt, CPU), Redis refresh 토큰 I/O

#### S8. 약점 분석 (AI) — 무거운 동기 호출 ⚠️
- 흐름: `GET weakness/eligible-solve-sessions` → `GET analysis-cost` → `POST weakness/reports`(AI 해석) → `GET reports/{id}`
- 비중: 조회 가벼움, **`POST reports` 가 외부 AI 동기 호출 포함 → 응답 매우 김**
- 패턴: 반드시 **별도 격리 측정** (다른 시나리오와 섞으면 전체 지연 왜곡). 낮은 VU + 긴 타임아웃.
- 핫스팟: 외부 AI 레이턴시·타임아웃, 코인 차감 트랜잭션

### 트래픽 믹스 (복합 부하 설계 시 참고)

실사용은 여러 시나리오가 섞여 들어온다. 복합 부하 스크립트를 짤 때 대략의 비중 가이드(실측 분석 전 추정치):

| 시나리오 | 추정 비중 | 성격 |
|----------|----------|------|
| S1 홈 진입 | ~40% | 읽기 fan-out |
| S2 학습 세션 | ~25% | 읽기+쓰기 |
| S5 복습/진도 | ~15% | 읽기 집계 |
| S3 데일리 퀴즈 | ~10% | 캐시+쓰기 |
| S4 게이미피케이션 | ~5% | 쓰기 동시성 |
| S6 소셜 | ~3% | 읽기 |
| S7 인증 | ~2% | 쓰기 |
| S8 약점분석 | 별도 격리 | 무거운 AI |

> 비중은 실제 접속 로그/분석 데이터가 쌓이면 교체할 것. 지금은 "읽기가 압도적이고 쓰기는 보상 트랜잭션에 집중된다"는 큰 그림만 유효하다.

### 설계 시 공통 유의점

- **Idempotency-Key 핫스팟**: `solve complete`, `shop purchase`, `random-box open`, `friend poke`, `weakness reports`. 동시성·중복 방지 검증 대상.
- **쓰기 집중점**: `solve complete`, `daily-quiz submit`, `attendance reward` — 모두 재화/보상 트랜잭션 동반 → 커넥션을 오래 점유(읽기보다 무릎 지점이 빠름).
- **비회원 부하 가능**(토큰 불필요): auth 전반, content 탐색, `solve-sessions/concept` 시작, `guest/complete`, `question hint`, `reward policies`.
- **쓰기 테스트는 데이터 시딩/정리 필요**: purchase·open 은 재화가 소진되면 실패하므로, 충분한 재화 시드 or 재화 지급 셋업이 선행돼야 실패율이 왜곡되지 않는다.

## 정리

```bash
docker compose -f infra/loadtest/docker-compose.observability.yml down
```
