package dev.danielbuitrago.postulaciones;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

// Con cero permisos de hash, ninguna petición que necesite un Argon2id puede empezar: simula la
// API saturada. Debe responder 503 con Retry-After en lugar de intentarlo y quedarse sin memoria.
@TestPropertySource(properties = {
		"app.hash.concurrentes=0",
		"app.hash.espera-ms=0" })
class HashSaturadoTests extends PruebaIntegracion {

	@Autowired
	MockMvc mockMvc;

	@Test
	void conLosHashesSaturadosElRegistroDa503ConRetryAfter() throws Exception {
		mockMvc.perform(post("/auth/registro").contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"saturado@example.com\",\"password\":\"una-clave-larga-123\"}"))
				.andExpect(status().isServiceUnavailable())
				.andExpect(header().exists("Retry-After"))
				.andExpect(jsonPath("$.status").value(503));
	}

	@Test
	void conLosHashesSaturadosElLoginTambienDa503() throws Exception {
		mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"nadie@example.com\",\"password\":\"cualquiera-123\"}"))
				.andExpect(status().isServiceUnavailable())
				.andExpect(header().exists("Retry-After"));
	}

}
