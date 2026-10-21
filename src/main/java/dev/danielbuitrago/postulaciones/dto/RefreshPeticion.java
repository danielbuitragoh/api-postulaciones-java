package dev.danielbuitrago.postulaciones.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RefreshPeticion(@NotBlank @Size(max = 200) String refreshToken) {
}
