package dev.danielbuitrago.postulaciones.dto;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// La empresa se indica con empresaId (una de las tuyas) o con empresa (se crea si no existe
// entre las tuyas, o se reutiliza si ya existe). Exactamente una de las dos.
public record PostulacionPeticion(
		@Schema(description = "Id de una de tus empresas. Alternativa a 'empresa'") Long empresaId,
		@Schema(description = "Empresa por nombre: se reutiliza la tuya con ese nombre o se crea. Alternativa a 'empresaId'")
		@Valid EmpresaPeticion empresa,
		@Schema(example = "Desarrollador Java Junior") @NotBlank @Size(max = 200) String puesto,
		@Schema(example = "LinkedIn") @Size(max = 100)
		@Pattern(regexp = "(?s).*\\S.*", message = "no puede estar en blanco") String fuente,
		@Schema(example = "https://ejemplo.com/ofertas/123") @Size(max = 1000) @Pattern(regexp = "^https?://.+", message = "debe empezar por http:// o https://") String urlOferta,
		@Schema(example = "24000", description = "Bruto anual") @PositiveOrZero Integer salarioMin,
		@Schema(example = "30000") @PositiveOrZero Integer salarioMax,
		@Schema(example = "EUR") @Pattern(regexp = "^[A-Z]{3}$", message = "debe ser un código de 3 letras mayúsculas, como EUR") String moneda,
		@Schema(example = "2026-10-20T09:00:00Z", description = "Si no se envía, el momento actual. No puede ser futura")
		@PastOrPresent Instant postuladoEn) {
}
