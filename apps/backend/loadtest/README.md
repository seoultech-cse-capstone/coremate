# 백엔드 부하테스트 (k6)

CoreMate 백엔드가 **동시 사용자 몇 명까지 버티는지(한계점)** 를 측정하기 위한 k6 스크립트 모음.

## 무엇을 테스트하나

이 디렉터리는 **일반 읽기 API**(Spring Boot → MySQL/Redis)만 다룬다. 부하 특성이 전혀 다른
다음 두 부류는 **일부러 제외**했다 — 섞으면 결과 해석이 불가능하다.

- **AI 연동 API** (`POST /api/v1/weakness/reports` 등, AI 서버 :8000 호출) → LLM 병목이라 별도 테스트 필요
- **이메일/외부 연동** (`POST /api/auth/email/send-code`) → 외부 SMTP를 때리게 되므로 부하테스트 대상 아님

대상 엔드포인트는 `config.js`의 `READ_ENDPOINTS` 참고(출석·콘텐츠·알림·진행률 조회).

## 사전 준비

1. **k6** — 설치 없이 Docker로 실행 가능(아래 명령 참고). 로컬 설치는 `brew install k6`.
2. **백엔드 기동** — `infra/`에서 `docker compose -f docker-compose.local.yml up -d`
3. **테스트 계정** — 로그인이 필요하다. 회원가입은 이메일 인증 게이트가 있으므로,
   앱/스웨거로 **계정을 1개 이상 미리 만들어 둔다.** 여러 계정을 쓰면 캐시 편향이 줄어 더 현실적이다.

## 실행

### 1) 스모크 (항상 먼저)
VU 1명으로 로그인·토큰·엔드포인트가 정상인지만 확인. 에러가 하나라도 나면 실패.
```bash
docker run --rm -i --network host \
  -e LOADTEST_EMAIL=lt1@example.com -e LOADTEST_PW='P@ssw0rd!' \
  grafana/k6 run - < smoke.js
```

### 2) 부하 → 스트레스
VU를 단계적으로 올리며 한계점을 찾는다.
```bash
docker run --rm -i --network host \
  -e LOADTEST_EMAIL=lt1@example.com -e LOADTEST_PW='P@ssw0rd!' \
  grafana/k6 run - < read-stress.js
```

여러 계정으로:
```bash
  -e LOADTEST_USERS='[{"email":"lt1@example.com","password":"P@ssw0rd!"},{"email":"lt2@example.com","password":"P@ssw0rd!"}]'
```

최고 VU 수만 빠르게 바꾸기:
```bash
  -e STAGE_PEAK=500
```

> macOS/Windows에서 `--network host`가 안 먹으면 `BASE_URL=http://host.docker.internal:8080`을 넘긴다.

## 결과 읽는 법

k6 요약 출력에서 이 값들을 본다:

| 지표 | 의미 | 위험 신호 |
|------|------|-----------|
| `http_reqs` (rate) | 처리량(RPS) | VU를 올려도 더 안 오르면 포화 |
| `http_req_duration` p95/p99 | 응답시간 | 특정 VU 구간부터 급증 → 무릎 지점 |
| `http_req_failed` rate | 에러율 | 1% 돌파 = 한계 도달 |
| `ok_req_duration` | 2xx만의 지연 | 5xx가 빨라서 p95가 낮아 보이는 착시 방지 |

부하가 오르는 동안 **다른 터미널에서 자원도 같이 관찰**한다 — 어느 자원이 먼저 바닥나는지가 병목의 정체다:
```bash
docker stats                      # 백엔드/MySQL 컨테이너 CPU·메모리
docker compose -f infra/docker-compose.local.yml logs -f backend   # 커넥션 풀 대기·예외
```

## 예상 병목 (측정으로 검증할 가설)

- **HikariCP 커넥션 풀** — 현재 `application.yaml`에 풀 설정이 없어 **기본 10개**. 동시 요청이
  이를 넘으면 요청들이 커넥션을 기다리며 응답시간이 급증한다. **1순위 용의자.**
  개선 실험: `spring.datasource.hikari.maximum-pool-size`를 20~30으로 올려 전후 비교.
- **MySQL** — 인덱스 미비, JPA N+1 쿼리.
- **JVM 힙 / 컨테이너 메모리 제한.**
