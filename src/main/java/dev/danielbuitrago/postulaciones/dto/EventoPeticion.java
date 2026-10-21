package dev.danielbuitrago.postulaciones.dto;

import java.time.Instant;

import dev.danielbuitrago.postulaciones.domain.TipoEvento;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record EventoPeticion(
		@Schema(example = "entrevista") @NotNull TipoEvento tipo,
		@Schema(example = "Entrevista técnica con el equipo de backend") @Size(max = 2000) String nota,
		@Schema(example = "2026-10-27T10:00:00Z", description = "Si no se envía, el momento actual") Instant ocurridoEn) {
}
