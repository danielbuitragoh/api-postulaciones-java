package dev.danielbuitrago.postulaciones.dto;

public record EstadisticasRespuesta(Embudo embudo, TiempoDeRespuesta tiempoDeRespuesta) {

	// Cada cifra cuenta postulaciones en las que el paso ocurrió alguna vez,
	// da igual cuál sea su estado actual.
	public record Embudo(long postuladas, long respondieron, long entrevista, long oferta) {
	}

	// mediaDias es la media de días hasta la PRIMERA respuesta, calculada solo con las que
	// respondieron. Las que nunca contestaron no cuentan como 0 días: van aparte en sinRespuesta.
	// Es null cuando todavía no ha respondido ninguna.
	public record TiempoDeRespuesta(long conRespuesta, long sinRespuesta, Double mediaDias) {
	}

}
