# Decisiones técnicas

Registro de las decisiones de arquitectura y diseño del proyecto, con su motivo y lo que se sacrifica en cada una. Cada entrada sigue el formato: **contexto → decisión → por qué → costo**.

---

## 1. Monorepo

- **Contexto:** el sistema tiene varios microservicios, y quien revisa el proyecto debe poder entenderlo y levantarlo con facilidad.
- **Decisión:** un solo repositorio con una carpeta por servicio. Cada servicio es un proyecto Maven independiente, sin `pom` padre.
- **Por qué:** soy un solo desarrollador, y un único repo da un solo README, un solo `docker-compose.yml` y cambios entre servicios en un solo commit.
- **Costo:** no hay ciclos de despliegue ni permisos independientes por servicio. En un entorno con varios equipos separaría cada servicio en su propio repositorio, con su pipeline y con los contratos de eventos publicados aparte.

---

## 2. Una base de datos por servicio (PostgreSQL + Flyway)

- **Contexto:** los servicios deben poder evolucionar sin depender del esquema de los demás.
- **Decisión:** cada servicio tiene su propia base (`accounts_db`, `transactions_db`, ...) en PostgreSQL, con migraciones versionadas con Flyway y `ddl-auto=validate`.
- **Por qué:** ningún servicio lee las tablas de otro. Flyway deja un historial versionado del esquema, y `validate` hace que Hibernate falle al arrancar si una entidad no coincide con las tablas.
- **Costo:** no hay *joins* entre servicios. Por eso `transaction-service` guarda el número de cuenta como texto y no valida que la cuenta exista: lo resuelve `account-service`.

---

## 3. Comunicación orientada a eventos con Kafka

- **Contexto:** una transferencia involucra varios servicios y no debe depender de que todos estén disponibles al mismo tiempo.
- **Decisión:** los servicios se comunican por eventos en Kafka. El contrato está en [`eventos.md`](eventos.md).
- **Por qué:** desacopla a los servicios y permite que cada uno reaccione a su ritmo. Kafka es la mensajería más pedida en ofertas de backend.
- **Costo:** consistencia eventual (la transferencia pasa por estados) y más piezas que operar.

---

## 4. Patrón Outbox

- **Contexto:** guardar en la base de datos y publicar en Kafka son dos operaciones que no se pueden hacer de forma atómica. Si una falla, el sistema queda inconsistente (transferencia guardada sin evento, o evento de una transferencia inexistente).
- **Decisión:** el evento se guarda en la tabla `outbox_event`, **en la misma transacción** que la transferencia y su historial. Un `OutboxPublisher` programado lo envía después a Kafka y marca `published_at`.
- **Detalles del publicador:**
  - Se ejecuta cada 2 segundos y procesa hasta 50 eventos pendientes, del más antiguo al más reciente.
  - Si un envío falla, detiene la vuelta y reintenta en la siguiente, para no adelantar un evento a otro.
  - El `eventId` es estable (`<servicio>-<id del outbox>`), para que los consumidores detecten duplicados.
- **Por qué:** si Kafka está caído, la transferencia se registra igual y el evento espera. Se verificó deteniendo Kafka y reiniciándolo.
- **Costo:** entrega *at-least-once* (puede haber duplicados) y una latencia de hasta 2 segundos. Con varias instancias del servicio habría que evitar que dos publicadores lean los mismos eventos (`FOR UPDATE SKIP LOCKED`).

---

## 5. Idempotencia

- **Contexto:** una petición de pago puede repetirse por un timeout, un doble clic o un reintento automático, y no debe duplicar el dinero movido.
- **Decisión:**
  - **En la API:** el cliente envía un header `Idempotency-Key`, obligatorio, que genera él mismo (un UUID por operación). La llave tiene una restricción única en `transfer`. Si ya existe, se devuelve la transferencia original.
  - **En los consumidores de Kafka:** deduplicación por `eventId` con la tabla `processed_message`. *(Planificado, Paso 5)*
- **Por qué:** la llave identifica la **intención del usuario**, no la petición, así que solo el cliente sabe cuándo es un reintento. Si la generara el servidor, el cliente no podría reutilizarla al reintentar.
- **Costo y pendiente:** hoy, una llave repetida con un body distinto devuelve en silencio la transferencia original. Se endurecerá respondiendo conflicto, y se manejará la carrera de dos peticiones simultáneas con la misma llave.

---

## 6. Transferencia dentro de `account-service`: una sola transacción local

