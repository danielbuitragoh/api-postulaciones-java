package dev.danielbuitrago.postulaciones;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.jayway.jsonpath.JsonPath;

// Cada prueba monta dos usuarios, A y B, y comprueba que B no puede tocar lo de A.
// La autorización vive en el WHERE de las consultas, así que estas pruebas van contra
// PostgreSQL real: con un mock de la base no detectarían que falta el filtro.
class AutorizacionTests extends PruebaApi {

	@Test
	void bNoPuedeVerLaPostulacionDeA() throws Exception {
		Sesion a = nuevaSesion();
		Sesion b = nuevaSesion();
		long idDeA = crearPostulacion(a, empresaUnica(), "Backend");

		mockMvc.perform(obtener(b, "/postulaciones/" + idDeA)).andExpect(status().isNotFound());
		mockMvc.perform(obtener(a, "/postulaciones/" + idDeA)).andExpect(status().isOk());
	}

	@Test
	void bNoPuedeModificarLaPostulacionDeA() throws Exception {
		Sesion a = nuevaSesion();
		Sesion b = nuevaSesion();
		long idDeA = crearPostulacion(a, empresaUnica(), "Original");

		mockMvc.perform(modificar(b, "/postulaciones/" + idDeA, "{\"puesto\":\"Secuestrado\"}"))
				.andExpect(status().isNotFound());

		mockMvc.perform(obtener(a, "/postulaciones/" + idDeA)).andExpect(jsonPath("$.puesto").value("Original"));
	}

	@Test
	void bNoPuedeBorrarLaPostulacionDeA() throws Exception {
		Sesion a = nuevaSesion();
		Sesion b = nuevaSesion();
		long idDeA = crearPostulacion(a, empresaUnica(), "Backend");

		mockMvc.perform(eliminar(b, "/postulaciones/" + idDeA)).andExpect(status().isNotFound());

		mockMvc.perform(obtener(a, "/postulaciones/" + idDeA)).andExpect(status().isOk());
	}

	@Test
	void bNoPuedeAnadirEventosAUnaPostulacionDeA() throws Exception {
		Sesion a = nuevaSesion();
		Sesion b = nuevaSesion();
		long idDeA = crearPostulacion(a, empresaUnica(), "Backend");

		mockMvc.perform(enviar(b, "/postulaciones/" + idDeA + "/eventos", "{\"tipo\":\"rechazo\"}"))
				.andExpect(status().isNotFound());

		assertThat(jdbc.queryForObject("SELECT count(*) FROM eventos WHERE postulacion_id = ?", Integer.class, idDeA))
				.isEqualTo(1); // solo el "postulada" inicial
		mockMvc.perform(obtener(a, "/postulaciones/" + idDeA)).andExpect(jsonPath("$.estado").value("postulada"));
	}

	@Test
	void cadaUsuarioSoloVeSusPropiasPostulacionesEnElListado() throws Exception {
		Sesion a = nuevaSesion();
		Sesion b = nuevaSesion();
		long deA1 = crearPostulacion(a, empresaUnica(), "A1");
		long deA2 = crearPostulacion(a, empresaUnica(), "A2");
		long deB = crearPostulacion(b, empresaUnica(), "B1");

		MvcResult listadoA = mockMvc.perform(obtener(a, "/postulaciones")).andExpect(status().isOk()).andReturn();
		MvcResult listadoB = mockMvc.perform(obtener(b, "/postulaciones")).andExpect(status().isOk()).andReturn();

		assertThat(ids(listadoA)).containsExactlyInAnyOrder(deA1, deA2);
		assertThat(ids(listadoB)).containsExactly(deB);
	}

	@Test
	void losFiltrosNoPermitenSaltarseElAislamiento() throws Exception {
		Sesion a = nuevaSesion();
		Sesion b = nuevaSesion();
		String empresaDeA = empresaUnica();
		long idDeA = crearPostulacion(a, empresaDeA, "Backend");
		crearEvento(a, idDeA, "entrevista", null);

		// B busca exactamente la empresa y el estado de la postulación de A.
		mockMvc.perform(obtener(b, "/postulaciones?empresa=" + empresaDeA.replace(" ", "%20")))
				.andExpect(jsonPath("$.totalElementos").value(0));
		mockMvc.perform(obtener(b, "/postulaciones?estado=entrevista")).andExpect(jsonPath("$.totalElementos").value(0));
	}

	@Test
	void unRecursoAjenoYUnoInexistenteSonIndistinguibles() throws Exception {
		Sesion a = nuevaSesion();
		Sesion b = nuevaSesion();
		long idDeA = crearPostulacion(a, empresaUnica(), "Backend");
		long idInexistente = idDeA + 1_000_000;

		MvcResult ajeno = mockMvc.perform(obtener(b, "/postulaciones/" + idDeA))
				.andExpect(status().isNotFound()).andReturn();
		MvcResult inexistente = mockMvc.perform(obtener(b, "/postulaciones/" + idInexistente))
				.andExpect(status().isNotFound()).andReturn();

		assertThat(campo(ajeno, "title")).isEqualTo(campo(inexistente, "title"));
		assertThat(campo(ajeno, "detail")).isEqualTo(campo(inexistente, "detail"));
	}

	@Test
	void sinTokenNingunaRutaDePostulacionesResponde() throws Exception {
		List<MockHttpServletRequestBuilder> peticiones = List.of(
				get("/postulaciones"),
				post("/postulaciones").contentType(MediaType.APPLICATION_JSON).content("{}"),
				get("/postulaciones/1"),
				patch("/postulaciones/1").contentType(MediaType.APPLICATION_JSON).content("{}"),
				delete("/postulaciones/1"),
				post("/postulaciones/1/eventos").contentType(MediaType.APPLICATION_JSON).content("{}"));

		for (MockHttpServletRequestBuilder peticion : peticiones) {
			mockMvc.perform(peticion).andExpect(status().isUnauthorized());
		}
	}

	@Test
	void unPatchNoPuedeCambiarElDuenoNiLaEmpresa() throws Exception {
		Sesion a = nuevaSesion();
		Sesion b = nuevaSesion();
		long idDeA = crearPostulacion(a, empresaUnica(), "Original");
		Long empresaAntes = jdbc.queryForObject("SELECT empresa_id FROM postulaciones WHERE id = ?", Long.class, idDeA);

		// A intenta regalarle la postulación a B y cambiarle la empresa; esos campos no existen en el PATCH.
		mockMvc.perform(modificar(a, "/postulaciones/" + idDeA,
				"{\"puesto\":\"Nuevo\",\"usuarioId\":%d,\"empresaId\":1}".formatted(b.usuarioId())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.puesto").value("Nuevo"));

		Long duenoDespues = jdbc.queryForObject("SELECT usuario_id FROM postulaciones WHERE id = ?", Long.class, idDeA);
		Long empresaDespues = jdbc.queryForObject("SELECT empresa_id FROM postulaciones WHERE id = ?", Long.class, idDeA);
		assertThat(duenoDespues).isEqualTo(a.usuarioId());
		assertThat(empresaDespues).isEqualTo(empresaAntes);
		mockMvc.perform(obtener(b, "/postulaciones/" + idDeA)).andExpect(status().isNotFound());
	}

	private static List<Long> ids(MvcResult resultado) throws Exception {
		List<Number> ids = JsonPath.read(resultado.getResponse().getContentAsString(), "$.contenido[*].id");
		return ids.stream().map(Number::longValue).toList();
	}

}
