# Una sola base PostgreSQL: talleres360

Los cinco microservicios utilizan una misma base `talleres360`, alojada en la EC2
de Órdenes. Todas las tablas están en `public`; cada aplicación conserva un
usuario con permisos sobre sus tablas. El BFF y el frontend no se conectan a SQL.

Este servicio utiliza el usuario `t360_reportes`. En `.env` configura:

```dotenv
DB_URL=jdbc:postgresql://IP_PRIVADA_EC2_ORDENES:5432/talleres360
DB_USERNAME=t360_reportes
DB_PASSWORD=CONTRASENA_PRIVADA_DEL_USUARIO
```

`compose.yml` levanta únicamente la aplicación. La infraestructura PostgreSQL
compartida está en el repositorio de Órdenes, `infra/database/compose.yml`.
El puerto 5432 se permite solo desde las EC2 de los microservicios; no desde Internet.
No se publican contraseñas ni los archivos privados de configuración.

Para mostrar todas las tablas, entra por SSH en la EC2 de Órdenes y ejecuta:

```bash
cd /home/ec2-user/ms-ordenes-talleres360
docker compose --env-file infra/database/.env -f infra/database/compose.yml exec postgres sh -c 'exec psql -U "$POSTGRES_USER" -d "$POSTGRES_DB"'
```

Dentro de PostgreSQL:

```sql
\dt public.*
SELECT id,nombre_cliente,estado,total FROM solicitudes ORDER BY id;
SELECT id,nombre,existencias,precio FROM productos ORDER BY id;
SELECT comando_id,solicitud_id,tipo,estado FROM notificaciones ORDER BY ocurrido_en DESC LIMIT 20;
SELECT count(*) FROM eventos_auditoria;
SELECT count(*) FROM eventos_reporteria;
```

`\q` sale de PostgreSQL. Las bases separadas anteriores se conservan como
respaldo de la migración y sus contenedores quedan detenidos. La base activa es
únicamente `talleres360`; sus datos se guardan en un volumen Docker persistente.
Centralizarla facilita la presentación, pero los cinco servicios dependen de esa
misma instancia de base de datos.

## Tablas de este microservicio

- `eventos_reporteria`

Las tablas y columnas de aplicación están en español. Los historiales de Flyway
conservan sus columnas técnicas para mantener compatibilidad con esa herramienta.
Los contratos JSON del BFF y del frontend no cambian por este renombrado físico.
