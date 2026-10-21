# api-postulaciones-java

[![CI](https://github.com/danielbuitragoh/api-postulaciones-java/actions/workflows/ci.yml/badge.svg)](https://github.com/danielbuitragoh/api-postulaciones-java/actions/workflows/ci.yml)

API REST para llevar el seguimiento de candidaturas de empleo, con el historial de cada postulación como eventos y estadísticas de respuesta. Es la reescritura en Java 21 y Spring Boot de mi [api-postulaciones](https://github.com/danielbuitragoh/api-postulaciones) (Node), para aprender el stack Java.

- **Demo:** [api-postulaciones-java.onrender.com](https://api-postulaciones-java.onrender.com/salud) · **Documentación interactiva (Swagger UI):** [/docs](https://api-postulaciones-java.onrender.com/docs), o `http://localhost:8080/docs` en local
- **Probarla sin instalar nada:** no hace falta una cuenta previa. En `/docs` abre `POST /auth/registro` con cualquier email (ficticio) y una contraseña de 8 caracteres o más, inicia sesión con `POST /auth/login`, pulsa **Authorize** con el `accessToken` y ya puedes crear postulaciones, añadir eventos y ver `/estadisticas`.
- **Aviso:** la demo corre en el plan gratuito de Render, que duerme el servicio tras un rato sin tráfico: la primera petición puede tardar ~40 s.
- **Arrancar en local** (necesita Docker): `cp .env.example .env`, rellena `JWT_SECRET` en `.env` y ejecuta:

```bash
docker compose up --build
```

**Stack:** Java 21 · Spring Boot 4.1 · Spring Data JPA · Spring Security · Flyway · PostgreSQL 16 · springdoc-openapi · Bucket4j · Testcontainers · Docker · GitHub Actions

---

## La decisión de diseño: el estado no es una columna

El estado de una postulación **no se sobrescribe**. Cada cambio es una fila en `eventos` (`postulada`, `respuesta`, `entrevista`, `oferta`, `rechazo`, `seguimiento`, `cerrada`) y el estado actual sale de una vista (`estado_actual`, con `DISTINCT ON`): el tipo del evento más reciente **en el tiempo**, no el último insertado. Sobrescribir pierde el "cuándo", y sin el cuándo no se puede responder "cuánto tardan de media en contestarme".

## Endpoints

| Método | Ruta | Qué hace |
|---|---|---|
| POST | `/auth/registro`, `/auth/login`, `/auth/refresh` | Cuenta, sesión y rotación del refresh token |
| GET | `/postulaciones?estado=&empresa=&pagina=&orden=` | Lista paginada (20 por página, la primera es la 0). `orden`: `reciente` (por defecto), `antigua` o `empresa` |
| GET, POST | `/empresas` | Mis empresas (con cuántas postulaciones tiene cada una) y alta. El nombre es único por usuario, sin distinguir mayúsculas |
| DELETE | `/empresas/{id}` | Borra una empresa; `409` si tiene postulaciones |
| POST | `/postulaciones` | Crea una postulación (y su evento `postulada`). La empresa va como `empresaId` (una de las tuyas) o como `empresa` (objeto con `nombre`: se reutiliza la tuya o se crea), pero no ambos |
| GET, PATCH, DELETE | `/postulaciones/{id}` | Detalle con eventos, cambios parciales y borrado |
| POST | `/postulaciones/{id}/eventos` | Añade un evento |
| GET | `/estadisticas` | Embudo y tiempo hasta la primera respuesta |
| GET | `/salud` | Estado del servicio |
| GET | `/docs` | Swagger UI |

### Estadísticas: qué cuenta y qué no

- **Respuesta de la empresa** es un evento `respuesta`, `entrevista`, `oferta` o `rechazo`. `seguimiento` y `cerrada` los pone el usuario y no cuentan.
- **Tiempo de respuesta:** días desde `postulado_en` hasta la **primera** respuesta, no la última.
- **Sin respuesta:** las postulaciones que nunca contestaron no cuentan como 0 días; salen aparte (`sinRespuesta`) y la media usa solo las que respondieron.
- **Embudo** (postuladas → respondieron → entrevista → oferta): cuenta postulaciones en las que el paso ocurrió alguna vez, sin mirar el estado actual. Una que pasó por entrevista y terminó en rechazo cuenta en "entrevista". Los escalones no se fuerzan a ser decrecientes.

Cada uno de los tres matices tiene una prueba con datos construidos a mano. Comprobé que la del primero se pone en rojo si cambio `MIN` por `MAX` en la consulta (devuelve `20.0` en lugar de `2.0`).

## Seguridad

| Qué | Cómo |
|---|---|
| Contraseñas | Argon2id (19 MiB, 2 iteraciones, 1 hilo). Una prueba lee el hash de la base y comprueba esos parámetros |
| Sesión | JWT de acceso de 15 min (HS256, algoritmo fijado al verificar) + refresh token opaco de 7 días, guardado como SHA-256 y rotado en cada uso |
| Refresh reutilizado | Si llega uno ya rotado, se revocan todos los tokens del usuario. Una prueba lo comprueba contra la base real |
| Validación | Bean Validation en todos los DTOs de entrada, antes de tocar la base |
| SQL | Solo consultas parametrizadas; el `ORDER BY` sale de una lista blanca, nunca del cliente |
| Autorización | `usuario_id` va en el `WHERE` de cada consulta. Un recurso ajeno responde 404, igual que uno inexistente |
| Login | Usuario inexistente y contraseña errónea dan la misma respuesta (comprobado con una prueba); el primero se compara contra un hash señuelo para igualar el coste. No he medido tiempos |
| Límite de peticiones | Bucket4j por IP: 5 por minuto en `/auth/login`, 100 en el resto. Al pasarse, `429` con `Retry-After`. Los contadores viven en memoria. En la demo la IP se lee de `CF-Connecting-IP` (ver "Despliegue") |
| Cabeceras y CORS | Cabeceras por defecto de Spring Security. CORS con lista blanca (`APP_CORS_ORIGENES`); un comodín impide arrancar la API |
| Errores | `ProblemDetail` (RFC 9457) también en los 401 de los filtros de seguridad; sin trazas, nombres de clases ni el motivo exacto del rechazo de un token |

### Cómo sé que las pruebas de autorización sirven

La autorización está en el `WHERE` de cada consulta (`usuarioId` va en la propia consulta, no en un `if` posterior), y un recurso ajeno responde 404. Que las pruebas pasen no demuestra nada si no se ponen en rojo cuando falta el filtro, así que lo comprobé dos veces. Las dos veces, con todo correcto, `./mvnw verify` daba 84 pruebas en verde, y neutralicé el filtro cambiando `usuarioId = :usuarioId` por `(usuarioId = :usuarioId or true)`, que equivale a quitarlo sin tocar las firmas. Al terminar, restauré las consultas con `git checkout` y `./mvnw verify` volvió a dar `BUILD SUCCESS`.

**Experimento 1: dos consultas de `PostulacionRepository` (`buscarResumen`, el detalle, y `borrar`).** Salida real de `./mvnw verify`:

```
[ERROR] Tests run: 13, Failures: 4, Errors: 0, Skipped: 0 -- in AutorizacionTests
[ERROR]   AutorizacionTests.bNoPuedeBorrarLaPostulacionDeA:53 Status expected:<404> but was:<204>
[ERROR]   AutorizacionTests.bNoPuedeVerLaPostulacionDeA:31 Status expected:<404> but was:<200>
[ERROR]   AutorizacionTests.unPatchNoPuedeCambiarElDuenoNiLaEmpresa:152 Status expected:<404> but was:<200>
[ERROR]   AutorizacionTests.unRecursoAjenoYUnoInexistenteSonIndistinguibles:109 Status expected:<404> but was:<200>
[ERROR] Tests run: 84, Failures: 4, Errors: 0, Skipped: 0
```

Se pusieron en rojo 4 pruebas, todas de `AutorizacionTests`, y las otras 80 siguieron en verde. Con `borrar` sin filtro, un usuario llegaba a borrar la postulación de otro (`204` en lugar de `404`). Lo que no se tocó (modificar, añadir eventos, listado y filtros) siguió pasando porque esas consultas conservaban su filtro. La cuarta prueba (`unPatchNoPuedeCambiarElDuenoNiLaEmpresa`) cae en su última comprobación, que mira que B no pueda ver la postulación de A: es consecuencia del filtro quitado en `buscarResumen`, no un fallo de `PATCH`.

**Experimento 2: dos consultas de `EmpresaRepository` (`listar` y `borrar`).** Salida real:

```
[ERROR] Tests run: 13, Failures: 2, Errors: 0, Skipped: 0 -- in AutorizacionTests
[ERROR] Tests run: 12, Failures: 4, Errors: 0, Skipped: 0 -- in EmpresaTests
[ERROR]   AutorizacionTests.bNoPuedeBorrarLaEmpresaDeA:176 Status expected:<404> but was:<204>
[ERROR]   AutorizacionTests.cadaUsuarioSoloVeSusEmpresas:189 JSON path "$.length()" expected:<2> but was:<16>
[ERROR]   EmpresaTests.elListadoVaPorNombreYCuentaLasPostulaciones:58 JSON path "$.length()" expected:<2> but was:<22>
[ERROR]   EmpresaTests.postularPorNombreReutilizaLaEmpresaDelUsuarioEnLugarDeDuplicarla:121 JSON path "$.length()" expected:<1> but was:<26>
[ERROR]   EmpresaTests.unaEmpresaConPostulacionesNoSePuedeBorrarYSeConserva:83 JSON path "$.length()" expected:<1> but was:<25>
[ERROR]   EmpresaTests.unaEmpresaSinPostulacionesSePuedeBorrar:72 JSON path "$.length()" expected:<0> but was:<26>
[ERROR] Tests run: 84, Failures: 6, Errors: 0, Skipped: 0
```

Aquí se pusieron en rojo 6 pruebas y las otras 78 siguieron en verde. Dos son de autorización (`bNoPuedeBorrarLaEmpresaDeA` y `cadaUsuarioSoloVeSusEmpresas`); las otras cuatro son pruebas funcionales de `EmpresaTests` que cuentan el listado de empresas y fallan porque, sin filtro, `GET /empresas` devolvía las empresas de todos los usuarios (16, 22, 25 y 26 filas en lugar de 2, 2, 1 y 0). Es decir, esta vez no fallaron "solo" las de autorización: el filtro quitado afecta también al comportamiento normal del listado.

## Pruebas

```bash
./mvnw verify
```

89 pruebas (JUnit 5, AssertJ, MockMvc) contra PostgreSQL 16 real con Testcontainers, sin mocks de la base de datos: los fallos de autorización viven en el SQL y un mock los esconde. Necesitan Docker en marcha. El mismo comando corre en GitHub Actions en cada push y pull request.

## Script auxiliar

`scripts/informe.py` es un script pequeño de Python (solo biblioteca estándar, sin dependencias) que llama a `/estadisticas` y muestra un informe legible en la terminal. No es un proyecto en Python: es una comodidad para mirar mis cifras sin abrir Swagger.

```bash
export API_TOKEN="el-accessToken-del-login"   # en PowerShell: $env:API_TOKEN = "..."
python scripts/informe.py                     # opcional: API_URL, por defecto http://localhost:8080
```

## Despliegue

API en [Render](https://render.com) (plan gratuito, imagen Docker, ver `render.yaml`) y base de datos en [Neon](https://neon.com) (plan gratuito). Variables de entorno de la API:

| Variable | Qué es |
|---|---|
| `DB_URL` | `jdbc:postgresql://<host>/<base>?sslmode=require` (host directo de Neon, no el `-pooler`) |
| `DB_USER`, `DB_PASSWORD` | Usuario y contraseña de la base |
| `JWT_SECRET` | Secreto de firma, de al menos 32 bytes. Render lo genera solo (`generateValue` en `render.yaml`) |
| `APP_CORS_ORIGENES` | Opcional: orígenes de navegador permitidos, separados por comas |
| `APP_RATE_LIMIT_CABECERA_IP_REAL` | Cabecera de la que se lee la IP del cliente para el límite de peticiones. En `render.yaml` vale `CF-Connecting-IP`. Vacía en local |

### La IP del cliente detrás de Render (un fallo real que apareció al desplegar)

Al desplegar, el límite de login **no funcionaba**: 9 intentos seguidos desde la misma IP daban 9 `401` y ningún `429`, mientras que en local y en la CI sí funcionaba. Detrás de Cloudflare y del proxy de Render, la IP que ve la API como origen de la conexión no es la del cliente y cambia entre peticiones, así que cada petición caía en un cubo distinto. Leer `X-Forwarded-For` no es una solución: Render añade entradas a esa cabecera en lugar de reemplazarla, y un cliente puede inventarse las primeras.

La solución fue leer la IP de `CF-Connecting-IP`, que fija Cloudflare, y hacerlo opcional por configuración. Render no documenta ese comportamiento, así que lo comprobé contra la demo desplegada (cada serie son peticiones a `POST /auth/login` con credenciales falsas, esperando un minuto entre series para que se rellene el cupo):

```
Sin cabeceras propias (control):                    401,401,401,401,401,429,429,429,429
X-Forwarded-For distinto en cada petición:          401,401,401,401,401,429,429,429,429
CF-Connecting-IP puesta por el cliente:             403,403,403,403
GET /salud (sin límite):                            200,200,200,200
```

Cinco intentos y luego `429`, también cuando el cliente falsea `X-Forwarded-For`; y si el cliente manda su propio `CF-Connecting-IP`, Cloudflare rechaza la petición con `403`, así que esa cabecera no se puede falsear desde fuera. Lo que **no he podido comprobar** es que dos clientes con IP distinta tengan cupos separados (necesitaría pedirlo desde dos redes diferentes), y depende de que Render siga delante de Cloudflare: si algún día cambia, habría que volver a verificarlo.

## Misma API en Node y en Java: qué cambió

Comparado leyendo el código de las dos versiones. Las cifras son líneas no vacías (incluyen comentarios) y no son del todo comparables: la versión Java tiene además Swagger, CORS y rate limiting propios.

| Tema | Node (TypeScript) | Java (Spring Boot) | Lo que vi al hacerlo |
|---|---|---|---|
| Tamaño | 14 archivos, 1.187 líneas (`src`, TS y SQL) | 51 clases, 1.730 líneas (`src/main`) + 145 del `pom.xml` | Más verboso en Java: una clase por entidad, DTO, excepción y repositorio. Los `record` evitaron Lombok y los getters a mano en los DTO |
| Pruebas | 43 | 89 | Java incluye 15 de autenticación, 13 de autorización, 12 de empresas, 8 de cabeceras, CORS y errores 401, y 8 de límite de peticiones |
| Validación | zod | Bean Validation (anotaciones en los `record`) | Las anotaciones leen bien, pero la comprobación `salarioMin <= salarioMax` cruza dos campos y hubo que hacerla además en el servicio (la base también la tiene como `CHECK`) |
| Acceso a datos | `pg` con SQL a mano | Spring Data JPA/Hibernate, y SQL a mano con `JdbcClient` para estadísticas | Hibernate con `ddl-auto=validate` detectó al arrancar que `hash_token` es `CHAR(64)` en la base y la entidad esperaba `varchar`. Las agregaciones (CTE, `bool_or`) en JPQL no tenían sentido |
| Autorización en el `WHERE` | `... where id = $1 and usuario_id = $2` | `findByIdAndUsuarioId` y `@Query` con `usuarioId` | Igual de explícito en las `@Query`; en los métodos derivados el filtro queda escondido en el nombre del método, y por eso dependo más de las pruebas |
| Crear o reutilizar una empresa | `insert ... on conflict (usuario_id, lower(nombre))` en una sola sentencia | Buscar y, si no está, guardar | En Java no es atómico: si dos altas simultáneas empatan, gana el índice único de la base y la otra recibe un `409` para que reintente |
| Transacciones | no comparado | `@Transactional` deshace todo si hay excepción | Al detectar un refresh reutilizado hay que revocar y a la vez lanzar un 401: sin `noRollbackFor` la revocación se perdía. Una prueba sin transacción propia lo detecta |
| JWT | `jsonwebtoken` con `algorithms: ['HS256']` | `NimbusJwtDecoder` con `macAlgorithm(HS256)` | Mismo resultado: la lista de algoritmos es explícita y `alg: none` se rechaza (hay prueba) |
| Cabeceras, CORS, límite | `helmet`, `cors`, `express-rate-limit` | Cabeceras por defecto de Spring Security, un bean de CORS y un filtro propio con Bucket4j | Spring no trae limitador de peticiones: es un filtro propio de unas 70 líneas más Caffeine para olvidar IPs |
| Estadísticas | 4 consultas en paralelo | 1 consulta con CTE | Java devuelve menos que Node (ver abajo) |
| Arranque del proyecto | `npm install` | JDK 21, Docker para las pruebas, y verificar versiones | Spring Boot 4 trae Jackson 3 y paquetes de prueba movidos; el id `.RELEASE` que devuelve Initializr no existe en Maven Central (la versión es `4.1.1`) |

### Diferencias de modelo y de funcionalidad con la versión Node

Esta API **no es una copia exacta** de la de Node. Las empresas, que son por usuario y tienen sus endpoints, sí se igualaron. El resto del diseño de Java partió de un esquema más pequeño y hay diferencias que conviene conocer:

- **Identificadores:** UUID en Node, `BIGINT` autogenerado en Java.
- **JSON:** los campos van en `snake_case` en Node (`sitio_web`, `url_oferta`) y en `camelCase` en Java (`sitioWeb`, `urlOferta`).
- **Contraseña mínima:** 12 caracteres en Node, 8 en Java.
- **Listado de postulaciones:** Node filtra por `estado`, `empresa_id`, `desde` y `hasta` y pagina con `limite` y `desplazamiento`. Java filtra por `estado` y `empresa` (parte del nombre), pagina de 20 en 20 con `pagina` y ordena con `orden`.
- **Fechas:** `postulado_en` es una fecha (`AAAA-MM-DD`) en Node y un instante con hora en Java.
- **`PATCH`:** en Node un campo se puede poner a `null` para vaciarlo; en Java no.
- **Tipos de evento:** Node usa `guardada, postulada, respuesta, entrevista, prueba, oferta, rechazo, retirada`. Java usa `postulada, respuesta, entrevista, oferta, rechazo, seguimiento, cerrada`.
- **Columnas que Java no tiene:** `usuarios.nombre`, `postulaciones.modalidad`, `ubicacion` y `notas`.
- **Estadísticas:** Node además devuelve estado por postulación, postulaciones por mes, mediana de días, rechazos en el embudo y tasas de entrevista y oferta. Java devuelve el embudo (postuladas, respondieron, entrevista, oferta) y la media, con los tres matices descritos arriba.

## Problemas reales que aparecieron y cómo se resolvieron

- **Una revocación que se perdía:** al detectar un refresh token reutilizado se revocan los del usuario y luego se lanza un 401, pero una excepción deshace la transacción y la revocación se perdía justo en ese caso. Se arregló con `noRollbackFor`. Para comprobar que la prueba lo detecta quité esa línea: la prueba se puso en rojo (devolvía `200` en lugar de `401`).
- **Esquema y entidad que no coincidían:** Hibernate, con `ddl-auto=validate`, se negó a arrancar porque `hash_token` era `CHAR(64)` en la base y la entidad esperaba `varchar`. Se corrigió la entidad, no la migración.
- **Un permiso que solo falla en Linux:** `mvnw` quedó guardado en git sin permiso de ejecución. En Windows no se nota, pero habría roto la CI y el despliegue en Render. Se corrigió en el índice de git, y la CI construye ahora también la imagen Docker.
- **Pruebas que no probaban nada:** quitando a propósito el filtro de usuario de varias consultas se vio qué pruebas lo detectaban (ver el experimento arriba). En el de las empresas, además, el listado sin filtro devolvía las empresas de todos los usuarios.

## Trabajo futuro

- Igualar con Node lo que aún difiere: tipos de evento, columnas (`nombre`, `modalidad`, `ubicacion`, `notas`), filtros por fecha y `PATCH` que pueda vaciar campos.
- Mediana de días de respuesta, tasas del embudo y desglose por mes.
- Los contadores del límite de peticiones están en memoria: con más de una instancia cada una contaría por su cuenta (haría falta Redis o la base de datos).
- Los refresh tokens caducados o revocados no se borran de la tabla.
- Sin verificación de email ni recuperación de contraseña.
- Sin pruebas de carga ni medición de tiempos del login.

## Licencia

MIT
