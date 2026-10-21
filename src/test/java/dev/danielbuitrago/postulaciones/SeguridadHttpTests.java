package dev.danielbuitrago.postulaciones;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import dev.danielbuitrago.postulaciones.config.CorsConfig;

// CORS con lista blanca (en las pruebas solo https://app.ejemplo.com) y cabeceras de seguridad.
class SeguridadHttpTests extends PruebaIntegracion {

	@Autowired
	MockMvc mockMvc;

	@Test
	void unOrigenDeLaListaBlancaPuedeHacerLaPeticionPreviaSinToken() throws Exception {
		mockMvc.perform(options("/postulaciones")
				.header("Origin", "https://app.ejemplo.com")
				.header("Access-Control-Request-Method", "GET")
				.header("Access-Control-Request-Headers", "authorization"))
				.andExpect(status().isOk())
				.andExpect(header().string("Access-Control-Allow-Origin", "https://app.ejemplo.com"));
	}

	@Test
	void unOrigenFueraDeLaListaSeRechaza() throws Exception {
		mockMvc.perform(options("/postulaciones")
				.header("Origin", "https://sitio-malicioso.example")
				.header("Access-Control-Request-Method", "GET"))
				.andExpect(status().isForbidden())
				.andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
	}

	@Test
	void laRespuestaNuncaDiceAsteriscoComoOrigenPermitido() throws Exception {
		mockMvc.perform(get("/salud").header("Origin", "https://app.ejemplo.com"))
				.andExpect(header().string("Access-Control-Allow-Origin", "https://app.ejemplo.com"));
	}

	@Test
	void unComodinEnLaConfiguracionImpideArrancar() {
		assertThatThrownBy(() -> new CorsConfig().corsConfigurationSource("*"))
				.isInstanceOf(IllegalStateException.class);
		assertThatThrownBy(() -> new CorsConfig().corsConfigurationSource("https://a.example, https://*.example"))
				.isInstanceOf(IllegalStateException.class);
	}

	@Test
	void conLaListaVaciaNingunOrigenQuedaPermitido() {
		assertThat(new CorsConfig().corsConfigurationSource("").getCorsConfiguration(
				new org.springframework.mock.web.MockHttpServletRequest("GET", "/postulaciones"))).isNull();
	}

	@Test
	void unaPeticionSinTokenDa401ConProblemDetailYSinContarElMotivo() throws Exception {
		mockMvc.perform(get("/postulaciones"))
				.andExpect(status().isUnauthorized())
				.andExpect(header().string("WWW-Authenticate", "Bearer"))
				.andExpect(header().string("Content-Type", "application/problem+json;charset=UTF-8"))
				.andExpect(jsonPath("$.status").value(401))
				.andExpect(jsonPath("$.title").value("No autorizado"));
	}

	@Test
	void unTokenInvalidoDaLaMismaRespuestaQueNoTenerToken() throws Exception {
		String sinToken = mockMvc.perform(get("/postulaciones")).andReturn().getResponse().getContentAsString();

		String conBasura = mockMvc.perform(get("/postulaciones").header("Authorization", "Bearer esto.no.vale"))
				.andExpect(status().isUnauthorized())
				.andReturn().getResponse().getContentAsString();

		assertThat(conBasura).isEqualTo(sinToken);
	}

	@Test
	void lasRespuestasLlevanLasCabecerasDeSeguridadDeSpringSecurity() throws Exception {
		mockMvc.perform(get("/salud"))
				.andExpect(header().string("X-Content-Type-Options", "nosniff"))
				.andExpect(header().string("X-Frame-Options", "DENY"))
				.andExpect(header().exists("Cache-Control"));
	}

}
