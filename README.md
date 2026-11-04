# financial-api

API REST para challenge fintech. Expone búsqueda de activos, consulta de portfolio, envío de órdenes MARKET/LIMIT y cancelación de órdenes `NEW`.

## Stack

- Java 21
- Spring Boot 4.1.2
- Gradle 9.8.2 (wrapper incluido)
- Spring Data JPA
- PostgreSQL (AWS Neon en runtime; Testcontainers en tests)
- SpringDoc OpenAPI 3.2.0

## Arquitectura

El proyecto sigue una arquitectura en capas de Spring con DDD táctico pragmático:

```text
com.agavide.financialapi
├── asset/          # Instrumentos y marketdata
├── portfolio/      # Portfolio, posiciones y holdings derivados del ledger
├── order/          # Órdenes, envío y cancelación
├── user/           # Usuarios
└── shared/
    ├── domain/     # Value objects (Money, ShareQuantity) y excepciones
    └── api/        # Manejo global de errores HTTP
```

- **Controllers**: validación de forma del request y construcción de respuestas HTTP.
- **Services**: orquestación de repositorios, transacciones y locks pesimistas por `userId`.
- **Domain**: entidades JPA como clases, VOs como records, enums persistidos como STRING; las reglas financieras viven en `Order`, `Portfolio`, `Position`, `Holdings` y `Money`.
- **Repositories**: acceso a datos con queries explícitas; sin lógica de negocio.

Las decisiones de diseño (D-1..D-5), supuestos y trade-offs están documentados en [`docs/decisions.md`](docs/decisions.md).

## API REST

| Método | Path | Descripción |
|---|---|---|
| `GET` | `/api/v1/assets?query={texto}` | Búsqueda de instrumentos por ticker y/o nombre |
| `GET` | `/api/v1/users/{userId}/portfolio` | Portfolio de un usuario |
| `POST` | `/api/v1/users/{userId}/orders` | Envío de orden MARKET/LIMIT BUY/SELL |
| `DELETE` | `/api/v1/users/{userId}/orders/{orderId}` | Cancelación de orden `NEW` |

Documentación interactiva disponible en:

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

Colección Postman incluida en `postman/financial-api-collection.json` con environment template en `postman/financial-api-environment.json`. Incluye requests de búsqueda, portfolio, MARKET/LIMIT, rechazos y cancelación.

### Códigos HTTP principales

| Código | Cuándo |
|---|---|
| `200 OK` | Consulta exitosa o orden aceptada (`FILLED`/`NEW`/`CANCELLED`). |
| `400 Bad Request` | Payload malformado, campos inválidos o instrumento MONEDA. No se persiste orden. |
| `404 Not Found` | Usuario, instrumento u orden no encontrada. No se persiste orden. |
| `409 Conflict` | Intentar cancelar una orden que no está en estado `NEW`. |
| `422 Unprocessable Content` | Rechazo de negocio (fondos/acciones insuficientes). Se persiste la orden como `REJECTED`. |

## Decisiones de diseño

Ver [`docs/decisions.md`](docs/decisions.md). Algunos puntos clave:

- Las órdenes `NEW` no reservan efectivo ni acciones (D-1).
- El rendimiento por posición es no realizado sobre la posición abierta, usando WAC (D-2).
- Rechazos de negocio se persisten como `REJECTED` y devuelven `422` (D-4).
- Sin cambios de esquema en la base existente; `ddl-auto: validate`.
- Idempotencia fuera del alcance inicial.

## Requisitos para ejecutar

- Java 21
- Docker (solo para tests con Testcontainers o para ejecutar con contenedores)
- Variables de entorno para conexión a PostgreSQL:
  - `POSTGRES_URL`
  - `POSTGRES_USER`
  - `POSTGRES_PASSWORD`

## Compilar y probar

```bash
./gradlew build
./gradlew test
```

Los tests usan Testcontainers con PostgreSQL 17; Docker debe estar corriendo. No apuntan a la base real.

## Ejecutar localmente

```bash
export POSTGRES_URL="jdbc:postgresql://<neon-hostname>/<database-name>?sslmode=require"
export POSTGRES_USER="<neon-user>"
export POSTGRES_PASSWORD="<neon-password>"

./gradlew bootRun
```

La aplicación levanta en `http://localhost:8080`.

## Docker

### Construir la imagen

```bash
docker build -t financial-api:latest .
```

### Ejecutar el contenedor

```bash
docker run --rm -p 8080:8080 \
  -e POSTGRES_URL="jdbc:postgresql://<neon-hostname>/<database-name>?sslmode=require" \
  -e POSTGRES_USER="<neon-user>" \
  -e POSTGRES_PASSWORD="<neon-password>" \
  financial-api:latest
```

**Importante:** `POSTGRES_URL` debe usar el hostname real de Neon, nunca `localhost` ni `host.docker.internal`, ya que la base es remota.

### Docker Compose

Copiar el ejemplo de variables y completar los valores reales:

```bash
cp .env.example .env
# editar .env con los datos de conexión
```

```bash
docker compose up --build
```

## Seguridad

- No commitear `.env` ni credenciales en el repositorio.
- No incluir secretos en el `Dockerfile`, `compose.yaml` ni en la imagen.
- La aplicación usa `ddl-auto: validate`; no ejecuta migraciones ni crea tablas contra la base provista.
