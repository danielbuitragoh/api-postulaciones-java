package dev.danielbuitrago.postulaciones.dto;

import java.time.Instant;

import dev.danielbuitrago.postulaciones.domain.TipoEvento;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record EventoPeticion(
		@NotNull TipoEvento tipo,
		@Size(max = 2000) String nota,
		Instant ocurridoEn) {
}
