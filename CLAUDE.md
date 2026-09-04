# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

A ticket/event sales system built with microservices architecture using Spring Boot 3.1.5, Spring Cloud 2022.0.4, Java 17, React 18, and Docker. The system handles user authentication, event management, order processing, payments, and notifications through independent services communicating via REST APIs and RabbitMQ messaging.

## Build and Run Commands

### Building the Project

```bash
# Build all microservices
mvn clean install

# Build specific service
cd <service-name>
mvn clean package

# Skip tests during build
mvn clean install -DskipTests
```

### Running Locally

```bash
# Start entire stack with Docker Compose
docker-compose up --build -d

# View logs
docker-compose logs -f <service-name>

# Stop all services
docker-compose down

# Stop and remove volumes (clean slate)
docker-compose down -v
```

### Running Individual Services

```bash
# Run service locally (requires dependencies running)
cd <service-name>
mvn spring-boot:run

# Run with specific profile
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### Testing

```bash
# Run all tests
mvn test

# Run tests for specific service
cd <service-name>
mvn test

# Run specific test class
mvn test -Dtest=OrderServiceTest

# Run specific test method
mvn test -Dtest=OrderServiceTest#testCreateOrder
```

### Frontend Development

```bash
cd frontend
npm install
npm start          # Development server on port 3000
npm test          # Run tests
npm run build     # Production build
```

## Architecture Deep Dive

### Service Communication Pattern

**Synchronous (REST)**: Frontend → API Gateway → Backend Services (via Eureka service discovery)

**Asynchronous (Events)**:
- Order created → RabbitMQ → Payments Service
- Payment processed → RabbitMQ → Orders Service + Notifications Service

### Critical Authentication Flow

1. User registers via `auth-service` (POST /api/auth/register)
2. User logs in, receives JWT token (POST /api/auth/login)
3. API Gateway validates JWT and extracts userId
4. Gateway forwards requests with `X-User-Id` header to backend services
5. Backend services use `@RequestHeader("X-User-Id")` to get authenticated user

**IMPORTANT**: Do NOT pass full User objects between services. Use userId (String) only.

### Order Processing Flow

1. **orders-service** receives order request with ticketTypeId and quantity
2. Calls **events-service** via Feign to:
   - Check ticket availability
   - Get ticket price
   - Decrement available quantity (optimistic locking)
3. Creates order with status PENDING
4. Publishes `OrderCreatedEvent` to RabbitMQ
5. **payments-service** receives event, processes payment, publishes `PaymentProcessedEvent`
6. **orders-service** updates order status (PAID or CANCELLED)
7. **notifications-service** sends email confirmation

**Circuit Breaker**: orders-service has Resilience4j circuit breaker on events-service calls. If events-service is down, orders fail gracefully.

### Database Migration Strategy

Uses **Flyway** for all database services. Migration files at:
- `src/main/resources/db/migration/V1__Initial_schema.sql`

**Hibernate DDL mode is set to `validate`** - schema changes MUST be done via Flyway migrations, never through entity changes alone.

To create new migration:
1. Create `V<next_number>__Description.sql` in db/migration folder
2. Write SQL DDL statements
3. Restart service (Flyway runs on startup)

### Configuration Management

All configuration centralized in `config-repo/` directory:
- `auth-service.yml`, `users-service.yml`, etc.
- **Config Server** serves these files to services at startup
- Environment variables override config values (see `.env.example`)

**Key Environment Variables**:
- `DB_USERNAME`, `DB_PASSWORD` - Database credentials
- `JWT_SECRET` - JWT signing key
- `DDL_AUTO` - Hibernate DDL mode (validate in prod)
- `MAIL_USERNAME`, `MAIL_PASSWORD` - SMTP credentials

### Exception Handling Pattern

All services use GlobalExceptionHandler with:
- `@ExceptionHandler(MethodArgumentNotValidException.class)` - Validation errors
- `@ExceptionHandler(ResourceNotFoundException.class)` - 404 errors
- `@ExceptionHandler(BusinessException.class)` - Business logic errors
- Returns standardized `ErrorResponse` with timestamp, message, and field errors

### Validation Pattern

Use Jakarta Bean Validation on DTOs, not entities:
- DTOs: `@Valid @RequestBody OrderRequest` with `@NotNull`, `@Min`, etc.
- Entities: Validation annotations for documentation only
- Reason: Entities are managed by JPA; validation on save can cause issues

## Service Ports

| Service | Port | Purpose |
|---------|------|---------|
| API Gateway | 8080 | Main entry point |
| Discovery Service | 8761 | Eureka dashboard |
| Config Server | 8888 | Configuration |
| Auth Service | 8081 | Authentication |
| Users Service | 8082 | User management |
| Servico Eventos | 8083 | Event/ticket management |
| Servico Pedidos | 8084 | Order management |
| Payments Service | 8085 | Payment processing |
| Notifications Service | 8086 | Email notifications |
| RabbitMQ UI | 15672 | Message broker admin |
| Prometheus | 9090 | Metrics collection |
| Grafana | 3001 | Metrics visualization |
| Frontend | 3000 | React app |

## Database Schema

Each service has its own PostgreSQL database:
- `auth_db`: users(id, username, password)
- `users_db`: users(id, username, email)
- `eventos_db`: events, ticket_types (1-to-many)
- `pedidos_db`: orders, order_items (1-to-many)
- `payments_db`: payments

**Indexes created on**:
- Foreign keys
- Frequently queried fields (status, userId, createdAt, etc.)
- See Flyway migration files for complete index definitions

## Common Gotchas

### Maven Multi-Module Build

This is a Maven multi-module project. Parent POM at root defines:
- Spring Boot version: 3.1.5
- Spring Cloud version: 2022.0.4
- Java version: 17

Always build from root first: `mvn clean install` before building individual modules.

### Docker Networking

Services communicate via Docker network `spring-cloud-network`.
- Service names in docker-compose.yml are DNS names
- Example: `events-service` calls `http://events-service:8083/api/events`
- Eureka uses service names for registration

