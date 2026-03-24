FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY schemas/pom.xml schemas/
COPY notification-service/pom.xml notification-service/
RUN chmod +x mvnw && ./mvnw dependency:go-offline -pl notification-service -am -B
COPY schemas/src schemas/src
COPY notification-service/src notification-service/src
RUN ./mvnw package -pl notification-service -am -DskipTests -B

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -g 1001 appgroup && adduser -u 1001 -G appgroup -D appuser
COPY --from=builder /app/notification-service/target/*.jar app.jar
RUN chown -R appuser:appgroup /app
USER appuser
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8085/actuator/health || exit 1
EXPOSE 8085
ENTRYPOINT ["java", "-jar", "app.jar"]
