# --- Stage 1: Build the Spring Boot application ---
# Use a JDK base image with Maven to build the project
FROM maven:3.9.5-eclipse-temurin-21-alpine AS build

LABEL authors="Mbonisi Mpala"

WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn clean package -DskipTests

# --- Stage 2: Create the final, lightweight image ---
FROM eclipse-temurin:21-jre-alpine

# Upgrade Alpine packages to patch OS-level CVEs
RUN apk upgrade --no-cache

COPY --from=build /app/target/*.jar /app/app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
