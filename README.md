# Microservicio de Reportería de Talleres360

Servicio independiente de ventas y auditoría. Java 17, Spring Boot 3.5.6, JPA, validación y Lombok. Puerto **8083**; PostgreSQL **report_db** en Docker y H2 en memoria para desarrollo. Rama **`backend-emmanuel`**. [Repositorio](https://github.com/EmmanuelhxGG/ms-reportes-talleres360).

Documentación del código al 6 de octubre de 2026.

## Responsabilidad y conexiones

Recibe eventos de negocio de Órdenes, conserva quién realizó cada acción y calcula ventas de órdenes entregadas. El BFF expone las consultas únicamente a Admin; los usuarios no editan ni borran eventos de auditoría.

```text
Órdenes/outbox ──► POST /internal/events ──► PostgreSQL report_db
BFF/Admin ──────► GET ventas y auditoría ──► Reportería :8083
```

Todas las rutas exigen `X-Internal-Key`. La clave coincide con BFF, Órdenes y Catálogo. Este servicio no recibe IDs de Azure ni valida JWT: la autenticación del usuario y sus permisos corresponden al BFF. El puerto debe quedar restringido a BFF y Órdenes, no al navegador.

## Estructura

Las rutas Java parten de `src/main/java/com/talleres360/report/`.

| Archivo/carpeta | Responsabilidad |
| --- | --- |
| `ReportApplication.java` | Inicio del servicio. |
| `controller/ReportController.java` | HTTP, validación de entradas y credencial interna. |
| `service/ReportService.java` | Ingesta idempotente, cálculo de ventas y consulta de auditoría. Define los contratos `EventInput` y `SalesReport`. |
| `model/BusinessEvent.java` | Evento persistido con actor, fecha, estado y total. |
| `repository/BusinessEventRepository.java` | Consultas por orden, fecha y tipo de evento. |
| `src/main/resources/application.yml` | Puerto, clave y perfiles local/PostgreSQL. |
| `Dockerfile`, `compose.yml`, `.env.example` | Construcción y despliegue independientes. |

## Eventos de negocio

`POST /internal/events` responde **204** cuando acepta el evento. La entrada contiene:

| Campo | Contenido |
| --- | --- |
| `eventId` | Identificador único del evento; clave primaria. |
| `orderId` | Orden relacionada, positivo. |
| `type` | Acción de negocio, por ejemplo `CREADA` o `ENTREGADA`. |
| `actor` | Actor recibido desde Órdenes; BFF deriva la identidad del token. |
| `reason` | Motivo de intervención cuando corresponde. |
| `occurredAt` | Instante ISO 8601 de la acción. |
| `status` | Estado de la orden al generar el evento. |
| `total` | Total monetario no negativo, almacenado con precisión decimal. |

Reenviar un `eventId` ya registrado no vuelve a guardarlo ni vuelve a sumar su venta. Órdenes conserva el evento en su outbox y reintenta el envío. Los eventos describen acciones históricas; eliminar una orden no elimina su auditoría.

## API de consulta

| Método/ruta | Resultado |
| --- | --- |
| `GET /api/reports/sales?from=<ISO>&to=<ISO>` | Cantidad de entregas e importe vendido en el período. |
| `GET /api/reports/audit` | Hasta 200 eventos recientes, ordenados del más nuevo al más antiguo. |
| `GET /api/reports/audit?orderId=1` | Eventos de esa orden, del más nuevo al más antiguo. |

Las llamadas del navegador pasan por BFF/Gateway con el access token de Admin. BFF incorpora la clave interna al comunicarse con Reportería.

### Ventas

Solo se suman eventos de tipo **`ENTREGADA`**. Crear o aceptar una solicitud no genera una venta. El intervalo es **`[from,to)`**: incluye el inicio y excluye el fin. Ambos parámetros son instantes ISO 8601 y `from` debe ser anterior a `to`.

Ejemplo de respuesta:

```json
{
  "from": "2026-10-01T00:00:00Z",
  "to": "2026-11-01T00:00:00Z",
  "deliveredOrders": 2,
  "revenue": 85000.00
}
```

El frontend transforma el último día elegido en el comienzo del día siguiente para incluir ese día completo. La respuesta es un cálculo sobre eventos guardados, no un documento de reporte editable.

### Actualización de datos

Órdenes publica aproximadamente cada segundo y el dashboard consulta aproximadamente cada diez segundos. Es actualización periódica con consistencia eventual: una entrega puede aparecer después si el envío a Reportería se reintenta. El stock se asigna en Catálogo al aceptar/editar informe; la entrega exige confirmar la asignación y no descuenta de nuevo. Reportería registra la venta, no modifica existencias.

## Configuración

Crea `.env` en la raíz, junto a `compose.yml`:

```dotenv
DB_USERNAME=talleres360
DB_PASSWORD=<CONTRASENA_DE_ESTA_BASE>
INTERNAL_API_KEY=<CLAVE_COMPARTIDA_CON_BFF_Y_MICROS>
```

Compose configura el puerto 8083, perfil `postgres` y conexión `jdbc:postgresql://postgres:5432/report_db`. PostgreSQL tiene su propio volumen `datos_postgres` y no publica 5432 en la EC2. Hibernate utiliza `ddl-auto=update`.

## Ejecutar

Con Git, Docker Engine, Buildx y Docker Compose instalados, desde este repositorio:

```bash
docker buildx version
docker compose version
docker compose config --quiet
docker compose up -d --build
docker compose ps
docker compose logs --tail=100 reportes
```

El Dockerfile incluye Maven y Java para compilar; no necesitas instalar Maven en la EC2 para este flujo.

Para desarrollo directo necesitas JDK 17 y Maven instalado; este repositorio no incluye Maven Wrapper:

```bash
mvn spring-boot:run
```

Exporta `INTERNAL_API_KEY` en la terminal para usar las rutas. Java ejecutado directamente no carga `.env` automáticamente. El perfil local utiliza H2 en memoria: los eventos desaparecen al detener el proceso.

## EC2 y actualización

[Guía de despliegue de esta EC2](DESPLIEGUE_EC2.md). Usa una instancia independiente en la misma VPC que los demás componentes. Autoriza 8083 desde BFF y Órdenes y limita SSH a los orígenes autorizados.

En **BFF y Órdenes**, configura:

```dotenv
REPORT_URL=http://<IP_PRIVADA_EC2_REPORTES>:8083
```

No añadas `/api` ni `/dev`. Si cambia esa IP, actualiza ambas configuraciones y recrea los contenedores correspondientes. No cambia la URL del frontend mientras Gateway siga siendo el mismo.

Con la rama comprobada y cambios locales preservados:

```bash
git pull --ff-only origin backend-emmanuel
docker compose up -d --build
```

El volumen conserva eventos al reconstruir/recrear contenedores. **`docker compose down -v` elimina los datos.** `restart: unless-stopped` reinicia los contenedores con Docker salvo detenciones manuales; requiere Docker habilitado al arrancar la EC2. No enciende una instancia apagada ni descarga cambios de GitHub.

## Comprobar empaquetado

```bash
mvn -DskipTests package
```

La revisión local del 8 de octubre verificó ventas e idempotencia con H2, no el despliegue AWS. Los archivos de pruebas no forman parte de esta versión. Una llamada sin clave válida devuelve 401. Mantén `.env`, contraseñas, tokens y PEM fuera del repositorio.
