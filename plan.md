# Distributed Task Processing Platform - Implementation Plan

## Overview
A microservices-based task processing system with full observability using Java 21 + Spring Boot 3.2+.

---

## Architecture Diagram
```
                                    ┌─────────────────┐
                                    │   Grafana       │
                                    │  (Dashboards)   │
                                    └────────┬────────┘
                                             │
                    ┌────────────────────────┼────────────────────────┐
                    │                        │                        │
              ┌─────▼─────┐           ┌──────▼──────┐          ┌──────▼──────┐
              │ Prometheus│           │    Loki     │          │   Tempo     │
              │ (Metrics) │           │   (Logs)    │          │  (Traces)   │
              └─────▲─────┘           └──────▲──────┘          └──────▲──────┘
                    │                        │                        │
    ════════════════╪════════════════════════╪════════════════════════╪═══════
                    │                        │                        │
                    │              ┌─────────┴─────────┐              │
                    │              │                   │              │
              ┌─────┴─────┐  ┌─────┴─────┐  ┌─────────┴┐  ┌──────────┴┐
              │   API     │  │   Task    │  │Orchestr- │  │  Worker   │
    User ────►│  Gateway  ├─►│  Service  │  │  ator    │  │  Service  │
              │  :8080    │  │  :8081    │  │  :8082   │  │  :8083    │
              └───────────┘  └─────┬─────┘  └────┬─────┘  └─────┬─────┘
                                   │             │              │
                                   │      ┌──────┴──────┐       │
                                   │      │             │       │
                                   ▼      ▼             ▼       ▼
                             ┌─────────────────────────────────────┐
                             │              Kafka                  │
                             │  task.created → task.process →      │
                             │  task.completed / task.failed       │
                             └─────────────────────────────────────┘
                                              │
                                   ┌──────────┴──────────┐
                                   │  Notification       │
                                   │  Service :8084      │
                                   └─────────────────────┘
```

---

## Project Structure
```
TODO-app/
├── docker-compose.yml           # Full infrastructure stack
├── docker-compose.dev.yml       # Dev overrides (infrastructure only)
├── .env                         # Environment variables
├── pom.xml                      # Parent POM (multi-module)
├── infrastructure/
│   ├── prometheus/prometheus.yml
│   ├── grafana/provisioning/
│   ├── loki/loki-config.yml
│   ├── tempo/tempo-config.yml
│   └── init-scripts/init.sql
├── common/                      # Shared DTOs, trace utilities
├── api-gateway/                 # Spring Cloud Gateway
├── task-service/                # Task CRUD + Kafka producer
├── orchestrator-service/        # Workflow logic
├── worker-service/              # Task execution + retry
└── notification-service/        # Event notifications
```

---

## Implementation Phases

### Phase 1: Infrastructure ✅ (Partially Complete)
| File | Status | Description |
|------|--------|-------------|
| `docker-compose.yml` | ✅ | Full stack with Kafka, Postgres, observability |
| `docker-compose.dev.yml` | ✅ | Dev mode (infra only) |
| `.env` | ✅ | Environment variables |
| `prometheus/prometheus.yml` | ✅ | Metrics scraping config |
| `loki/loki-config.yml` | ✅ | Log aggregation config |
| `tempo/tempo-config.yml` | ✅ | Distributed tracing config |
| `init-scripts/init.sql` | ✅ | Database schema |
| `grafana/provisioning/` | ✅ | Datasources configured |

### Phase 2: Common Module
| Component | Description |
|-----------|-------------|
| `TaskEvent` | DTO for Kafka messages |
| `TaskStatus` | Enum: PENDING, PROCESSING, COMPLETED, FAILED |
| `TaskPriority` | Enum: LOW, NORMAL, HIGH, CRITICAL |
| `TraceContext` | Trace ID/Span ID wrapper |
| `KafkaHeaderUtils` | Trace propagation helpers |

