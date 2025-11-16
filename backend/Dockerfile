FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /app

# Copy Maven files
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .

# Download dependencies
RUN ./mvnw dependency:go-offline -B

# Copy source code
COPY src ./src

# Build application
RUN ./mvnw clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Install required tools (for Java Compiler API - we need JDK, not JRE)
# So we'll use JDK image instead
FROM eclipse-temurin:21-jdk-alpine

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

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Run application
ENTRYPOINT ["java", "-Xmx1g", "-Xms512m", "-jar", "app.jar"]
