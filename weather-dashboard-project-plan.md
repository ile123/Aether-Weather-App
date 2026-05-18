# Real-Time Weather Dashboard — Project Plan

## Project Overview

A microservices-based real-time weather dashboard demonstrating AOP, Reactive Programming, and Microservices architecture with Spring Boot backend and Angular frontend.

Open Meteo is used for weather data. No authentication — all endpoints are open. User identity is passed as a plain `X-User-Id` request header.

---

## Tech Stack

| Layer | Technology |
|---|---|
| **Backend** | Spring Boot 3.4.x, Spring Cloud 2024.x, Spring WebFlux, Spring Data R2DBC |
| **Frontend** | Angular 17+, RxJS, Angular Material |
| **Service Discovery** | Kubernetes DNS (built-in) |
| **API Gateway** | Spring Cloud Gateway (Reactive) |
| **Database** | PostgreSQL with R2DBC |
| **Message Broker** | Kafka (KRaft mode) |
| **Caching** | Redis |
| **Tracing** | Zipkin + Micrometer |
| **Containerization** | Docker Compose (local) · Kubernetes (prod) |

---

## Architecture

```
Angular 17+ SPA
      │
      ▼
API Gateway (Spring Cloud Gateway) :8081
      │
      ├──▶ Weather Service :8082
      │         └── Open-Meteo API (external)
      │         └── PostgreSQL (weather_db)
      │         └── Redis (cache)
      │
      └──▶ Alert Service :8083
                └── Kafka (weather-updates topic)
                └── PostgreSQL (alert_db)

Shared Infrastructure:
  PostgreSQL · Redis · Kafka · Zipkin
```

**How user identity works without auth**: Every request that needs a user ID (saved locations, alert rules) reads the `X-User-Id` header. The Angular frontend sets this header on every request using an HTTP interceptor. No tokens, no login flow, no Keycloak.

---




## Tickets

### TICKET-001: Project Structure and Parent POM

**Description**: Multi-module Maven project with parent POM.

**Tasks**:
- Parent pom.xml with Spring Boot 3.4.4 and packaging pom
- Spring Cloud BOM 2024.0.1 in dependencyManagement
- Shared dependencies: spring-boot-starter-validation, spring-boot-starter-test, mapstruct, lombok
- maven-compiler-plugin with mapstruct-processor and lombok annotation processors
- Modules: api-gateway, weather-core, weather-service

**How to verify**:
```bash
./mvnw clean validate
# Expected: BUILD SUCCESS
# (local build check — no cluster needed for this ticket)
```

---

### TICKET-002: Docker Compose Infrastructure

**Description**: Full local development infrastructure via Docker Compose.

**Services running**: PostgreSQL, PGAdmin, Kafka (KRaft), Kafka-UI, Redis, Zipkin

**Key decisions**:
- Bind mounts use :z suffix for SELinux compatibility on Fedora
- Named volumes for all stateful services
- depends_on with condition: service_healthy throughout

**How to verify**:
```bash
# Infrastructure runs inside Kubernetes — use build-images.sh
./build-images.sh
kubectl get pods -n weather-app
# All infrastructure pods should show Running
```

---

### TICKET-003: Kubernetes Setup

**Description**: Full K8s manifests with kustomize overlays.

**Structure**: k8s/base/ + k8s/overlays/dev|prod/

**Includes**: Namespace weather-app, ConfigMap, Secrets, all infrastructure Deployments and Services, ResourceQuota, LimitRange

**How to deploy**:
```bash
./build-images.sh
# Resets minikube, builds images, deploys all manifests, starts port-forwards
```

**Endpoints after deploy**:
- API Gateway: http://localhost:8081 (port-forward → gateway pod)
- Zipkin: http://localhost:9411 (add `kubectl port-forward -n weather-app svc/zipkin 9411:9411 &` to build-images.sh if needed)

---

### TICKET-004: API Gateway

**Description**: Spring Cloud Gateway with CORS and circuit breakers. No JWT validation.

**Port**: 8081

