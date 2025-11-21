FROM maven:3.9.11-eclipse-temurin-25-alpine AS builder

WORKDIR /app/

ARG GITHUB_ACTOR
ARG GITHUB_TOKEN
ENV GITHUB_ACTOR=${GITHUB_ACTOR}
ENV GITHUB_TOKEN=${GITHUB_TOKEN}

COPY settings.xml /root/.m2/settings.xml
COPY pom.xml /app/pom.xml
COPY /src/ /app/src/

RUN mvn -s /root/.m2/settings.xml -DskipTests package -B

FROM openjdk:25-ea-jdk

WORKDIR /app/

COPY --from=builder /app/target/*.jar app.jar

ENV BACKEND_URL="http://localhost:8081"
ENV SERVER_PORT=8080
EXPOSE ${SERVER_PORT}

CMD ["java", "-jar", "/app/app.jar"]