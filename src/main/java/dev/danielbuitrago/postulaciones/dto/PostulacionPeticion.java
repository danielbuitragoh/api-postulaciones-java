package dev.danielbuitrago.postulaciones.dto;

import java.time.Instant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

// "empresa" es el nombre: si ya existe una empresa con ese nombre se reutiliza (y entonces
// sitioWeb y sector se ignoran, porque las empresas son compartidas); si no, se crea.
public record PostulacionPeticion(
		@NotBlank @Size(max = 200) String empresa,
		@Size(max = 500) @Pattern(regexp = "^https?://.+", message = "debe empezar por http:// o https://") String sitioWeb,
		@Size(max = 100) String sector,
		@NotBlank @Size(max = 200) String puesto,
		@Size(max = 100) String fuente,
		@Size(max = 1000) @Pattern(regexp = "^https?://.+", message = "debe empezar por http:// o https://") String urlOferta,
		@PositiveOrZero Integer salarioMin,
		@PositiveOrZero Integer salarioMax,
		@Pattern(regexp = "^[A-Z]{3}$", message = "debe ser un código de 3 letras mayúsculas, como EUR") String moneda,
		Instant postuladoEn) {
}
