# CoreMate

AI 사용자 약점 분석과 아바타 성장 요소를 결합한 CS 전공지식 학습 플랫폼.

## Project Structure

coremate/
  apps/
    backend/          Spring Boot backend
    mobile/           React Native Expo app
  tools/
    ai-pipeline/      Python content generation pipeline
  docs/               Product, API, DB, AI docs
  infra/              Local/AWS infrastructure settings
  scripts/            Seed and local scripts

## AI Pipeline Setup

cd tools/ai-pipeline
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
cp .env.example .env

After copying .env.example to .env, fill in your actual OpenAI API key.

## Do Not Commit

.venv/
.env
__pycache__/
data/raw/
data/extracted/
outputs/
