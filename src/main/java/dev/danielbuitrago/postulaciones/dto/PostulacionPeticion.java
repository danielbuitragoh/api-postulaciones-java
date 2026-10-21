package dev.danielbuitrago.postulaciones.dto;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

// "empresa" es el nombre: si ya existe una empresa con ese nombre se reutiliza (y entonces
// sitioWeb y sector se ignoran, porque las empresas son compartidas); si no, se crea.
public record PostulacionPeticion(
		@Schema(example = "Acme Consulting") @NotBlank @Size(max = 200) String empresa,
		@Size(max = 500) @Pattern(regexp = "^https?://.+", message = "debe empezar por http:// o https://") String sitioWeb,
		@Schema(example = "Consultoría") @Size(max = 100) String sector,
		@Schema(example = "Desarrollador Java Junior") @NotBlank @Size(max = 200) String puesto,
		@Schema(example = "LinkedIn") @Size(max = 100) String fuente,
		@Schema(example = "https://ejemplo.com/ofertas/123") @Size(max = 1000) @Pattern(regexp = "^https?://.+", message = "debe empezar por http:// o https://") String urlOferta,
		@Schema(example = "24000", description = "Bruto anual") @PositiveOrZero Integer salarioMin,
		@Schema(example = "30000") @PositiveOrZero Integer salarioMax,
		@Schema(example = "EUR") @Pattern(regexp = "^[A-Z]{3}$", message = "debe ser un código de 3 letras mayúsculas, como EUR") String moneda,
		@Schema(example = "2026-10-20T09:00:00Z", description = "Si no se envía, el momento actual") Instant postuladoEn) {
}
