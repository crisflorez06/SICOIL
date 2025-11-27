# Multi-stage build: primero compilamos el proyecto y luego copiamos el JAR resultante
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app
COPY --from=build /app/target/papeleria-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
