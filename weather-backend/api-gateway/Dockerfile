FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /app

COPY pom.xml .
COPY weather-core/pom.xml weather-core/pom.xml
COPY weather-service/pom.xml weather-service/pom.xml
COPY api-gateway/pom.xml api-gateway/pom.xml

RUN mvn dependency:go-offline -pl api-gateway

COPY api-gateway/src api-gateway/src
RUN mvn clean package -pl api-gateway -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=builder /app/api-gateway/target/api-gateway-*.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-Dspring.profiles.active=${SPRING_PROFILES_ACTIVE:-dev}", "-jar", "app.jar"]