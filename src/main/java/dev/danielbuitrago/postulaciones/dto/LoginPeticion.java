package dev.danielbuitrago.postulaciones.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginPeticion(
		@Schema(example = "ana@example.com") @NotBlank @Size(max = 255) String email,
		@Schema(example = "una-clave-larga-123") @NotBlank @Size(max = 128) String password) {
}
