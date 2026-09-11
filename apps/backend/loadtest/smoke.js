// 스모크 테스트: VU 1명으로 시나리오가 에러 없이 도는지만 확인한다.
// 본 부하테스트(read-stress.js) 전에 항상 먼저 돌려서 계정/토큰/엔드포인트가
// 정상인지 검증하는 용도. 부하를 재는 테스트가 아니다.
//
//   docker run --rm -i --network host \
//     -e LOADTEST_EMAIL=lt1@ex.com -e LOADTEST_PW='P@ssw0rd!' \
//     grafana/k6 run - < smoke.js

import { sleep, check } from 'k6';
import http from 'k6/http';
import { BASE_URL, READ_ENDPOINTS, loginAll, authParams } from './config.js';

export const options = {
  vus: 1,
  iterations: READ_ENDPOINTS.length, // 엔드포인트를 한 번씩만 훑는다
  thresholds: {
    http_req_failed: ['rate==0'], // 스모크는 에러가 하나라도 나면 실패
  },
};

export function setup() {
  return loginAll();
}

export default function (data) {
  const token = data.tokens[0];
  const path = READ_ENDPOINTS[(__ITER) % READ_ENDPOINTS.length];
  const res = http.get(`${BASE_URL}${path}`, authParams(token, `GET ${path}`));
  check(res, { [`${path} → 2xx`]: (r) => r.status >= 200 && r.status < 300 });
  sleep(0.3);
}
