# Distributed Task Processing Platform

A microservices-based task processing system built with Java 21, Spring Boot 3.2, Apache Kafka, and a full observability stack. Designed to reflect enterprise patterns used by companies like LinkedIn, Uber, and Netflix.

---

## Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                                                                             │
│    ┌─────────┐     ┌─────────────┐     ┌─────────────┐                      │
│    │  NGINX  │────▶│ API GATEWAY │────▶│AUTH SERVICE │                      │
│    │  (VPS)  │     │    :8080    │     │    :8081    │                      │
│    └─────────┘     └──────┬──────┘     └─────────────┘                      │
│                           │                                                 │
│                           ▼                                                 │
│                    ┌─────────────┐                                          │
│                    │TASK SERVICE │                                          │
│                    │    :8082    │                                          │
│                    └──────┬──────┘                                          │
│                           │ Kafka events                                    │
│         ┌─────────────────┼─────────────────┐                               │
│         ▼                 ▼                 ▼                               │
│  ┌─────────────┐   ┌─────────────┐   ┌─────────────┐                        │
│  │ORCHESTRATOR │   │   WORKER    │   │NOTIFICATION │                        │
│  │    :8083    │   │    :8084    │   │    :8085    │                        │
│  └─────────────┘   └─────────────┘   └─────────────┘                        │
│                                                                             │
│  ════════════════════ OBSERVABILITY ════════════════════                    │
│  Prometheus :9090 │ Grafana :3000 │ Loki │ Tempo :3200                      │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Request Flow (End-to-End)

```
1. User → POST /api/tasks (Authorization: Bearer <token>)
2. Nginx (production) → SSL termination → forward to API Gateway
3. API Gateway → validate JWT with auth-service → add X-User-Id header → forward to task-service
4. Task Service → save to PostgreSQL → publish TaskEvent to Kafka topic "task.created"
5. Orchestrator → consume "task.created" → validate → publish to "task.process"
6. Worker → consume "task.process" → execute → publish "task.completed" or "task.failed"
7. Notification → consume result → send notification
```

---

## Tech Stack

| Category | Technology | Purpose |
|----------|------------|---------|
| Language | Java 21 (LTS) | Virtual threads, modern features |
| Framework | Spring Boot 3.2 | Microservices foundation |
| Build | Maven (multi-module) | Shared dependency management |
| Gateway | Spring Cloud Gateway | Routing, JWT validation |
| Messaging | Apache Kafka | Async event streaming |
| Schema | Avro + Schema Registry | Typed message contracts |
| Database | PostgreSQL 16 | Task and user persistence |
| Auth | Spring Security + JJWT | JWT issuance and validation |
| Tracing | OpenTelemetry + Tempo | Distributed trace correlation |
| Metrics | Micrometer + Prometheus | Per-service metrics |
| Logging | Logback + Loki | Structured JSON logs |
| Dashboards | Grafana | Unified observability UI |
| Frontend | SvelteKit + nginx | SPA served via Docker, proxies `/api/*` to gateway |

---

## Project Structure

