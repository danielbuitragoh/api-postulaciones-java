package dev.danielbuitrago.postulaciones;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.jayway.jsonpath.JsonPath;

// Ayudas comunes para las pruebas que llaman a la API con un usuario autenticado.
// Estas pruebas no son @Transactional (ver AuthTests); cada una crea sus propios usuarios.
abstract class PruebaApi extends PruebaIntegracion {

	static final String PASSWORD = "una-clave-larga-123";

	@Autowired
	MockMvc mockMvc;

	@Autowired
	JdbcTemplate jdbc;

	record Sesion(Long usuarioId, String accessToken) {
	}

	Sesion nuevaSesion() throws Exception {
		String email = "u-" + UUID.randomUUID() + "@example.com";
		mockMvc.perform(post("/auth/registro").contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, PASSWORD)))
				.andExpect(status().isCreated());
		MvcResult login = mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, PASSWORD)))
				.andExpect(status().isOk()).andReturn();
		Long id = jdbc.queryForObject("SELECT id FROM usuarios WHERE email = ?", Long.class, email);
		return new Sesion(id, campo(login, "accessToken"));
	}

	static String empresaUnica() {
		return "Empresa " + UUID.randomUUID();
	}

	// ---------- peticiones con el token de una sesión ----------

	MockHttpServletRequestBuilder obtener(Sesion sesion, String ruta) {
		return get(ruta).header("Authorization", "Bearer " + sesion.accessToken());
	}

	MockHttpServletRequestBuilder enviar(Sesion sesion, String ruta, String json) {
		return post(ruta).header("Authorization", "Bearer " + sesion.accessToken())
				.contentType(MediaType.APPLICATION_JSON).content(json);
	}

	MockHttpServletRequestBuilder modificar(Sesion sesion, String ruta, String json) {
		return patch(ruta).header("Authorization", "Bearer " + sesion.accessToken())
				.contentType(MediaType.APPLICATION_JSON).content(json);
	}

	MockHttpServletRequestBuilder eliminar(Sesion sesion, String ruta) {
		return delete(ruta).header("Authorization", "Bearer " + sesion.accessToken());
	}

	// ---------- datos de ejemplo ----------

	long crearEmpresa(Sesion sesion, String nombre) throws Exception {
		MvcResult resultado = mockMvc.perform(enviar(sesion, "/empresas", "{\"nombre\":\"%s\"}".formatted(nombre)))
				.andExpect(status().isCreated()).andReturn();
		return ((Number) JsonPath.read(resultado.getResponse().getContentAsString(), "$.id")).longValue();
	}

	long crearPostulacionParaEmpresa(Sesion sesion, long empresaId, String puesto) throws Exception {
		return crearPostulacionJson(sesion, "{\"empresaId\":%d,\"puesto\":\"%s\"}".formatted(empresaId, puesto));
	}

	long crearPostulacion(Sesion sesion, String empresa, String puesto) throws Exception {
		return crearPostulacionJson(sesion, "{\"empresa\":{\"nombre\":\"%s\"},\"puesto\":\"%s\"}".formatted(empresa, puesto));
	}

	long crearPostulacionJson(Sesion sesion, String json) throws Exception {
		MvcResult resultado = mockMvc.perform(enviar(sesion, "/postulaciones", json))
				.andExpect(status().isCreated()).andReturn();
		return ((Number) JsonPath.read(resultado.getResponse().getContentAsString(), "$.id")).longValue();
	}

	void crearEvento(Sesion sesion, long postulacionId, String tipo, String ocurridoEn) throws Exception {
		String fecha = ocurridoEn == null ? "" : ",\"ocurridoEn\":\"%s\"".formatted(ocurridoEn);
		mockMvc.perform(enviar(sesion, "/postulaciones/" + postulacionId + "/eventos",
				"{\"tipo\":\"%s\"%s}".formatted(tipo, fecha))).andExpect(status().isCreated());
	}

	static String campo(MvcResult resultado, String nombre) throws Exception {
		return JsonPath.read(resultado.getResponse().getContentAsString(), "$." + nombre);
	}

}
