# ══════════════════════════════════════════════════════
#  Multi-stage Dockerfile — Pooler Backend
#  JDK 21 + Virtual Threads
# ══════════════════════════════════════════════════════

# ── Stage 1: Build ────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app
COPY pom.xml .
COPY src ./src

# Install Maven and build (skip tests for Docker image)
RUN apk add --no-cache maven \
    && mvn clean package -DskipTests -q

# ── Stage 2: Extract layers (Spring Boot layertools) ──
# - Check work dir and place this project that location then will be read
FROM eclipse-temurin:21-jdk-alpine AS layers
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
RUN java -Djarmode=tools -jar app.jar extract --layers --destination extracted

# ── Stage 3: Runtime image ────────────────────────────
FROM eclipse-temurin:21-jre-alpine AS runtime

# Security: non-root user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

# Copy layers in dependency-change order (best layer cache)
COPY --from=layers /app/extracted/dependencies/           ./
COPY --from=layers /app/extracted/spring-boot-loader/     ./
COPY --from=layers /app/extracted/snapshot-dependencies/  ./
COPY --from=layers /app/extracted/application/            ./

RUN chown -R appuser:appgroup /app
USER appuser

# JVM tuning for containers. SPRING_PROFILES_ACTIVE is supplied by Docker/EC2.
ENV SPRING_PROFILES_ACTIVE=prod
ENV JAVA_OPTS="-XX:+UseContainerSupport \
               -XX:MaxRAMPercentage=75.0 \
               -XX:+UseZGC \
               -Djava.security.egd=file:/dev/./urandom"

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
  CMD wget -qO- http://localhost:8080/pooler-backend/api/v1/public/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dspring.profiles.active=${SPRING_PROFILES_ACTIVE:-prod} -jar app.jar"]
