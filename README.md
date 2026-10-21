# api-postulaciones-java

API para llevar el seguimiento de candidaturas de empleo, hecha con Java 21 y Spring Boot. Es la reescritura de [api-postulaciones](https://github.com/danielbuitragoh/api-postulaciones) (Node) para aprender el stack Java.

> Estado: en construcción. De momento existen el esquema de base de datos, `/salud` y la autenticación (`/auth/registro`, `/auth/login`, `/auth/refresh`).

## Arrancar en local

Requiere Docker.

```bash
cp .env.example .env
# Rellena JWT_SECRET en .env (instrucciones dentro del archivo)
docker compose up --build
```

La API queda en `http://localhost:8080/salud`.

## Pruebas

```bash
./mvnw verify
```

Las pruebas usan Testcontainers con PostgreSQL 16 real, así que necesitan Docker en marcha.

## Stack

Java 21 · Spring Boot 4.1 · Spring Data JPA · Spring Security · Flyway · PostgreSQL 16 · Testcontainers · Docker

## Licencia

MIT
