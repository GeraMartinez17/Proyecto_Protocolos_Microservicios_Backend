# Microservicio de Productos y Ventas — Backend

API RESTful construida con **Spring Boot 4.1** y **MySQL 8** para administrar un catálogo de productos con inventario y registrar ventas. Es un único microservicio modular con dos módulos funcionales, **productos** y **ventas**, y una arquitectura por capas (controller, service, repository, entity, DTO, exception, config).

El registro de una venta es **transaccional**: valida existencia, estado y stock de todos los productos, calcula subtotales y total, y descuenta el inventario de forma atómica. Las filas de producto se bloquean con `SELECT … FOR UPDATE` para evitar inconsistencias entre ventas concurrentes.

## Tecnologías

| Tecnología | Versión |
|---|---|
| Java (OpenJDK) | 21 |
| Spring Boot (Web MVC, Data JPA, Validation, Test) | 4.1.2 |
| Hibernate ORM | 7.4.13 |
| MySQL Server / Connector/J | 8.0 / 9.7.0 |
| Flyway (core + mysql) | 12.4.0 |
| springdoc-openapi (Swagger UI) | 3.2.0 |
| JUnit Jupiter / Mockito / AssertJ | 6.0.3 / 5.23.0 / 3.27.7 |
| Maven (incluye Maven Wrapper) | 3.8+ |

## Requisitos

- JDK 21 (`javac -version`)
- MySQL 8.0 o superior en ejecución
- Maven 3.6.3 o superior, o el wrapper incluido (`./mvnw`)

## Instalación y configuración

1. **Crear la base de datos y un usuario dedicado** (como administrador de MySQL; elija su propia contraseña):

   ```sql
   CREATE DATABASE db_microservicios CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   CREATE DATABASE db_microservicios_test CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   CREATE USER 'micro_user'@'localhost' IDENTIFIED BY 'su_contraseña';
   GRANT ALL PRIVILEGES ON db_microservicios.*      TO 'micro_user'@'localhost';
   GRANT ALL PRIVILEGES ON db_microservicios_test.* TO 'micro_user'@'localhost';
   ```

   Las tablas **no** se crean a mano: Flyway aplica `V1__crear_tablas.sql` y `V2__datos_iniciales.sql` al arrancar.

2. **Configurar las credenciales.** Copie `.env.example` como `.env` y escriba sus valores. El archivo `.env` está excluido de git.

   ```bash
   cp .env.example .env
   ```

   | Variable | Descripción | Valor por defecto |
   |---|---|---|
   | `DB_URL` | URL JDBC | `jdbc:mysql://localhost:3306/db_microservicios?serverTimezone=America/Mexico_City` |
   | `DB_USERNAME` | Usuario | `micro_user` |
   | `DB_PASSWORD` | Contraseña | *(vacío)* |
   | `TEST_DB_URL` | Base de las pruebas de integración | `…/db_microservicios_test…` |
   | `CORS_ALLOWED_ORIGINS` | Orígenes permitidos (separados por coma) | `http://localhost:5173,http://127.0.0.1:5173` |
   | `SERVER_PORT` | Puerto HTTP | `8080` |

   También pueden definirse como variables de entorno del sistema.

## Ejecución

```bash
./mvnw spring-boot:run          # o: mvn spring-boot:run
```

- API: <http://localhost:8080/api>
- Swagger UI: <http://localhost:8080/swagger-ui.html>
- OpenAPI JSON: <http://localhost:8080/v3/api-docs>

Generar el JAR ejecutable:

```bash
./mvnw clean package
java -jar target/microservicio-productos-ventas-1.0.0.jar
```

## Pruebas

```bash
./mvnw test
```

Ejecuta **64 pruebas**: 25 unitarias (Mockito), 16 de capa web (`@WebMvcTest` + MockMvc) y 23 de integración (`@SpringBootTest` + MockMvc + MySQL real). Las de integración usan el perfil `test` y la base `db_microservicios_test`, que se limpia y migra con Flyway antes de cada prueba. Incluyen una prueba de concurrencia: 20 ventas simultáneas sobre 8 unidades producen exactamente 8 respuestas `201` y 12 respuestas `409`.

### Colección de Postman

En `postman/` se incluyen `Microservicio_Productos_Ventas.postman_collection.json` (34 solicitudes, 70 aserciones) y el entorno `Local.postman_environment.json`. Puede importarse en Postman o ejecutarse con Newman:

