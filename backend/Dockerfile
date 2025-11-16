# Multi-stage build for Spring Boot backend
FROM maven:3.9-eclipse-temurin-21-alpine AS build

WORKDIR /app

# Copy Maven files
COPY pom.xml .

# Download dependencies (cache layer)
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build application
RUN mvn clean package -DskipTests

# Runtime stage (must use JDK for Java Compiler API)
FROM eclipse-temurin:21-jdk-alpine

# Install curl for health checks
RUN apk add --no-cache curl

WORKDIR /app

# Create non-root user
RUN addgroup -g 1001 appgroup && \
    adduser -D -u 1001 -G appgroup appuser

# Create directories
RUN mkdir -p /app/xsd-storage /app/temp/compile /app/temp/classes /app/logs && \
    chown -R appuser:appgroup /app

# Copy JAR from build stage
COPY --from=build /app/target/*.jar app.jar

# Change ownership
RUN chown appuser:appgroup app.jar

# Switch to non-root user
USER appuser

# Expose port
EXPOSE 8080

# Set default JVM options (can be overridden by environment variable)
ENV JAVA_OPTS="-Xmx1g -Xms512m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# Run application with environment variable support
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
