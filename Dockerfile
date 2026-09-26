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
ENV SERVER_ONLY=true
RUN ./gradlew :server:shadowJar --no-daemon --stacktrace

# ─── Runtime stage ─────────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Copy only the fat JAR from the build stage
COPY --from=builder /app/server/build/libs/guess-whose-song-server.jar app.jar

EXPOSE 8080

CMD ["java", "-jar", "app.jar"]
