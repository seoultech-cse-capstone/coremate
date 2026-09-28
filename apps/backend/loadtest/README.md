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

## 정리

```bash
docker compose -f infra/loadtest/docker-compose.observability.yml down
```
