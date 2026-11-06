# Decisiones de diseño — financial-api

Repositorio: `java-financial-api`  
Challenge: backend-challenge (Fintech)  
Fecha: 2026-11-04  
Estado: Etapa 0 — decisiones aprobadas antes de codificar

Este documento registra las decisiones de diseño, supuestos y trade-offs acordados para la implementación del challenge. Cualquier punto no cubierto aquí debe tratarse como ambigüedad pendiente antes de escribir código que lo afecte.

---

## Glosario de acrónimos

| Acrónimo | Significado | Uso en este documento |
|---|---|---|
| **WAC** | **Weighted Average Cost** (costo promedio ponderado) | Política para calcular el costo de adquisición de las acciones que aún se mantienen en una posición abierta. |
| **P&L** | **Profit and Loss** (ganancias y pérdidas) | Resultado financiero de una operación. En particular, *P&L realizado* se refiere a la ganancia/pérdida obtenida al vender acciones. |

---

## 1. Alcance funcional interpretado del challenge

El challenge pide tres capacidades principales en su resumen:

1. **Portfolio**: valor total de la cuenta, pesos disponibles y listado de activos con cantidad, valor de mercado y rendimiento.
2. **Búsqueda de activos**: por ticker y/o nombre.
3. **Envío de órdenes**: BUY/SELL, MARKET/LIMIT.

Además, el texto funcional exige explícitamente que las órdenes `NEW` puedan cancelarse y queden en estado `CANCELLED`. Por eso se agrega un **cuarto endpoint** de cancelación, no como nice-to-have ni como parte del endpoint de envío.

No se simula el mercado: una orden LIMIT enviada queda en estado `NEW` y no hay proceso que la ejecute posteriormente.

---

## 2. Contrato REST propuesto

El challenge no define paths ni status HTTP. Se acuerda el siguiente contrato mínimo:

### 2.1 Endpoints

| Método | Path | Descripción |
|---|---|---|
| `GET` | `/api/v1/assets?query={texto}` | Búsqueda de instrumentos por ticker y/o nombre |
| `GET` | `/api/v1/users/{userId}/portfolio` | Portfolio de un usuario |
| `POST` | `/api/v1/users/{userId}/orders` | Envío de orden |
| `DELETE` | `/api/v1/users/{userId}/orders/{orderId}` | Cancelación de orden `NEW` |

### 2.2 Códigos HTTP

- `200 OK`: consulta exitosa.
- `201 Created`: orden aceptada y persistida (`FILLED` o `NEW`).
- `400 Bad Request`: payload malformado, campos requeridos ausentes o valores inválidos. **No se persiste orden.**
- `404 Not Found`: usuario, instrumento u orden solicitada no existe.
- `409 Conflict`: intento de cancelar una orden que no está en estado `NEW`.
- `422 Unprocessable Content`: rechazo de regla de negocio (fondos/acciones insuficientes, monto insuficiente para 1 acción). **Se persiste la orden como `REJECTED`.**

### 2.3 Decisión sobre rechazos

Solo las solicitudes que superan la validación de forma y de existencia de usuario/instrumento llegan a ser evaluadas como órdenes. Si fallan por regla de negocio, se guarda una fila `REJECTED` y se devuelve `422`. Los errores de request (`400`/`404`) no contaminan el ledger.

---

## 3. Decisiones funcionales

### D-1 — Las órdenes `NEW` no reservan recursos

**Decisión**: en el alcance actual, ni las órdenes LIMIT `NEW` ni ninguna orden pendiente reservan efectivo ni acciones.

**Razón**: el challenge no exige reservas y no existe un proceso que ejecute órdenes LIMIT posteriormente. Sin ejecución diferida, una reserva no afecta ningún comportamiento observable salvo el valor reportado como disponible.

**Cálculo de disponibilidad**:
- Efectivo disponible = suma de movimientos `FILLED` (`CASH_IN`, `CASH_OUT`, `SELL`, `BUY`).
- Acciones disponibles = suma neta de `BUY`/`SELL` `FILLED` por instrumento.
- Las órdenes `NEW` se validan contra esos recursos pero no los reducen.

**Efecto observable en el seed del usuario 1**:
- Efectivo liquidado: ARS 753.000
- Dos BUY `NEW`: 50×$710 + 60×$1.500 = ARS 125.500 de compromiso potencial
- Efectivo disponible reportado: ARS 753.000 (no se descuentan las `NEW`)

**Trade-off**: se pueden aceptar varias órdenes LIMIT cuyo compromiso agregado supere los recursos reales. Esto es aceptable para el challenge sin simulación de mercado. Si en el futuro se agrega ejecución diferida o integración con un mercado real, esta decisión debe revisarse y probablemente implementar reservas.

---

### D-2 — Rendimiento: posición abierta con WAC, sin P&L realizado

**Decisión**: el indicador porcentual de cada posición se calcula como rendimiento no realizado de la posición abierta, usando costo promedio ponderado (Weighted Average Cost, WAC) para las acciones que aún se poseen. No incluye ganancias/pérdidas (Profit and Loss, P&L) realizadas por ventas anteriores.

**Fórmula**:

