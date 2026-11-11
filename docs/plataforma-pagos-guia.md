# Plataforma de pagos con microservicios — Guía de inicio a fin

Proyecto de portafolio, solo backend. Transferencias entre cuentas con arquitectura orientada a eventos y notificaciones reactivas en tiempo real.

**Regla de oro:** no pases al siguiente paso hasta que el anterior funcione de punta a punta con una prueba.
  
---

## 1. Stack

| Capa | Tecnología |
|---|---|
| Lenguaje / framework | Java 21, Spring Boot 4.x |
| Reactivo (solo notificaciones) | Spring WebFlux + R2DBC, SSE |
| Mensajería | Apache Kafka |
| Base de datos | PostgreSQL (una base por servicio) + Flyway |
| Seguridad | Keycloak (JWT) |
| Descubrimiento / entrada | Eureka + Spring Cloud Gateway |
| Pruebas | JUnit 5, Testcontainers |
| CI | GitHub Actions |
| Observabilidad | Actuator, Micrometer, OpenTelemetry |
| Nube (opcional) | Azure Container Apps, ACR, Key Vault, Monitor |

---

## 2. Servicios

| Servicio | Responsabilidad | Fase |
|---|---|---|
| `account-service` | Clientes, cuentas, saldos, movimientos | 1 |
| `transaction-service` | Orquesta la transferencia (saga) | 1 |
| `notification-service` | Notifica en tiempo real (WebFlux, SSE) | 1 |
| `fraud-service` | Reglas simples de fraude | 2 |
| `api-gateway` | Entrada única, JWT, rate limiting | 2 |
| Keycloak | Autenticación (solo configurar) | 3 |

---

## 3. Modelo de datos

### account-service
- **Customer:** `id (UUID)`, `documentType`, `documentNumber`, `fullName`, `email`, `status`, `createdAt`
- **Account:** `id (UUID)`, `customerId`, `accountNumber (único)`, `currency`, `balance (NUMERIC 19,4)`, `status (ACTIVE | BLOCKED | CLOSED)`, `version (@Version)`, `createdAt`
- **AccountMovement:** `id`, `accountId`, `transferId`, `type (DEBIT | CREDIT)`, `amount`, `balanceAfter`, `createdAt` — índice único en `(transferId, accountId, type)`
- **OutboxEvent:** `id`, `aggregateId`, `eventType`, `payload (jsonb)`, `createdAt`, `publishedAt (nullable)`
- **ProcessedMessage:** `messageId (PK)`, `processedAt`

### transaction-service
- **Transfer:** `id (UUID)`, `idempotencyKey (único)`, `sourceAccount`, `targetAccount`, `amount`, `currency`, `description`, `status`, `failureReason`, `createdAt`, `updatedAt`, `version`
- **TransferStatus:** `PENDING → FRAUD_CHECK → APPROVED → COMPLETED` (salidas: `REJECTED`, `FAILED`)
- **TransferStatusHistory:** `id`, `transferId`, `status`, `reason`, `changedAt`
- **OutboxEvent**, **ProcessedMessage** (iguales que en account)

### notification-service
- **Notification:** `id`, `accountNumber`, `transferId`, `type`, `message`, `createdAt`
- `Sinks.Many<Notification>` en memoria para emitir en vivo

### fraud-service
- **FraudRule:** `id`, `code`, `threshold`, `enabled` (ej. `MAX_AMOUNT`, `MAX_TX_PER_MINUTE`)
- **FraudCheck:** `id`, `transferId`, `result (APPROVED | REJECTED)`, `reason`, `createdAt`

> Reglas: montos con `BigDecimal` / `NUMERIC(19,4)`, nunca `double`. Migraciones con Flyway, no `ddl-auto=update`.

---

## 4. Endpoints

