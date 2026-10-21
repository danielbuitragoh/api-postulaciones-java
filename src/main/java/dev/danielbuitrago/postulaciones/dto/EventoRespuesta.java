package dev.danielbuitrago.postulaciones.dto;

import java.time.Instant;

import dev.danielbuitrago.postulaciones.domain.Evento;
import dev.danielbuitrago.postulaciones.domain.TipoEvento;

public record EventoRespuesta(Long id, TipoEvento tipo, String nota, Instant ocurridoEn) {

	public static EventoRespuesta de(Evento evento) {
		return new EventoRespuesta(evento.getId(), evento.getTipo(), evento.getNota(), evento.getOcurridoEn());
	}

}
