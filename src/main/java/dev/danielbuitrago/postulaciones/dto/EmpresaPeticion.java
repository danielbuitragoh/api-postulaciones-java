package dev.danielbuitrago.postulaciones.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record EmpresaPeticion(
		@Schema(example = "Acme Consulting") @NotBlank @Size(max = 200) String nombre,
		@Schema(example = "https://acme.example") @Size(max = 500)
		@Pattern(regexp = "^https?://.+", message = "debe empezar por http:// o https://") String sitioWeb,
		@Schema(example = "Consultoría") @Size(max = 100) String sector) {
}
