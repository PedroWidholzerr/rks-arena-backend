FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /app

COPY backend/rks-system/.mvn .mvn
COPY backend/rks-system/mvnw .
COPY backend/rks-system/pom.xml .

RUN chmod +x mvnw
RUN ./mvnw -q -DskipTests dependency:go-offline

COPY backend/rks-system/src src

RUN ./mvnw -q -DskipTests package

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

CMD ["java", "-jar", "app.jar"]