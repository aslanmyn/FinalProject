FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /build

COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q -DskipTests clean package

FROM eclipse-temurin:17-jre
WORKDIR /app

RUN addgroup --system app && adduser --system --ingroup app app

COPY --from=build /build/target/*.jar /app/app.jar

ENV APP_PROFILE=postgres \
    DB_URL=jdbc:postgresql://postgres:5432/final_project \
    DB_USER=postgres \
    DB_PASSWORD=change_me \
    APP_STORAGE_ROOT=/app/storage \
    JAVA_OPTS=""

EXPOSE 8080

RUN mkdir -p /app/storage && chown -R app:app /app
USER app

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dserver.port=${PORT:-${SERVER_PORT:-8080}} -jar /app/app.jar"]
