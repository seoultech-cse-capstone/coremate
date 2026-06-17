# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

CoreMate는 CS 전공지식 반복 학습 앱이다. 문제 풀이 기록 기반으로 AI 약점 분석표를 제공하고, 아바타·XP·코인 보상으로 학습을 유도한다.

## 모노레포 구조

```
coremate/
├─ apps/
│  ├─ backend/       # Java 21 + Spring Boot 4.x REST API
│  └─ mobile/        # React Native + Expo + TypeScript 모바일 앱
├─ tools/
│  └─ ai-pipeline/   # Python 기반 CS 콘텐츠/문제 생성 파이프라인 (배포 대상 아님)
├─ docs/             # 기획서, DB 명세, API 명세, AI 파이프라인 문서
├─ infra/            # Docker, AWS ECS/Fargate, RDS, S3 설정
└─ scripts/          # DB 시드 데이터, 로컬 실행 스크립트
```

## 기술 스택

| 영역 | 기술 |
|---|---|
| 모바일 앱 | React Native, Expo, TypeScript |
| 백엔드 | Java 21, Spring Boot 4.x, Spring Security, Spring Data JPA |
| DB | MySQL (로컬), Amazon RDS for MySQL (운영), utf8mb4 |
| AI 분석 | OpenAI API (약점 분석표 생성) |
| AI 파이프라인 | Python, OpenAI API (CS 개념/문제 콘텐츠 생성) |
| 배포 | Docker, AWS ECS Fargate, ECR, S3, CloudWatch |
| CI/CD | GitHub Actions (예정) |
| 푸시 알림 | Firebase Cloud Messaging, Expo Notifications (예정) |

## 브랜치 관리

| 브랜치 | 역할 |
|---|---|
| `main` | 안정화된 최종 코드 |
| `develop` | 개발 통합 브랜치 |
| `feat/*` | 기능 개발 |
| `fix/*` | 버그 수정 |
| `docs/*` | 문서 수정 |
| `refactor/*` | 구조 개선 |
| `chore/*` | 빌드/설정/도구 변경 |

## 환경변수 관리

실제 키 값은 GitHub/Notion에 올리지 않는다. `.env.example`만 공유하고 팀원별 로컬에서 직접 설정한다.

```
OPENAI_API_KEY=your_openai_api_key
DB_HOST=localhost / DB_PORT=3306 / DB_NAME=coremate
JWT_SECRET=your_jwt_secret
```
