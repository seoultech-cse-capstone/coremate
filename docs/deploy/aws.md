# CoreMate AWS 배포 런북

운영 서버는 AWS 서울 리전(`ap-northeast-2`)의 **ECS Fargate** 에서 돈다. 이 문서는 배포 구조, 배포·롤백 방법,
시크릿·로그 위치, 비용과 주의 사항을 모은 것이다. 리소스 이름의 `dev` 는 6월 1차 세팅 때 붙인 환경 이름이고,
지금은 이것이 유일한 운영 환경이다.

## 한눈에 보기

```
앱 ─HTTPS→ CloudFront (dupz4in9l5zw6.cloudfront.net, 캐시 끔)
            └→ 내부 ALB coremate-dev-alb (퍼블릭 IP 없음, CloudFront 에서 오는 요청만 받음)
                └→ ECS 서비스 coremate-dev-backend (Fargate Spot, 태스크 1개)
                     ├─ backend    Spring Boot :8080
                     ├─ ai-server  FastAPI 127.0.0.1:8000 (백엔드만 호출)
                     └─ redis      127.0.0.1:6379 (인증번호·스케줄 락)
                    └→ RDS MySQL 8.4 coremate-dev-rds (private DB 서브넷)
```

- **API 주소**: `https://dupz4in9l5zw6.cloudfront.net`
- 태스크는 퍼블릭 서브넷에 퍼블릭 IP 를 달고 뜬다. NAT 게이트웨이(월 $40+) 없이 외부 호출(OpenAI·FCM·Gmail·소셜 로그인)을
  하기 위해서다. 보안 그룹이 ALB 에서 오는 8080 만 허용하므로 밖에서 태스크로 직접 들어올 수는 없다.
- HTTPS 는 도메인이 없어 CloudFront 기본 인증서를 쓴다. http 요청은 https 로 리다이렉트되고 HSTS 헤더가 붙는다.

## 배포

**main 에 머지하면 자동 배포된다.** `.github/workflows/deploy.yml` 이 이미지를 빌드해 ECR 에 올리고
(`coremate-dev-backend`, `coremate-dev-ai-server`, 태그 = 커밋 SHA), `infra/aws/ecs-task-definition.json` 에
그 이미지를 넣어 새 태스크 정의를 등록한 뒤 서비스를 교체한다.

- 개발은 지금처럼 dev 로 PR·머지하고, 서버에 반영할 때 **dev → main** PR 을 머지한다.
- `apps/backend/**`, `tools/ai-pipeline/**`, `infra/aws/**`, 워크플로 파일이 바뀐 머지만 배포한다(모바일·문서만 바뀌면 건너뜀).
- 수동 재배포: GitHub → Actions → **Deploy to AWS (ECS)** → Run workflow (main).
- 새 태스크가 ALB 헬스 체크(`GET /api/v1/content/subjects` 200)를 통과해야 배포가 끝난다. 실패하면 ECS 배포 서킷
  브레이커가 이전 버전으로 되돌린다. 백엔드 기동은 0.5 vCPU 에서 약 90초(첫 배포 실측 87.7초)라, 워크플로 시작부터
  새 버전이 응답하기까지 10분 안팎 걸린다.
- 첫 배포: 2026-09-28, PR #288 (태스크 정의 `coremate-dev-backend:2`).

### 롤백

- 가장 쉬운 길: 문제 커밋을 되돌리는 PR 을 main 에 머지한다(자동 재배포).
- 급할 때: ECS 콘솔 → 클러스터 `coremate-dev-cluster` → 서비스 `coremate-dev-backend` → **서비스 업데이트** →
  태스크 정의 개정을 이전 번호로 골라 배포.

## 시크릿·설정 (Parameter Store)

ECS 가 태스크를 띄울 때 `/coremate/dev/...` 값을 꺼내 환경변수로 넣는다. **값을 바꾼 뒤에는 새 태스크가 떠야 반영된다**
(수동 재배포 또는 ECS 서비스 업데이트의 "새 배포 강제").

