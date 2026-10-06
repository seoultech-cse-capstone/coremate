// CoreMate 읽기 API 부하/스트레스 테스트 (이슈 #273)
//
// 목적: 동시 사용자를 단계적으로 늘려 읽기 API가 어디서 무너지는지(무릎 지점)를 찾고,
// 그때 서버 쪽 병목(HikariCP 풀 대기 등)을 Grafana(actuator/prometheus)로 함께 관찰한다.
//
// 실행 예:
//   # 1) 스모크 — 시나리오·계정·토큰 정상 확인 (VU 1)
//   k6 run -e SCENARIO=smoke -e BASE_URL=http://localhost:8080 \
//     -e EMAIL=loadtest@coremate.dev -e PASSWORD='P@ssw0rd!' read-apis.js
//
//   # 2) 부하 — 기준선 (VU 50 유지)
//   k6 run -e SCENARIO=load  ... read-apis.js
//
//   # 3) 스트레스 — 무릎 지점 탐색 (VU 계단식 증가)
//   k6 run -e SCENARIO=stress ... read-apis.js
//
// 중요: 부하 생성기(k6)는 서버와 "다른 머신"에서 돌려야 서버의 진짜 한계가 보인다.
// 같은 맥북에서 앱+DB+k6 를 함께 돌리면 클라이언트가 먼저 포화된다.

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Rate } from 'k6/metrics';

// ---- 설정 (환경변수로 오버라이드) ----
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const EMAIL = __ENV.EMAIL || 'loadtest@coremate.dev';
const PASSWORD = __ENV.PASSWORD || 'P@ssw0rd!';
const SCENARIO = __ENV.SCENARIO || 'smoke';

// 측정 대상 읽기 엔드포인트. 경로 파라미터가 없어 토큰만 있으면 바로 때릴 수 있는 것들로 구성.
// 필요하면 여기서 추가/삭제한다. weight 는 요청 비율(트래픽 분포 흉내).
const ENDPOINTS = [
  { name: 'daily_quiz_today', method: 'GET', path: '/api/v1/daily-quizzes/today', weight: 3 }, // 캐시 적용됨(#293) — 대조군
  { name: 'content_subjects', method: 'GET', path: '/api/v1/content/subjects', weight: 2 },     // 전역·준정적, 캐시 없음(후보)
  { name: 'progress_summary', method: 'GET', path: '/api/v1/progress/concepts/summary', weight: 2 }, // 사용자별 집계(후보)
  { name: 'progress_subjects', method: 'GET', path: '/api/v1/progress/subjects', weight: 2 },   // 집계(후보)
  // 주의: /api/attendance/today 는 오늘 출석 기록이 없으면 설계상 404(ATTENDANCE_NOT_FOUND).
  // 갓 만든 테스트 계정에선 100% 404 라 실패율을 왜곡한다. 포함하려면 계정에 먼저 체크인을 시드하라.
  { name: 'user_me', method: 'GET', path: '/api/users/me', weight: 1 },
  { name: 'notif_unread', method: 'GET', path: '/api/notifications/unread-count', weight: 1 },
];

// 엔드포인트별 지연/실패를 따로 본다 (어느 API가 먼저 무너지는지 구분).
const latency = {};
const failRate = {};
for (const e of ENDPOINTS) {
  latency[e.name] = new Trend(`ep_latency_${e.name}`, true);
  failRate[e.name] = new Rate(`ep_failed_${e.name}`);
}

// weight 를 펼친 가중 추첨 테이블.
const WEIGHTED = ENDPOINTS.flatMap((e) => Array(e.weight).fill(e));

// ---- 시나리오 정의 ----
const SCENARIOS = {
  // 스모크: 최소 부하로 파이프라인 검증.
  smoke: {
    executor: 'constant-vus',
    vus: 1,
    duration: '30s',
  },
  // 부하: 목표 동시성 유지, 기준선(에러율<1%, p95) 측정.
  load: {
    executor: 'ramping-vus',
    startVUs: 0,
    stages: [
      { duration: '30s', target: 50 },
      { duration: '3m', target: 50 },
      { duration: '30s', target: 0 },
    ],
  },
  // 스트레스: 계단식으로 올려 RPS 정체·에러 급증하는 무릎 지점 탐색.
  stress: {
    executor: 'ramping-vus',
    startVUs: 0,
    stages: [
      { duration: '1m', target: 50 },
      { duration: '1m', target: 100 },
      { duration: '1m', target: 200 },
      { duration: '1m', target: 400 },
      { duration: '1m', target: 800 },
      { duration: '30s', target: 0 },
    ],
  },
};

export const options = {
  scenarios: { [SCENARIO]: SCENARIOS[SCENARIO] },
  thresholds: {
    // 기준선: 전체 실패율 1% 미만, p95 500ms 미만. 무릎 지점을 넘기면 여기서 빨갛게 뜬다.
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<500'],
  },
  // 요약에 p99 도 노출.
  summaryTrendStats: ['avg', 'min', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
};

// ---- 로그인: VU 당 1회 토큰 발급 ----
export function setup() {
  const res = http.post(
    `${BASE_URL}/api/auth/login`,
    JSON.stringify({ email: EMAIL, password: PASSWORD }),
    { headers: { 'Content-Type': 'application/json' }, tags: { name: 'login' } },
  );
  const ok = check(res, { 'login 200': (r) => r.status === 200 });
  if (!ok) {
    throw new Error(
      `로그인 실패 (status=${res.status}). EMAIL/PASSWORD 환경변수와 계정 존재 여부를 확인하라. body=${res.body}`,
    );
  }
  const token = res.json('accessToken');
  if (!token) throw new Error('accessToken 이 응답에 없다. LoginResponse 필드명을 확인하라.');
  return { token };
}

export default function (data) {
  const params = {
    headers: { Authorization: `Bearer ${data.token}` },
  };

  // 가중 추첨으로 한 엔드포인트 선택 (__ITER 기반 — Math.random 대신 결정적으로 분산).
  const e = WEIGHTED[__ITER % WEIGHTED.length];
  const res = http.request(e.method, `${BASE_URL}${e.path}`, null, {
    ...params,
    tags: { name: e.name }, // http_req_duration 을 엔드포인트별로 쪼갠다.
  });

  const passed = check(res, { [`${e.name} 2xx`]: (r) => r.status >= 200 && r.status < 300 });
  latency[e.name].add(res.timings.duration);
  failRate[e.name].add(!passed);

  sleep(0.5); // think time — 실제 사용자 간격 흉내.
}
