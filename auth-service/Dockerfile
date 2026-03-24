# Build stage
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Copy Maven wrapper and pom files
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY schemas/pom.xml schemas/
COPY auth-service/pom.xml auth-service/

# Download dependencies
RUN chmod +x mvnw && ./mvnw dependency:go-offline -pl auth-service -am -B

# Copy source code
COPY schemas/src schemas/src
COPY auth-service/src auth-service/src

# Build the application
RUN ./mvnw package -pl auth-service -am -DskipTests -B

# Runtime stage
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Add non-root user for security
RUN addgroup -g 1001 appgroup && adduser -u 1001 -G appgroup -D appuser

# Copy the built JAR
COPY --from=builder /app/auth-service/target/*.jar app.jar

# Change ownership
RUN chown -R appuser:appgroup /app

USER appuser

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8081/actuator/health || exit 1

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]
