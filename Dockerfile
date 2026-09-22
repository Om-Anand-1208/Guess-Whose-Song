# ─── Build stage ───────────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jdk-jammy AS builder
WORKDIR /app

# Copy Gradle wrapper and build files first (layer caching)
COPY gradle/ gradle/
COPY gradlew .
COPY gradle/wrapper/gradle-wrapper.jar gradle/wrapper/
COPY settings.gradle.kts .
COPY build.gradle.kts .
COPY gradle/libs.versions.toml gradle/

# Copy all modules
COPY shared/ shared/
COPY server/ server/

# Build the fat JAR
RUN ./gradlew :server:shadowJar --no-daemon --stacktrace

# ─── Runtime stage ─────────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Copy only the fat JAR from the build stage
COPY --from=builder /app/server/build/libs/guess-whose-song-server.jar app.jar

EXPOSE 8080

# Health check for Render
HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
    CMD curl -f http://localhost:8080/health || exit 1

CMD ["java", "-jar", "app.jar"]

