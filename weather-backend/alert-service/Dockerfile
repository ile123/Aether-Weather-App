FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /app
COPY pom.xml .
COPY weather-core/pom.xml weather-core/pom.xml
COPY weather-service/pom.xml weather-service/pom.xml
COPY api-gateway/pom.xml api-gateway/pom.xml
COPY alert-service/pom.xml alert-service/pom.xml
RUN mvn dependency:go-offline -pl alert-service -am
COPY weather-core/src weather-core/src
COPY alert-service/src alert-service/src
RUN mvn clean package -pl alert-service -am -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/alert-service/target/alert-service-*.jar app.jar

EXPOSE 8083

ENTRYPOINT ["sh", "-c", "java -Dspring.profiles.active=${SPRING_PROFILES_ACTIVE:-dev} -jar app.jar"]