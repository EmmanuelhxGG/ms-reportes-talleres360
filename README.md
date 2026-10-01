# ms-talleres360-report

Servicio Spring Boot independiente (puerto 8083, base `report_db`). Conserva eventos de negocio de orders para auditoría y calcula ventas de **órdenes entregadas**, no de solicitudes ni de órdenes aceptadas.

## Ejecutar

El Compose local `infra/apps/compose.yml` y el de EC2 `infra/ms/compose.yml` crean el servicio y su PostgreSQL independiente. Configura la **misma** `INTERNAL_API_KEY` en backend y BFF. Para desarrollo aislado, `mvn spring-boot:run` inicia con H2 en memoria; no conserva eventos al detener. Este módulo no tiene wrapper propio: desde su carpeta puedes usar `../ms-talleres360-orders/mvnw -f pom.xml spring-boot:run` (Windows: `..\ms-talleres360-orders\mvnw.cmd -f pom.xml spring-boot:run`). Sustituye el objetivo por `test` para las pruebas.

## API interna

Todas las llamadas requieren `X-Internal-Key`; el BFF expone solo las lecturas a Admin.

| Método y ruta | Uso |
| --- | --- |
| `POST /internal/events` | Recibir eventos persistidos en el outbox de orders, con ID idempotente. |
| `GET /api/reports/sales?from=<ISO>&to=<ISO>` | Cantidad y total de entregas en `[from,to)`. |
| `GET /api/reports/audit` | Últimos 200 eventos. |
| `GET /api/reports/audit?orderId=1` | Todos los eventos de esa orden. |

El evento conserva `eventId`, `orderId`, `type`, `actor` (derivado del access token por el BFF), `reason` cuando Admin interviene, `occurredAt`, `status` y `total`. El publicador de orders reintenta eventos pendientes; repetir el mismo `eventId` no duplica ventas. Para entrega, reporta solo después de que Catálogo confirmó el descuento.

**No es tiempo real estricto:** el outbox se sondea aproximadamente cada segundo y puede retrasarse si un servicio está caído. El dashboard consulta cada 10 segundos. Si falla el descuento de stock de una entrega, su venta no se publica hasta que Catálogo lo confirme; la orden puede figurar como entregada entretanto. Se requieren alertas, reconciliación, retención y exportación si el sistema pasa a producción.
