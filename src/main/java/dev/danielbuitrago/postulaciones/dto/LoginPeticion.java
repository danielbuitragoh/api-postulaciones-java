package dev.danielbuitrago.postulaciones.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginPeticion(
		@NotBlank @Size(max = 255) String email,
		@NotBlank @Size(max = 128) String password) {
}