| 파라미터 | 넣는 곳 | 비고 |
|---|---|---|
| `/coremate/dev/db/url` | `SPRING_DATASOURCE_URL` | RDS 주소, `sslMode=REQUIRED` |
| `/coremate/dev/db/password` | `SPRING_DATASOURCE_PASSWORD` | CloudShell 에서 무작위 생성. 사용자는 `coremate_admin` |
| `/coremate/dev/backend/jwt-secret` | `JWT_SECRET` | base64. 바꾸면 모든 로그인이 풀린다 |
| `/coremate/dev/backend/google-client-id` | `GOOGLE_CLIENT_ID` | 공개값(앱 webClientId 와 같음) |
| `/coremate/dev/backend/mail-username` / `mail-password` | `MAIL_USERNAME` / `MAIL_PASSWORD` | 인증 메일 발신용 Gmail 과 앱 비밀번호 |
| `/coremate/dev/backend/firebase-credentials-json` | `FIREBASE_CREDENTIALS_JSON` | FCM 서비스 계정 JSON 전체 |
| `/coremate/dev/ai/openai-api-key` | `OPENAI_API_KEY` (ai-server) | |

⚠️ Firebase·메일·OpenAI 값이 틀려도 서버는 **기동한다**(해당 기능만 조용히 실패). 값을 바꿨다면 아래 로그에서 확인할 것.

## 로그

CloudWatch 로그 그룹 `/ecs/coremate-dev-backend` (14일 보관). 스트림 접두어로 컨테이너를 구분한다:
`backend/…`, `ai-server/…`, `redis/…`, `db-import/…`.

- 정상 기동: `Started BackendApplication`
- 푸시 알림 준비: `FCM 전송기를 사용한다` (없고 `Firebase 가 초기화되지 않아…` 가 보이면 Firebase 시크릿 확인).
  바로 앞의 `Firebase 초기화 완료. projectId=null` 은 정상이다 — 옵션에 프로젝트 ID 를 따로 넣지 않아 null 로 찍힐 뿐,
  발송은 서비스 계정 JSON 의 프로젝트로 나간다(로컬도 같다).
- CloudShell 에서 보기: `aws logs tail /ecs/coremate-dev-backend --since 30m --log-stream-name-prefix backend`

## 데이터베이스

- `coremate-dev-rds` — MySQL **8.4** (8.0 은 2026-07-31 표준 지원 종료라 새로 만들면 Extended Support 과금),
  `db.t4g.micro`, 20GB gp3, Single-AZ, 백업 1일, 파라미터 그룹 `coremate-dev-mysql84-params`
  (`utf8mb4_unicode_ci`, `time_zone=Asia/Seoul`).
- private DB 서브넷에만 있고, 보안 그룹이 백엔드 태스크에서 오는 3306 만 허용한다. 로컬에서 직접 붙을 수 없다.
- 스키마와 정책 시드는 **Flyway 마이그레이션**(`apps/backend/src/main/resources/db/migration`)이 관리한다. 새 태스크가
  뜰 때 아직 적용하지 않은 파일을 실행해 `flyway_schema_history` 에 남기고, 이어서 Hibernate 가 엔티티와 테이블이 맞는지
  검사한다(`ddl-auto: validate`). 안 맞으면 태스크가 뜨지 않고 서킷 브레이커가 이전 버전으로 되돌린다. 작성 규칙은 그 폴더의 `README.md`.
- ⚠️ **앱이 이전 버전으로 돌아가도 DB 는 돌아가지 않는다.** 적용된 마이그레이션은 남는다(Flyway 무료판은 되돌리기가 없다).
  그래서 마이그레이션은 직전 버전 코드와 같이 돌아도 되게 쓰고, 컬럼 삭제·이름 변경은 두 번의 배포로 나눈다.

### Flyway 전환 (도입 후 첫 배포)

Flyway 를 넣고 처음 main 에 배포하면, 운영 DB 는 테이블은 있는데 이력 테이블이 없다. 백엔드가 이 상태를 `V1__baseline` 이
적용된 것으로 기록하고(`spring.flyway.baseline-on-migrate`) 정책 시드(`R__`)를 한 번 실행한다(멱등이라 값은 그대로).

