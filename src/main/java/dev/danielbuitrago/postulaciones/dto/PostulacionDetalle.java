package dev.danielbuitrago.postulaciones.dto;

import java.time.Instant;
import java.util.List;

public record PostulacionDetalle(
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
		Instant estadoDesde,
		List<EventoRespuesta> eventos) {

	public static PostulacionDetalle de(PostulacionRespuesta p, List<EventoRespuesta> eventos) {
		return new PostulacionDetalle(p.id(), p.empresa(), p.puesto(), p.fuente(), p.urlOferta(), p.salarioMin(),
				p.salarioMax(), p.moneda(), p.postuladoEn(), p.estado(), p.estadoDesde(), eventos);
	}

}