| Servicio | Endpoint |
|---|---|
| account | `POST /customers`, `POST /accounts`, `GET /accounts/{number}`, `GET /accounts/{number}/movements`, `POST /accounts/{number}/deposit` |
| transaction | `POST /transfers` (header `Idempotency-Key`), `GET /transfers/{id}`, `GET /transfers/{id}/history` |
| notification | `GET /notifications/stream?account=XXXX` (SSE), `GET /notifications?account=XXXX` |

---

## 5. Catálogo de eventos

Sobre común: `eventId`, `eventType`, `occurredAt`, `correlationId` (= `transferId`), `payload`.

| Evento | Topic | Publica | Consumen |
|---|---|---|---|
| `TransferInitiated` | `transfer.events` | transaction | fraud, notification |
| `FraudCheckApproved` / `FraudCheckRejected` | `fraud.events` | fraud | transaction, account |
| `FundsMoved` / `FundsFailed` | `account.events` | account | transaction |
| `TransferCompleted` / `TransferFailed` | `transfer.events` | transaction | notification |

Débito y crédito se hacen en **una sola transacción local** dentro de `account-service`. La saga es entre servicios, no dentro del movimiento de dinero.
Si aún no existe fraud, salta de `TransferInitiated` directo a `FundsMoved`.

---

## 6. Flujo de una transferencia

```
Cliente ──POST /transfers──▶ [Gateway] ──▶ transaction-service
                                              │ guarda PENDING + outbox
                                              ▼
                                   TransferInitiated (Kafka)
                                    │                  │
                                    ▼                  ▼
                              fraud-service     notification-service
                                    │              (SSE: "recibida")
                       Approved / Rejected
                                    │
                    ┌───────────────┴───────────────┐
                    ▼                               ▼
             account-service                transaction-service
        (debita+acredita, outbox)           (REJECTED, notifica)
                    │
             FundsMoved / FundsFailed
                    ▼
            transaction-service
      (COMPLETED / FAILED + historial)
                    │
          TransferCompleted / TransferFailed
                    ▼
            notification-service ──SSE──▶ Cliente
```

---

## 7. Estructura del repositorio (monorepo)

```
payments-platform/
├── docker-compose.yml
├── README.md
├── docs/
│   ├── decisiones.md
│   ├── eventos.md
│   └── arquitectura.png
├── account-service/
├── transaction-service/
├── notification-service/
├── fraud-service/
├── api-gateway/
└── discovery-service/
```

---

## 8. Ruta de construcción

### Paso 0 — Preparación
- [ ] Crear repositorio y estructura de carpetas
- [ ] Escribir `docs/eventos.md` con el catálogo de eventos (el mapa del proyecto)
- [ ] Instalar Docker, JDK 21, Maven o Gradle, Postman

### Paso 1 — `account-service` (sin eventos)
- [ ] Proyecto Spring Boot con Web, JPA, PostgreSQL, Flyway, Validation
- [ ] Migraciones V1 con `customer`, `account`, `account_movement`
- [ ] CRUD de clientes y cuentas, depósito de prueba
- [ ] Bloqueo optimista con `@Version` en `Account`
- **Listo cuando:** creas cuentas, depositas y ves saldo y movimientos por Postman

### Paso 2 — Infraestructura local
- [ ] `docker-compose.yml` con Postgres (bases `accounts_db`, `transactions_db`, `notifications_db`, `fraud_db`), Kafka y Kafka UI
- **Listo cuando:** `docker compose up` levanta todo sin errores

### Paso 3 — `transaction-service` + Outbox
- [ ] Entidad `Transfer`, `TransferStatusHistory`, `OutboxEvent`
- [ ] `POST /transfers` guarda `PENDING` y el evento en la misma transacción
- [ ] Publicador del outbox hacia Kafka (tarea programada)
- **Listo cuando:** `TransferInitiated` aparece en el topic

### Paso 4 — Movimiento de fondos
- [ ] `account-service` consume `TransferInitiated`, debita y acredita en una transacción
- [ ] Publica `FundsMoved` / `FundsFailed` (con outbox)
- [ ] `transaction-service` consume y actualiza el estado a `COMPLETED` / `FAILED`
- **Listo cuando:** una transferencia termina en `COMPLETED` y los saldos cuadran