```
remainingCostBasis = Σ(BUY_FILLED size × price) − Σ(SELL_FILLED size × WAC_al_momento)
marketValue = shares × close
rendimiento (%) = ((marketValue − remainingCostBasis) / remainingCostBasis) × 100
```

**Razón**: el challenge no define política contable ni especifica si el rendimiento total debe incluir P&L realizado. WAC sobre posición abierta es verificable, reproduce con el seed y evita definir un denominador global cuando hay ventas parciales.

**Convención en la API**: aunque el challenge use el término "rendimiento total", la respuesta del portfolio debe documentar que el porcentaje es "rendimiento no realizado de la posición abierta". El `totalAccountValue` ya incorpora los proceeds de ventas `FILLED` a través del cash balance, por lo que no se suma el P&L realizado por separado.

**Caso borde**: si no hay posición abierta (`shares = 0`) o el costo restante es cero, no se calcula el porcentaje; se devuelve ausencia explícita del indicador.

---

### D-3 — Última marketdata

**Decisión**: para obtener el precio de un instrumento se usa la fila de `marketdata` con la fecha más reciente (`ORDER BY date DESC`). Si hay empate, se desempata por `id DESC`.

**Razón**: el esquema no tiene una restricción de unicidad por instrumento/fecha. El seed tiene una sola fila por instrumento y fecha, salvo 14 instrumentos que aparecen en dos días.

**Marketdata ausente**: si un instrumento no tiene fila en `marketdata`, se reporta `marketValue = 0` acompañado de una marca explícita de "precio no disponible". No se inventa un precio.

**Precio de ejecución MARKET**: se usa el `close` de la última `marketdata` del instrumento. No se usan precios de fecha posterior al momento de la orden para determinar su ejecución.

---

### D-4 — Frontera entre rechazo de negocio y error de request

**Decisión**:

| Situación | ¿Se persiste orden? | HTTP | Estado |
|---|---|---|---|
| Fondos insuficientes (BUY) | Sí | `422` | `REJECTED` |
| Acciones insuficientes (SELL) | Sí | `422` | `REJECTED` |
| Monto que convierte a 0 acciones | Sí | `422` | `REJECTED` |
| Payload malformado | No | `400` | — |
| `amountArs` menor o igual a 0 | No | `400` | — |
| Usuario/instrumento inexistente | No | `404` | — |
| Orden no cancelable | No | `409` | — |

**Razón**: el challenge dice que las órdenes rechazadas por fondos o acciones insuficientes deben quedar guardadas en estado `REJECTED`. Las solicitudes que no llegan a ser evaluables como orden no deben contaminar el ledger.

---

### D-5 — Posiciones con cantidad cero

**Decisión**: si un instrumento tiene cantidad neta cero tras aplicar las órdenes `FILLED`, no se incluye en el listado de holdings del portfolio. No se reporta rendimiento de posición abierta para ese instrumento.

**Razón**: simplifica la respuesta y evita divisiones por cero. Los proceeds de la venta que cerró la posición siguen reflejados en el cash balance.

---

## 4. Decisiones técnicas

### Idempotencia en `POST /orders`

**Decisión**: queda fuera del alcance inicial.

**Razón**: el challenge no la requiere. Implementarla correctamente implicaría persistir una clave de idempotencia por usuario junto con una huella del request y una restricción de unicidad en PostgreSQL. Eso requiere modificar el esquema, lo cual no se justifica para el alcance actual.

**Riesgo**: un reintento del cliente tras un timeout puede crear una segunda orden `FILLED` y duplicar el efecto. Se documenta este riesgo para que quien consuma la API lo maneje.

**Iteración futura**: si se decide agregar idempotencia, se propone:
- Header opcional `Idempotency-Key`.
- Columna `idempotency_key` (o tabla propia) con restricción de unicidad por usuario.
- Almacenar hash del request.
- Si `key + hash` coinciden, devolver la orden original.
- Si la misma `key` se usa con otro request, devolver `409 Conflict`.

### Concurrencia

**Decisión**: usar lock pesimista (`SELECT FOR UPDATE`) sobre la fila de `users` para serializar envíos y cancelaciones del mismo `userId`.

**Razón**: evita el doble gasto en órdenes MARKET concurrentes sin agregar complejidad de aislamiento `SERIALIZABLE` con reintentos.

**Limitación**: el lock no evita que varias LIMIT `NEW` comprometan en conjunto más recursos de los disponibles, porque D-1 excluye las reservas.

### Cambios de esquema

**Decisión**: no modificar la base de datos existente para el alcance inicial.

**Razón**: el esquema soporta los casos del challenge. Cualquier cambio (índices, constraints, campos de idempotencia) se justificará y documentará por separado si se vuelve necesario.

### Tipo de datos monetarios

**Decisión**: usar `BigDecimal` para todo cálculo monetario. Prohibido `double`/`float`.

**Decisión**: las cantidades de acciones son enteras. La conversión de monto a acciones usa `floor(amount / price)`.

---

## 5. Hallazgo en los datos de prueba

El ledger del usuario 1 en `database.sql` produce una cantidad neta de **−10** para el instrumento 31 si se aplican solo sus órdenes `FILLED`:

