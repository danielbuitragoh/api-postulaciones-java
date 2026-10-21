package dev.danielbuitrago.postulaciones;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

class EmpresaTests extends PruebaApi {

	@Test
	void crearDevuelve201ConLosDatosDeLaEmpresa() throws Exception {
		Sesion yo = nuevaSesion();
		String nombre = empresaUnica();

		mockMvc.perform(enviar(yo, "/empresas", """
				{"nombre":"%s","sitioWeb":"https://acme.example","sector":"Consultoría"}
				""".formatted(nombre)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").isNumber())
				.andExpect(jsonPath("$.nombre").value(nombre))
				.andExpect(jsonPath("$.sector").value("Consultoría"))
				.andExpect(jsonPath("$.postulaciones").value(0));
	}

	@Test
	void unNombreRepetidoDa409AunqueCambienLasMayusculas() throws Exception {
		Sesion yo = nuevaSesion();
		String nombre = empresaUnica();
		crearEmpresa(yo, nombre);

		mockMvc.perform(enviar(yo, "/empresas", "{\"nombre\":\"%s\"}".formatted(nombre.toUpperCase())))
				.andExpect(status().isConflict());
	}

	@Test
	void dosUsuariosPuedenTenerEmpresasConElMismoNombre() throws Exception {
		Sesion a = nuevaSesion();
		Sesion b = nuevaSesion();
		String nombre = empresaUnica();

		long deA = crearEmpresa(a, nombre);
		long deB = crearEmpresa(b, nombre);

		assertThat(deA).isNotEqualTo(deB);
	}

	@Test
	void elListadoVaPorNombreYCuentaLasPostulaciones() throws Exception {
		Sesion yo = nuevaSesion();
		long beta = crearEmpresa(yo, "Beta " + java.util.UUID.randomUUID());
		crearEmpresa(yo, "Alfa " + java.util.UUID.randomUUID());
		crearPostulacionParaEmpresa(yo, beta, "Backend");
		crearPostulacionParaEmpresa(yo, beta, "Frontend");

		mockMvc.perform(obtener(yo, "/empresas"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[0].nombre").value(org.hamcrest.Matchers.startsWith("Alfa")))
				.andExpect(jsonPath("$[0].postulaciones").value(0))
				.andExpect(jsonPath("$[1].nombre").value(org.hamcrest.Matchers.startsWith("Beta")))
				.andExpect(jsonPath("$[1].postulaciones").value(2));
	}

	@Test
	void unaEmpresaSinPostulacionesSePuedeBorrar() throws Exception {
		Sesion yo = nuevaSesion();
		long id = crearEmpresa(yo, empresaUnica());

		mockMvc.perform(eliminar(yo, "/empresas/" + id)).andExpect(status().isNoContent());

		mockMvc.perform(obtener(yo, "/empresas")).andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	void unaEmpresaConPostulacionesNoSePuedeBorrarYSeConserva() throws Exception {
		Sesion yo = nuevaSesion();
		long empresa = crearEmpresa(yo, empresaUnica());
		long postulacion = crearPostulacionParaEmpresa(yo, empresa, "Backend");

		mockMvc.perform(eliminar(yo, "/empresas/" + empresa)).andExpect(status().isConflict());

		mockMvc.perform(obtener(yo, "/empresas")).andExpect(jsonPath("$.length()").value(1));
		mockMvc.perform(obtener(yo, "/postulaciones/" + postulacion)).andExpect(status().isOk());

		// Cuando se borran las postulaciones, la empresa ya se puede borrar.
		mockMvc.perform(eliminar(yo, "/postulaciones/" + postulacion)).andExpect(status().isNoContent());
		mockMvc.perform(eliminar(yo, "/empresas/" + empresa)).andExpect(status().isNoContent());
	}

	@Test
	void unaEmpresaInexistenteDa404() throws Exception {
		Sesion yo = nuevaSesion();

		mockMvc.perform(eliminar(yo, "/empresas/999999999")).andExpect(status().isNotFound());
	}

	@Test
	void laValidacionRechazaNombresVaciosYWebsNoHttp() throws Exception {
		Sesion yo = nuevaSesion();

		mockMvc.perform(enviar(yo, "/empresas", "{\"nombre\":\"  \"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errores.nombre").exists());
		mockMvc.perform(enviar(yo, "/empresas", "{\"nombre\":\"X\",\"sitioWeb\":\"javascript:alert(1)\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errores.sitioWeb").exists());
	}

	// ---------- empresa al crear una postulación ----------

	@Test
	void postularPorNombreReutilizaLaEmpresaDelUsuarioEnLugarDeDuplicarla() throws Exception {
		Sesion yo = nuevaSesion();
		String nombre = empresaUnica();

		crearPostulacion(yo, nombre, "Backend");
		crearPostulacion(yo, nombre.toUpperCase(), "Frontend");

		mockMvc.perform(obtener(yo, "/empresas"))
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].postulaciones").value(2));
	}

	@Test
	void postularConEmpresaIdUsaLaEmpresaIndicada() throws Exception {
		Sesion yo = nuevaSesion();
		String nombre = empresaUnica();
		long empresa = crearEmpresa(yo, nombre);

		mockMvc.perform(enviar(yo, "/postulaciones", "{\"empresaId\":%d,\"puesto\":\"QA\"}".formatted(empresa)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.empresa").value(nombre));
	}

	@Test
	void hayQueIndicarExactamenteUnaDeEmpresaIdOEmpresa() throws Exception {
		Sesion yo = nuevaSesion();
		long empresa = crearEmpresa(yo, empresaUnica());

		mockMvc.perform(enviar(yo, "/postulaciones", "{\"puesto\":\"QA\"}"))
				.andExpect(status().isBadRequest());
		mockMvc.perform(enviar(yo, "/postulaciones",
				"{\"empresaId\":%d,\"empresa\":{\"nombre\":\"X\"},\"puesto\":\"QA\"}".formatted(empresa)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void unEmpresaIdInexistenteDa404() throws Exception {
		Sesion yo = nuevaSesion();

		mockMvc.perform(enviar(yo, "/postulaciones", "{\"empresaId\":999999999,\"puesto\":\"QA\"}"))
				.andExpect(status().isNotFound());
	}

}
