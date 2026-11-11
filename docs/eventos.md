# Catálogo de eventos

Contrato de comunicación asíncrona entre los servicios. Se actualiza cada vez que se agrega o cambia un evento.

> **Estado:** `Implementado` = ya existe en el código. `Borrador` = diseño previsto, se confirma al implementarlo.

---

## 1. Convenciones

### Mensaje en Kafka

| Elemento | Valor |
|---|---|
| **Key** | `transferId` como `String`. Todos los eventos de una misma transferencia caen en la misma partición y se leen en orden |
| **Value** | JSON (como `String`) con el sobre descrito abajo |
| **Header `eventType`** | Nombre del evento, para filtrar sin parsear el cuerpo |

### Sobre del evento

```json
{
  "eventId": "transaction-service-1",
  "eventType": "TransferInitiated",
  "occurredAt": "2026-11-10T18:37:39.638953",
  "correlationId": "6e92683e-18bd-4ac3-8c49-299f6b147300",
  "payload": { }
}
```

| Campo | Tipo | Descripción |
|---|---|---|
| `eventId` | `String` | Id **estable** del evento: `<servicio>-<id en outbox_event>`. Se repite igual en cada reenvío, y los consumidores lo usan para detectar duplicados |
| `eventType` | `String` | Nombre del evento |
| `occurredAt` | `String` (ISO-8601) | Fecha de creación del evento en el outbox. Hoy es `LocalDateTime`, sin zona horaria |
| `correlationId` | `String` (UUID) | Id de la transferencia a la que pertenece el evento |
| `payload` | `Object` | Datos de negocio del evento |

### Garantías

- **Entrega al menos una vez (*at-least-once*).** Un evento puede llegar repetido, por ejemplo si se publicó pero falló el marcado de `published_at`.
- **Los consumidores deben ser idempotentes**, deduplicando por `eventId` con la tabla `processed_message`.
- **El orden solo está garantizado dentro de una misma transferencia** (misma key, misma partición).

---

## 2. Topics

| Topic | Particiones | Eventos | Estado |
|---|---|---|---|
| `transfer.events` | 3 | `TransferInitiated`, `TransferCompleted`, `TransferFailed` | Creado |
| `account.events` | por definir | `FundsMoved`, `FundsFailed` | Borrador |
| `fraud.events` | por definir | `FraudCheckApproved`, `FraudCheckRejected` | Borrador |

---

## 3. Resumen

| Evento | Topic | Publica | Consumen | Estado |
|---|---|---|---|---|
| `TransferInitiated` | `transfer.events` | transaction-service | account-service, notification-service (y fraud-service desde el Paso 7) | Implementado |
| `FundsMoved` | `account.events` | account-service | transaction-service | Borrador |
| `FundsFailed` | `account.events` | account-service | transaction-service | Borrador |
| `TransferCompleted` | `transfer.events` | transaction-service | notification-service | Borrador |
| `TransferFailed` | `transfer.events` | transaction-service | notification-service | Borrador |
| `FraudCheckApproved` | `fraud.events` | fraud-service | transaction-service, account-service | Borrador |
| `FraudCheckRejected` | `fraud.events` | fraud-service | transaction-service | Borrador |

Sin `fraud-service` (Fase 1), `account-service` reacciona directamente a `TransferInitiated`. Con él (Fase 2), reacciona a `FraudCheckApproved`.

---

## 4. Detalle de eventos

### TransferInitiated — `Implementado`

Se publica cuando se crea una transferencia en estado `PENDING`. El evento se guarda en `outbox_event` en la misma transacción que la transferencia.

| Campo del payload | Tipo | Descripción |
|---|---|---|
| `transferId` | `UUID` | Id de la transferencia |
| `sourceAccount` | `String` | Número de la cuenta origen |
| `targetAccount` | `String` | Número de la cuenta destino |
| `amount` | `Number` | Monto, hasta 4 decimales. Los consumidores lo leen como `BigDecimal` |
| `currency` | `String` | `PEN` o `USD` |
| `description` | `String` \| `null` | Descripción opcional |

```json
{
  "eventId": "transaction-service-1",
  "eventType": "TransferInitiated",
  "occurredAt": "2026-11-10T18:37:39.638953",
  "correlationId": "6e92683e-18bd-4ac3-8c49-299f6b147300",
  "payload": {
    "transferId": "6e92683e-18bd-4ac3-8c49-299f6b147300",
    "sourceAccount": "123456789012",
    "targetAccount": "210987654321",
    "amount": 50.0000,
    "currency": "PEN",
    "description": "transfer"
  }
}
```

---

### FundsMoved — `Borrador`

Se publica cuando `account-service` debita la cuenta origen y acredita la destino, en una sola transacción local.

| Campo del payload | Tipo |
|---|---|
| `transferId` | `UUID` |
| `sourceAccount` | `String` |
| `targetAccount` | `String` |
| `amount` | `Number` |
| `currency` | `String` |

**Efecto en `transaction-service`:** la transferencia pasa a `COMPLETED`, se registra en el historial y se publica `TransferCompleted`.

### FundsFailed — `Borrador`

Se publica cuando no se pudo mover el dinero (cuenta inexistente, cuenta inactiva, saldo insuficiente o moneda distinta).

| Campo del payload | Tipo |
|---|---|
| `transferId` | `UUID` |
| `reason` | `String` |

**Efecto en `transaction-service`:** la transferencia pasa a `FAILED` con `failureReason` y se publica `TransferFailed`.

### TransferCompleted — `Borrador`

| Campo del payload | Tipo |
|---|---|
| `transferId` | `UUID` |
| `sourceAccount` | `String` |
| `targetAccount` | `String` |
| `amount` | `Number` |
| `currency` | `String` |

### TransferFailed — `Borrador`

| Campo del payload | Tipo |
|---|---|
| `transferId` | `UUID` |
| `reason` | `String` |

### FraudCheckApproved / FraudCheckRejected — `Borrador`

| Campo del payload | Tipo |
|---|---|
| `transferId` | `UUID` |
| `reason` | `String` \| `null` (solo en `Rejected`) |

---

## 5. Reglas de evolución

- **Agregar campos opcionales** a un payload es compatible.
- **No renombrar, eliminar ni cambiar el tipo** de un campo existente. Si hace falta, se crea un evento nuevo o una versión (`TransferInitiatedV2`).
- **Los consumidores ignoran los campos que no conocen.**
- Los montos viajan como número JSON y se procesan siempre como `BigDecimal`, nunca como `double`.

---

## 6. Pendientes

- [ ] `occurredAt` sin zona horaria: pasar a UTC (`Instant`) más adelante.
- [ ] Definir particiones de `account.events` y `fraud.events`.
- [ ] Evaluar validación de esquemas (JSON Schema o schema registry).
