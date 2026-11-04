# CoreMate 관리자 웹

문의/버그 신고를 조회하고 답변하고, 메일로 온 계정 삭제 요청을 처리하는 운영자용 웹. Next.js(App Router) + TypeScript.
브라우저에서 백엔드 API를 직접 호출하는 클라이언트 앱이라 Vercel 무료 플랜에 바로 올릴 수 있다.

## 로컬 실행

```bash
cp .env.local.example .env.local   # NEXT_PUBLIC_API_BASE_URL 확인
npm install
npm run dev                        # http://localhost:3000
```

## 관리자 계정

별도 회원가입이 없다. 기존 사용자 계정을 ADMIN으로 승격해서 쓴다.

```sql
UPDATE users SET role = 'ADMIN' WHERE email = '<운영자 이메일>';
```

로그인은 앱과 동일한 `/api/auth/login`을 쓰고, 발급된 토큰으로 `/api/v1/admin/**`를 호출한다.

## 배포 (Vercel)

1. 이 디렉터리를 프로젝트 루트로 import.
2. 환경변수 `NEXT_PUBLIC_API_BASE_URL` = 백엔드 공개 URL.
3. **백엔드 CORS 허용**: 백엔드 환경변수 `ADMIN_WEB_ALLOWED_ORIGINS`에
   Vercel 도메인(예: `https://coremate-admin.vercel.app`)을 추가한다. 콤마로 여러 개 가능.

## API

| 메서드 | 경로 | 설명 |
| --- | --- | --- |
| GET | `/api/v1/admin/support/inquiries?status=` | 문의 목록 |
| GET | `/api/v1/admin/support/inquiries/{id}` | 문의 상세 |
| PATCH | `/api/v1/admin/support/inquiries/{id}/answer` | 답변 등록 |
| GET | `/api/v1/admin/support/bug-reports?status=` | 버그 신고 목록 |
| PATCH | `/api/v1/admin/support/bug-reports/{id}/status?status=` | 상태 변경 |
| GET | `/api/v1/admin/users?keyword=` | 회원 찾기 (가입 이메일 또는 닉네임이 정확히 같은 회원) |
| POST | `/api/v1/admin/users/{userId}/withdraw` | 회원 탈퇴 처리 (앱 탈퇴와 같은 범위, 운영자 계정은 불가) |
