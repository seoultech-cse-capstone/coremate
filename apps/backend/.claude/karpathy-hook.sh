#!/bin/bash
python3 -c "
import json
guidelines = '''## Karpathy Coding Guidelines (자동 로드됨)

### 1. Think Before Coding
- 구현 전 가정을 명시적으로 말할 것
- 여러 해석이 가능하면 제시하고 선택 요청 (묵묵히 선택 X)
- 더 단순한 방법이 있으면 말할 것
- 헷갈리면 멈추고 질문할 것

### 2. Simplicity First
- 요청한 것만 구현. 추측성 기능 추가 금지
- 단일 사용 코드에 추상화 금지
- 불가능한 시나리오에 에러 핸들링 금지
- 200줄로 될 걸 50줄로 할 수 있으면 다시 작성

### 3. Surgical Changes
- 요청한 것만 수정. 인접 코드 개선 금지
- 기존 스타일 유지 (내 방식이 달라도)
- 내 변경으로 생긴 미사용 import/변수는 제거
- 기존 dead code는 언급만, 삭제는 요청 시에만

### 4. Goal-Driven Execution
- 검증 가능한 목표로 변환: \"검증: [확인방법]\"
- 여러 단계 작업은 간단한 계획 먼저 제시
- 강한 성공 기준을 정해야 독립적으로 루프 가능
'''
print(json.dumps({
    'hookSpecificOutput': {
        'hookEventName': 'SessionStart',
        'additionalContext': guidelines.strip()
    }
}))
"
