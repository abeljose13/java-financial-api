# syntax=docker/dockerfile:1

# ---- Build stage ----
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Copy Gradle wrapper and project descriptors first to leverage Docker cache
COPY gradle/ gradle/
COPY gradlew .
COPY build.gradle settings.gradle ./

# Copy source code
COPY src ./src

# Build the executable JAR
RUN ./gradlew bootJar --no-daemon

# ---- Runtime stage ----
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Create a non-root user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copy only the generated JAR from the builder stage
COPY --from=builder /app/build/libs/*.jar app.jar

USER appuser

EXPOSE 8080

# Run the application. Database credentials and URL are injected at runtime
# via environment variables (POSTGRES_URL, POSTGRES_USER, POSTGRES_PASSWORD).
ENTRYPOINT ["java", "-jar", "app.jar"]
