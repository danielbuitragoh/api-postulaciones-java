package dev.danielbuitrago.postulaciones;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasKey;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

// La documentación es pública (sin token), para poder probar la API desde el navegador.
class DocsTests extends PruebaIntegracion {

	@Autowired
	MockMvc mockMvc;

	@Test
	void laDescripcionOpenApiEsPublicaYListaTodosLosEndpoints() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.paths", hasKey("/auth/registro")))
				.andExpect(jsonPath("$.paths", hasKey("/auth/login")))
				.andExpect(jsonPath("$.paths", hasKey("/auth/refresh")))
				.andExpect(jsonPath("$.paths", hasKey("/postulaciones")))
				.andExpect(jsonPath("$.paths", hasKey("/postulaciones/{id}")))
				.andExpect(jsonPath("$.paths", hasKey("/postulaciones/{id}/eventos")))
				.andExpect(jsonPath("$.paths", hasKey("/estadisticas")));
	}

	@Test
	void elEsquemaDeSeguridadEsUnBearerJwtYElLoginNoLoPide() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
				.andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
				.andExpect(jsonPath("$.components.securitySchemes.bearerAuth.bearerFormat").value("JWT"))
				.andExpect(jsonPath("$.security[0].bearerAuth").exists())
				.andExpect(jsonPath("$.paths['/auth/login'].post.security", empty()));
	}

	@Test
	void elUsuarioDelTokenNoApareceComoParametroDeLaPeticion() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
				.andExpect(content().string(org.hamcrest.Matchers.not(containsString("\"name\":\"jwt\""))));
	}

	@Test
	void laRaizRedirigeALaDocumentacionEnLugarDeDar401() throws Exception {
		mockMvc.perform(get("/"))
				.andExpect(status().is3xxRedirection())
				.andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl("/docs"));
	}

	@Test
	void swaggerUiSeSirveEnDocsSinToken() throws Exception {
		// /docs redirige a la página de Swagger UI, que se sirve sin autenticación.
		mockMvc.perform(get("/docs")).andExpect(status().is3xxRedirection());
		mockMvc.perform(get("/swagger-ui/index.html"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("swagger-ui")));
	}

}