전제는 **운영 스키마 = V1** 이다. 배포 전에 아래 방법으로 `scripts/db/schema_fingerprint.sql` 을 운영에 돌려, 빈 MySQL 8.4 에
Flyway 로 만든 DB 에서 같은 파일을 돌린 결과와 MD5 가 같은지 본다(2026-09-30 기준값 `e174a734c3b315f1d46f0aae662d49ea`, 51개 테이블).
전환이 끝나면 `application.yaml` 의 `baseline-on-migrate` 를 지운다.

**2026-09-30 대조 결과: 일치.** 운영 51개 테이블 지문이 기준값과 한 줄도 다르지 않았다(MD5 `e174a734…` 동일).

### 운영 DB 에 조회 돌리기

RDS 에는 로컬에서 붙을 수 없어서, 일회성 태스크 `coremate-dev-db-import` 의 두 컨테이너 command 를 `--overrides` 로 바꿔
돌린다. `fetch` 는 `--version` 으로 바꿔 아무것도 받지 않게 하고, `import` 는 SQL 만 실행하게 한다. 결과는 로그 스트림
`db-import/import/…` 에 남는다(한 번에 1~2분).

⚠️ **command 를 덮어쓰지 않고 돌리면 아래 「초기 데이터」 덤프를 운영 DB 에 다시 붓는다(기존 데이터를 덮어씀).**
`import` 의 기본 command 가 `gunzip -c /work/coremate-baseline.sql.gz | mysql ... coremate` 다.

CloudShell 에서 돌린다. SQL 은 파일로 올리고(작업 → 파일 업로드) 환경변수로 넘긴다 — 셸 따옴표에 걸리지 않고,
overrides 는 8KB 까지라 주석 줄은 뺀다. 2026-09-30 스키마 대조를 이 형태로 돌렸다.

```bash
grep -v '^--' query.sql > q.sql
jq -n --rawfile sql q.sql '{containerOverrides:[
  {name:"fetch",  command:["--version"]},
  {name:"import", command:["echo Q-BEGIN; MYSQL_PWD=\"$DB_PASSWORD\" mysql -h \"$DB_HOST\" -u coremate_admin -N coremate -e \"$Q\"; echo \"Q-END rc=$?\""],
                  environment:[{name:"Q", value:$sql}]}]}' > ov.json
jq -r '.containerOverrides[] | "\(.name) => \(.command | join(" "))"' ov.json   # 실행 전에 눈으로 확인
grep -c -E 'gunzip|/work' ov.json                                                # 0 이어야 한다

T=$(aws ecs run-task --cluster coremate-dev-cluster --task-definition coremate-dev-db-import --launch-type FARGATE \
  --network-configuration 'awsvpcConfiguration={subnets=[subnet-08c7d3c717f2d8cb7,subnet-018c335c9a65b8185],securityGroups=[sg-03d7b22381ec12c08],assignPublicIp=ENABLED}' \
  --overrides file://ov.json --query 'tasks[0].taskArn' --output text)
aws ecs wait tasks-stopped --cluster coremate-dev-cluster --tasks "$T"
aws logs get-log-events --log-group-name /ecs/coremate-dev-backend --log-stream-name "db-import/import/${T##*/}" \
  --start-from-head --query 'events[].message' --output json | jq -r '.[]' > out.txt
sed -n '/^Q-BEGIN$/,/^Q-END/p' out.txt | sed '1d;$d'   # 결과만. 마지막 줄 Q-END rc=0 이 mysql 성공
```

스키마 대조는 `query.sql` 자리에 `scripts/db/schema_fingerprint.sql` 을 넣고, 마지막 줄에 `| md5sum` 을 붙여 기준값과 비교한다.

- 적용 이력: `SELECT installed_rank, version, description, type, success, installed_on FROM flyway_schema_history ORDER BY installed_rank;`
- **마이그레이션이 중간에 실패했을 때**: MySQL DDL 은 문장마다 바로 확정되므로 실패한 파일의 앞부분은 이미 적용돼 있고,
  이력에 `success = 0` 행이 남아 이후 배포가 전부 막힌다. 반쯤 적용된 변경을 손으로 되돌리고,
  `DELETE FROM flyway_schema_history WHERE success = 0;` 로 실패 기록을 지운 뒤, 고친 파일로 다시 배포한다
  (그 파일이 처음부터 다시 돈다. 마저 적용해 버리면 재실행이 "이미 있음"으로 또 실패한다).

