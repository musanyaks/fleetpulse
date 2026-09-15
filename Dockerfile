# syntax=docker/dockerfile:1

# ---------- build stage ----------
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /build

# 1) POMs only -> dependency download layer stays cached until deps change.
#    ALL module POMs must exist or the reactor scan fails.
COPY pom.xml .
COPY common/pom.xml common/pom.xml
COPY services/ingestion-service/pom.xml services/ingestion-service/pom.xml
COPY services/telemetry-service/pom.xml services/telemetry-service/pom.xml
COPY simulator/pom.xml simulator/pom.xml
RUN mvn -q -B -DskipTests dependency:go-offline || true

# 2) Full source (target/ and .git excluded via .dockerignore) -> build everything
COPY . .
RUN mvn -q -B -DskipTests package

# ---------- runtime stage ----------
FROM eclipse-temurin:21-jre AS runtime
ARG MODULE_DIR
WORKDIR /app
COPY --from=build /build/${MODULE_DIR}/target/*-0.1.0.jar app.jar
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]