# Multi-stage build for the Aegis legacy monolith.
# Deliberately built on a Java 8 base image to match production.
FROM maven:3.8-openjdk-8 AS build
WORKDIR /build
COPY pom.xml .
# Warm the dependency cache first for faster incremental builds.
RUN mvn -B -q dependency:go-offline || true
COPY src ./src
RUN mvn -B -q clean package -DskipTests

FROM eclipse-temurin:8-jre
WORKDIR /app
RUN mkdir -p /var/aegis/uploads
COPY --from=build /build/target/aegis-claims-platform.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
