# Despliegue independiente: reportes

Este repositorio se ejecuta en su propia EC2 Amazon Linux 2023 x86_64. El Compose construye la imagen con el Dockerfile y levanta únicamente reportes y su PostgreSQL. Necesita Git, Docker Engine, Buildx y Docker Compose instalados.

## 1. Configurar

Desde la raíz del repositorio:

```bash
cp .env.example .env
chmod 600 .env
nano .env
```

Completa DB_PASSWORD con una contraseña propia y INTERNAL_API_KEY con la misma clave compartida por los tres microservicios y el BFF. Puedes generar cada secreto con `openssl rand -hex 32`. No publiques .env.

## 2. Construir y levantar

```bash
docker buildx version
docker compose version
docker compose config --quiet
docker compose up -d --build
docker compose ps
docker compose logs --tail=100 reportes
```

La base debe mostrar healthy y el microservicio debe iniciar sin errores en sus logs. Que el contenedor esté running no garantiza que la API esté lista.

## 3. Red

Publica únicamente TCP 8083 para la API. No abras PostgreSQL (5432). En el Security Group permite 8083 desde el grupo del BFF y desde el grupo de Órdenes, sin abrirlo a Internet.

En el BFF configura REPORT_URL=http://IP_PRIVADA_DE_ESTA_EC2:8083.

## 4. Actualizar y conservar los datos

```bash
git pull --ff-only origin backend-emmanuel
docker compose up -d --build
```

El volumen datos_postgres conserva la base al recrear contenedores. No ejecutes `docker compose down -v`: elimina la base. `restart: unless-stopped` reinicia los contenedores después de un reinicio de la EC2 si Docker está habilitado; no descarga cambios de Git.

Para consultar la API de este servicio usa el encabezado X-Internal-Key con la clave configurada. El navegador debe llamar a API Gateway/BFF, no directamente a esta EC2.
