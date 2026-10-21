package dev.danielbuitrago.postulaciones;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

// Límites bajos para poder agotarlos: 3 logins y 5 peticiones generales por minuto y por IP.
// Cada prueba usa su propia IP, así que no se pisan entre sí.
@TestPropertySource(properties = {
		"app.rate-limit.login-por-minuto=3",
		"app.rate-limit.general-por-minuto=5" })
class RateLimitTests extends PruebaIntegracion {

	@Autowired
	MockMvc mockMvc;

	@Test
	void laCuartaPeticionDeLoginDesdeLaMismaIpDa429() throws Exception {
		String ip = "203.0.113.10";

		for (int i = 0; i < 3; i++) {
			mockMvc.perform(loginDesde(ip)).andExpect(status().isUnauthorized());
		}

		mockMvc.perform(loginDesde(ip))
				.andExpect(status().isTooManyRequests())
				.andExpect(header().exists("Retry-After"))
				.andExpect(header().string("Content-Type", "application/problem+json;charset=UTF-8"))
				.andExpect(jsonPath("$.status").value(429))
				.andExpect(jsonPath("$.title").value("Demasiadas peticiones"));
	}

	@Test
	void otraIpNoQuedaBloqueadaPorLaPrimera() throws Exception {
		agotarLogin("203.0.113.20");

		mockMvc.perform(loginDesde("203.0.113.21")).andExpect(status().isUnauthorized());
	}

	@Test
	void losIntentosDeLoginNoGastanElCupoGeneral() throws Exception {
		String ip = "203.0.113.30";
		agotarLogin(ip);

		// El login ya está bloqueado, pero el resto de la API sigue respondiendo a esa IP.
		mockMvc.perform(desde(get("/postulaciones"), ip)).andExpect(status().isUnauthorized());
	}

	@Test
	void elRestoDeLaApiTambienTieneLimitePeroMasAlto() throws Exception {
		String ip = "203.0.113.40";

		for (int i = 0; i < 5; i++) {
			mockMvc.perform(desde(get("/postulaciones"), ip)).andExpect(status().isUnauthorized());
		}

		mockMvc.perform(desde(get("/postulaciones"), ip)).andExpect(status().isTooManyRequests());
	}

	@Test
	void saludNoSeLimita() throws Exception {
		String ip = "203.0.113.50";

		for (int i = 0; i < 20; i++) {
			mockMvc.perform(desde(get("/salud"), ip)).andExpect(status().isOk());
		}
	}

	private void agotarLogin(String ip) throws Exception {
		for (int i = 0; i < 3; i++) {
			mockMvc.perform(loginDesde(ip)).andExpect(status().isUnauthorized());
		}
		mockMvc.perform(loginDesde(ip)).andExpect(status().isTooManyRequests());
	}

	private static MockHttpServletRequestBuilder loginDesde(String ip) {
		return desde(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"nadie@example.com\",\"password\":\"cualquiera-123\"}"), ip);
	}

	private static MockHttpServletRequestBuilder desde(MockHttpServletRequestBuilder peticion, String ip) {
		return peticion.with(request -> {
			request.setRemoteAddr(ip);
			return request;
		});
	}

}
