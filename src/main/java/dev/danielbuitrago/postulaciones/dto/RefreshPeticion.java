package dev.danielbuitrago.postulaciones.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RefreshPeticion(@Schema(description = "El refreshToken que devolvió el login o el refresh anterior") @NotBlank @Size(max = 200) String refreshToken) {
}
