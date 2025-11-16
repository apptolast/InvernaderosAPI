# ============================================
# Stage 1: Build stage
# ============================================
FROM gradle:8.14.3-jdk21-alpine AS builder

WORKDIR /app

# Copy only dependency-related files first (for better layer caching)
COPY build.gradle.kts settings.gradle.kts ./
COPY gradle ./gradle

# Download dependencies (this layer will be cached if build files don't change)
RUN gradle dependencies --no-daemon || true

# Copy source code
COPY src ./src

# Build the application (skip tests for faster builds, run tests in CI/CD)
RUN gradle clean build -x test --no-daemon

# ============================================
# Stage 2: Runtime stage
# ============================================
FROM eclipse-temurin:21-jre-alpine

LABEL maintainer="AppToLast <info@apptolast.com>"
LABEL description="Invernaderos API - IoT Greenhouse Management System"
LABEL version="0.0.1-SNAPSHOT"

# Create non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring

# Install curl for health checks
RUN apk add --no-cache curl

WORKDIR /app

# Copy the built JAR from builder stage
COPY --from=builder /app/build/libs/*.jar app.jar

# Change ownership to spring user
RUN chown -R spring:spring /app

# Switch to non-root user
USER spring:spring

# Expose application port
EXPOSE 8080

# Environment variables (can be overridden)
ENV JAVA_OPTS="-Xms256m -Xmx512m" \
    SPRING_PROFILES_ACTIVE="default"

# Health check using actuator endpoint
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Run the application
# Note: application.yaml should be mounted as a volume or ConfigMap in Kubernetes
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
