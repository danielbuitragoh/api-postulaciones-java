package dev.danielbuitrago.postulaciones.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

// PATCH: un campo ausente (null) no se toca. La empresa y el dueño no se pueden cambiar.
// Un texto presente no puede estar en blanco: @Size(min = 1) dejaba pasar " " y se guardaba vacío.
// Se usa @Pattern y no @NotBlank porque @NotBlank rechazaría también el campo ausente (null).
public record PostulacionCambios(
		@Schema(example = "Desarrollador Java") @Size(max = 200)
		@Pattern(regexp = "(?s).*\\S.*", message = "no puede estar en blanco") String puesto,
		@Size(max = 100) @Pattern(regexp = "(?s).*\\S.*", message = "no puede estar en blanco") String fuente,
		@Size(max = 1000) @Pattern(regexp = "^https?://.+", message = "debe empezar por http:// o https://") String urlOferta,
		@PositiveOrZero Integer salarioMin,
		@Schema(example = "32000") @PositiveOrZero Integer salarioMax,
		@Pattern(regexp = "^[A-Z]{3}$", message = "debe ser un código de 3 letras mayúsculas, como EUR") String moneda) {
}
