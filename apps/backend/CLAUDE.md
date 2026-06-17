# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
./gradlew build          # 빌드
./gradlew test           # 전체 테스트
./gradlew test --tests "com.coremate.backend.SomeTest"  # 단일 테스트 클래스
./gradlew bootRun        # 로컬 서버 실행
```

서버 기동 후 Swagger UI: `http://localhost:8080/swagger-ui/index.html`

## 패키지 구조

`com.coremate.backend` 하위 도메인별 수평 분리:

```
com.coremate.backend
├─ BackendApplication.java
├─ global
│  ├─ config/         # OpenApiConfig 등 설정 클래스
│  ├─ exception/      # ErrorCode (enum), CoreMateException
│  ├─ handler/        # GlobalExceptionHandler (@RestControllerAdvice)
│  └─ response/       # ApiResponse<T>, ErrorResponse, PageResponse
│
├─ auth/              # 회원가입, 로그인, 토큰 재발급
├─ user/              # 내 프로필, 닉네임 수정, 학습 통계
├─ content/           # 과목·분류·개념 탐색
├─ problemset/        # 문제집 목록·상세·문제 조회
├─ solve/             # 풀이 세션 생성·제출·완료·복습
├─ progress/          # 문제집·개념·과목별 진행률
├─ bookmark/          # 문제 즐겨찾기 추가·조회·메모
├─ dailyquiz/         # 오늘의 CS 퀴즈 세트·제출·보상
├─ attendance/        # 출석 체크·달력·보상
├─ weakness/          # AI 약점 분석 리포트 생성·조회
├─ reward/            # 보상 요약, 코인·XP 내역
├─ avatar/            # 아바타 프로필·장착 아이템 변경
├─ inventory/         # 보유 아이템 목록
├─ shop/              # 상점 상품·구매, 랜덤박스 오픈
└─ friend/            # 친구 요청·수락·목록·콕 찌르기
```

각 도메인의 내부 구조:
```
{domain}/
  controller/      # @RestController
  dto/
    request/       # 요청 DTO (@Valid 검증)
    response/      # 응답 DTO
    type/          # 도메인 Enum (해당 도메인에만 있는 경우)
```

## 코드 컨벤션

**응답 형식** — `ResponseEntity<ApiResponse<T>>` 사용:
```java
// 성공
return ResponseEntity.ok(ApiResponse.success(data));
return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("생성되었습니다", data));
```

**에러 처리** — `CoreMateException`을 throw하면 `GlobalExceptionHandler`가 자동 처리:
```java
throw new CoreMateException(ErrorCode.USER_NOT_FOUND);
throw new CoreMateException(ErrorCode.SOLVE_SESSION_ALREADY_COMPLETED);
```

에러 응답 JSON 구조: `{ "code": "USER_NOT_FOUND", "message": "사용자를 찾을 수 없습니다" }`

새 에러 코드 추가 시 `global/exception/ErrorCode.java`에 도메인별로 추가.

**DTO 스타일:**
- 응답 DTO: Java `record` 우선 사용
- 요청 DTO: Lombok `@Builder` + getter 조합 사용

**Swagger 어노테이션 패턴:**
```java
@Tag(name = "DomainName", description = "...")   // 컨트롤러 클래스
@Operation(summary = "...", description = "...")  // 메서드
@Parameter(description = "...", example = "1")   // @RequestParam / @PathVariable
@Schema(description = "...", example = "...")     // DTO 필드
```

## 의존성 (build.gradle)

- `spring-boot-starter-webmvc` — REST API
- `spring-boot-starter-validation` — @Valid 요청 검증
- `spring-boot-starter-data-jpa` — DB 연동
- `spring-boot-starter-security` — JWT 인증
- `mysql-connector-j` — MySQL 연결
- `springdoc-openapi-starter-webmvc-ui:3.0.3` — Swagger UI
- `lombok` — 보일러플레이트 제거
