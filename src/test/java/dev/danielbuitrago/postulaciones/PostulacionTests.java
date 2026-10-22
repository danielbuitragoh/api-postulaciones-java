package dev.danielbuitrago.postulaciones;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import com.jayway.jsonpath.JsonPath;

class PostulacionTests extends PruebaApi {

	// ---------- alta y detalle ----------

	@Test
	void crearDevuelve201ConEstadoPostuladaYSuPrimerEvento() throws Exception {
		Sesion yo = nuevaSesion();
		String empresa = empresaUnica();

		mockMvc.perform(enviar(yo, "/postulaciones", """
				{"empresa":{"nombre":"%s"},"puesto":"Desarrollador Java","fuente":"LinkedIn",
				 "urlOferta":"https://ejemplo.com/oferta/1","salarioMin":24000,"salarioMax":30000,"moneda":"EUR"}
				""".formatted(empresa)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.empresa").value(empresa))
				.andExpect(jsonPath("$.puesto").value("Desarrollador Java"))
				.andExpect(jsonPath("$.moneda").value("EUR"))
				.andExpect(jsonPath("$.estado").value("postulada"))
				.andExpect(jsonPath("$.eventos.length()").value(1))
				.andExpect(jsonPath("$.eventos[0].tipo").value("postulada"));
	}

	@Test
	void dosPostulacionesALaMismaEmpresaReutilizanLaFila() throws Exception {
		Sesion yo = nuevaSesion();
		String empresa = empresaUnica();

		crearPostulacion(yo, empresa, "Backend");
		crearPostulacion(yo, empresa.toUpperCase(), "Frontend");

		Integer filas = jdbc.queryForObject("SELECT count(*) FROM empresas WHERE lower(nombre) = lower(?)",
				Integer.class, empresa);
		assertThat(filas).isEqualTo(1);
	}

	@Test
	void elDetalleIncluyeLosEventosEnOrdenCronologico() throws Exception {
		Sesion yo = nuevaSesion();
		long id = crearPostulacion(yo, empresaUnica(), "QA");
		crearEvento(yo, id, "respuesta", "2030-01-02T10:00:00Z");
		crearEvento(yo, id, "seguimiento", "2030-01-01T10:00:00Z");

		mockMvc.perform(obtener(yo, "/postulaciones/" + id))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.eventos.length()").value(3))
				.andExpect(jsonPath("$.eventos[0].tipo").value("postulada"))
				.andExpect(jsonPath("$.eventos[1].tipo").value("seguimiento"))
				.andExpect(jsonPath("$.eventos[2].tipo").value("respuesta"));
	}

	// ---------- el estado sale de los eventos ----------

	@Test
	void elEstadoActualCambiaConCadaEvento() throws Exception {
		Sesion yo = nuevaSesion();
		long id = crearPostulacion(yo, empresaUnica(), "Backend");

		crearEvento(yo, id, "respuesta", null);
		mockMvc.perform(obtener(yo, "/postulaciones/" + id)).andExpect(jsonPath("$.estado").value("respuesta"));

		crearEvento(yo, id, "entrevista", null);
		mockMvc.perform(obtener(yo, "/postulaciones/" + id)).andExpect(jsonPath("$.estado").value("entrevista"));
	}

	@Test
	void elEstadoLoDaElEventoMasRecienteEnElTiempoNoElUltimoInsertado() throws Exception {
		Sesion yo = nuevaSesion();
		long id = crearPostulacion(yo, empresaUnica(), "Backend");

		// Se registra tarde un evento que ocurrió hace años: no puede ser el estado actual.
		crearEvento(yo, id, "seguimiento", "2020-01-01T00:00:00Z");

		mockMvc.perform(obtener(yo, "/postulaciones/" + id)).andExpect(jsonPath("$.estado").value("postulada"));
	}

	// ---------- modificar y borrar ----------

	@Test
	void patchCambiaSoloLosCamposEnviados() throws Exception {
		Sesion yo = nuevaSesion();
		long id = crearPostulacionJson(yo, """
				{"empresa":{"nombre":"%s"},"puesto":"Junior","fuente":"InfoJobs","salarioMin":20000,"salarioMax":25000,"moneda":"EUR"}
				""".formatted(empresaUnica()));

		mockMvc.perform(modificar(yo, "/postulaciones/" + id, "{\"puesto\":\"Java Junior\",\"salarioMax\":28000}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.puesto").value("Java Junior"))
				.andExpect(jsonPath("$.salarioMax").value(28000))
				.andExpect(jsonPath("$.salarioMin").value(20000))
				.andExpect(jsonPath("$.fuente").value("InfoJobs"));
	}

	@Test
	void unSalarioMinimoMayorQueElMaximoDa400() throws Exception {
		Sesion yo = nuevaSesion();
		long id = crearPostulacionJson(yo, "{\"empresa\":{\"nombre\":\"%s\"},\"puesto\":\"X\",\"salarioMin\":20000,\"salarioMax\":25000}"
				.formatted(empresaUnica()));

		mockMvc.perform(modificar(yo, "/postulaciones/" + id, "{\"salarioMin\":30000}"))
				.andExpect(status().isBadRequest());
		mockMvc.perform(enviar(yo, "/postulaciones",
				"{\"empresa\":{\"nombre\":\"A\"},\"puesto\":\"X\",\"salarioMin\":9,\"salarioMax\":1}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void borrarDevuelve204EliminaSusEventosYDespuesDa404() throws Exception {
		Sesion yo = nuevaSesion();
		long id = crearPostulacion(yo, empresaUnica(), "Backend");
		crearEvento(yo, id, "respuesta", null);

		mockMvc.perform(eliminar(yo, "/postulaciones/" + id)).andExpect(status().isNoContent());

		mockMvc.perform(obtener(yo, "/postulaciones/" + id)).andExpect(status().isNotFound());
		assertThat(jdbc.queryForObject("SELECT count(*) FROM eventos WHERE postulacion_id = ?", Integer.class, id))
				.isZero();
	}

	@Test
	void unPatchConCadenasEnBlancoDa400YNoCambiaNada() throws Exception {
		Sesion yo = nuevaSesion();
		long id = crearPostulacionJson(yo,
				"{\"empresa\":{\"nombre\":\"%s\"},\"puesto\":\"Original\",\"fuente\":\"InfoJobs\"}".formatted(empresaUnica()));

		for (String cuerpo : new String[] { "{\"puesto\":\" \"}", "{\"puesto\":\"\"}", "{\"fuente\":\"   \"}" }) {
			mockMvc.perform(modificar(yo, "/postulaciones/" + id, cuerpo))
					.andExpect(status().isBadRequest());
		}

		mockMvc.perform(obtener(yo, "/postulaciones/" + id))
				.andExpect(jsonPath("$.puesto").value("Original"))
				.andExpect(jsonPath("$.fuente").value("InfoJobs"));
	}

	@Test
	void unaFuenteEnBlancoAlCrearDa400() throws Exception {
		Sesion yo = nuevaSesion();

		mockMvc.perform(enviar(yo, "/postulaciones",
				"{\"empresa\":{\"nombre\":\"A\"},\"puesto\":\"B\",\"fuente\":\"  \"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errores.fuente").exists());
	}

	// ---------- validación de entrada ----------

	@Test
	void laValidacionRechazaDatosNoValidosAntesDeTocarLaBase() throws Exception {
		Sesion yo = nuevaSesion();

		mockMvc.perform(enviar(yo, "/postulaciones", "{\"empresa\":{\"nombre\":\"\"},\"puesto\":\"\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errores['empresa.nombre']").exists())
				.andExpect(jsonPath("$.errores.puesto").exists());
		mockMvc.perform(enviar(yo, "/postulaciones",
				"{\"empresa\":{\"nombre\":\"A\"},\"puesto\":\"B\",\"urlOferta\":\"javascript:alert(1)\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errores.urlOferta").exists());
		mockMvc.perform(enviar(yo, "/postulaciones", "{\"empresa\":{\"nombre\":\"A\"},\"puesto\":\"B\",\"moneda\":\"eur\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errores.moneda").exists());
	}

	@Test
	void unTipoDeEventoDesconocidoDa400SinRevelarNombresInternos() throws Exception {
		Sesion yo = nuevaSesion();
		long id = crearPostulacion(yo, empresaUnica(), "Backend");

		MvcResult resultado = mockMvc.perform(enviar(yo, "/postulaciones/" + id + "/eventos", "{\"tipo\":\"inventado\"}"))
				.andExpect(status().isBadRequest()).andReturn();

		assertThat(resultado.getResponse().getContentAsString()).doesNotContain("dev.danielbuitrago", "TipoEvento");
	}

	// ---------- listado: filtros, paginación y orden ----------

	@Test
	void elListadoPaginaDeVeinteEnVeinte() throws Exception {
		Sesion yo = nuevaSesion();
		for (int i = 0; i < 25; i++) {
			crearPostulacion(yo, empresaUnica(), "Puesto " + i);
		}

		mockMvc.perform(obtener(yo, "/postulaciones"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.contenido.length()").value(20))
				.andExpect(jsonPath("$.totalElementos").value(25))
				.andExpect(jsonPath("$.totalPaginas").value(2))
				.andExpect(jsonPath("$.pagina").value(0));
		mockMvc.perform(obtener(yo, "/postulaciones?pagina=1"))
				.andExpect(jsonPath("$.contenido.length()").value(5))
				.andExpect(jsonPath("$.pagina").value(1));
	}

	@Test
	void sePuedeFiltrarPorEstadoYPorEmpresa() throws Exception {
		Sesion yo = nuevaSesion();
		String google = "Globex " + UUID.randomUUID();
		long conEntrevista = crearPostulacion(yo, google, "Backend");
		crearPostulacion(yo, empresaUnica(), "Frontend");
		crearEvento(yo, conEntrevista, "entrevista", null);

		mockMvc.perform(obtener(yo, "/postulaciones?estado=entrevista"))
				.andExpect(jsonPath("$.totalElementos").value(1))
				.andExpect(jsonPath("$.contenido[0].id").value(conEntrevista));
		mockMvc.perform(obtener(yo, "/postulaciones?estado=postulada"))
				.andExpect(jsonPath("$.totalElementos").value(1));
		mockMvc.perform(obtener(yo, "/postulaciones?empresa=" + google.substring(0, 10).toLowerCase()))
				.andExpect(jsonPath("$.totalElementos").value(1))
				.andExpect(jsonPath("$.contenido[0].empresa").value(google));
	}

	@Test
	void elOrdenSeEligePorFechaOPorEmpresa() throws Exception {
		Sesion yo = nuevaSesion();
		crearPostulacionJson(yo, "{\"empresa\":{\"nombre\":\"Beta\"},\"puesto\":\"B\",\"postuladoEn\":\"2030-01-02T00:00:00Z\"}");
		crearPostulacionJson(yo, "{\"empresa\":{\"nombre\":\"Alfa\"},\"puesto\":\"A\",\"postuladoEn\":\"2030-01-03T00:00:00Z\"}");
		crearPostulacionJson(yo, "{\"empresa\":{\"nombre\":\"Gamma\"},\"puesto\":\"G\",\"postuladoEn\":\"2030-01-01T00:00:00Z\"}");

		MvcResult reciente = mockMvc.perform(obtener(yo, "/postulaciones")).andReturn();
		MvcResult antigua = mockMvc.perform(obtener(yo, "/postulaciones?orden=antigua")).andReturn();
		MvcResult porEmpresa = mockMvc.perform(obtener(yo, "/postulaciones?orden=empresa")).andReturn();

		assertThat(empresas(reciente)).containsExactly("Alfa", "Beta", "Gamma");
		assertThat(empresas(antigua)).containsExactly("Gamma", "Beta", "Alfa");
		assertThat(empresas(porEmpresa)).containsExactly("Alfa", "Beta", "Gamma");
	}

	@Test
	void unOrdenOUnaPaginaNoValidosDan400() throws Exception {
		Sesion yo = nuevaSesion();

		mockMvc.perform(obtener(yo, "/postulaciones?orden=password;drop")).andExpect(status().isBadRequest());
		mockMvc.perform(obtener(yo, "/postulaciones?pagina=-1")).andExpect(status().isBadRequest());
		mockMvc.perform(obtener(yo, "/postulaciones?estado=inventado")).andExpect(status().isBadRequest());
	}

	private static List<String> empresas(MvcResult resultado) throws Exception {
		return JsonPath.read(resultado.getResponse().getContentAsString(), "$.contenido[*].empresa");
	}

}
