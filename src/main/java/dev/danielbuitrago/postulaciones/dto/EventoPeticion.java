package dev.danielbuitrago.postulaciones.dto;

import java.time.Instant;

import dev.danielbuitrago.postulaciones.domain.TipoEvento;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

// ocurridoEn no puede ser futura: el estado actual es el del evento con la fecha más reciente, y un
// evento fechado en el futuro se quedaría como estado aunque después llegaran otros. Tampoco puede
// ser anterior a la fecha de la postulación; eso lo comprueba el servicio, que conoce la postulación.
public record EventoPeticion(
		@Schema(example = "entrevista") @NotNull TipoEvento tipo,
		@Schema(example = "Entrevista técnica con el equipo de backend") @Size(max = 2000) String nota,
		@Schema(example = "2026-10-21T10:00:00Z",
				description = "Cuándo ocurrió. Si no se envía, el momento actual. No puede ser futura ni anterior a la fecha de la postulación")
		@PastOrPresent Instant ocurridoEn) {
}
