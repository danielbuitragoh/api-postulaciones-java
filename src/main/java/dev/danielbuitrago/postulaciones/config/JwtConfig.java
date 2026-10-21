package dev.danielbuitrago.postulaciones.config;

import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
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
		decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(EMISOR));
		return decoder;
	}

}
