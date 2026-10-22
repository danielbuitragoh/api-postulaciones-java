package dev.danielbuitrago.postulaciones;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.Base64;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.RestClient;

// Estas pruebas usan HTTP de verdad (servidor en un puerto aleatorio) y no MockMvc: lo que se
// comprueba depende de cómo Tomcat decodifica la ruta y de si cree o no la cabecera
// X-Forwarded-For, y MockMvc no pasa por Tomcat.
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
		"app.rate-limit.login-por-minuto=3",
		"app.rate-limit.general-por-minuto=10" })
class LimiteDeIpHttpRealTests {

	private static final String CUERPO_LOGIN = "{\"email\":\"nadie@example.com\",\"password\":\"cualquiera-123\"}";

	static final String SECRETO_JWT = Base64.getEncoder()
			.encodeToString((UUID.randomUUID() + UUID.randomUUID().toString()).getBytes());

	@DynamicPropertySource
	static void propiedades(DynamicPropertyRegistry registro) {
		registro.add("app.jwt.secret", () -> SECRETO_JWT);
	}

	@LocalServerPort
	int puerto;

	// Spring Security y Spring MVC deciden la ruta ya decodificada y normalizada, así que estas
	// variantes llegan todas al login. El limitador tiene que decidir igual: si compara la ruta
	// tal como llega, caen en el cubo general (más holgado) y se saltan el límite estricto.
	// Tres variantes gastan el cupo de 3 del login; la cuarta, con la ruta normal, ya da 429.
	@Test
	void lasVariantesDeLaRutaDeLoginGastanElMismoCupoEstrictoQueLaNormal() {
		assertThat(loginEn("/auth/%6Cogin")).isEqualTo(401);
		assertThat(loginEn("/auth/login;x=1")).isEqualTo(401);
		assertThat(loginEn("/auth/%6cogin")).isEqualTo(401);

		assertThat(loginEn("/auth/login")).isEqualTo(429);
	}

	private int loginEn(String ruta) {
		return RestClient.create().post()
				.uri(URI.create("http://localhost:" + puerto + ruta))
				.contentType(MediaType.APPLICATION_JSON)
				.body(CUERPO_LOGIN)
				.exchange((peticion, respuesta) -> respuesta.getStatusCode().value());
	}

}
