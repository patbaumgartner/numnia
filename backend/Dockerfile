# syntax=docker/dockerfile:1.7
# Numnia backend — Spring Boot 4.0.6 / Java 25 LTS
# Multi-stage build: Maven build → minimal JRE runtime.

# ─── Stage 1: build ───────────────────────────────────────────────
# Use a Maven+JDK image because the repo's mvnw is a thin stub that
# delegates to a system `mvn` (no wrapper jar in .mvn/wrapper).
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /workspace

# Copy POM first for better layer caching of dependencies
COPY pom.xml ./
RUN mvn -B -ntp dependency:go-offline

# Copy sources and build
COPY src ./src
RUN mvn -B -ntp -DskipTests package \
    && mkdir -p target/extracted \
    && java -Djarmode=tools -jar target/*.jar extract --layers --launcher --destination target/extracted

# ─── Stage 2: runtime ─────────────────────────────────────────────
FROM eclipse-temurin:25-jre AS runtime
WORKDIR /app

# Non-root user (security baseline — NFR-SEC)
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system --gid 1001 numnia \
    && useradd  --system --uid 1001 --gid numnia --home /app numnia

# Layered jar for fast incremental deploys
COPY --from=build --chown=numnia:numnia /workspace/target/extracted/dependencies/    ./
COPY --from=build --chown=numnia:numnia /workspace/target/extracted/spring-boot-loader/ ./
COPY --from=build --chown=numnia:numnia /workspace/target/extracted/snapshot-dependencies/ ./
COPY --from=build --chown=numnia:numnia /workspace/target/extracted/application/    ./

USER numnia
EXPOSE 8080

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"
ENV SPRING_PROFILES_ACTIVE=docker

HEALTHCHECK --interval=15s --timeout=5s --start-period=40s --retries=10 \
    CMD curl -fsS http://127.0.0.1:8080/actuator/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["sh","-c","exec java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
