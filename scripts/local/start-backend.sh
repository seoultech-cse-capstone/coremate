#!/bin/sh
# ================================================================
# CoreMate 백엔드 로컬 실행 + 로그 보기 (Git Bash에서 실행)
#
# 사용법 (리포 루트에서):
#   ./scripts/local/start-backend.sh          # 컨테이너 시작 + 백엔드 로그 표시
#   ./scripts/local/start-backend.sh --build  # 백엔드 코드 변경 시 이미지 재빌드 후 시작
#
# 로그는 Ctrl+C로 빠져나와도 서버는 계속 돌아갑니다.
# 서버 내리기: cd infra && docker compose -f docker-compose.local.yml down
# ================================================================
set -e

# docker CLI가 PATH에 없으면 Docker Desktop 설치 경로 추가
if ! command -v docker >/dev/null 2>&1; then
  export PATH="$(cygpath "$LOCALAPPDATA")/Programs/DockerDesktop/resources/bin:$PATH"
fi

# Docker Desktop(엔진)이 안 떠 있으면 실행하고 대기
if ! docker info >/dev/null 2>&1; then
  echo ">> Docker Desktop이 꺼져 있어 시작합니다..."
  DOCKER_EXE="$(cygpath "$LOCALAPPDATA")/Programs/DockerDesktop/Docker Desktop.exe"
  [ -f "$DOCKER_EXE" ] || DOCKER_EXE="/c/Program Files/Docker/Docker/Docker Desktop.exe"
  start "" "$DOCKER_EXE" 2>/dev/null || "$DOCKER_EXE" &
  printf ">> 엔진 대기 중"
  i=0
  until docker info >/dev/null 2>&1; do
    i=$((i + 1))
    if [ "$i" -gt 36 ]; then
      echo ""
      echo "!! 3분 내에 엔진이 시작되지 않았습니다. Docker Desktop 창을 확인하세요."
      exit 1
    fi
    printf "."
    sleep 5
  done
  echo " 준비 완료!"
fi

# 스크립트 자신의 절대 경로(scripts/local). 아래에서 디렉토리를 옮겨다녀도
# 안전하게 다시 참조할 수 있도록 cd 전에 미리 구해둔다.
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

# infra 디렉토리로 이동
cd "$SCRIPT_DIR/../../infra"

echo ">> 백엔드 스택 시작 (MySQL + Redis + Backend)"
if [ "$1" = "--build" ]; then
  docker compose -f docker-compose.local.yml up --build -d
else
  docker compose -f docker-compose.local.yml up -d
fi

echo ""
echo ">> 모바일 앱 .env의 LAN IP 갱신"
node "$SCRIPT_DIR/sync-ip.js"

echo ""
echo "================================================"
echo " 백엔드: http://localhost:8080"
echo " Swagger: http://localhost:8080/swagger-ui.html"
echo " 로그를 표시합니다. Ctrl+C로 종료 (서버는 유지됨)"
echo "================================================"
echo ""
docker compose -f docker-compose.local.yml logs -f backend
