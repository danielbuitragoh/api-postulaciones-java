package dev.danielbuitrago.postulaciones.config;

import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;

// Documentación en /docs (Swagger UI). Todas las rutas piden el token salvo las de /auth,
// que lo anulan con un @SecurityRequirements vacío en su controlador.
@Configuration
@OpenAPIDefinition(
		info = @Info(
				title = "API de postulaciones",
				version = "0.1.0",
				description = """
						Seguimiento de candidaturas de empleo. Cada cambio de estado es un evento con su fecha, \
						y el estado actual de una postulación es el de su evento más reciente.

						**Cómo probarla desde aquí:** 1) `POST /auth/registro`, 2) `POST /auth/login`, \
						3) copia el `accessToken` de la respuesta y pulsa **Authorize**, \
						4) crea una postulación, añade eventos y mira `/estadisticas`.

						El token de acceso dura 15 minutos; después usa `POST /auth/refresh` con el `refreshToken`."""),
		security = @SecurityRequirement(name = "bearerAuth"))
@SecurityScheme(
		name = "bearerAuth",
		type = SecuritySchemeType.HTTP,
		scheme = "bearer",
		bearerFormat = "JWT",
		description = "El `accessToken` que devuelve POST /auth/login, pegado sin la palabra Bearer.")
public class OpenApiConfig {
}