### Feign Client Configuration

Feign clients require:
1. `@EnableFeignClients` on main application class
2. `@FeignClient(name = "service-name")` annotation
3. Circuit breaker enabled: `spring.cloud.openfeign.circuitbreaker.enabled=true`
4. Fallback class implementing the client interface

### RabbitMQ Exchange Configuration

All exchanges/queues defined in `RabbitMQConfig` classes:
- Exchange: `ticket.exchange`
- Queues: `order.created.queue`, `payment.processed.queue`, `notifications.queue`
- Routing keys pattern: `order.created`, `payment.processed`

## Monitoring and Observability

**Actuator endpoints** enabled on all services at `/actuator`:
- `/actuator/health` - Health check
- `/actuator/prometheus` - Metrics for Prometheus
- `/actuator/metrics` - Individual metrics
- `/actuator/info` - Application info

**Prometheus scraping** configured in `monitoring/prometheus/prometheus.yml` to pull metrics from all services every 15s.

**Grafana dashboards** should be configured to query Prometheus for:
- Request rates
- Error rates
- Circuit breaker states
- JVM metrics

## Development Workflow

1. **Making code changes**:
   - Modify code in service
   - Run tests: `mvn test`
   - Rebuild: `mvn clean package`
   - Rebuild Docker: `docker-compose up --build -d <service-name>`

2. **Adding new endpoint**:
   - Create DTO with validation annotations
   - Add controller method with `@Valid`
   - Implement service logic
   - Add exception handling if needed
   - Service will auto-register with Eureka

3. **Database schema change**:
   - Create new Flyway migration file
   - Run locally to test migration
   - Commit migration file with code changes

4. **Adding new service communication**:
   - Create Feign client interface
   - Add fallback class for circuit breaker
   - Configure Resilience4j in service config YAML
   - Add circuit breaker configuration

## Frontend-Backend Integration

Frontend uses environment variable `REACT_APP_API_URL` (default: http://localhost:8080) to call API Gateway.

**Authentication flow**:
1. Login → receives JWT token
2. Store token in localStorage
3. Include in Authorization header: `Bearer <token>`
4. Gateway validates and adds X-User-Id header

**API patterns**:
- All API calls go through `/api/<service-route>`
- Gateway routes by first path segment after /api
- Example: `/api/events` → events-service, `/api/orders` → orders-service

## Health Checks

Docker Compose health checks configured for all services using:
- Spring services: `curl http://localhost:<port>/actuator/health`
- PostgreSQL: `pg_isready`
- RabbitMQ: `rabbitmq-diagnostics -q ping`

Services marked as healthy after `start_period` and passing health checks.
