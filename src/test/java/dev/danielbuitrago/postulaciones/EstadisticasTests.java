package dev.danielbuitrago.postulaciones;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

// Cada prueba construye a mano los datos de un matiz concreto, con fechas fijas, para
// que el resultado esperado se pueda calcular de cabeza.
class EstadisticasTests extends PruebaApi {

	@Test
	void sinPostulacionesTodoEsCeroYLaMediaEsNula() throws Exception {
		Sesion yo = nuevaSesion();

		mockMvc.perform(obtener(yo, "/estadisticas"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.embudo.postuladas").value(0))
				.andExpect(jsonPath("$.embudo.respondieron").value(0))
				.andExpect(jsonPath("$.tiempoDeRespuesta.sinRespuesta").value(0))
				.andExpect(jsonPath("$.tiempoDeRespuesta.mediaDias").doesNotExist());
	}

	// Matiz 1: el tiempo de respuesta es hasta la PRIMERA respuesta, no la última.
	@Test
	void elTiempoDeRespuestaEsHastaLaPrimeraRespuestaNoLaUltima() throws Exception {
		Sesion yo = nuevaSesion();
		long id = postulacionEl(yo, "2030-01-01T00:00:00Z");
		crearEvento(yo, id, "respuesta", "2030-01-03T00:00:00Z");   // +2 días  <- la primera
		crearEvento(yo, id, "entrevista", "2030-01-11T00:00:00Z");  // +10 días
		crearEvento(yo, id, "oferta", "2030-01-21T00:00:00Z");      // +20 días

		mockMvc.perform(obtener(yo, "/estadisticas"))
				.andExpect(jsonPath("$.tiempoDeRespuesta.mediaDias").value(2.0));
	}

	// Matiz 2: las que nunca contestaron no cuentan como 0 días; se informan aparte.
	@Test
	void lasQueNuncaContestaronNoCuentanComoCeroDias() throws Exception {
		Sesion yo = nuevaSesion();
		long tardoCuatro = postulacionEl(yo, "2030-02-01T00:00:00Z");
		long tardoSeis = postulacionEl(yo, "2030-02-01T00:00:00Z");
		postulacionEl(yo, "2030-02-01T00:00:00Z"); // nunca contestó
		crearEvento(yo, tardoCuatro, "respuesta", "2030-02-05T00:00:00Z");
		crearEvento(yo, tardoSeis, "rechazo", "2030-02-07T00:00:00Z");

		// Media de las dos que respondieron: (4 + 6) / 2 = 5. Si la tercera contara como 0 saldría 3,3.
		mockMvc.perform(obtener(yo, "/estadisticas"))
				.andExpect(jsonPath("$.tiempoDeRespuesta.mediaDias").value(5.0))
				.andExpect(jsonPath("$.tiempoDeRespuesta.conRespuesta").value(2))
				.andExpect(jsonPath("$.tiempoDeRespuesta.sinRespuesta").value(1));
	}

	@Test
	void seguimientoYCerradaLosPoneElUsuarioYNoSonUnaRespuesta() throws Exception {
		Sesion yo = nuevaSesion();
		long id = postulacionEl(yo, "2030-03-01T00:00:00Z");
		crearEvento(yo, id, "seguimiento", "2030-03-05T00:00:00Z");
		crearEvento(yo, id, "cerrada", "2030-03-10T00:00:00Z");

		mockMvc.perform(obtener(yo, "/estadisticas"))
				.andExpect(jsonPath("$.tiempoDeRespuesta.conRespuesta").value(0))
				.andExpect(jsonPath("$.tiempoDeRespuesta.sinRespuesta").value(1))
				.andExpect(jsonPath("$.tiempoDeRespuesta.mediaDias").doesNotExist());
	}

	// Matiz 3: el embudo mira si el paso ocurrió alguna vez, no cuál es el estado final.
	@Test
	void elEmbudoCuentaLosPasosQueOcurrieronAunqueElEstadoFinalSeaOtro() throws Exception {
		Sesion yo = nuevaSesion();
		long entrevistaYRechazo = postulacionEl(yo, "2030-04-01T00:00:00Z");
		long conOferta = postulacionEl(yo, "2030-04-01T00:00:00Z");
		postulacionEl(yo, "2030-04-01T00:00:00Z"); // sin movimientos
		crearEvento(yo, entrevistaYRechazo, "entrevista", "2030-04-05T00:00:00Z");
		crearEvento(yo, entrevistaYRechazo, "rechazo", "2030-04-10T00:00:00Z");
		crearEvento(yo, conOferta, "oferta", "2030-04-08T00:00:00Z");

		// La primera acabó en "rechazo", pero pasó por entrevista y cuenta en ese escalón.
		mockMvc.perform(obtener(yo, "/postulaciones/" + entrevistaYRechazo))
				.andExpect(jsonPath("$.estado").value("rechazo"));
		mockMvc.perform(obtener(yo, "/estadisticas"))
				.andExpect(jsonPath("$.embudo.postuladas").value(3))
				.andExpect(jsonPath("$.embudo.respondieron").value(2))
				.andExpect(jsonPath("$.embudo.entrevista").value(1))
				.andExpect(jsonPath("$.embudo.oferta").value(1));
	}

	// Autorización: el usuario va en el WHERE también en las agregaciones.
	@Test
	void cadaUsuarioSoloVeLasEstadisticasDeSusPostulaciones() throws Exception {
		Sesion a = nuevaSesion();
		Sesion b = nuevaSesion();
		long deA = postulacionEl(a, "2030-05-01T00:00:00Z");
		crearEvento(a, deA, "entrevista", "2030-05-03T00:00:00Z");

		mockMvc.perform(obtener(a, "/estadisticas"))
				.andExpect(jsonPath("$.embudo.postuladas").value(1))
				.andExpect(jsonPath("$.embudo.entrevista").value(1));
		mockMvc.perform(obtener(b, "/estadisticas"))
				.andExpect(jsonPath("$.embudo.postuladas").value(0))
				.andExpect(jsonPath("$.embudo.entrevista").value(0))
				.andExpect(jsonPath("$.tiempoDeRespuesta.mediaDias").doesNotExist());
	}

	@Test
	void sinTokenDa401() throws Exception {
		mockMvc.perform(get("/estadisticas")).andExpect(status().isUnauthorized());
	}

	private long postulacionEl(Sesion sesion, String fecha) throws Exception {
		return crearPostulacionJson(sesion,
				"{\"empresa\":\"%s\",\"puesto\":\"X\",\"postuladoEn\":\"%s\"}".formatted(empresaUnica(), fecha));
	}

}
