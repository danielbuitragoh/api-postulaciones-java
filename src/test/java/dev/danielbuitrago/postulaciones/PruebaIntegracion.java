package dev.danielbuitrago.postulaciones;

import java.util.Base64;
import java.util.UUID;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

// Base común: contexto completo contra PostgreSQL real (Testcontainers). Todas las clases
// de prueba comparten el mismo contexto, así que el contenedor se arranca una sola vez.
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
abstract class PruebaIntegracion {

	// Secreto aleatorio por ejecución: no hay ninguna clave escrita en el repositorio.
	static final String SECRETO_JWT = Base64.getEncoder()
			.encodeToString((UUID.randomUUID() + UUID.randomUUID().toString()).getBytes());

	@DynamicPropertySource
	static void propiedades(DynamicPropertyRegistry registro) {
		registro.add("app.jwt.secret", () -> SECRETO_JWT);
	}

}
