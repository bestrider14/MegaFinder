FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /build
COPY pom.xml mvnw ./
COPY .mvn .mvn
RUN chmod +x mvnw
COPY src src
RUN ./mvnw package -DskipTests

FROM eclipse-temurin:21-jre

WORKDIR /app
COPY --from=build /build/target/demo-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
