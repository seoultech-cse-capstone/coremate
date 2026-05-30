# CoreMate Mobile

CoreMate 모바일 앱은 React Native, Expo, TypeScript, Expo Router 기반의 CS 학습 MVP 앱입니다. 현재 화면은 실제 API 연동 전 단계이며, 로그인, 문제집, Weakness, 친구, 마이페이지 흐름을 mock data와 placeholder로 구성합니다.

## 설치

```powershell
npm install
```

## 실행

```powershell
npx expo start
```

Expo CLI가 표시하는 QR 코드를 Expo Go 앱으로 스캔하면 기기에서 확인할 수 있습니다.

## Expo Go 확인

- iPhone: App Store에서 Expo Go 설치 후 카메라 또는 Expo Go로 QR 코드를 스캔합니다.
- Android: Google Play에서 Expo Go 설치 후 Expo Go 앱으로 QR 코드를 스캔합니다.
- PC와 휴대폰은 같은 Wi-Fi에 연결되어 있어야 합니다.

## 로컬 백엔드 연결

휴대폰의 Expo Go에서 PC의 Spring Boot 서버에 붙으려면 `localhost`가 아니라 PC의 같은 Wi-Fi IPv4 주소를 사용해야 합니다.

예시:

```env
EXPO_PUBLIC_API_BASE_URL=http://192.168.0.12:8080/api/v1
```

API 주소는 `.env` 또는 `.env.example`의 `EXPO_PUBLIC_API_BASE_URL` 값으로 관리합니다. 실제 `.env` 파일은 로컬 환경에 맞게 생성해서 사용하세요.

## 라우팅 구조

```text
app/
  _layout.tsx
  index.tsx
  login.tsx
  register.tsx
  (tabs)/
    _layout.tsx
    home.tsx
    friends.tsx
    mypage.tsx
    problemset.tsx
    weakness/
      _layout.tsx
      index.tsx
      [sessionId].tsx
```

## Weakness 탭

Weakness 탭은 AI 분석 결과만 보여주는 화면이 아니라, 사용자가 푼 문제집 목록에서 AI 약점 분석으로 진입하는 흐름입니다.

- `app/(tabs)/weakness/index.tsx`: 푼 문제집 목록 및 AI 분석 진입 화면
- `app/(tabs)/weakness/[sessionId].tsx`: 특정 문제집의 AI 약점 분석 상세/생성 화면

상세 화면은 Weakness 탭 내부 Stack에 있으므로 하단 탭바가 유지됩니다.

## 공통 API 클라이언트

공통 fetch 클라이언트는 `src/shared/api/httpClient.ts`에 있습니다.

- `EXPO_PUBLIC_API_BASE_URL` 사용
- `get`, `post`, `put`, `delete` 제공
- JSON 요청/응답 처리
- 기본 에러 구조 포함
- 추후 accessToken 확장 가능

## 다음 개발 단계

- 백엔드 API 계약 확정 후 auth, problemset, weakness feature에 API 연동
- 문제집 상세, 개념 선택, 문제 풀이, 풀이 결과 화면 추가
- 사용자 인증 토큰 저장 방식 결정
- 아바타, 상점, 친구 기능의 실제 데이터 구조 설계
- 화면별 로딩, 에러, 빈 상태 UI 정리