### 초기 데이터 (2026-09-28 적재)

로컬 DB 의 **콘텐츠만** 옮겼다(회원·풀이 기록 제외). 테이블 51, 문항 3,300, 개념 473, 오개념 1,852.

1. 로컬에서 MySQL 8.4 컨테이너에 현재 백엔드를 띄워 스키마·시드를 만들고, 기존 로컬 DB 의 콘텐츠 9개 테이블
   (`subject`, `category_lv1`, `category_lv2`, `concept`, `concept_relation`, `common_mistake`, `question`,
   `question_diagnostic_profile`, `concept_diagnostic_profile`)을 부은 뒤 통째로 `mysqldump` 한다.
2. 덤프(`coremate-baseline.sql.gz`)를 `s3://coremate-dev-assets-yongju/ops/` 에 올린다.
3. 일회성 태스크 정의 `coremate-dev-db-import` 를 실행한다. `fetch` 컨테이너(aws-cli)가 S3 에서 받고
   `import` 컨테이너(mysql:8.4)가 RDS 에 넣은 뒤 건수를 로그(`db-import/…`)에 남긴다.

```bash
aws ecs run-task --cluster coremate-dev-cluster --task-definition coremate-dev-db-import --launch-type FARGATE \
  --network-configuration 'awsvpcConfiguration={subnets=[subnet-08c7d3c717f2d8cb7,subnet-018c335c9a65b8185],securityGroups=[sg-03d7b22381ec12c08],assignPublicIp=ENABLED}'
```

⚠️ 이 덤프는 `CREATE TABLE` 을 포함한다. **이미 데이터가 쌓인 운영 DB 에 다시 돌리면 덮어쓴다.** 새 DB 를 만들 때만 쓴다.

## 앱을 운영 서버에 붙이기

`apps/mobile/.env`:

```
EXPO_PUBLIC_API_BASE_URL=https://dupz4in9l5zw6.cloudfront.net
```

`npm start` 때 도는 `sync-ip` 는 https 주소를 건드리지 않는다. 로컬 백엔드로 돌아가려면 `http://<PC LAN IP>:8080` 으로
되돌리면 된다. Metro 가 떠 있으면 완전히 껐다 켜야 반영된다.

## 안드로이드 릴리스 APK (팀 배포용)

운영 서버 주소가 들어간 설치 파일을 만든다. 주소는 빌드할 때 앱에 박히므로 명령행 환경변수로 넘긴다
(`.env` 의 LAN IP 보다 우선한다).

```bash
cd apps/mobile
npx expo prebuild -p android --no-install   # app.json 을 바꿨을 때만(알림 아이콘 등 네이티브 설정)
cd android
JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home \
ANDROID_HOME=$HOME/Library/Android/sdk \
EXPO_PUBLIC_API_BASE_URL=https://dupz4in9l5zw6.cloudfront.net \
./gradlew assembleRelease
```

- 결과: `apps/mobile/android/app/build/outputs/apk/release/app-release.apk` (약 160MB, Android 7.0 이상, 첫 빌드 약 20분).
- ⚠️ **JDK 21 로 빌드할 것.** Android Studio 내장 JDK(25)로는 CMake 설정 단계가 "restricted method" 경고로 실패한다.
- 서명은 템플릿 기본 debug 키다(개발 빌드와 같은 키라 카카오 키 해시 `Xo8WBi6jzSxKDVR4drqm84yr9iU=` 가 그대로 통한다).
  Play 스토어에 올릴 때는 새 업로드 키를 만들어 안전하게 보관하고, 그 키의 SHA-1·키 해시를 카카오·구글 콘솔에 추가해야 한다.
- 카카오·네이버 로그인은 각 개발자 콘솔에서 테스트 앱(카카오)·개발 중(네이버) 상태라 **등록된 테스터만** 된다.
  출시 전에 카카오는 본 앱 키로 바꿔 다시 빌드하고, 네이버는 검수를 받아야 한다.