```bash
npx newman run postman/Microservicio_Productos_Ventas.postman_collection.json -e postman/Local.postman_environment.json
```

La colección crea sus propios datos y debe ejecutarse en orden.

## Endpoints

| Método | Endpoint | Descripción | Éxito | Errores |
|---|---|---|---|---|
| GET | `/api/productos` | Listar productos | 200 | — |
| GET | `/api/productos/{id}` | Obtener un producto | 200 | 400, 404 |
| POST | `/api/productos` | Registrar un producto | 201 | 400, 409 |
| PUT | `/api/productos/{id}` | Actualizar un producto | 200 | 400, 404, 409 |
| DELETE | `/api/productos/{id}` | Eliminar un producto sin ventas | 204 | 404, 409 |
| GET | `/api/productos/buscar?nombre=` | Buscar por nombre | 200 | 400 |
| GET | `/api/ventas` | Listar ventas | 200 | — |
| GET | `/api/ventas/{id}` | Obtener una venta | 200 | 404 |
| POST | `/api/ventas` | Registrar una venta | 201 | 400, 404, 409 |
| GET | `/api/ventas/fecha?inicio=&fin=` | Ventas entre fechas (`AAAA-MM-DD`) | 200 | 400 |
| GET | `/api/ventas/{id}/detalle` | Detalle de una venta | 200 | 404 |
| DELETE | `/api/ventas/{id}` | Cancelar una venta y reponer inventario | 204 | 404, 409 |

Los errores siguen el formato **Problem Details (RFC 9457)**.

## Ejemplos de uso

Registrar un producto:

```bash
curl -i -X POST http://localhost:8080/api/productos \
  -H 'Content-Type: application/json' \
  -d '{"nombre":"Bocina Bluetooth JBL Go 4","descripcion":"Bocina portátil","precio":799.00,"stock":15}'
```

```http
HTTP/1.1 201
Location: http://localhost:8080/api/productos/11
Content-Type: application/json

{"id":11,"nombre":"Bocina Bluetooth JBL Go 4","descripcion":"Bocina portátil","precio":799.00,"stock":15,"estado":"ACTIVO", ...}
```

Registrar una venta:

```bash
curl -i -X POST http://localhost:8080/api/ventas \
  -H 'Content-Type: application/json' \
  -d '{"productos":[{"productoId":2,"cantidad":2},{"productoId":5,"cantidad":4}]}'
```

Vender más unidades de las disponibles:

```http
HTTP/1.1 409
Content-Type: application/problem+json

{"type":"about:blank","title":"Conflicto con las reglas de negocio","status":409,
 "detail":"Stock insuficiente para 'Laptop Lenovo IdeaPad 3': disponible 8, solicitado 500","instance":"/api/ventas"}
```

## Reglas de negocio

1. Una venta debe incluir al menos un producto (400).
2. Las cantidades deben ser mayores que cero (400).
3. No se venden productos inexistentes (404) ni inactivos (409).
4. No se venden más unidades de las disponibles (409).
5. Subtotal = cantidad × precio unitario; total = suma de subtotales (calculados en el servidor).
6. El inventario se descuenta al vender y se repone al cancelar.
7. Registro de venta y actualización de inventario en una sola transacción (sin cambios parciales).
8. Bloqueo pesimista ordenado para ventas concurrentes; bloqueo optimista (`version`) para ediciones de productos.
9. No se puede eliminar un producto con ventas (409); se recomienda marcarlo como `INACTIVO`.

## Estructura del proyecto

```text
src/main/java/com/proyecto/microservicio/
├── MicroservicioApplication.java
├── config/          CorsConfig, OpenApiConfig, ClockConfig
├── exception/       GlobalExceptionHandler y excepciones de negocio
├── productos/       controller · service · repository · entity · dto
└── ventas/          controller · service · repository · entity · dto
src/main/resources/
├── application.yml, application-test.yml
└── db/migration/    V1__crear_tablas.sql, V2__datos_iniciales.sql
src/test/java/com/proyecto/microservicio/
├── unit/            pruebas unitarias con Mockito
├── web/             pruebas de controladores con MockMvc
└── integracion/     pruebas de integración contra MySQL
postman/             colección y entorno de Postman
```

## Frontend

La interfaz web en Vue.js que consume esta API está en el repositorio `microservicio-productos-ventas-frontend`.
