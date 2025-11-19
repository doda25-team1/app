FROM maven:3.9.11-eclipse-temurin-25-alpine AS builder

WORKDIR /app/

COPY pom.xml /app/pom.xml
COPY /src/ /app/src/

RUN mvn -DskipTests package -DskipTests -B

FROM openjdk:25-ea-jdk

WORKDIR /app/

COPY --from=builder /app/target/*.jar app.jar

ENV BACKEND_URL="http://model-service:8081"
ENV SERVER_PORT=8080
EXPOSE ${SERVER_PORT}

CMD ["sh", "-c", "java -jar -Dserver.port=${SERVER_PORT} /app/app.jar"]