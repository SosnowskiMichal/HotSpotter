FROM maven:3.9.11-eclipse-temurin-25-alpine AS builder

WORKDIR /app

COPY . .

RUN mvn clean package -DskipTests


FROM openjdk:25-ea-25-jdk-slim

WORKDIR /app

ARG NODE_VERSION=24
ARG SONAR_SCANNER_VERSION=7.3.0.5189
ARG SONAR_SCANNER_HOME=/opt/sonar-scanner

RUN apt-get update \
 && apt-get install -y curl gnupg ca-certificates \
 && curl -fsSL "https://deb.nodesource.com/setup_${NODE_VERSION}.x" | bash - \
 && apt-get install -y nodejs git cloc unzip \
 && rm -rf /var/lib/apt/lists/* \
 && curl -sSL "https://binaries.sonarsource.com/Distribution/sonar-scanner-cli/sonar-scanner-cli-${SONAR_SCANNER_VERSION}.zip" -o /tmp/sonar-scanner.zip \
 && unzip /tmp/sonar-scanner.zip -d /opt \
 && mv /opt/sonar-scanner-${SONAR_SCANNER_VERSION} ${SONAR_SCANNER_HOME} \
 && rm /tmp/sonar-scanner.zip \
 && ln -s ${SONAR_SCANNER_HOME}/bin/sonar-scanner /usr/local/bin/sonar-scanner

ENV SONAR_SCANNER_HOME=${SONAR_SCANNER_HOME}
ENV PATH="${PATH}:${SONAR_SCANNER_HOME}/bin"

ARG JAR_FILE=target/*.jar

COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]