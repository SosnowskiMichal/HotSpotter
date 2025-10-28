FROM maven:3.9.11-eclipse-temurin-25-alpine AS builder

WORKDIR /app

COPY . .

RUN mvn clean package -DskipTests


FROM openjdk:25-jdk-slim

WORKDIR /app

RUN apt-get update && \
    apt-get install -y git cloc && \
    rm -rf /var/lib/apt/lists/*

ARG JAR_FILE=target/*.jar

COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]