```
TODO-app/
├── pom.xml                          # Parent POM (multi-module)
├── mvnw / mvnw.cmd                  # Maven wrapper (used by CI)
├── docker-compose.yml               # Full stack (infra + services + frontend)
├── docker-compose.dev.yml           # Dev mode (infrastructure only)
├── .env                             # Environment variables
│
├── schemas/                         # Avro schema definitions (source of truth)
│   └── src/main/avro/
│       ├── task-event.avsc          # Main Kafka message contract
│       ├── task-status.avsc         # Enum: PENDING, PROCESSING, COMPLETED...
│       ├── task-priority.avsc       # Enum: LOW, NORMAL, HIGH, CRITICAL
│       ├── task-type.avsc           # Enum: COMPUTE, IO, BATCH...
│       ├── user-role.avsc           # Enum: USER, ADMIN, SERVICE
│       └── auth-event.avsc          # Auth audit events
│
├── api-gateway/                     # Edge service - routing & auth validation
├── auth-service/                    # User registration, login, JWT tokens
├── task-service/                    # Task CRUD + Kafka producer
├── orchestrator-service/            # Validates tasks, routes to workers
├── worker-service/                  # Executes tasks, retry, DLQ
├── notification-service/            # Consumes completed/failed events
│
├── frontend/                        # SvelteKit SPA
│   ├── Dockerfile                   # node:20 build → nginx:alpine serve
│   ├── nginx.conf                   # Proxies /api/* → api-gateway, SPA fallback
│   └── src/
│       ├── lib/api.js               # Fetch wrapper with auto token refresh
│       ├── lib/stores.js            # Auth store (token + user)
│       └── routes/
│           ├── login/               # Login page
│           ├── register/            # Register page
│           ├── tasks/               # Task list + create modal
│           └── tasks/[id]/          # Task detail view
│
├── nginx/
│   └── taskplatform.conf            # VPS Nginx config (SSL, rate limiting)
│
├── infrastructure/
│   ├── prometheus/prometheus.yml
│   ├── grafana/provisioning/
│   ├── loki/loki-config.yml
│   ├── tempo/tempo-config.yml
│   └── init-scripts/init.sql        # DB schema (users, tasks, audit logs)
│
└── .github/workflows/ci.yml         # CI/CD: build → docker → deploy
```

---

## Quick Start

### Prerequisites

- Java 21+
- Maven 3.9+
- Docker & Docker Compose
- Node.js 20+ *(only if running frontend locally outside Docker)*

### Option A — Full Stack in Docker (recommended)

Runs everything: infrastructure + all services + frontend.

```bash
docker-compose up -d
```

| URL | What |
|-----|------|
| http://localhost:3001 | Frontend (SvelteKit) |
| http://localhost:8080 | API Gateway |
| http://localhost:3000 | Grafana (admin/admin) |
| http://localhost:9090 | Prometheus |

### Option B — Dev Mode (infrastructure in Docker, code runs locally)

Use this when actively developing a specific service — faster restarts, no Docker rebuilds.

```bash
# Step 1 — start infrastructure only
docker-compose -f docker-compose.yml -f docker-compose.dev.yml up -d

# Step 2 — run backend services (one terminal each)
./mvnw spring-boot:run -pl auth-service
./mvnw spring-boot:run -pl task-service
./mvnw spring-boot:run -pl orchestrator-service
./mvnw spring-boot:run -pl worker-service
./mvnw spring-boot:run -pl notification-service
./mvnw spring-boot:run -pl api-gateway

# Step 3 — run frontend
cd frontend && npm install && npm run dev
# http://localhost:3001
```

### Verify Infrastructure

```bash
docker-compose ps

# Kafka topics
docker exec -it kafka kafka-topics --list --bootstrap-server localhost:9092

# Schema Registry
curl http://localhost:8086/subjects
```

### Test the API (curl)

```bash
# 1. Register
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username": "testuser", "email": "test@example.com", "password": "Password123"}'

# 2. Login → copy accessToken from response
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "testuser", "password": "Password123"}'

# 3. Create task
curl -X POST http://localhost:8080/api/tasks \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <accessToken>" \
  -d '{"name": "Process Data", "type": "COMPUTE", "priority": "HIGH"}'

# 4. List tasks
curl http://localhost:8080/api/tasks \
  -H "Authorization: Bearer <accessToken>"
```

---

## How It Works

### API Gateway Routing

The gateway matches routes by **path predicates** — first match wins:

```yaml
routes:
  # Route 1: public (no auth)
  - id: auth-public
    uri: http://auth-service:8081
    predicates:
      - Path=/api/auth/login, /api/auth/register, /api/auth/refresh

  # Route 2: protected auth endpoints
  - id: auth-protected
    uri: http://auth-service:8081
    predicates:
      - Path=/api/auth/**
    filters:
      - AuthenticationFilter        # validates JWT

  # Route 3: task endpoints
  - id: task-service
    uri: http://task-service:8082
    predicates:
      - Path=/api/tasks/**
    filters:
      - AuthenticationFilter        # validates JWT
```

