# ---- Build stage ----
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

RUN chmod +x gradlew && ./gradlew dependencies --no-daemon -q

COPY src src
RUN ./gradlew bootJar --no-daemon -x test -q

# ---- Runtime stage ----
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# 앱은 타임존이 없는 LocalDateTime 을 그대로 저장하고 응답한다. 즉 컨테이너 시각이 곧
# 서비스 시각이다. 베이스 이미지 기본값이 UTC 라서 지정하지 않으면 사용자에게 보이는
# 모든 시각이 9시간 어긋나고, 날짜 기준 로직(출석·일일 보상)은 KST 09시에 하루가 바뀐다.
# 이미지에 담아 두면 로컬이든 운영이든 어디서 띄워도 같은 시각을 쓴다.
ENV TZ=Asia/Seoul
RUN apk add --no-cache tzdata

COPY --from=builder /app/build/libs/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
