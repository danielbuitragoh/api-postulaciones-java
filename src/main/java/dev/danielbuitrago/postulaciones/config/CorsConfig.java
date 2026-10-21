package dev.danielbuitrago.postulaciones.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

// CORS con lista blanca explícita. Los orígenes permitidos vienen de app.cors.origenes
// (variable de entorno APP_CORS_ORIGENES, separados por comas). Si está vacía, ningún otro
// origen puede llamar a la API desde un navegador. Nunca se admite "*".
@Configuration
public class CorsConfig {

	@Bean
	public CorsConfigurationSource corsConfigurationSource(@Value("${app.cors.origenes:}") String origenes) {
		List<String> permitidos = Arrays.stream(origenes.split(","))
				.map(String::trim)
				.filter(origen -> !origen.isEmpty())
				.toList();
		if (permitidos.stream().anyMatch(origen -> origen.contains("*"))) {
			throw new IllegalStateException("app.cors.origenes no admite comodines: lista cada origen de forma explícita");
		}

		UrlBasedCorsConfigurationSource fuente = new UrlBasedCorsConfigurationSource();
		if (!permitidos.isEmpty()) {
			CorsConfiguration configuracion = new CorsConfiguration();
			configuracion.setAllowedOrigins(permitidos);
			configuracion.setAllowedMethods(List.of("GET", "POST", "PATCH", "DELETE", "OPTIONS"));
			configuracion.setAllowedHeaders(List.of("Authorization", "Content-Type"));
			// El token viaja en la cabecera Authorization, no en cookies: no hacen falta credenciales.
			configuracion.setAllowCredentials(false);
			configuracion.setMaxAge(3600L);
			fuente.registerCorsConfiguration("/**", configuracion);
		}
		return fuente;
	}

}