## 비용 (2026-09 기준, 월)

| 항목 | 금액 |
|---|---|
| Fargate Spot 0.5 vCPU / 2GB | 약 $7~10 (On-demand 면 $24.5) |
| 내부 ALB | 약 $17 |
| RDS t4g.micro + 20GB | 약 $21 |
| 태스크 퍼블릭 IPv4 | $3.7 |
| CloudFront·SSM·로그·ECR | 약 $1 |
| **합계** | **약 $50** |

- 계정은 **Free Plan** 이라 크레딧에서 차감된다. **2026-11-20 에 Free Plan 이 끝나면 계정이 닫히므로**, 그 뒤에도
  운영하려면 그 전에 Paid Plan 으로 전환해야 한다(남은 크레딧은 이어짐).
- **시연 날 끊기면 안 되면** 서비스를 On-demand 로 바꾼다: ECS 서비스 업데이트 → 용량 공급자 전략을 `FARGATE` 로 → 새 배포 강제.
  Spot 은 드물게 AWS 가 태스크를 회수해 1~2분 끊길 수 있다.
- 잠시 끄기: 서비스 태스크 수를 0 으로(Fargate 비용 0). RDS 는 "일시 중지" 가능하지만 7일 뒤 자동으로 다시 켜진다.
  ALB 는 켜져 있는 동안 계속 과금된다.

## 주의 사항

- **태스크는 1개로 고정한다.** Redis 가 태스크 안 사이드카라 태스크마다 따로 있다. 2개 이상이면 인증번호 검증이
  다른 태스크로 가서 실패하고, 정기 알림 스케줄 락도 서로를 못 본다. 늘리려면 ElastiCache 가 먼저다.
- **CloudFront 는 응답을 최대 60초 기다린다.** 약점 분석표 생성(실측 27~36초)이 60초를 넘으면 앱은 504 를 받고,
  서버는 분석표를 끝까지 만든다.
- Swagger 는 배포 프로필(`application-dev.yaml`)에서 꺼져 있다.

## 리소스 목록

| 종류 | 이름 / ID |
|---|---|
| VPC | `coremate-dev-vpc` (`vpc-0ae828438c16749d7`) |
| 태스크 서브넷(퍼블릭) | `subnet-08c7d3c717f2d8cb7` (2a), `subnet-018c335c9a65b8185` (2c) |
| ALB 서브넷(private app) | `subnet-022aef12ef218ebba` (2a), `subnet-05b72d5f2beaba0d9` (2c) |
| DB 서브넷 그룹 | `coremate-dev-db-subnet-group` (private db 2a·2c) |
| 보안 그룹 | ALB `sg-01a1b4b9938ff7e9c`(80 ← CloudFront VPC 오리진 SG), 백엔드 `sg-03d7b22381ec12c08`(8080 ← ALB), RDS `sg-027a233346947d347`(3306 ← 백엔드) |
| CloudFront | 배포 `E6428KKTYDM87`, VPC 오리진 `coremate-dev-alb` |
| ALB / 대상 그룹 | `coremate-dev-alb` (internal, 유휴 타임아웃 180초) / `coremate-dev-backend-tg` (ip, 8080) |
| ECS | 클러스터 `coremate-dev-cluster`, 서비스·태스크 정의 `coremate-dev-backend`, 일회성 `coremate-dev-db-import` |
| ECR | `coremate-dev-backend`, `coremate-dev-ai-server` (각각 최근 5개 유지) |
| RDS | `coremate-dev-rds`, 파라미터 그룹 `coremate-dev-mysql84-params` |
| IAM | 실행 역할 `coremate-dev-ecs-task-execution-role`(+`/coremate/dev/*` 읽기), 태스크 역할 `coremate-dev-backend-task-role`(S3·SSM), GitHub 역할 `coremate-dev-github-actions-role`(이 레포 main 만 신뢰, ECR push·ECS 배포·PassRole) |
| 로그 | CloudWatch `/ecs/coremate-dev-backend` (14일) |
