# syntax=docker/dockerfile:1
# Jamix catalog-service (Spring Boot 4, Java 21). Build:  docker build -t jamix-catalog .
# Run locally:  docker run -p 8080:8080 --env-file .env jamix-catalog

# ---------- 1. Build: compile and package the jar ---------------------------------------------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Dependencies first: this layer is cached until pom.xml changes.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
# Tests run in CI / locally (npm test); skipping them keeps image builds fast on small builders.
RUN mvn -B -q package -DskipTests \
 && cp target/catalog-service-*.jar application.jar \
 # Split the jar into layers: dependencies change rarely, our code often -> faster pushes/pulls.
 && java -Djarmode=tools -jar application.jar extract --layers --destination extracted

# ---------- 2. Run: small JRE image, non-root ---------------------------------------------------
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S jamix && adduser -S jamix -G jamix
USER jamix

COPY --from=build --chown=jamix:jamix /build/extracted/dependencies/ ./
COPY --from=build --chown=jamix:jamix /build/extracted/spring-boot-loader/ ./
COPY --from=build --chown=jamix:jamix /build/extracted/snapshot-dependencies/ ./
COPY --from=build --chown=jamix:jamix /build/extracted/application/ ./

# Render/Railway/Fly inject PORT; application.yml reads it (server.port=${PORT:8080}).
ENV PORT=8080
EXPOSE 8080

# Sized for small containers (e.g. 512 MB free tiers): heap = 75% of the container's RAM,
# SerialGC has the lowest memory overhead for a single small instance.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+UseSerialGC -XX:+ExitOnOutOfMemoryError"

HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=3 \
  CMD wget -qO /dev/null "http://127.0.0.1:${PORT}/api/v1/products?category=ella" || exit 1

ENTRYPOINT ["java", "-jar", "application.jar"]
