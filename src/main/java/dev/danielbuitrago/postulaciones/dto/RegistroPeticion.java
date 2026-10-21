package dev.danielbuitrago.postulaciones.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// El máximo de la contraseña evita que alguien mande un texto enorme para gastar CPU en Argon2.
public record RegistroPeticion(
		@Schema(example = "ana@example.com") @NotBlank @Email @Size(max = 255) String email,
		@Schema(example = "una-clave-larga-123") @NotBlank @Size(min = 8, max = 128) String password) {
}