When `AuthenticationFilter` runs:
1. Extracts `Authorization: Bearer <token>` header
2. Calls `auth-service /api/auth/validate?token=<token>`
3. If valid → adds `X-User-Id`, `X-User-Name`, `X-User-Roles` headers → forwards request
4. If invalid → returns `401 Unauthorized` immediately

### Worker Retry Logic

```
Task arrives (retryCount = 0)
       │
  ┌────▼────┐
  │ EXECUTE │
  └────┬────┘
       │
  ┌────┴────┐
  │ SUCCESS │──→ publish task.completed
  │ FAILURE │──→ retryCount < 3?
  └─────────┘         │
                 YES ─┤─ NO
                  │        │
          retryCount++    task.dlq
          publish task.failed
          (worker picks up again)
```

Exponential backoff: 1s → 2s → 4s → DLQ after 3rd failure.

### Avro Schema Flow

```
schemas/src/main/avro/task-event.avsc
          │
          │ mvn generate-sources
          ▼
schemas/target/generated-sources/avro/
  com/taskplatform/schemas/TaskEvent.java   ← auto-generated
  com/taskplatform/schemas/TaskStatus.java  ← auto-generated
          │
          │ services add schemas as dependency
          ▼
task-service    → builds TaskEvent, sends to Kafka (binary)
orchestrator    → reads TaskEvent from Kafka (type-safe)
worker          → reads TaskEvent from Kafka (type-safe)
```

Schema Registry validates every message — breaking schema changes are **rejected at publish time**, not at runtime.

> **Real world:** The `schemas/` module lives in its own dedicated repository, versioned with semver, published to Nexus/Artifactory. Each service pins to a specific version. We keep it in this repo for simplicity.

---

## Services

### API Gateway (`:8080`)

| Route | Target | Auth |
|-------|--------|------|
| `POST /api/auth/login` | auth-service | No |
| `POST /api/auth/register` | auth-service | No |
| `POST /api/auth/refresh` | auth-service | No |
| `* /api/auth/**` | auth-service | Yes |
| `* /api/tasks/**` | task-service | Yes |

### Auth Service (`:8081`)

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/auth/register` | POST | Create user, return tokens |
| `/api/auth/login` | POST | Validate credentials, return tokens |
| `/api/auth/refresh` | POST | Rotate refresh token |
| `/api/auth/validate` | GET | Validate JWT (called by gateway) |
| `/api/auth/logout` | POST | Revoke all refresh tokens |

JWT tokens:
- **Access token:** 15 minutes, used for API calls
- **Refresh token:** 7 days, stored hashed in DB, rotated on use

### Task Service (`:8082`)

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/tasks` | POST | Create task → publishes `task.created` |
| `/api/tasks` | GET | List tasks (paginated, sortable) |
| `/api/tasks/{id}` | GET | Get task by ID |
| `/api/tasks/count` | GET | Count user's tasks |

### Orchestrator (`:8083`)

- Consumes: `task.created`
- Validates task fields
- Enriches with metadata
- Publishes: `task.process` (valid) or `task.failed` (invalid)

### Worker (`:8084`)

- Consumes: `task.process`
- Executes task (simulated based on type: COMPUTE / IO / BATCH)
- Retries up to 3 times with exponential backoff
- Publishes: `task.completed`, `task.failed`, or `task.dlq`

### Notification Service (`:8085`)

- Consumes: `task.completed`, `task.failed`
- Logs notifications (production would integrate email/Slack/webhook)

---

## Frontend (`:3001`)

A SvelteKit SPA served via nginx inside Docker.

### Pages

| Route | Description |
|-------|-------------|
| `/login` | Sign in with username + password |
| `/register` | Create a new account |
| `/tasks` | Task list with status filters, pagination, create modal |
| `/tasks/:id` | Task detail — fields, timeline, error info, trace ID |

### How it connects to the backend

The frontend container runs nginx which **proxies `/api/*` requests to `api-gateway:8080`** on the internal Docker network. The browser never talks to the backend directly — no CORS issues, no hardcoded backend URLs in the JS bundle.

