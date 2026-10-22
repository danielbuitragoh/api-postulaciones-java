package dev.danielbuitrago.postulaciones.config;

import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

@Configuration
public class JwtConfig {

	public static final String EMISOR = "api-postulaciones-java";

	// HS256 pide al menos 256 bits de clave.
	private static final int LONGITUD_MINIMA_SECRETO = 32;

	@Bean
	SecretKey claveJwt(@Value("${app.jwt.secret}") String secreto) {
		byte[] bytes = secreto.getBytes(StandardCharsets.UTF_8);
		if (bytes.length < LONGITUD_MINIMA_SECRETO) {
			throw new IllegalStateException("JWT_SECRET debe tener al menos " + LONGITUD_MINIMA_SECRETO + " bytes");
		}
		return new SecretKeySpec(bytes, "HmacSHA256");
	}

	@Bean
	JwtEncoder jwtEncoder(SecretKey claveJwt) {
		return new NimbusJwtEncoder(new ImmutableSecret<>(claveJwt));
	}

	// La lista de algoritmos aceptados es explícita: solo HS256. Un token con "alg": "none"
	// o firmado con otro algoritmo se rechaza antes de mirar sus claims.
	@Bean
	JwtDecoder jwtDecoder(SecretKey claveJwt) {
		NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(claveJwt)
				.macAlgorithm(MacAlgorithm.HS256)
				.build();
		// El validador por defecto solo comprueba la caducidad si el token la trae: uno firmado con
		// la clave buena pero sin "exp" valdría para siempre. Aquí se exige.
		OAuth2TokenValidator<Jwt> exigeCaducidad = jwt -> jwt.getExpiresAt() != null
				? OAuth2TokenValidatorResult.success()
				: OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "El token no tiene caducidad", null));
		decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
				JwtValidators.createDefaultWithIssuer(EMISOR), exigeCaducidad));
		return decoder;
	}

}
