# --- Build Stage ---
FROM gradle:9.5.1-jdk17 AS build
WORKDIR /app

COPY . .
RUN chmod +x gradlew
RUN ./gradlew build -x test --no-daemon

# --- Run Stage ---
FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080

# DB_URL/DB_USERNAME/DB_PASSWORD (.env 참고, 예: Aiven MySQL)를 런타임에 주입해야 한다.
# 컨테이너 안에서는 application.properties 기본값(localhost:3306)이 컨테이너 자기 자신을 가리켜 연결되지 않으므로
# 반드시 --env-file .env 또는 -e DB_URL=... -e DB_USERNAME=... -e DB_PASSWORD=... 로 실행할 것.
ENTRYPOINT ["java", "-jar", "app.jar"]
