# api-postulaciones-java

API para llevar el seguimiento de candidaturas de empleo, hecha con Java 21 y Spring Boot. Es la reescritura de [api-postulaciones](https://github.com/danielbuitragoh/api-postulaciones) (Node) para aprender el stack Java.

> Estado: en construcción. Existen el esquema de base de datos, la autenticación, el CRUD de postulaciones con eventos, las estadísticas, el rate limiting y la documentación en `/docs`. Faltan el CI y el despliegue.

La documentación interactiva (Swagger UI) está en `http://localhost:8080/docs`: registra una cuenta, inicia sesión, pulsa **Authorize** con el `accessToken` y prueba el resto desde ahí.

## Endpoints disponibles

| Método | Ruta | Qué hace |
|---|---|---|
| POST | `/auth/registro`, `/auth/login`, `/auth/refresh` | Cuenta, sesión y rotación del refresh token |
| GET | `/postulaciones?estado=&empresa=&pagina=&orden=` | Lista paginada (20 por página, la primera es la 0). `orden`: `reciente` (por defecto), `antigua` o `empresa` |
| POST | `/postulaciones` | Crea una postulación (y su evento `postulada`) |
| GET, PATCH, DELETE | `/postulaciones/{id}` | Detalle con eventos, cambios parciales y borrado |
| POST | `/postulaciones/{id}/eventos` | Añade un evento (`postulada`, `respuesta`, `entrevista`, `oferta`, `rechazo`, `seguimiento`, `cerrada`) |
| GET | `/estadisticas` | Embudo y tiempo hasta la primera respuesta (ver abajo) |
| GET | `/salud` | Estado del servicio |
| GET | `/docs` | Swagger UI |

El estado de una postulación no es una columna: es el tipo de su evento más reciente en el tiempo (vista `estado_actual`).

## Estadísticas: qué cuenta y qué no

- **Respuesta de la empresa** es un evento `respuesta`, `entrevista`, `oferta` o `rechazo`. `seguimiento` y `cerrada` los pone el usuario y no cuentan.
- **Tiempo de respuesta:** días desde `postulado_en` hasta la **primera** respuesta, no la última.
- **Sin respuesta:** las postulaciones que nunca contestaron no cuentan como 0 días; salen aparte (`sinRespuesta`) y la media usa solo las que respondieron.
- **Embudo** (postuladas → respondieron → entrevista → oferta): cuenta postulaciones en las que el paso ocurrió alguna vez, sin mirar el estado actual. Una postulación que pasó por entrevista y terminó en rechazo cuenta en "entrevista". Los escalones no se fuerzan a ser decrecientes: si alguien registra una oferta sin entrevista, sale tal cual.

## Límite de peticiones

Por IP y por minuto: 5 en `POST /auth/login` y 100 en el resto (no se limitan `/salud` ni la documentación). Al pasarse, la API responde `429` con la cabecera `Retry-After`. Los contadores viven en memoria y se vacían al reiniciar la API.

## Cómo sé que las pruebas de autorización sirven

La autorización está en el `WHERE` de cada consulta (`usuarioId` va en la propia consulta, no en un `if` posterior), y un recurso ajeno responde 404. Que las pruebas pasen no demuestra nada si no se ponen en rojo cuando falta el filtro, así que lo comprobé:

1. Con todo correcto (en ese momento del proyecto), `./mvnw verify` daba 43 pruebas en verde.
2. Neutralicé a propósito el filtro de usuario en dos consultas de `PostulacionRepository`: `buscarResumen` (detalle) y `borrar`. Cambié `p.usuarioId = :usuarioId` por `(p.usuarioId = :usuarioId or true)`, que equivale a quitarlo sin tocar las firmas.
3. Resultado real de `./mvnw verify`:

```
[ERROR] Tests run: 9, Failures: 4, Errors: 0, Skipped: 0 -- in AutorizacionTests
[ERROR]   AutorizacionTests.bNoPuedeBorrarLaPostulacionDeA:53 Status expected:<404> but was:<204>
[ERROR]   AutorizacionTests.bNoPuedeVerLaPostulacionDeA:31 Status expected:<404> but was:<200>
[ERROR]   AutorizacionTests.unPatchNoPuedeCambiarElDuenoNiLaEmpresa:149 Status expected:<404> but was:<200>
[ERROR]   AutorizacionTests.unRecursoAjenoYUnoInexistenteSonIndistinguibles:109 Status expected:<404> but was:<200>
[ERROR] Tests run: 43, Failures: 4, Errors: 0, Skipped: 0
```

Se pusieron en rojo 4 pruebas, todas de `AutorizacionTests`, y las otras 39 siguieron en verde. Con `borrar` sin filtro, un usuario llegaba a borrar la postulación de otro (`204` en lugar de `404`). Lo que no se tocó (modificar, añadir eventos, listado y filtros) siguió pasando porque esas consultas conservaban su filtro.
4. Restauré las dos consultas y `./mvnw verify` volvió a dar `BUILD SUCCESS`.

Matiz: la cuarta prueba fallida (`unPatchNoPuedeCambiarElDuenoNiLaEmpresa`) cae en su última comprobación, que mira que B no pueda ver la postulación de A; es una consecuencia del filtro quitado en `buscarResumen`, no un fallo de `PATCH`.

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

Java 21 · Spring Boot 4.1 · Spring Data JPA · Spring Security · Flyway · PostgreSQL 16 · springdoc-openapi · Bucket4j · Testcontainers · Docker

## Licencia

MIT