**Routes**: /api/weather/** to weather-service, /api/alerts/** to alert-service

**Key decisions**:
- No Redis rate limiting (incompatibility with Boot 3.4.x)
- Resilience4j circuit breakers with fallback controller
- CORS configured for http://localhost:4200
- All requests permitted — no auth filter

**How to verify**:
```bash
# build-images.sh must be running (port-forwards active)
curl "http://localhost:8081/api/weather/current?location=London"
# Expected: 200 with weather data going through the K8s gateway
```

---

### TICKET-005: weather-core Shared Library

**Description**: Shared Maven module used by all services.

**Contents**:
- DTOs: ApiResponse, ErrorResponse, ErrorItem, WeatherCurrentDto, WeatherForecastDto, WeatherLocationDto, SaveLocationRequest, GeocodingResponse, CurrentWeatherResponse, ForecastResponse
- Exceptions: WeatherException, ResourceNotFoundException, ValidationException, ExternalApiException, ServiceUnavailableException
- AOP Annotations: @LogExecutionTime, @ValidateLocation, @Audited
- AOP Aspects: LogExecutionTimeAspect, ValidateLocationAspect, AuditAspect
- Utils: DateUtils, ValidationUtils, WeatherDescriptionMapper
- Constants: ApiConstants, CacheConstants
- Enums: AlertType, AlertCondition, TemperatureUnit, WindSpeedUnit
- Config: WeatherMapperConfig (MapStruct base config)

**How to verify**:
```bash
./mvnw clean install -pl weather-core -DskipTests
# Local build check — weather-core has no runtime presence in K8s (it's a shared library)
```

---

### TICKET-006: Weather Service Setup

**Description**: Spring WebFlux + R2DBC service with Liquibase migrations.

**Port**: 8082

**Key config**:
- R2DBC for reactive DB access, JDBC only for Liquibase
- application.yml uses localhost defaults
- application-k8s.yml uses Kubernetes service names
- SecurityConfig: `.anyExchange().permitAll()` — no auth required

**How to verify**:
```bash
# Build and deploy to K8s
./build-images.sh

# Check the weather-service pod is running
kubectl get pods -n weather-app -l app=weather-service
# NAME                               READY   STATUS    RESTARTS
# weather-service-xxx                1/1     Running

# Check health through the gateway
curl http://localhost:8081/actuator/health
# Expected: {"status":"UP"}
```

---

### TICKET-007: Weather Service Domain Layer

**Description**: Entities, repositories, and Liquibase migrations for weather data.

**Entities**: WeatherLocation (includes user_id column), WeatherCurrent, WeatherForecast

**Repositories**: WeatherLocationRepository, WeatherCurrentRepository, WeatherForecastRepository

**How to verify**:
```bash
# Check tables exist in the K8s-hosted PostgreSQL
kubectl exec -n weather-app   $(kubectl get pods -n weather-app -l app=postgres -o jsonpath='{.items[0].metadata.name}')   -- psql -U postgres -d weather_db -c "\dt"
# Should show: weather_location, weather_current, weather_forecast
```

---

### TICKET-008: Open-Meteo API Client

**Description**: OpenMeteoClient using WebClient with caching, retries, and circuit breakers.

**Features**:
- Exponential backoff retry (3 attempts)
- Resilience4j circuit breaker with fallback
- Redis caching: geocoding 24h TTL, current weather 5min TTL, forecast 30min TTL

**How to verify**:
```bash
# Make a request through the K8s gateway to populate cache
curl "http://localhost:8081/api/weather/current?location=London"

# Check cache keys inside the K8s Redis pod
kubectl exec -n weather-app   $(kubectl get pods -n weather-app -l app=redis -o jsonpath='{.items[0].metadata.name}')   -- redis-cli KEYS "weather:*"
# Should show cached keys like weather:current:51.5:-0.1
```

---

### TICKET-09: Weather Service — Business Logic and AOP

**Description**: WeatherService orchestrating OpenMeteoClient and repositories with AOP.

**Methods implemented**:
- `getCurrentWeather(String locationName)` returning `Mono<WeatherCurrentDto>`
- `getForecast(String locationName, int days)` returning `Flux<WeatherForecastDto>`
- `getSavedLocations(String userId)` returning `Flux<WeatherLocationDto>`
- `saveLocation(String userId, String locationName)` returning `Mono<WeatherLocationDto>`

**AOP applied**:
- `@LogExecutionTime` on getCurrentWeather and getForecast
- `@ValidateLocation` on getCurrentWeather and getForecast
- `@Audited(action = "FETCH_WEATHER")` on getCurrentWeather

**How to verify**:
```bash
# Make a request through the K8s gateway
curl "http://localhost:8081/api/weather/current?location=London"

# Check weather-service logs for AOP output
kubectl logs -n weather-app -l app=weather-service --tail=20
# Should show lines like:
# [LogExecutionTime] getCurrentWeather completed in Xms
# [AUDIT] action=FETCH_WEATHER status=SUCCESS
```

---

### TICKET-010: Weather Service — REST API and Global Exception Handler

**Description**: WeatherController with reactive endpoints and GlobalExceptionHandler.

**Endpoints**:
- `GET /api/weather/current?location={location}`
- `GET /api/weather/forecast?location={location}&days={days}`
- `GET /api/weather/locations` — reads `X-User-Id` header
- `POST /api/weather/locations` — reads `X-User-Id` header

**User identity**: `@RequestHeader(value = "X-User-Id", defaultValue = "anonymous") String userId`

**Error handling**: GlobalExceptionHandler returns RFC 9457 problem+json with correlationId for all exceptions.

**How to verify**:
```bash
# All requests go through the K8s gateway on port 8081

# Happy path
curl "http://localhost:8081/api/weather/current?location=London"
# Expected: 200 with weather data

# Saved locations for a user
curl -H "X-User-Id: user-123" http://localhost:8081/api/weather/locations
# Expected: 200 with locations array (empty initially)

# Save a location
curl -X POST -H "X-User-Id: user-123" \
  -H "Content-Type: application/json" \
  -d '{"locationName":"London"}' \
  http://localhost:8081/api/weather/locations
# Expected: 201 with location DTO

# Verify it was saved in K8s PostgreSQL
kubectl exec -n weather-app \
  $(kubectl get pods -n weather-app -l app=postgres -o jsonpath='{.items[0].metadata.name}') \
  -- psql -U postgres -d weather_db \
  -c "SELECT name, country FROM weather_location WHERE user_id = 'user-123';"

# Validation error — days out of range
curl -v "http://localhost:8081/api/weather/forecast?location=London&days=20"
# Expected: 400, Content-Type: application/problem+json
```

---

### TICKET-011: Weather Service — SSE Real-Time Streaming

**Description**: Server-Sent Events endpoint streaming weather updates every 30 seconds.

**Endpoints**:
- `GET /api/weather/stream?location={location}` — streams updates for one location
- `GET /api/weather/stream/all` — streams updates for all of a user's saved locations (reads X-User-Id header)

**Features**:
- Keep-alive comment events every 15 seconds
- Backpressure drop logging
- Client disconnection logging

**How to verify**:
```bash
# SSE through the K8s gateway
curl -N "http://localhost:8081/api/weather/stream?location=London"
# Stays open, prints weather data every 30 seconds
# Press Ctrl+C to disconnect

# Check weather-service logs show the disconnection
kubectl logs -n weather-app -l app=weather-service --tail=10
# Should show: "SSE client disconnected for location: London"
```


---

### TICKET-012: Alert Service — Project Setup

**Description**: Create the alert-service Maven module — same reactive stack as weather-service with Kafka added. No auth dependency.

**Tasks**:

- **Create alert-service Maven module**
  - Create `alert-service/` folder inside `weather-backend/`.
  - Create `alert-service/pom.xml` with parent `com.ile:weather:0.0.1-SNAPSHOT`.
  - Add `<module>alert-service</module>` to the root `pom.xml`.
  - Create `AlertServiceApplication.java` in `com.ile.alert`.

- **Add dependencies to alert-service/pom.xml**
  - `spring-boot-starter-webflux`, `spring-boot-starter-data-r2dbc`, `r2dbc-postgresql`
  - `spring-boot-starter-jdbc`, `postgresql` (for Liquibase only)
  - `liquibase-core`, `spring-boot-starter-actuator`
  - `spring-kafka`
  - `com.ile:weather-core:0.0.1-SNAPSHOT`
  - `spring-boot-maven-plugin` in build section
  - Note: no `spring-boot-starter-oauth2-resource-server` — no auth needed

- **Create application.yml**
  - Port: `8083`
  - R2DBC URL pointing to `alert_db`
  - JDBC datasource URL for Liquibase (same DB)
  - `spring.kafka.bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}`
  - Kafka consumer: `group-id: alert-service`, `auto-offset-reset: earliest`, `key-deserializer/value-deserializer: StringDeserializer`

- **Create application-k8s.yml**
  - Override DB host to `postgres`, Kafka to `kafka:9092`

- **Create SecurityConfig.java** — permit everything, no auth
  ```java
  @Bean
  public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
      return http
          .csrf(ServerHttpSecurity.CsrfSpec::disable)
          .authorizeExchange(exchanges -> exchanges.anyExchange().permitAll())
          .build();
  }
  ```

- **Create R2dbcConfig.java** with `@EnableR2dbcAuditing`

- **Create Liquibase changelog — 001-create-alert-tables.xml**
  - `alert_rules`: `id UUID PK`, `user_id VARCHAR`, `location VARCHAR`, `type VARCHAR`, `condition VARCHAR`, `threshold DECIMAL(10,2)`, `enabled BOOLEAN DEFAULT true`, `created_at TIMESTAMP`
  - `alert_notifications`: `id UUID PK`, `user_id VARCHAR`, `rule_id UUID FK→alert_rules(id)`, `message TEXT`, `is_read BOOLEAN DEFAULT false`, `triggered_at TIMESTAMP`, `created_at TIMESTAMP`
  - Indexes: `idx_alert_rules_user_id` on `alert_rules(user_id)`, `idx_alert_notifications_user_id` on `alert_notifications(user_id, is_read)`

- **Add alert_db to postgres init scripts**
  - In `infrastructure/postgres/init-scripts/01-create-databases.sql`, add `CREATE DATABASE alert_db;` with grants.

- **Create Dockerfile** — same multi-stage pattern as weather-service

- **Create Kubernetes manifests**
  - `k8s/base/alert-service/deployment.yaml` on port 8083
  - `k8s/base/alert-service/service.yaml`
  - Add both to `k8s/base/kustomization.yaml`

- **Add gateway route** for alert-service in api-gateway application.yaml
  - Predicate `Path=/api/alerts/**`, URI `http://alert-service:8083`

**Acceptance Criteria**:
- `curl http://localhost:8083/actuator/health` returns `{"status":"UP"}`
- Tables `alert_rules` and `alert_notifications` exist in `alert_db`
- Kafka connection established (startup logs show no connection errors)

**How to test**:
```bash
# Build image and deploy to K8s
./build-images.sh

# Check alert-service pod is running
kubectl get pods -n weather-app -l app=alert-service
# NAME                            READY   STATUS    RESTARTS
# alert-service-xxx               1/1     Running

# Health check through the gateway
curl http://localhost:8081/actuator/health
# or port-forward directly for alert-service health
kubectl port-forward -n weather-app svc/alert-service 8083:8083 &
curl http://localhost:8083/actuator/health
# Expected: {"status":"UP"}

# Check tables exist in K8s PostgreSQL
kubectl exec -n weather-app   $(kubectl get pods -n weather-app -l app=postgres -o jsonpath='{.items[0].metadata.name}')   -- psql -U postgres -d alert_db -c "\dt"
# Expected: alert_rules, alert_notifications

# Check Kafka connection in startup logs
kubectl logs -n weather-app -l app=alert-service | grep -i kafka
# Should show successful broker connection, no errors
```

---

### TICKET-013: Alert Service — Domain and Rules Engine

**Description**: Domain entities, repositories, and a strategy-pattern rules evaluation engine.

**Tasks**:

- **Create AlertRule.java** in `com.ile.alert.domain.entity`
  - Fields: `UUID id`, `String userId`, `String location`, `String type` (stored as String for AlertType enum), `String condition` (stored as String for AlertCondition enum), `BigDecimal threshold`, `boolean enabled`, `LocalDateTime createdAt`.
  - Helper methods: `getAlertType()` and `getAlertCondition()` that parse the String fields back to enums.

- **Create AlertNotification.java** entity
  - Fields: `UUID id`, `String userId`, `UUID ruleId`, `String message`, `boolean isRead`, `LocalDateTime triggeredAt`, `LocalDateTime createdAt`.

- **Create AlertRuleRepository** extending `ReactiveCrudRepository<AlertRule, UUID>`
  - `Flux<AlertRule> findByUserId(String userId)`
  - `Flux<AlertRule> findByLocationAndEnabled(String location, boolean enabled)` — used by Kafka listener

- **Create AlertNotificationRepository** extending `ReactiveCrudRepository<AlertNotification, UUID>`
  - `Flux<AlertNotification> findByUserIdOrderByTriggeredAtDesc(String userId)`
  - `Mono<Long> countByUserIdAndIsRead(String userId, boolean isRead)`

- **Create WeatherSnapshot** — plain data class (not an entity)
  - Fields: `String locationName`, `BigDecimal temperature`, `BigDecimal windSpeed`, `BigDecimal precipitation`, `Integer humidity`.

- **Create RuleEvaluationStrategy interface** in `com.ile.alert.engine`
  - `boolean evaluate(AlertRule rule, WeatherSnapshot snapshot)`

- **Create strategy implementations — one per AlertType**
  - `TemperatureEvaluator`, `WindEvaluator`, `PrecipitationEvaluator`, `HumidityEvaluator`.
  - Each reads the relevant field from snapshot, compares against threshold using AlertCondition:
    - `ABOVE: value.compareTo(threshold) > 0`
    - `BELOW: value.compareTo(threshold) < 0`
    - `EQUALS: value.compareTo(threshold) == 0`

- **Create AlertRuleEvaluator** as a Spring `@Component`
  - `Map<AlertType, RuleEvaluationStrategy>` populated in constructor.
  - `Flux<AlertRule> evaluateAll(List<AlertRule> rules, WeatherSnapshot snapshot)` — returns only triggered rules.

**Acceptance Criteria**:
- `TemperatureEvaluator` correctly evaluates ABOVE/BELOW/EQUALS
- `evaluateAll()` returns only triggered rules

**How to test**:
```java
@Test
void temperatureEvaluator_whenAboveThreshold_shouldTrigger() {
    AlertRule rule = new AlertRule();
    rule.setType("TEMPERATURE");
    rule.setCondition("ABOVE");
    rule.setThreshold(new BigDecimal("30"));

    WeatherSnapshot snapshot = new WeatherSnapshot();
    snapshot.setTemperature(new BigDecimal("35"));

    assertTrue(new TemperatureEvaluator().evaluate(rule, snapshot));
}
```

---

### TICKET-014: Alert Service — Kafka Event Listeners

**Description**: Consume weather-updates events from Kafka, evaluate alert rules, and persist triggered notifications.

**Tasks**:

- **Create KafkaTopicConfig.java** in `com.ile.alert.config`
  - `@Bean NewTopic` for `weather-updates` — 3 partitions, replication factor 1.
  - `@Bean NewTopic` for `alert-notifications` — 3 partitions, replication factor 1.

- **Create WeatherUpdateEvent.java** — plain Java record
  - Fields: `String locationName`, `BigDecimal temperature`, `BigDecimal windSpeed`, `BigDecimal precipitation`, `Integer humidity`, `LocalDateTime recordedAt`.

- **Create WeatherUpdateListener.java** in `com.ile.alert.messaging`
  - `@KafkaListener(topics = "weather-updates", groupId = "alert-service")` on a method accepting `String message`.
  - Deserialize with `ObjectMapper.readValue(message, WeatherUpdateEvent.class)`.
  - Convert to `WeatherSnapshot` and call `alertProcessingService.processWeatherUpdate(snapshot)`.
  - Annotate with `@LogExecutionTime`.
  - Wrap in try-catch — log errors but do not rethrow (prevents infinite redelivery).

- **Create AlertProcessingService.java** in `com.ile.alert.service`
  - `Mono<Void> processWeatherUpdate(WeatherSnapshot snapshot)`:
    - `alertRuleRepository.findByLocationAndEnabled(locationName, true).collectList()`
    - `alertRuleEvaluator.evaluateAll(rules, snapshot)`
    - For each triggered rule: create `AlertNotification` with message like `"Temperature in London is 38.5 which is ABOVE your threshold of 35"`
    - `alertNotificationRepository.save(notification)`
  - Annotate with `@Audited(action = "ALERT_TRIGGERED")`.

- **Add Kafka publishing to weather-service**
  - Add `spring-kafka` dependency to `weather-service/pom.xml`.
  - In `WeatherService.getCurrentWeather()`, after saving the weather data, publish a `WeatherUpdateEvent` JSON string to `weather-updates` topic using `KafkaTemplate<String, String>`.
  - Use the location name as the Kafka message key.

**Acceptance Criteria**:
- Publishing a message to `weather-updates` via Kafka-UI creates a row in `alert_notifications` when a rule matches
- Logs show `@LogExecutionTime` and `@Audited` output

**How to test**:
```bash
# 1. Insert a test rule directly into K8s PostgreSQL
kubectl exec -n weather-app \
  $(kubectl get pods -n weather-app -l app=postgres -o jsonpath='{.items[0].metadata.name}') \
  -- psql -U postgres -d alert_db -c "
INSERT INTO alert_rules (id, user_id, location, type, condition, threshold, enabled)
VALUES (gen_random_uuid(), 'user-123', 'London', 'TEMPERATURE', 'ABOVE', 30, true);"

# 2. Port-forward Kafka-UI if not already in build-images.sh
kubectl port-forward -n weather-app svc/kafka-ui 8090:8080 &

# Open http://localhost:8090 → Topics → weather-updates → Produce Message
# Key: London
# Value: {"locationName":"London","temperature":38.5,"windSpeed":12.0,"precipitation":0.0,"humidity":45,"recordedAt":"2024-01-01T12:00:00"}

# 3. Check notification was created in K8s PostgreSQL
kubectl exec -n weather-app \
  $(kubectl get pods -n weather-app -l app=postgres -o jsonpath='{.items[0].metadata.name}') \
  -- psql -U postgres -d alert_db \
  -c "SELECT * FROM alert_notifications ORDER BY triggered_at DESC LIMIT 5;"

# 4. Check alert-service logs for AOP output
kubectl logs -n weather-app -l app=alert-service --tail=20
# Should show:
# [LogExecutionTime] processWeatherUpdate completed in Xms
# [AUDIT] action=ALERT_TRIGGERED status=SUCCESS
```

---

### TICKET-015: Alert Service — REST API

**Description**: CRUD endpoints for managing alert rules and reading notifications. User identity comes from `X-User-Id` header.

**Tasks**:

- **Create AlertRuleService.java** in `com.ile.alert.service`
  - `createRule(String userId, CreateAlertRuleRequest request)` — validate, save, return DTO.
  - `getRulesForUser(String userId)` returning `Flux<AlertRuleDto>`.
  - `deleteRule(String userId, UUID ruleId)` — verify ownership (rule's userId must match), delete.

- **Create AlertNotificationService.java**
  - `getNotificationsForUser(String userId)` returning `Flux<AlertNotificationDto>`.
  - `markAsRead(String userId, UUID notificationId)` — verify ownership, set isRead = true, save.
  - `getUnreadCount(String userId)` returning `Mono<Long>`.

- **Create request/response records**
  - `CreateAlertRuleRequest`: `@NotBlank String location`, `@NotNull AlertType type`, `@NotNull AlertCondition condition`, `@NotNull @Positive BigDecimal threshold`.
  - `AlertRuleDto` and `AlertNotificationDto` matching entity fields.

- **Create AlertController.java** in `com.ile.alert.controller`
  - Annotate with `@RestController`, `@RequestMapping("/api/alerts")`, `@Validated`.
  - Extract userId: `@RequestHeader(value = "X-User-Id", defaultValue = "anonymous") String userId`.

- **Implement all endpoints**:

| Method | Path | Returns |
|---|---|---|
| `GET` | `/api/alerts/rules` | `Flux<AlertRuleDto>` |
| `POST` | `/api/alerts/rules` | `Mono<ResponseEntity<AlertRuleDto>>` 201 |
| `DELETE` | `/api/alerts/rules/{id}` | `Mono<ResponseEntity<Void>>` 204 |
| `GET` | `/api/alerts/notifications` | `Flux<AlertNotificationDto>` |
| `PUT` | `/api/alerts/notifications/{id}/read` | `Mono<ResponseEntity<AlertNotificationDto>>` 200 |

- **Add GlobalExceptionHandler** — same as weather-service

**Acceptance Criteria**:
- CRUD for alert rules works with `X-User-Id` header
- Notifications appear after a Kafka event triggers a rule
- Gateway routes `/api/alerts/**` to alert-service

**How to test**:
```bash
# All requests through the K8s gateway on port 8081

# Create a rule
curl -X POST -H "X-User-Id: user-123" \
  -H "Content-Type: application/json" \
  -d '{"location":"London","type":"TEMPERATURE","condition":"ABOVE","threshold":30}' \
  http://localhost:8081/api/alerts/rules
# Expected: 201 with AlertRuleDto

# List rules
curl -H "X-User-Id: user-123" http://localhost:8081/api/alerts/rules
# Expected: 200 with the rule just created

# Trigger the rule via Kafka-UI (port-forward: kubectl port-forward -n weather-app svc/kafka-ui 8090:8080 &)
# Topics → weather-updates → Produce Message → Key: London → Value: temperature 38.5
# Then check notifications
curl -H "X-User-Id: user-123" http://localhost:8081/api/alerts/notifications
# Expected: notification with message "Temperature in London is 38.5 which is ABOVE your threshold of 30"

# Test ownership — wrong user cannot delete the rule
RULE_ID=$(curl -s -H "X-User-Id: user-123" http://localhost:8081/api/alerts/rules | jq -r '.[0].id')
curl -X DELETE -H "X-User-Id: different-user" http://localhost:8081/api/alerts/rules/$RULE_ID
# Expected: 403 Forbidden

# Verify alert data in K8s PostgreSQL
kubectl exec -n weather-app \
  $(kubectl get pods -n weather-app -l app=postgres -o jsonpath='{.items[0].metadata.name}') \
  -- psql -U postgres -d alert_db \
  -c "SELECT location, type, condition, threshold FROM alert_rules WHERE user_id = 'user-123';"
```

---

### TICKET-016: Zipkin Tracing (basic)

**Description**: Add trace IDs to logs so you can follow a request across gateway → weather-service → alert-service in Zipkin.

**Tasks**:

- **Add Micrometer Tracing dependencies** to root `pom.xml` shared dependencies
  - `io.micrometer:micrometer-tracing-bridge-brave`
  - `io.zipkin.reporter2:zipkin-reporter-brave`

- **Configure Zipkin endpoint** in each service's `application.yml`
  - `management.zipkin.tracing.endpoint: http://${ZIPKIN_HOST:localhost}:9411/api/v2/spans`
  - `management.tracing.sampling.probability: 1.0`

- **Add trace ID to log pattern** in each service's `application.yml`
  - `logging.pattern.level: "%5p [${spring.application.name:},%X{traceId:-},%X{spanId:-}]"`

- **Set spring.application.name** in each service's `application.yml` if not already set

**Acceptance Criteria**:
- Log lines include `[service-name,traceId,spanId]` format
- Zipkin UI at `http://localhost:9411` shows traces with spans from multiple services

**How to test**:
```bash
# Make a request through the K8s gateway
curl "http://localhost:8081/api/weather/current?location=London"

# Port-forward Zipkin if not already in build-images.sh
kubectl port-forward -n weather-app svc/zipkin 9411:9411 &

# Open http://localhost:9411 → Run Query
# Click a trace — should show spans for api-gateway AND weather-service
# Spans are generated by the K8s-running services and sent to the K8s-running Zipkin

# Verify trace IDs appear in K8s service logs
kubectl logs -n weather-app -l app=weather-service --tail=10
# Should see log lines like:
# INFO [weather-service,abc123def456,789ghi] c.i.w.s.WeatherService - ...
```

---

### TICKET-017: Health Checks and Actuator

**Description**: Liveness and readiness probes for Kubernetes.

**Tasks**:

- **Enable probe groups** in each service's `application.yml`
  - `management.endpoint.health.probes.enabled: true`
  - `management.endpoint.health.show-details: always`
  - `management.endpoints.web.exposure.include: health,info,metrics`

- **Configure readiness to include DB check**
  - `management.endpoint.health.group.readiness.include: readinessState,r2dbc`
  - Liveness must NOT include r2dbc — a DB outage should not restart the pod.

- **Verify Kubernetes probe config** in all deployment YAMLs
  - Liveness: `/actuator/health/liveness`, `initialDelaySeconds: 60`
  - Readiness: `/actuator/health/readiness`, `initialDelaySeconds: 45`

**Acceptance Criteria**:
- `/actuator/health/liveness` returns 200 even when PostgreSQL is down
- `/actuator/health/readiness` returns 503 when PostgreSQL is down

**How to test**:
```bash
# Port-forward each service to check its probes directly
kubectl port-forward -n weather-app svc/api-gateway     8081:8081 &
kubectl port-forward -n weather-app svc/weather-service 8082:8082 &
kubectl port-forward -n weather-app svc/alert-service   8083:8083 &

# Check all health endpoints
for port in 8081 8082 8083; do
  echo "=== Port $port ==="
  curl -s http://localhost:$port/actuator/health/liveness
  curl -s http://localhost:$port/actuator/health/readiness
done

# Test readiness when DB is down — scale postgres to 0 replicas
kubectl scale deployment postgres -n weather-app --replicas=0

curl -s http://localhost:8082/actuator/health/readiness
# Expected: 503 Unhealthy (DB is down)

curl -s http://localhost:8082/actuator/health/liveness
# Expected: 200 (liveness not affected by DB)

# Restore postgres
kubectl scale deployment postgres -n weather-app --replicas=1
kubectl wait --for=condition=Ready pod -l app=postgres -n weather-app --timeout=60s

# Verify Kubernetes is using the probes by checking pod events
kubectl describe pods -n weather-app -l app=weather-service | grep -A3 "Liveness\|Readiness"
# Should show the probe configuration and last check result
```

---

### TICKET-018: Angular Project Setup

**Description**: Create the Angular 17+ project with Material, routing, and proxy configuration. No auth library needed.

**Tasks**:

- **Create the project** from the `weather-backend/` folder
  - `ng new weather-frontend --standalone --routing --style=scss --strict`

- **Install dependencies**
  - `ng add @angular/material` — choose a theme, yes to typography and animations
  - `npm install chart.js ng2-charts`
  - `ng add @angular-eslint/schematics`

- **Create environment files** — `src/environments/environment.ts`
  ```typescript
  export const environment = {
    production: false,
    apiUrl: 'http://localhost:8081'
  };
  ```

- **Configure dev proxy** — create `proxy.conf.json`
  ```json
  { "/api": { "target": "http://localhost:8081", "changeOrigin": true } }
  ```
  Add to `angular.json` under serve options: `"proxyConfig": "proxy.conf.json"`.

- **Create folder structure**
  ```
  src/app/
  ├── core/
  │   └── services/        ← WeatherApiService
  ├── features/
  │   └── dashboard/       ← location search, weather card, forecast, chart
  └── shared/
      └── models/          ← TypeScript interfaces
  ```

- **Set up Prettier**
  - `npm install --save-dev prettier eslint-config-prettier`
  - `.prettierrc`: `{ "singleQuote": true, "trailingComma": "es5" }`

**Acceptance Criteria**:
- `ng serve` starts at `http://localhost:4200`
- `ng build --configuration=production` — zero TypeScript errors

**How to test**:
```bash
# build-images.sh must be running (port-forwards to K8s active)
./build-images.sh &  # or in a separate terminal

cd weather-frontend
npm install
ng serve
# Open http://localhost:4200
# Angular talks to the K8s gateway at http://localhost:8081 via proxy

ng build --configuration=production
# Expected: dist/ folder, zero TypeScript errors
```

---

### TICKET-019: Angular Services

**Description**: Create typed HTTP services for the weather API. No auth, no interceptors, no user-specific features — just plain HTTP calls to fetch and stream weather data.

**Tasks**:

- **Define TypeScript interfaces** in `shared/models/weather.model.ts`
  ```typescript
  export interface WeatherData {
    locationName: string;
    temperature: number;
    apparentTemperature: number;
    relativeHumidity: number;
    windSpeed: number;
    weatherCode: number;
    description: string;
    isDay: boolean;
    recordedAt: string;
  }

  export interface WeatherForecast {
    locationName: string;
    forecastTime: string;
    temperature: number;
    precipitationProbability: number;
    precipitation: number;
    weatherCode: number;
    description: string;
  }
  ```

- **Create `WeatherApiService`** in `core/services/weather-api.service.ts`
  ```typescript
  @Injectable({ providedIn: 'root' })
  export class WeatherApiService {
    private http = inject(HttpClient);
    private apiUrl = environment.apiUrl;

    getCurrentWeather(location: string): Observable<WeatherData> {
      return this.http.get<WeatherData>(
        `${this.apiUrl}/api/weather/current`, { params: { location } }
      );
    }

    getForecast(location: string, days = 7): Observable<WeatherForecast[]> {
      return this.http.get<WeatherForecast[]>(
        `${this.apiUrl}/api/weather/forecast`, { params: { location, days } }
      );
    }

    streamWeatherUpdates(location: string): Observable<WeatherData> {
      return new Observable(observer => {
        const es = new EventSource(
          `${this.apiUrl}/api/weather/stream?location=${location}`
        );
        es.addEventListener('weather-update', (e: MessageEvent) =>
          observer.next(JSON.parse(e.data)));
        es.onerror = err => observer.error(err);
        return () => es.close();
      });
    }
  }
  ```

- **Register `HttpClient`** in `app.config.ts`
  - `provideHttpClient()` — no interceptors needed.

**Acceptance Criteria**:
- `getCurrentWeather('London')` returns typed `WeatherData`
- SSE stream emits data every 30 seconds
- Zero `any` types in service files

**How to test**:
```bash
# build-images.sh must be running (port-forwards active)
ng serve

# Open browser console and inject the service:
# this.weatherService.getCurrentWeather('London').subscribe(console.log)
# Should print weather data fetched from K8s weather-service through K8s gateway

# DevTools → Network → GET /api/weather/current?location=London
# Request goes to http://localhost:8081 (K8s gateway port-forward)
# Not to localhost:8082 directly

# Confirm SSE also goes through the gateway
# this.weatherService.streamWeatherUpdates('London').subscribe(console.log)
# DevTools → Network → EventStream tab
# Connection to http://localhost:8081/api/weather/stream (K8s gateway)
```

---

### TICKET-020: Dashboard Component

**Description**: Build the main dashboard — location search, current weather card, forecast row, real-time SSE updates, and a temperature trend chart. No user accounts, no saved locations, no alerts page.

**Tasks**:

- **Create `DashboardComponent`** via `ng generate component features/dashboard/dashboard`
  - `changeDetection: ChangeDetectionStrategy.OnPush`
  - Layout: location search bar at the top, current weather card below, forecast row, temperature chart at the bottom.

- **Create `LocationSelectorComponent`**
  - `MatFormField` text input with a search button.
  - `@Output() locationSelected = new EventEmitter<string>()` — emits when user clicks search or presses Enter.

- **Create `WeatherCardComponent`**
  - `@Input() weather: WeatherData | null`.
  - Shows: temperature, apparent temperature, humidity, wind speed, description, and a weather emoji (map WMO code to emoji — 0 → ☀️, 45 → 🌫️, 61 → 🌧️, 95 → ⛈️ etc.).
  - Shows `MatProgressSpinner` when weather is null (loading state).

- **Create `ForecastCardComponent`**
  - `@Input() forecast: WeatherForecast`.
  - Shows: time, temperature, precipitation probability, weather emoji.
  - Displayed as a horizontal scrolling row of cards using `overflow-x: auto`.

- **Wire up SSE updates in `DashboardComponent`**
  - On location selection: call `weatherApiService.getCurrentWeather(location)` for the immediate result, then subscribe to `weatherApiService.streamWeatherUpdates(location)` for live updates.
  - Use `takeUntilDestroyed()` to auto-unsubscribe when the component is destroyed.
  - Store current weather in a `BehaviorSubject<WeatherData | null>` — the template binds to it via `async` pipe.
  - When a new location is selected, unsubscribe from the previous SSE stream and start a new one.

- **Add temperature trend chart**
  - Use `ng2-charts` with `BaseChartDirective`.
  - Call `weatherApiService.getForecast(location)` on location selection.
  - Extract `temperature` values and `forecastTime` labels from the response array.
  - Build a `ChartData<'line'>` object and bind it to the chart component.
  - Update chart data whenever a new location is selected.

- **Configure routing in `app.routes.ts`**
  ```typescript
  export const routes: Routes = [
    { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
    { path: 'dashboard', component: DashboardComponent },
  ];
  ```

- **Create `NavbarComponent`**
  - Shows the app name and a link to `/dashboard`.
  - No login/logout buttons — nothing auth-related.

**Acceptance Criteria**:
- Searching a location loads the weather card and forecast row
- SSE stream updates the weather card every 30 seconds without user action
- Temperature chart updates when a new location is selected
- Searching a second location replaces the first (no stacking of SSE connections)

**How to test**:
```bash
# build-images.sh must be running (K8s port-forwards active)
ng serve
# Open http://localhost:4200

# Type "London" → click Search
# ✅ Weather card shows temperature and description (data from K8s weather-service)
# ✅ Forecast cards appear in a horizontal row
# ✅ Temperature chart renders with hourly data

# Open DevTools → Network → EventStream tab
# ✅ One SSE connection to http://localhost:8081/api/weather/stream?location=London
#    (through K8s gateway port-forward — NOT direct to weather-service)
# ✅ Events arriving every 30 seconds from the K8s-running weather-service

# Search "Paris" — previous SSE connection should close
# ✅ Only one EventStream connection visible in DevTools (not two)
# ✅ Weather card updates to Paris data

# Verify the request chain in K8s logs
kubectl logs -n weather-app -l app=weather-service --tail=20
# Should show geocoding and weather fetch log entries for London and Paris

# Verify Redis cache was populated from the K8s Redis pod
kubectl exec -n weather-app \
  $(kubectl get pods -n weather-app -l app=redis -o jsonpath='{.items[0].metadata.name}') \
  -- redis-cli KEYS "weather:*"
# Should show keys for both London and Paris
```

---

## Running the Full Stack

**Kubernetes (primary — use for all testing)**:
```bash
./build-images.sh
# Resets minikube, builds images, deploys everything, starts port-forwards
# Gateway:  http://localhost:8081
# Zipkin:   http://localhost:9411 (add port-forward to build-images.sh if needed)
# Kafka-UI: http://localhost:8090 (add port-forward to build-images.sh if needed)

cd weather-frontend && ng serve
# Angular at http://localhost:4200 — talks to K8s via proxy
```

**Quick curl tests (all through K8s gateway)**:
```bash
# Current weather
curl "http://localhost:8081/api/weather/current?location=London"

# Saved locations for a user
curl -H "X-User-Id: user-123" http://localhost:8081/api/weather/locations

# Save a location
curl -X POST -H "X-User-Id: user-123" \
  -H "Content-Type: application/json" \
  -d '{"locationName":"London"}' \
  http://localhost:8081/api/weather/locations

# Alert rules
curl -H "X-User-Id: user-123" http://localhost:8081/api/alerts/rules
```

**Useful Kubernetes commands**:
```bash
# Watch all pods
kubectl get pods -n weather-app --watch

# Tail logs for a service
kubectl logs -n weather-app -l app=weather-service -f
kubectl logs -n weather-app -l app=alert-service -f

# Restart a service after code change
kubectl rollout restart deployment/weather-service -n weather-app
kubectl rollout restart deployment/alert-service -n weather-app

# Shell into postgres
kubectl exec -it -n weather-app \
  $(kubectl get pods -n weather-app -l app=postgres -o jsonpath='{.items[0].metadata.name}') \
  -- psql -U postgres

# Shell into redis
kubectl exec -it -n weather-app \
  $(kubectl get pods -n weather-app -l app=redis -o jsonpath='{.items[0].metadata.name}') \
  -- redis-cli
```
