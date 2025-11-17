FROM maven:3.9.11-eclipse-temurin-25-alpine AS builder

WORKDIR /app/

COPY pom.xml /app/pom.xml
COPY /src/ /app/src/

RUN mvn -DskipTests package -DskipTests -B

FROM openjdk:25-ea-jdk

WORKDIR /app/

COPY --from=builder /app/target/*.jar app.jar

ENV MODEL_HOST="http://localhost:8081"
EXPOSE 8080

CMD [ "java", "-jar", "/app/app.jar" ]