```
Browser → localhost:3001/api/tasks
       → nginx (frontend container)
       → http://api-gateway:8080/api/tasks
```

### Running locally (outside Docker)

```bash
cd frontend
npm install
npm run dev     # http://localhost:3001
```

> Note: In dev mode (`npm run dev`), Vite proxies `/api/*` to `http://localhost:8080` automatically so the backend services must be running locally or via `docker-compose.dev.yml`.

---

## Kafka Topics

| Topic | Producer | Consumer | Purpose |
|-------|----------|----------|---------|
| `task.created` | task-service | orchestrator | New task |
| `task.process` | orchestrator | worker | Ready to execute |
| `task.completed` | worker | notification | Success |
| `task.failed` | worker | notification | Failure (also triggers retry) |
| `task.dlq` | worker | manual review | Exhausted all retries |

---

## Observability

| Tool | URL | Purpose |
|------|-----|---------|
| Grafana | http://localhost:3000 (admin/admin) | Dashboards, logs, traces |
| Prometheus | http://localhost:9090 | Raw metrics |
| Tempo | via Grafana | Distributed traces |
| Loki | via Grafana | Log aggregation |

### Key Metrics

```promql
rate(task_created_total[5m])          # task throughput
rate(task_completed_total[5m])         # success rate
task_dlq_total                         # dead letter queue (should be 0)
histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m]))
```

Every log line includes `traceId` — you can paste a traceId from logs directly into Tempo to see the full request path across all services.

---

## Configuration

### Environment Variables (`.env`)

```bash
POSTGRES_DB=taskdb
POSTGRES_USER=taskuser
POSTGRES_PASSWORD=taskpass

KAFKA_BOOTSTRAP_SERVERS=kafka:9092
SCHEMA_REGISTRY_URL=http://schema-registry:8081

JWT_SECRET=your-256-bit-secret-key-here   # min 32 chars

API_GATEWAY_PORT=8080
AUTH_SERVICE_PORT=8081
TASK_SERVICE_PORT=8082
ORCHESTRATOR_SERVICE_PORT=8083
WORKER_SERVICE_PORT=8084
NOTIFICATION_SERVICE_PORT=8085
```

### Spring Profiles

| Profile | When Used | DB/Kafka host |
|---------|-----------|---------------|
| `default` | Local dev | `localhost` |
| `docker` | Docker Compose | Container names (`postgres`, `kafka`) |

---

## Production Deployment (VPS)

### 1. Server Setup

```bash
# Install Docker
curl -fsSL https://get.docker.com | sh

# Install Nginx + Certbot
sudo apt install nginx certbot python3-certbot-nginx
```

### 2. Deploy

```bash
git clone <repo> /opt/task-platform
cd /opt/task-platform
cp .env.example .env          # fill in real secrets
docker-compose up -d
```

### 3. Configure Nginx

```bash
sudo cp nginx/taskplatform.conf /etc/nginx/sites-available/
# Edit: replace taskplatform.example.com with your domain
sudo ln -s /etc/nginx/sites-available/taskplatform.conf /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl reload nginx
```

### 4. SSL

```bash
sudo certbot --nginx -d your-domain.com
```

Nginx handles: SSL termination, rate limiting (10 req/s API, 5 req/s auth), security headers.

### CI/CD (GitHub Actions)

`.github/workflows/ci.yml` runs on every push to `main`:

1. **Build** — Maven build + tests (uses `./mvnw`)
2. **Docker** — Build and push images to GHCR (one job per service)
3. **Deploy** — SSH to VPS, `docker-compose pull && docker-compose up -d`

#### Required Setup

**1. Create a `production` environment**

Go to repo → **Settings → Environments → New environment** → name it `production`.

**2. Add secrets to the `production` environment**

| Secret | Value |
|--------|-------|
| `VPS_HOST` | Your VPS IP or hostname |
| `VPS_USERNAME` | SSH user (e.g. `ubuntu`) |
| `VPS_SSH_KEY` | Private SSH key (contents of `~/.ssh/id_rsa`) |