- **Contexto:** mover dinero implica debitar una cuenta y acreditar otra.
- **Decisión:** como ambas cuentas viven en la misma base de `account-service`, el débito y el crédito se hacen en **una transacción local**. La saga se da **entre servicios** (transaction ↔ account ↔ fraud), no dentro del movimiento del dinero.
- **Por qué:** una transacción de base de datos garantiza todo o nada, y evita tener que compensar un débito si falla el crédito.
- **Costo:** si algún día las cuentas se reparten en bases distintas, esta decisión se revisa.

---

## 7. Modelo de dinero

- **Decisión:**
  - Montos con `BigDecimal` en Java y `NUMERIC(19,4)` en la base. Nunca `double` ni `float`.
  - El saldo nunca se edita a mano: solo cambia mediante operaciones, y cada una deja un registro inmutable en `account_movement` con `balance_after`.
  - Bloqueo optimista (`@Version`) en `account`, para que dos operaciones simultáneas sobre la misma cuenta no se pisen.
  - Las cuentas no se borran: se cambia su `status`.
  - Los movimientos no se cargan dentro de la entidad `Account` ni se borran en cascada: se consultan paginados desde su repositorio.
- **Por qué:** un libro mayor debe poder auditarse y crecer sin límite.
- **Costo:** un conflicto de versión hace fallar una operación (responde 409) y el cliente debe reintentar.

---

## 8. Convenciones de código

| Tema | Decisión | Motivo |
|---|---|---|
| **DTO** | `record`, con método estático `fromEntity(...)` para convertir desde la entidad | Inmutables, sin Lombok y la conversión queda en un solo lugar |
| **Mapper** | Conversión manual, sin MapStruct | Hay pocos DTO con pocos campos; una librería añade complejidad sin beneficio |
| **Entidades** | `class` con Lombok | JPA exige constructor vacío y objetos mutables |
| **Service** | Interfaz + implementación (`XxxService` / `XxxServiceImpl`), con `@Transactional` en la implementación | Consistencia en todos los servicios |
| **Métodos del service** | Los que pide cada endpoint, no un CRUD completo | Un sistema de pagos no edita saldos ni borra cuentas |
| **Errores** | Excepciones de negocio traducidas por un `GlobalExceptionHandler` (404, 409, 422) | El controller no contiene lógica de errores |
| **Controller** | Solo recibe, valida con `@Valid` y delega | Sin lógica de negocio |
| **Creación** | `@ResponseStatus(CREATED)` en los `POST` | Simplicidad |

---

## 9. Fechas de auditoría en `transaction-service`

- **Decisión:** las fechas (`createdAt`, `updatedAt`, `changeAt`) se asignan **manualmente** con `LocalDateTime`, sin `@CreationTimestamp` ni `@UpdateTimestamp`. En `account-service` se usan las anotaciones de Hibernate.
- **Convención:** al crear, `updated_at` es igual a `created_at`, y se actualiza en cada cambio posterior.
- **Costo:** hay que acordarse de pasar la fecha en cada punto de escritura. Si falta, falla el `INSERT` por la restricción `NOT NULL`. Si se vuelve incómodo, `@PrePersist` y `@PreUpdate` la asignan automáticamente.
- **Pendiente:** unificar el criterio entre ambos servicios.

---

## 10. Estrategia de pruebas

- **Unitarias** del service con JUnit 5 y Mockito (la lógica de negocio, sin base de datos).
- **De controller** con `@WebMvcTest` y `MockMvc` (rutas, validaciones y códigos HTTP).
- **De integración** con Testcontainers (Postgres y Kafka reales). *(Planificado, Paso 9)*
- Se aplica TDD donde hay dinero o casos borde (depósito, idempotencia, transferencia). Para el CRUD simple, las pruebas se escriben después.
- Los tests (nombres y contenido) se escriben en inglés, con el patrón `method_condition_result`.

---

## 11. Decisiones pendientes

| Tema | Pregunta abierta |
|---|---|
| **Event Sourcing / CQRS** | ¿Aplicarlo solo en `account-service` (saldo como suma de eventos) o dejarlo fuera? |
| **Seguridad** | Keycloak con JWT, validado en el gateway |
| **Descubrimiento y entrada** | Eureka y Spring Cloud Gateway, ¿o APIM si se despliega en Azure? |
| **Despliegue** | Azure Container Apps, ACR, Key Vault y Monitor |
| **Credenciales** | Sacar las contraseñas de los `application.properties` (variables de entorno, luego Key Vault) |
| **Migración a Quarkus** | Servicio por servicio, empezando por `notification-service` |
