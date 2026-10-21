package dev.danielbuitrago.postulaciones.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

// PATCH: un campo ausente (null) no se toca. La empresa y el dueño no se pueden cambiar.
public record PostulacionCambios(
		@Schema(example = "Desarrollador Java") @Size(min = 1, max = 200) String puesto,
		@Size(max = 100) String fuente,
		@Size(max = 1000) @Pattern(regexp = "^https?://.+", message = "debe empezar por http:// o https://") String urlOferta,
		@PositiveOrZero Integer salarioMin,
		@Schema(example = "32000") @PositiveOrZero Integer salarioMax,
		@Pattern(regexp = "^[A-Z]{3}$", message = "debe ser un código de 3 letras mayúsculas, como EUR") String moneda) {
}