> Secrets must be added to the **environment**, not just repository secrets, because the deploy job uses `environment: production`.

**3. Maven wrapper permissions**

The `mvnw` script is committed with the executable bit set (`chmod 755`). If you re-generate it on Windows, run:

```bash
git update-index --chmod=+x mvnw
```

---

## What This Project Intentionally Excludes

This project covers ~80% of real-world microservice patterns. The following are deliberately excluded to keep the focus on core concepts:

| Feature | What It Does | Why Excluded |
|---------|-------------|--------------|
| **Kubernetes** | Orchestrates containers across multiple servers, auto-scaling, self-healing | Steep learning curve; Docker Compose teaches the same patterns |
| **Service Registry** (Eureka) | Dynamic service discovery | Made redundant by K8s DNS; Docker Compose handles hostnames |
| **Circuit Breaker** | Stops calling a failing service immediately, prevents cascade failures | Dependency included (Resilience4j), not wired up — straightforward addition |
| **Rate Limiting in Gateway** | Per-client request throttling | Nginx covers edge rate limiting; Gateway-level needs Redis |
| **API Versioning** (`/v1/`, `/v2/`) | Run old and new API contracts simultaneously | No external clients yet; add when making breaking changes |
| **Idempotency Keys** | Prevent duplicate task creation on client retry | Needs Redis; critical for payments, not needed here |
| **Service Mesh** (Istio) | Automatic mTLS, retries, metrics via sidecar proxies | Requires K8s; adds operational complexity for 50+ service setups |
| **Secrets Management** (Vault) | Centralized secret rotation, no secrets in env files | Good practice; `.env` is acceptable for learning |
| **Separate DB per service** | Each service owns its schema, no shared tables | Simplified to single PostgreSQL instance for learning |

### Learning Path

```
Now      → Docker Compose + Spring Boot microservices (this project)
Next     → Add Redis (enable rate limiting + idempotency)
Then     → Learn Kubernetes basics (minikube locally)
Then     → Wire up Circuit Breaker (Resilience4j already in pom.xml)
Then     → Explore Istio / Linkerd (service mesh)
Advanced → Temporal (workflow orchestration), separate DBs, Vault
```

---

## Development Guide

### Adding a New Service

1. Create module directory and `pom.xml` with parent reference
2. Add `<module>new-service</module>` to root `pom.xml`
3. Add service to `docker-compose.yml` with health check
4. Add Prometheus scrape config in `infrastructure/prometheus/prometheus.yml`

### Adding a New Avro Schema

```bash
# 1. Add .avsc file to schemas/src/main/avro/

# 2. Regenerate Java classes
./mvnw generate-sources -pl schemas

# 3. Classes appear in schemas/target/generated-sources/avro/
#    Import and use in any service
```

### Schema Compatibility Rules

| Change | Allowed |
|--------|---------|
| Add optional field with default | Yes |
| Remove optional field | Yes |
| Add required field (no default) | No |
| Rename a field | No |
| Change field type | No |

---

## Troubleshooting

### Service Health

```bash
curl http://localhost:8080/actuator/health  # gateway
curl http://localhost:8081/actuator/health  # auth
curl http://localhost:8082/actuator/health  # task
curl http://localhost:8083/actuator/health  # orchestrator
curl http://localhost:8084/actuator/health  # worker
curl http://localhost:8085/actuator/health  # notification
```

### Kafka

```bash
docker logs kafka
docker exec -it kafka kafka-topics --list --bootstrap-server localhost:9092
docker exec -it kafka kafka-consumer-groups --bootstrap-server localhost:9092 --list
```

### Schema Registry

```bash
curl http://localhost:8086/subjects                              # list schemas
curl http://localhost:8086/subjects/task.created-value/versions # list versions
```

### Database

```bash
docker exec -it postgres psql -U taskuser -d taskdb -c "\dt"   # list tables
docker exec -it postgres psql -U taskuser -d taskdb -c "SELECT id, name, status FROM tasks LIMIT 10;"
```

---

## License

MIT License
