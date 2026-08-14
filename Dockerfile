# syntax=docker/dockerfile:1
#
# Two-stage build for the Spring Boot backend:
#   1. build   - compiles the app and packages it into a jar with Maven
#   2. runtime - copies just the jar into a slim JRE image to keep the
#                final image small and free of build tooling/source
#
# Build:  docker build -t smartcart-backend .
# Run:    docker run -p 8080:8080 --env SPRING_DATASOURCE_PASSWORD=*** smartcart-backend

# ---------------------------------------------------------------------------
# Stage 1: build
# ---------------------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copy only the pom first so dependency resolution is cached in its own
# layer and only reruns when pom.xml actually changes.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q clean package -DskipTests

# ---------------------------------------------------------------------------
# Stage 2: runtime
# ---------------------------------------------------------------------------
FROM eclipse-temurin:17-jre-alpine AS runtime
WORKDIR /app

# Run as a non-root user
RUN addgroup -S spring && adduser -S spring -G spring
COPY --from=build /app/target/*.jar app.jar
RUN chown spring:spring app.jar
USER spring

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]