### Phase 3: Task Service
| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/tasks` | POST | Create new task |
| `/api/tasks/{id}` | GET | Get task by ID |
| `/api/tasks` | GET | List tasks (paginated) |
| `/api/tasks/{id}/status` | GET | Get task status |

**Flow:**
1. Receive task request
2. Validate and save to PostgreSQL
3. Publish `task.created` event to Kafka
4. Return task ID with trace ID

### Phase 4: API Gateway
| Feature | Description |
|---------|-------------|
| Routing | Route `/api/tasks/**` → task-service |
| Tracing | Inject/propagate trace headers |
| Logging | Request/response logging with trace context |
| Rate Limiting | Optional rate limiting |

### Phase 5: Orchestrator Service
| Input Topic | Output Topic | Logic |
|-------------|--------------|-------|
| `task.created` | `task.process` | Validate, enrich, route |

**Workflow:**
1. Consume `task.created`
2. Validate task payload
3. Enrich with metadata
4. Determine execution strategy
5. Publish to `task.process`

### Phase 6: Worker Service
| Feature | Description |
|---------|-------------|
| Consumption | Consume from `task.process` |
| Execution | Simulate task processing |
| Retry | Max 3 retries, exponential backoff |
| DLQ | Send to `task.dlq` after max retries |
| Completion | Publish `task.completed` or `task.failed` |

**Retry Strategy:**
- Attempt 1: Immediate
- Attempt 2: Wait 1 second
- Attempt 3: Wait 4 seconds
- Attempt 4: Send to DLQ

### Phase 7: Notification Service
| Input Topics | Action |
|--------------|--------|
| `task.completed` | Log success notification |
| `task.failed` | Log failure notification |

### Phase 8: Observability
| Tool | Purpose | Access |
|------|---------|--------|
| Prometheus | Metrics collection | http://localhost:9090 |
| Grafana | Dashboards | http://localhost:3000 |
| Loki | Log aggregation | Via Grafana |
| Tempo | Distributed traces | Via Grafana |

**Key Metrics:**
- `task_created_total` - Counter
- `task_completed_total` - Counter
- `task_failed_total` - Counter
- `task_processing_duration_seconds` - Histogram
- `kafka_consumer_lag` - Gauge

### Phase 9: CI/CD
| Stage | Actions |
|-------|---------|
| Build | Maven build + tests |
| Package | Docker image build |
| Push | Push to Docker Hub |
| Deploy | Deploy to VPS |

---

## Kafka Topics

| Topic | Producer | Consumer | Purpose |
|-------|----------|----------|---------|
| `task.created` | task-service | orchestrator | New task notification |
| `task.process` | orchestrator | worker | Ready for execution |
| `task.completed` | worker | notification | Task succeeded |
| `task.failed` | worker | notification | Task failed |
| `task.dlq` | worker | (manual) | Dead letter queue |

---

## Technology Stack

| Category | Technology | Version |
|----------|------------|---------|
| Language | Java | 21 (LTS) |
| Framework | Spring Boot | 3.2+ |
| Build | Maven | 3.9+ |
| Gateway | Spring Cloud Gateway | 4.1+ |
| Messaging | Spring Kafka | 3.1+ |
| Database | PostgreSQL | 16 |
| ORM | Spring Data JPA | 3.2+ |
| Tracing | OpenTelemetry | 1.32+ |
| Metrics | Micrometer | 1.12+ |
| Logging | Logback + JSON | 1.4+ |

---

## Quick Start

```bash
# 1. Start infrastructure only (dev mode)
docker-compose -f docker-compose.yml -f docker-compose.dev.yml up -d

# 2. Build all modules
./mvnw clean package -DskipTests

# 3. Run services locally (separate terminals)
./mvnw spring-boot:run -pl task-service
./mvnw spring-boot:run -pl orchestrator-service
./mvnw spring-boot:run -pl worker-service
./mvnw spring-boot:run -pl notification-service
./mvnw spring-boot:run -pl api-gateway

# 4. Test the flow
curl -X POST http://localhost:8080/api/tasks \
  -H "Content-Type: application/json" \
  -d '{"name": "Test Task", "type": "COMPUTE", "payload": {"data": "test"}}'

# 5. View observability
# Grafana: http://localhost:3000 (admin/admin)
# Prometheus: http://localhost:9090
```

---

## Next Steps (Practice Together)

1. **Phase 2**: Create parent `pom.xml` and `common` module
2. **Phase 3**: Build `task-service` with REST + Kafka
3. **Phase 4**: Configure `api-gateway` routing
4. **Phase 5**: Implement `orchestrator-service`
5. **Phase 6**: Build `worker-service` with retry logic
6. **Phase 7**: Create `notification-service`
7. **Phase 8**: Add Grafana dashboards
8. **Phase 9**: Set up GitHub Actions CI/CD

---

## Files Created So Far

- [x] `.env`
- [x] `docker-compose.yml`
- [x] `docker-compose.dev.yml`
- [x] `infrastructure/prometheus/prometheus.yml`
- [x] `infrastructure/loki/loki-config.yml`
- [x] `infrastructure/tempo/tempo-config.yml`
- [x] `infrastructure/init-scripts/init.sql`
- [x] `infrastructure/grafana/provisioning/datasources/datasources.yml`
- [x] `infrastructure/grafana/provisioning/dashboards/dashboards.yml`