- BUY `FILLED`: 20 unidades
- SELL `FILLED`: 30 unidades

El esquema no contiene una tabla de inventario inicial ni una posición de apertura. El challenge tampoco define el tratamiento de short selling. Por lo tanto:

- No se asume que el sistema permita posiciones cortas.
- No se corregirá silenciosamente el dato con un clamp o con una cantidad mínima de cero.
- Se documenta el hallazgo como dato histórico incompleto.
- Los tests de aceptación del portfolio usarán fixtures internamente coherentes (con inventario inicial suficiente) en lugar de depender de que el seed histórico arroje posiciones no negativas.

---

## 6. Supuestos adicionales

- El instrumento `ARS` (`id = 66`, tipo `MONEDA`) representa el efectivo en pesos. Sus movimientos `CASH_IN`/`CASH_OUT` usan `price = 1` en el seed.
- No hay autenticación ni autorización; `userId` se usa directamente como parámetro de path.
- No hay comisiones ni impuestos.
- No hay ejecución diferida de órdenes LIMIT.
- La fecha/hora de las órdenes se toma del sistema al insertar (`datetime`).
- El ordenamiento del ledger usa `datetime` y, como desempate, `id`.

---

## 7. Resumen de estados de orden

| Estado | Significado | ¿Afecta cash/acciones? |
|---|---|---|
| `NEW` | Orden LIMIT recién enviada | No |
| `FILLED` | Orden ejecutada (MARKET inmediata o LIMIT ejecutada históricamente) | Sí |
| `REJECTED` | Orden rechazada por fondos/acciones insuficientes | No |
| `CANCELLED` | Orden `NEW` cancelada por el usuario | No |

---

## 8. Próximos pasos

1. Etapa 1: mapeo de entidades JPA y VOs, validación del esquema con Testcontainers.
2. Etapa 2: búsqueda de activos.
3. Etapa 3: portfolio.
4. Etapa 4: envío de órdenes.
5. Etapa 5: cancelación.
6. Etapa 6: ejecución local y Docker contra Neon.
7. Etapa 7: OpenAPI y colección Postman.

---

## 9. Extensibilidad

### 9.1 Diseño actual por tipo de orden

El comportamiento que depende del tipo de orden (`OrderType`) está encapsulado en `OrderTypeStrategy`:

- `type()`: tipo que atiende la estrategia.
- `resolvePrice(command, instrument)`: precio de referencia de la orden y validación de sus campos.
- `initialStatus()`: estado con el que nace una orden válida (`FILLED` o `NEW`).

Implementaciones actuales:

| Clase | Tipo | Precio | Estado inicial |
|---|---|---|---|
| `MarketOrderStrategy` | `MARKET` | Último `close` de `marketdata`; `0` si no hay datos | `FILLED` |
| `LimitOrderStrategy` | `LIMIT` | `limitPrice` redondeado a 2 decimales | `NEW` |

`OrderService` no tiene ramas por tipo. Recibe `List<OrderTypeStrategy>` por inyección de Spring y la indexa por `OrderType` en un `EnumMap`. Una orden de tipo sin estrategia registrada responde `400`.

### 9.2 Ajuste requerido para agregar un nuevo tipo

Ejemplo: agregar `STOP`.

1. `order/OrderType.java`: agregar la constante `STOP`.
2. Crear una clase `@Component` en el paquete `order` que implemente `OrderTypeStrategy`:
   - `type()` retorna `OrderType.STOP`.
   - `resolvePrice(...)` valida y devuelve el precio de referencia de la orden.
   - `initialStatus()` retorna `NEW` si la orden queda pendiente, o `FILLED` si se ejecuta de inmediato.
3. Si el tipo requiere campos nuevos (ej. `stopPrice`):
   - Agregar el campo a `OrderRequest` y `SendOrderCommand`.
   - Validarlo dentro de la estrategia, no en `OrderController`.
4. Documentar el tipo en las anotaciones OpenAPI de `OrderController` y en la colección Postman (`postman/financial-api-collection.json`).
5. Agregar tests: al menos un test de controller para el nuevo tipo y un test de transición de estado.

No requieren cambios: `OrderService`, `Order`, `Holdings`, `Position` y `PortfolioService`. Estas clases dependen del lado (`side`) y del estado (`status`), no del tipo.

### 9.3 Restricciones actuales

- La columna `orders.type` es `VARCHAR(10)` sin restricción `CHECK` (según `src/test/resources/schema-test.sql`). Un nombre de tipo debe caber en 10 caracteres. Verificar la base real antes de agregar un tipo, porque el esquema productivo no está versionado en este repositorio.
- Si el nuevo tipo deja la orden en `NEW` y requiere ejecución posterior, se necesita un proceso de ejecución diferida. Hoy no existe (ver D-1 y sección 6).
- Agregar un nuevo lado (distinto de `BUY`/`SELL`/`CASH_IN`/`CASH_OUT`) sí requiere cambios en `Holdings`, `Position` y las validaciones de `OrderService.sendOrder`. Es un cambio mayor, distinto de agregar un tipo.
