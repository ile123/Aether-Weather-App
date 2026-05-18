# Aether Weather Dashboard

A real-time weather dashboard built to demonstrate **Reactive Programming**, **AOP**, **Microservices**, and **Event-Driven Architecture** using Spring Boot and Angular — deployed on Kubernetes.

---

## What It Does

- Search any city and get current weather conditions
- View a 48-hour hourly forecast with a temperature trend chart
- Receive live weather updates every 30 seconds via Server-Sent Events (SSE)
- Define alert rules (e.g. "notify me when London temperature is ABOVE 35°C")
- Alert notifications are triggered automatically when weather data is fetched

---

## Architecture

```
Angular 18 SPA (ng serve → localhost:4200)
        │
        ▼
API Gateway (Spring Cloud Gateway) :8081
        │
        ├──▶ Weather Service :8082
        │         ├── Open-Meteo API (external — free weather data)
        │         ├── PostgreSQL (weather_db)
        │         └── Redis (geocoding + weather cache)
        │
        └──▶ Alert Service :8083
                  ├── Kafka consumer (weather-updates topic)
                  └── PostgreSQL (alert_db)

Infrastructure: PostgreSQL · Redis · Kafka (KRaft) · Zipkin · Kafka-UI
```

**Request flow for a weather search:**
1. Angular calls `GET /api/weather/current?location=London` through the gateway
2. Gateway routes it to Weather Service
3. Weather Service checks Redis cache — if miss, calls Open-Meteo API
4. Weather data is saved to PostgreSQL and returned to Angular
5. Weather Service publishes a `WeatherUpdateEvent` to Kafka
6. Alert Service consumes the event, evaluates alert rules against the data
7. If a rule matches, an `AlertNotification` is saved to PostgreSQL

---

## Tech Stack

| Layer | Technology |
|---|---|
| **Backend** | Spring Boot 3.4.4 · Spring WebFlux · Spring Data R2DBC |
| **API Gateway** | Spring Cloud Gateway (Reactive) |
| **Messaging** | Apache Kafka (KRaft mode) |
| **Database** | PostgreSQL (R2DBC for reactive queries, JDBC for Liquibase migrations) |
| **Caching** | Redis |
| **Tracing** | Zipkin + Micrometer Tracing |
| **Migrations** | Liquibase |
| **Frontend** | Angular 18 · Angular Material · ng2-charts · RxJS |
| **Container** | Docker + Kubernetes (minikube + kustomize) |

---

## Key Concepts Used

### Reactive Programming (Spring WebFlux + R2DBC)
The entire backend is non-blocking. No thread-per-request — everything uses `Mono<T>` and `Flux<T>` chains. The database layer uses R2DBC (Reactive Relational Database Connectivity) instead of blocking JDBC. SSE streaming is a `Flux` that emits every 30 seconds.

### AOP (Aspect-Oriented Programming)
Cross-cutting concerns are handled via custom annotations in `weather-core`:
- `@LogExecutionTime` — logs how long a method took
- `@ValidateLocation` — validates the location string before the method runs
- `@Audited(action = "FETCH_WEATHER")` — logs audit events for state-changing operations

These run automatically via Spring AOP proxies — the service methods don't know they're being intercepted.

### Microservices with Event-Driven Communication
Weather Service and Alert Service are independent — they don't call each other directly. Communication happens through Kafka. When weather data is fetched, Weather Service publishes an event. Alert Service consumes it and acts independently. If Alert Service is down, the events wait in Kafka until it recovers.

### Circuit Breaker (Resilience4j)
The API Gateway wraps backend routes in Resilience4j circuit breakers. If Weather Service starts failing, the circuit opens and a fallback response is returned immediately instead of letting requests pile up. The circuit automatically tries to recover after 30 seconds.

### Caching (Redis)
Open-Meteo API calls are cached in Redis:
- Geocoding results: 24-hour TTL
- Current weather: 5-minute TTL
- Forecast: 30-minute TTL

Cache keys are based on coordinates, so searching "London" twice doesn't hit the external API twice.

### Strategy Pattern (Alert Rules Engine)
Alert rules are evaluated using the Strategy Pattern. Each alert type (`TEMPERATURE`, `WIND`, `PRECIPITATION`, `HUMIDITY`) has its own evaluator class implementing `RuleEvaluationStrategy`. An `AlertRuleEvaluator` holds a `Map<AlertType, RuleEvaluationStrategy>` and dispatches to the correct evaluator — adding a new alert type only requires a new class and one map entry.

---

## Project Structure

```
weather-backend/
├── api-gateway/          Spring Cloud Gateway — single entry point for all traffic
├── weather-core/         Shared library — DTOs, exceptions, AOP annotations/aspects, enums
├── weather-service/      Weather data service — Open-Meteo client, caching, SSE streaming
├── alert-service/        Alert rules engine — Kafka consumer, rule evaluation, notifications
├── k8s/                  Kubernetes manifests (kustomize)
│   ├── base/             Base manifests for all services and infrastructure
│   └── overlays/dev/     Dev overrides
└── build-images.sh       One-command deploy to minikube

weather-frontend/
└── src/app/
    ├── core/services/    WeatherApiService — HTTP + SSE calls
    ├── features/dashboard/ Dashboard, WeatherCard, ForecastCard, LocationSelector
    └── shared/models/    TypeScript interfaces
```

---

## Running the Project

### Requirements
- Java 21
- Node 18+
- minikube
- kubectl
- Docker

### Deploy to Kubernetes

```bash
cd weather-backend
./build-images.sh
```

This will:
1. Reset minikube and start fresh
2. Build Docker images for all three services
3. Deploy everything to the `weather-app` namespace
4. Start port-forwards automatically

**Available endpoints after deploy:**

| Service | URL |
|---|---|
| API Gateway | http://localhost:8081 |
| Kafka UI | http://localhost:8090 |
| Alert Service (direct) | http://localhost:8083 |
| Zipkin | `kubectl port-forward -n weather-app svc/zipkin 9411:9411` |

### Start the Frontend

```bash
cd weather-frontend
npm install
ng serve
# Open http://localhost:4200
```

---

## Quick API Reference

All requests go through the gateway at `http://localhost:8081`.

```bash
# Current weather
curl "http://localhost:8081/api/weather/current?location=London"

# 7-day hourly forecast
curl "http://localhost:8081/api/weather/forecast?location=London&days=7"

# Live SSE stream (stays open)
curl -N "http://localhost:8081/api/weather/stream?location=London"

# Create an alert rule
curl -X POST -H "X-User-Id: user-123" \
  -H "Content-Type: application/json" \
  -d '{"location":"London","type":"TEMPERATURE","condition":"ABOVE","threshold":30}' \
  http://localhost:8081/api/alerts/rules

# Get notifications
curl -H "X-User-Id: user-123" http://localhost:8081/api/alerts/notifications
```

---

## Observability

**Health checks** — all services expose `/actuator/health/liveness` and `/actuator/health/readiness`. Kubernetes uses these as probes. Readiness includes the database check — if PostgreSQL is down, the pod stops receiving traffic. Liveness does not include the database — a DB outage should not restart the pod.

**Distributed tracing** — all services send traces to Zipkin. Every log line includes `[service-name,traceId,spanId]` so you can correlate a request across the gateway, weather-service, and alert-service in a single trace.

**Kafka UI** — available at `http://localhost:8090` after deploy. Shows topics, messages, consumer group lag, and lets you produce test messages directly.
