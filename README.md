# SICOIL

Stack completo (Spring Boot + Angular + MySQL) listo para ejecutarse con Docker Compose desde cualquier máquina que tenga Docker instalado.

## Requisitos

- Docker Engine y Docker Compose plugin (v2.20 o superior recomendado).

## Configurar variables de entorno

1. Duplica el archivo `.env.example` como `.env`.
2. Cambia las contraseñas por valores seguros (`MYSQL_ROOT_PASSWORD`, `MYSQL_PASSWORD`).

El archivo `.env` es ignorado por Git para evitar exponer secretos.

## Levantar los contenedores

Desde la raíz del proyecto:

```bash
docker compose up --build
```

Docker construirá las imágenes del backend (`Dockerfile`) y del frontend (`frontend/Dockerfile`), creará un contenedor de MySQL y otro de phpMyAdmin. La compilación de Maven y Angular ocurre dentro de los contenedores; no necesitas tener Java ni Node instalados localmente.

### Servicios expuestos

- Frontend Angular: `http://localhost:${FRONTEND_PORT:-4200}`
- Backend REST: `http://localhost:${BACKEND_PORT:-8080}`
- phpMyAdmin: `http://localhost:8081` (usar las credenciales definidas en el `.env`)

La base de datos persiste en el volumen `mysql_data`, por lo que los datos sobreviven a reinicios.

## Comandos útiles

- Ver logs: `docker compose logs -f backend`
- Reconstruir desde cero: `docker compose build --no-cache`
- Detener todo y eliminar contenedores (conservando el volumen): `docker compose down`
- Eliminar también el volumen: `docker compose down -v`

## Flujo local alternativo

Si prefieres desarrollar sin Docker:

1. Levanta MySQL manualmente reutilizando los valores del `.env`.
2. Backend: `mvn -DskipTests package` o `mvn spring-boot:run`.
3. Frontend: dentro de `frontend/`, `npm install` y `npm run start -- --host 0.0.0.0`.

De cualquier manera, mantener el `.env` actualizado garantiza que los contenedores funcionen en cualquier computadora con solo clonar el repositorio y ejecutar `docker compose up`.
