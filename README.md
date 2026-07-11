# tenpo-backend-challenge

API REST en **Spring Boot 3 (Java 21)** que suma dos números y les aplica un
porcentaje dinámico obtenido de un servicio externo (mockeado). Incluye
reintentos ante fallos del servicio externo, historial de invocaciones
persistido de forma asíncrona en PostgreSQL, rate limiting y manejo
centralizado de errores.

Desarrollado como desafío técnico para el proceso de selección de Backend
Developer en **Tenpo**.

## Índice

- [Arquitectura](#arquitectura)
- [Stack técnico](#stack-técnico)
- [Cómo levantar el proyecto](#cómo-levantar-el-proyecto)
- [Documentación de la API (Swagger)](#documentación-de-la-api-swagger)
- [Endpoints y ejemplos](#endpoints-y-ejemplos)
- [Tests](#tests)
- [Decisiones técnicas](#decisiones-técnicas)

## Arquitectura

El proyecto sigue **arquitectura hexagonal (Ports & Adapters)** en vez de un
MVC/layered clásico.

### Por qué hexagonal

- El "servicio de porcentaje" es un mock hoy, pero en un escenario real sería
  una llamada HTTP a un servicio externo. Modelarlo como **puerto** (interfaz
  de dominio) con un **adapter** que lo implementa permite reemplazar el mock
  por un cliente HTTP real sin tocar una sola línea de lógica de negocio ni
  de tests.
- El **retry** (Resilience4j) es una responsabilidad de infraestructura y
  vive en el adapter externo, no en el caso de uso — el dominio no sabe (ni
  le importa) que hay reintentos.
- Permite testear el dominio (`CalculationService`) con JUnit + Mockito puro,
  sin levantar contexto de Spring, separado de los tests de integración con
  TestContainers.
- Regla dura: el paquete `domain` nunca importa Spring, JPA, ni ninguna
  librería de infraestructura — solo Java puro (records + interfaces).

### Capas

```
com.tenpo.challenge
├── domain/
│   ├── model/              Calculation, CallHistory (records inmutables)
│   ├── port/in/             CalculationUseCase (caso de uso)
│   └── port/out/            PercentageProviderPort, CallHistoryPort
├── application/
│   └── service/             CalculationService (orquesta los puertos)
└── infrastructure/
    ├── adapter/in/web/       Controllers, DTOs, GlobalExceptionHandler,
    │                         aspecto de logging de historial
    ├── adapter/out/external/ Adapter mock del servicio de porcentaje (+ retry)
    ├── adapter/out/persistence/ Entidad JPA, repositorio, adapter async
    └── config/               Async, RateLimiting, Resilience4j, Swagger
```

**Nota de diseño:** `CallHistoryPort` (dominio) expone únicamente
`save(CallHistory)`. La lectura paginada del historial (`GET /history`) usa
`Pageable`/`Page<T>` de Spring Data — tipos que **no pueden** entrar al
paquete `domain` sin romper la regla de pureza de arriba. Por eso la lectura
paginada vive enteramente en infraestructura (`CallHistoryReader` +
`CallHistoryController`), sin forzar un puerto de dominio artificial solo
para simetría.

## Stack técnico

| Requisito              | Herramienta                                        |
|-------------------------|-----------------------------------------------------|
| Framework               | Spring Boot 3.3.5 (MVC/blocking)                    |
| Lenguaje                 | Java 21                                              |
| Retry                    | Resilience4j (`@Retry`)                              |
| Rate limiting (3 RPM)    | Bucket4j vía `OncePerRequestFilter`                  |
| Historial async          | `@Async` + `CompletableFuture`                       |
| Persistencia             | Spring Data JPA + PostgreSQL                         |
| Paginación               | `Pageable` / `Page<T>` nativo de Spring Data         |
| Documentación API        | springdoc-openapi (Swagger UI)                       |
| Testing                  | JUnit 5 + Mockito (unitarios), TestContainers (integración) |
| Contenedores             | Dockerfile multi-stage (build con Maven, runtime con JRE Alpine) + Docker Compose (API + PostgreSQL) |

## Cómo levantar el proyecto

### Opción A: todo en Docker (recomendado, levanta servicio y base de datos en conjunto)

Prerequisito: Docker Desktop.

```bash
docker compose up --build
```

Esto construye la imagen de la API (`Dockerfile` multi-stage: build con
`maven:3.9-eclipse-temurin-21`, runtime con `eclipse-temurin:21-jre-alpine`,
copiando solo el jar final) y levanta dos contenedores:

- `tenpo-postgres`: Postgres con healthcheck (`pg_isready`).
- `tenpo-api`: la API, con `depends_on: condition: service_healthy` — no
  arranca hasta que Postgres esté realmente listo para aceptar conexiones,
  evitando problemas de timing en el primer arranque.

La API queda disponible en `http://localhost:8080` sin ningún paso manual
adicional (ni crear la base, ni esperar a Postgres a mano).

### Opción B: API local + Postgres en Docker

Prerequisitos:
- Java 21
- Docker Desktop (para PostgreSQL)
- No hace falta tener Maven instalado: el proyecto incluye Maven Wrapper (`./mvnw` / `mvnw.cmd`)

```bash
docker compose up -d postgres
./mvnw spring-boot:run
```

Por defecto corre con el perfil `dev` (`application-dev.yml`), que apunta a
`localhost:5432`. El perfil `docker` (`application-docker.yml`), usado por
el contenedor de la API, apunta al hostname `postgres` en vez de
`localhost`.

### Correr los tests

```bash
./mvnw test
```

## Documentación de la API (Swagger)

Con la app corriendo:

- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs

## Endpoints y ejemplos

### `POST /api/v1/calculate`

Suma `num1` + `num2` y aplica el porcentaje obtenido del servicio externo
(mockeado en `FixedPercentageProviderAdapter`, fijo en 10%).

```bash
curl -X POST http://localhost:8080/api/v1/calculate \
  -H "Content-Type: application/json" \
  -d '{"num1": 5, "num2": 5}'
```

```json
{"num1": 5, "num2": 5, "percentage": 10, "result": 11.00}
```

**Error de validación** (falta un campo):

```bash
curl -X POST http://localhost:8080/api/v1/calculate \
  -H "Content-Type: application/json" \
  -d '{"num1": 5}'
```

```json
{
  "timestamp": "2026-07-10T04:06:40.643Z",
  "status": 400,
  "error": "Bad Request",
  "message": "num2: num2 is required",
  "path": "/api/v1/calculate"
}
```

**Servicio externo caído tras agotar reintentos** → `503 Service Unavailable`
con el mismo formato de error, `message` describiendo el fallo.

> **Cómo probar este escenario:** con la app corriendo tal cual no se puede
> disparar, porque `FixedPercentageProviderAdapter` es un mock fijo que
> siempre devuelve 10% y nunca falla. El escenario está cubierto
> automáticamente en `PercentageProviderRetryIntegrationTest` (un test
> double simula fallas continuas y verifica que, tras agotar los 3
> intentos, se lanza `ExternalServiceException` → `503`). Para verlo en
> vivo por curl/Swagger, la forma más simple es modificar temporalmente
> `FixedPercentageProviderAdapter.getPercentage()` para que lance una
> excepción siempre, reiniciar la app, pegarle a `/calculate` y observar el
> `503` (con el delay del backoff exponencial), y después revertir el
> cambio.

### `GET /api/v1/history`

Historial de invocaciones a la API, paginado y ordenado por fecha
descendente. Persistido de forma asíncrona, no bloquea el request de
`/calculate`. Registra tanto llamadas exitosas como fallidas (validación,
servicio externo caído, errores no controlados, rate limit) — con la
excepción del propio endpoint `/history`, que no se auto-registra.

`params`/`response` se devuelven como objetos JSON anidados (no como string
escapado): se persisten como `jsonb` en Postgres. La respuesta es un DTO
propio (`PagedResponse`), no el `Page` de Spring Data crudo — así no se
exponen campos internos de implementación como `pageable` o `sort`
duplicado.

```bash
curl "http://localhost:8080/api/v1/history?page=0&size=10"
```

```json
{
  "content": [
    {
      "id": 2,
      "timestamp": "2026-07-10T19:33:46.639Z",
      "endpoint": "/api/v1/calculate",
      "params": {"num1": 5, "num2": null},
      "response": null,
      "error": "num2: num2 is required",
      "status": 400
    },
    {
      "id": 1,
      "timestamp": "2026-07-10T18:04:07.821Z",
      "endpoint": "/api/v1/calculate",
      "params": {"num1": 5, "num2": 5},
      "response": {"num1": 5, "num2": 5, "percentage": 10, "result": 11.00},
      "error": null,
      "status": 200
    }
  ],
  "page": 0,
  "size": 10,
  "totalElements": 2,
  "totalPages": 1,
  "last": true
}
```

### Rate limiting (3 requests por minuto)

El límite es **global**, compartido por todos los clientes que llamen a
`/api/**` (no es por IP). Al superarlo:

```json
{
  "timestamp": "2026-07-10T05:27:46.364Z",
  "status": 429,
  "error": "Too Many Requests",
  "message": "Rate limit exceeded: maximum 3 requests per minute allowed. Retry after 53 seconds.",
  "path": "/api/v1/history"
}
```

con header `Retry-After` y status `429`. El body usa el mismo `ErrorResponse`
que el resto de los errores de la API (no un formato especial): el tiempo de
espera va en el header y, en texto, dentro de `message`.

## Tests

- **Unitarios** (`CalculationServiceTest`, `GlobalExceptionHandlerTest`):
  JUnit 5 + Mockito puro, sin contexto de Spring.
- **Web slice** (`CalculationControllerErrorHandlingTest`,
  `CallHistoryControllerErrorHandlingTest`): `@WebMvcTest` con
  `addFilters = false` (para no arrastrar `RateLimitFilter`), verifican los
  códigos y el body de error de cada controller en aislamiento.
- **Integración liviana** (`PercentageProviderRetryIntegrationTest`): usa
  `ApplicationContextRunner` para levantar solo la autoconfiguración de
  Resilience4j + AOP y verificar el retry real, sin necesitar Postgres.
- **Integración con contexto completo** (`CalculationApiApplicationTests`,
  `RateLimitFilterTest`): requieren PostgreSQL corriendo (`docker compose up -d`).
- **Integración con TestContainers** (`CallHistoryIntegrationTest`): levanta
  un Postgres real en un contenedor efímero vía `@ServiceConnection`, sin
  depender del `docker-compose` local. Verifica el flujo completo
  `/calculate` → historial → `/history`, incluyendo que `params`/`response`
  lleguen como objetos JSON anidados (no strings) y que el body raíz no
  tenga campos internos de `Page` (`pageable`, `sort`).

## Decisiones técnicas

- **BigDecimal en vez de double**: evita
  errores de redondeo binario. `result` se redondea a 2 decimales
  (`RoundingMode.HALF_UP`).
- **Retry con Resilience4j** (`@Retry`, política `percentageProvider`):
  máximo 3 intentos, backoff exponencial (500ms, x2). Vive en el adapter
  externo (`FixedPercentageProviderAdapter`), no en el caso de uso — el
  dominio no conoce la existencia de reintentos. Al agotarse, el
  `fallbackMethod` lanza `ExternalServiceException` (excepción de dominio,
  Java puro), que el `GlobalExceptionHandler` traduce a `503`.
- **Rate limiting con Bucket4j** (`OncePerRequestFilter` + `shouldNotFilter`
  para limitar el alcance a `/api/**`): un único bucket global de 3 tokens
  con refill de 3 cada minuto. Se eligió un límite global (no por IP/cliente)
  porque es la interpretación literal de "la API debe soportar un máximo de 3 RPM (requests por minuto)"; migrar a límite por cliente sería agregar una clave (IP, API key) al bucket. Al principio se pensó en hacer el ajuste solo para el 
  endpoint de cálculo, pero como la instruccion dice "la API" se dejaron los dos endpoints que tiene el servicio con el rate limit requerido.
- **Historial asíncrono** (`@Async` + `CompletableFuture<Void>`): un
  `@Around` aspect intercepta `CalculationController.calculate(..)` y
  delega el guardado a `CallHistoryPort`, cuya implementación está anotada
  `@Async` — el guardado corre en otro hilo y no bloquea la respuesta al
  cliente. Es *fire-and-forget*: no se espera el `CompletableFuture` ni se
  loguean fallos del guardado async (aceptable para el alcance de este
  challenge).
  - El historial también registra errores, no solo el happy path: el
    `@Around` cubre las excepciones lanzadas dentro de `calculate()`
    (servicio externo caído, errores no controlados), y `GlobalExceptionHandler`
    cubre el caso que ese aspect estructuralmente no puede ver — errores de
    validación (`@Valid`), que fallan durante el binding del argumento,
    antes de que el método del controller (el join point) se llegue a
    invocar. `RateLimitFilter` loguea los rechazos por rate limit (429) de
    la misma forma, ya que ocurren en un filtro, antes del dispatch de
    Spring MVC. El propio `/api/v1/history` está excluido de este logueo
    (por la logica del servicio me parece que no tendría sentido que una 
    consulta al historial quede registrada, aunque en el punto 3 del documento
    del challengue dice explicitamente: Implementa un endpoint para consultar un historial de todas las llamadas realizadas a los "endpoints" de la API.).
- **`params`/`response` como `jsonb`, no texto**: `CallHistoryEntity` usa
  `@JdbcTypeCode(SqlTypes.JSON)` (Hibernate 6) con campos `JsonNode`, y
  `CallHistoryResponse` expone esos mismos campos como `JsonNode` en vez de
  `String` — así Jackson los serializa como objeto JSON anidado real, no
  como string escapado. Esto se hizo ya que al principio los tenia como texto y no eran muy legibles en la response. El dominio (`CallHistory`) sigue guardando
  `params`/`response` como `String` (JSON crudo) para no filtrar el tipo
  `JsonNode` de Jackson en el paquete `domain`; el parseo a `JsonNode`
  ocurre recién en `CallHistoryPersistenceAdapter`, en el borde de
  infraestructura.
- **`PagedResponse<T>` propio en vez de `Page<T>` crudo**: el endpoint
  `/history` devuelve un DTO propio (`content, page, size, totalElements,
  totalPages, last`) mapeado desde el `Page` de Spring Data, en vez de
  serializar `Page<T>` directamente — evita exponer campos internos de
  implementación (`pageable`, `sort` duplicado) como parte del contrato
  público de la API.
- **Paginación con `Pageable`/`Page<T>` nativo de Spring Data** (a nivel
  interno, no en el contrato de la API — ver punto anterior): el
  controller fuerza el orden `timestamp DESC` construyendo su propio
  `PageRequest`, ignorando cualquier `sort` que mande el cliente, ya que el
  requisito pide explícitamente ese orden.
- **Manejo centralizado de errores** (`@RestControllerAdvice`): DTO de error
  único (`timestamp, status, error, message, path`) para validación (400),
  servicio externo caído (503) y errores no controlados (500, sin exponer
  detalles internos al cliente).
- **Dockerfile multi-stage**: la etapa de build usa `maven:3.9-eclipse-temurin-21`
  (compila y empaqueta el jar, sin necesitar Maven ni JDK en el runtime),
  la etapa final usa `eclipse-temurin:21-jre-alpine` (solo JRE, no JDK
  completo) y copia únicamente el jar final — imagen final más liviana,
  sin herramientas de build ni código fuente.
- **`depends_on` con healthcheck (no solo orden de arranque)**: el
  contenedor de la API espera a que Postgres esté `healthy` según
  `pg_isready`, no solo a que el contenedor exista — evita que la API
  intente conectarse antes de que Postgres acepte conexiones reales,
  sin necesitar lógica de retry manual adicional.
- **Arquitectura hexagonal**: ver sección [Arquitectura](#arquitectura).
