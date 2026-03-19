FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /app/rks-system

COPY rks-system/.mvn .mvn
COPY rks-system/mvnw .
COPY rks-system/pom.xml .

RUN chmod +x mvnw
RUN ./mvnw -q -DskipTests dependency:go-offline

COPY rks-system/src src

RUN ./mvnw -q -DskipTests package

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /app/rks-system/target/*.jar app.jar

CMD ["java", "-jar", "app.jar"]