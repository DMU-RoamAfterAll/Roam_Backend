# 1. Java 17 베이스 이미지
FROM eclipse-temurin:17-jdk AS builder

# 2. 작업 디렉토리 생성
WORKDIR /app

# 3. Gradle 캐시 최적화를 위해 필요한 파일 먼저 복사
COPY build.gradle settings.gradle gradlew /app/
COPY gradle /app/gradle

# 4. Gradle wrapper로 의존성 먼저 다운
RUN ./gradlew dependencies --no-daemon || return 0

# 5. 소스 복사
COPY . /app

# 6. Spring Boot 빌드 (fat jar 생성)
RUN ./gradlew bootJar --no-daemon

# -----------------------
# 실행 이미지 (최종)
# -----------------------
FROM eclipse-temurin:17-jdk

# 1. 작업 디렉토리
WORKDIR /app

# 2. builder 단계에서 빌드된 jar 가져오기
COPY --from=builder /app/build/libs/*.jar app.jar

# 3. 실행 (포트 8080)
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
