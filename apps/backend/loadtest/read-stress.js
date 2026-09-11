// 읽기 API 부하 → 스트레스 테스트.
// 가상 사용자(VU)를 단계적으로 올리며 "무릎 지점"(RPS 는 안 오르는데 응답시간/에러율만
// 폭증하는 한계)을 찾는다. 각 VU 는 홈/복습 탭을 훑는 사용자처럼 읽기 엔드포인트들을
// 돌아가며 호출한다.
//
// 실행:
//   docker run --rm -i --network host \
//     -e LOADTEST_EMAIL=lt1@ex.com -e LOADTEST_PW='P@ssw0rd!' \
//     grafana/k6 run - < read-stress.js
//
// 여러 계정으로 더 현실적으로(캐시 편향 줄이기):
//   -e LOADTEST_USERS='[{"email":"lt1@ex.com","password":"..."},{"email":"lt2@ex.com","password":"..."}]'
//
// 단계를 바꾸고 싶으면 아래 stages 를 직접 수정하거나, 빠르게 바꾸려면:
//   -e STAGE_PEAK=500   (최고 VU 수만 조정)

import { sleep, check } from 'k6';
import http from 'k6/http';
import { Trend, Counter } from 'k6/metrics';
import { BASE_URL, READ_ENDPOINTS, loginAll, authParams } from './config.js';

const peak = parseInt(__ENV.STAGE_PEAK || '300', 10);
// think time(화면 사이 텀) 상한(초). 현실적 부하는 기본 3초.
// 서버의 순수 한계(무릎 지점)를 찾으려면 THINK_TIME=0 으로 텀을 없애고 STAGE_PEAK 를 올린다.
const thinkMax = parseFloat(__ENV.THINK_TIME !== undefined ? __ENV.THINK_TIME : '3');

// 커스텀 지표: 성공 응답만 모은 지연(실패 응답의 빠른 5xx가 p95를 낮추는 착시 방지)
const okDuration = new Trend('ok_req_duration', true);
const bizErrors = new Counter('business_errors');

export const options = {
  scenarios: {
    ramping_stress: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '30s', target: 20 },              // 워밍업
        { duration: '1m', target: 50 },               // 기준 부하 유지 (기준선 측정)
        { duration: '1m', target: Math.round(peak / 2) },
        { duration: '1m', target: peak },             // 한계까지 밀어붙이기
        { duration: '30s', target: 0 },               // 회복 관찰
      ],
      gracefulRampDown: '10s',
    },
  },
  thresholds: {
    // 임계선을 넘으면 k6가 실패로 표시한다 = "여기가 한계였다"는 신호.
    http_req_failed: ['rate<0.01'],           // 에러율 1% 미만
    http_req_duration: ['p(95)<800'],         // p95 800ms 미만
    ok_req_duration: ['p(95)<800', 'p(99)<2000'],
  },
};

export function setup() {
  return loginAll();
}

export default function (data) {
  // VU 번호로 계정을 고르게 분배(계정이 1개면 전원 같은 토큰).
  const token = data.tokens[(__VU - 1) % data.tokens.length];

  // 한 번의 반복에서 홈 화면 진입처럼 여러 조회를 순차 호출한다.
  for (const path of READ_ENDPOINTS) {
    const res = http.get(`${BASE_URL}${path}`, authParams(token, `GET ${path}`));
    const ok = check(res, { [`${path} → 2xx`]: (r) => r.status >= 200 && r.status < 300 });
    if (ok) {
      okDuration.add(res.timings.duration);
    } else {
      bizErrors.add(1);
    }
  }
  // 실제 사용자처럼 화면 사이 텀. THINK_TIME=0 이면 텀 없이 최대 부하로 한계를 탐색한다.
  if (thinkMax > 0) {
    sleep(Math.random() * thinkMax); // 0~thinkMax초
  }
}