### Paso 5 — Idempotencia
- [ ] Header `Idempotency-Key` con índice único en `Transfer`
- [ ] Tabla `ProcessedMessage` en consumidores
- **Listo cuando:** enviar dos veces la misma solicitud mueve el dinero una sola vez

### Paso 6 — `notification-service` (reactivo)
- [ ] WebFlux + R2DBC, consumidor reactivo de Kafka
- [ ] Endpoint SSE por cuenta
- **Listo cuando:** `curl -N` al stream muestra los estados de la transferencia en vivo

### Paso 7 — `fraud-service`
- [ ] Reglas `MAX_AMOUNT` y `MAX_TX_PER_MINUTE`
- [ ] Insertar la verificación entre `TransferInitiated` y el movimiento de fondos
- **Listo cuando:** un monto alto termina en `REJECTED` y notifica

### Paso 8 — Gateway, descubrimiento y seguridad
- [ ] Eureka (`discovery-service`) y Spring Cloud Gateway
- [ ] Keycloak en Docker, validación de JWT en el gateway
- [ ] Rate limiting básico
- **Listo cuando:** sin token responde 401 y con token funciona el flujo completo

### Paso 9 — Calidad y presentación
- [ ] Pruebas de integración con Testcontainers (Postgres + Kafka)
- [ ] Prueba de extremo a extremo: transferencia completa y transferencia rechazada
- [ ] GitHub Actions: build + tests
- [ ] Actuator, Micrometer y trazas
- [ ] README con diagrama, cómo ejecutar, decisiones técnicas y GIF de las notificaciones
- **Listo cuando:** cualquiera clona el repo y lo levanta con un comando

---

## 9. Fase opcional — Azure

Solo cuando el paso 9 esté terminado.

- [ ] Presupuesto con alerta en Cost Management (ej. 5 USD)
- [ ] Key Vault: mover contraseñas y claves fuera del `application.yml`
- [ ] Azure Container Registry: subir imágenes
- [ ] Azure Database for PostgreSQL Flexible Server (una instancia, varias bases)
- [ ] Container Apps: desplegar primero `account-service`, luego el resto
- [ ] Azure Monitor / Application Insights: trazas entre servicios
- [ ] Opcional: APIM delante del gateway
- [ ] Opcional: AKS temporal (borrarlo el mismo día)

> Verifica en el portal qué es gratis para tu cuenta antes de crear recursos.

---

## 10. Fase opcional — Migración a Quarkus

Orden sugerido, un servicio a la vez, respetando los contratos de eventos y endpoints:

1. `notification-service` (el más aislado)
2. `fraud-service`
3. `account-service` (con pruebas sólidas)
4. `transaction-service` y gateway (opcional)

Equivalencias clave: Spring Data → Hibernate ORM con Panache, Spring Kafka → SmallRye Reactive Messaging, Spring Security → `quarkus-oidc`, Testcontainers → Dev Services.
Al final, documentar una comparación Spring vs Quarkus (arranque, memoria, tamaño de imagen).

---

## 11. Decisiones para documentar en `docs/decisiones.md`

- Por qué Kafka y no RabbitMQ
- Por qué una base de datos por servicio
- Por qué outbox + idempotencia
- Por qué débito y crédito en una sola transacción local
- Por qué WebFlux solo en notificaciones
- Por qué CQRS/Event Sourcing solo se considera en cuentas (si se aplica)

---

## 12. Checklist final del CV

- [ ] Repositorio público con README claro y diagrama
- [ ] `docker-compose up` levanta todo
- [ ] Pruebas automatizadas corriendo en CI
- [ ] Demo de notificaciones en tiempo real (GIF o video corto)
- [ ] Sección de decisiones técnicas
- [ ] Una línea en el CV: *"Plataforma de pagos con microservicios, Kafka, patrón Outbox, saga e idempotencia, notificaciones reactivas con WebFlux/SSE"*
