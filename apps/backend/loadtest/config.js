// 부하테스트 공통 설정 & 로그인 헬퍼.
// smoke.js / read-stress.js 가 이 파일을 import 해서 쓴다.
//
// 환경변수로 조정한다(전부 선택, 기본값 있음):
//   BASE_URL        대상 서버 (기본 http://localhost:8080)
//   LOADTEST_USERS  테스트 계정 JSON 배열. 예: '[{"email":"lt1@ex.com","password":"..."}]'
//   LOADTEST_EMAIL  단일 계정 이메일 (LOADTEST_USERS 없을 때 사용)
//   LOADTEST_PW     단일 계정 비밀번호
//
// 실행 예:
//   docker run --rm -i --network host \
//     -e LOADTEST_EMAIL=lt1@ex.com -e LOADTEST_PW='P@ssw0rd!' \
//     grafana/k6 run - < read-stress.js

import http from 'k6/http';
import { check, fail } from 'k6';

export const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

// 인증이 필요 없는(또는 있어도 무해한) 가벼운 읽기 엔드포인트들.
// 실제 홈/복습 탭 진입 시 호출되는 조회 API 위주로 골랐다.
export const READ_ENDPOINTS = [
  // /api/attendance/today 는 오늘 출석 레코드가 없으면 404(정상 비즈니스 응답)라
  // 데이터 상태에 의존적이고 에러율 지표를 오염시켜 제외. 대신 calendar 로 출석 조회 부하를 준다.
  '/api/attendance/calendar',
  '/api/v1/content/subjects',
  '/api/notifications/unread-count',
  '/api/v1/progress/concepts/summary',
  '/api/v1/progress/subjects',
];

// 테스트에 쓸 계정 목록을 환경변수에서 읽는다.
function loadUsers() {
  if (__ENV.LOADTEST_USERS) {
    try {
      const arr = JSON.parse(__ENV.LOADTEST_USERS);
      if (Array.isArray(arr) && arr.length > 0) return arr;
    } catch (e) {
      fail(`LOADTEST_USERS 파싱 실패: ${e}`);
    }
  }
  if (__ENV.LOADTEST_EMAIL && __ENV.LOADTEST_PW) {
    return [{ email: __ENV.LOADTEST_EMAIL, password: __ENV.LOADTEST_PW }];
  }
  fail('테스트 계정이 없다. LOADTEST_USERS 또는 LOADTEST_EMAIL/LOADTEST_PW 를 지정하라. README 참고.');
}

// setup()에서 한 번만 실행: 모든 계정으로 로그인해 accessToken 배열을 만든다.
// 각 VU 는 default()에서 이 토큰을 돌아가며 쓴다(자세한 분배는 각 스크립트 참고).
export function loginAll() {
  const users = loadUsers();
  const tokens = [];
  for (const u of users) {
    const res = http.post(
      `${BASE_URL}/api/auth/login`,
      JSON.stringify({ email: u.email, password: u.password }),
      { headers: { 'Content-Type': 'application/json' }, tags: { name: 'POST /api/auth/login' } },
    );
    const ok = check(res, {
      'login 200': (r) => r.status === 200,
      'accessToken 존재': (r) => !!r.json('accessToken'),
    });
    if (!ok) {
      fail(`로그인 실패 (${u.email}): status=${res.status} body=${res.body}`);
    }
    tokens.push(res.json('accessToken'));
  }
  return { tokens };
}

// Authorization 헤더 + 응답시간 태그(name)를 붙인 요청 파라미터를 만든다.
// name 태그를 endpoint 경로로 고정해야 k6 요약에서 URL별로 뭉치지 않고 엔드포인트별로 집계된다.
export function authParams(token, name) {
  return {
    headers: { Authorization: `Bearer ${token}` },
    tags: { name },
  };
}
