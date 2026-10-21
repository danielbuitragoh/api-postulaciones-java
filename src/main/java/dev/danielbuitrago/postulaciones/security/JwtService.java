package dev.danielbuitrago.postulaciones.security;

import java.time.Duration;
import java.time.Instant;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import dev.danielbuitrago.postulaciones.config.JwtConfig;
import dev.danielbuitrago.postulaciones.domain.Usuario;

@Service
public class JwtService {

	private static final Duration DURACION_ACCESO = Duration.ofMinutes(15);

	private final JwtEncoder encoder;

	public JwtService(JwtEncoder encoder) {
		this.encoder = encoder;
	}

	public String crearAccessToken(Usuario usuario) {
		Instant ahora = Instant.now();
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.issuer(JwtConfig.EMISOR)
				.subject(String.valueOf(usuario.getId()))
				.claim("rol", usuario.getRol())
				.issuedAt(ahora)
				.expiresAt(ahora.plus(DURACION_ACCESO))
				.build();
		JwsHeader cabecera = JwsHeader.with(MacAlgorithm.HS256).build();
		return encoder.encode(JwtEncoderParameters.from(cabecera, claims)).getTokenValue();
	}

	public long segundosDeVidaDelAcceso() {
		return DURACION_ACCESO.toSeconds();
	}

}
