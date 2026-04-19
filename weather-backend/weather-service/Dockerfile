FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /app

COPY pom.xml .
COPY weather-core/pom.xml weather-core/pom.xml
COPY weather-service/pom.xml weather-service/pom.xml
COPY api-gateway/pom.xml api-gateway/pom.xml
# -am builds also the dependecies of weather-service(in this case weather-core)
RUN mvn dependency:go-offline -pl weather-service -am

COPY weather-core/src weather-core/src
COPY weather-service/src weather-service/src
RUN mvn clean package -pl weather-service -am -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=builder /app/weather-service/target/weather-service-*.jar app.jar

EXPOSE 8082

ENTRYPOINT ["java", "-Dspring.profiles.active=${SPRING_PROFILES_ACTIVE:-dev}", "-jar", "app.jar"]
