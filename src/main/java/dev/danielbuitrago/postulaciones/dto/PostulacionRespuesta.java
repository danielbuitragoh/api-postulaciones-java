package dev.danielbuitrago.postulaciones.dto;

import java.time.Instant;

// Se construye directamente desde la consulta JPQL (constructor expression) del repositorio.
public record PostulacionRespuesta(
		Long id,
		String empresa,
		String puesto,
		String fuente,
		String urlOferta,
		Integer salarioMin,
		Integer salarioMax,
		String moneda,
		Instant postuladoEn,
		String estado,
		Instant estadoDesde) {
}
