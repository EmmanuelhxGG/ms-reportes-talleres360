# ms-talleres360-report

Servicio Spring Boot independiente (puerto 8083, base `report_db`). Conserva eventos de negocio de orders para auditoría y calcula ventas de **órdenes entregadas**, no de solicitudes ni de órdenes aceptadas.

## Ejecutar

Este repositorio tiene su propio `compose.yml`: copia `.env.example` a `.env`, configura DB_PASSWORD e INTERNAL_API_KEY, y ejecuta `docker compose up -d --build`. Levanta solo Reportería y su PostgreSQL persistente. La clave interna debe coincidir con Órdenes, Catálogo y BFF. Consulta [DESPLIEGUE_EC2.md](DESPLIEGUE_EC2.md) para su EC2 independiente.

Para desarrollo aislado usa Java 17 y Maven instalado: `mvn spring-boot:run` inicia con H2 en memoria y no conserva eventos al detener. Este repositorio no incluye Maven Wrapper; el Dockerfile incluye Maven para compilar sin depender de otros repositorios. Con Maven instalado, ejecuta `mvn test` para las pruebas.

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
