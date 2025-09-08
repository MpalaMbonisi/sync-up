# --- Stage 1: Build the Spring Boot application ---
# Use a JDK base image with Maven to build the project
FROM maven:3.9.5-eclipse-temurin-21-alpine AS build

LABEL authors="Mbonisi Mpala"

WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn clean package -DskipTests

# --- Stage 2: Create the final, lightweight image ---
# This image contains only what's necessary to run the application, reducing its size
FROM eclipse-temurin:21-jre-alpine

COPY --from=build /app/target/*.jar /app/app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
