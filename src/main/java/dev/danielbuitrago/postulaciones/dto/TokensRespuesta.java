package dev.danielbuitrago.postulaciones.dto;

public record TokensRespuesta(String accessToken, String refreshToken, String tokenType, long expiresIn) {
}
