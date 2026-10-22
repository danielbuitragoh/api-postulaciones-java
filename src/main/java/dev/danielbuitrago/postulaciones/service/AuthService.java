package dev.danielbuitrago.postulaciones.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.danielbuitrago.postulaciones.domain.RefreshToken;
import dev.danielbuitrago.postulaciones.domain.Usuario;
import dev.danielbuitrago.postulaciones.dto.TokensRespuesta;
import dev.danielbuitrago.postulaciones.error.CredencialesInvalidasException;
import dev.danielbuitrago.postulaciones.error.EmailYaRegistradoException;
import dev.danielbuitrago.postulaciones.error.RefreshTokenInvalidoException;
import dev.danielbuitrago.postulaciones.repository.RefreshTokenRepository;
import dev.danielbuitrago.postulaciones.repository.UsuarioRepository;
import dev.danielbuitrago.postulaciones.security.JwtService;
import dev.danielbuitrago.postulaciones.security.LimitadorDeHashes;
import dev.danielbuitrago.postulaciones.security.TokensRefresh;

@Service
public class AuthService {

	private static final Duration DURACION_REFRESH = Duration.ofDays(7);

	private final UsuarioRepository usuarios;
	private final RefreshTokenRepository refreshTokens;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;
	private final LimitadorDeHashes limitadorDeHashes;

	// Hash de una contraseña que nadie conoce. Cuando el email no existe se compara contra
	// este hash para que el login tarde lo mismo que con un usuario real y no se pueda
	// averiguar por el tiempo de respuesta qué emails están registrados.
	private final String hashSenuelo;

	public AuthService(UsuarioRepository usuarios, RefreshTokenRepository refreshTokens,
			PasswordEncoder passwordEncoder, JwtService jwtService, LimitadorDeHashes limitadorDeHashes) {
		this.usuarios = usuarios;
		this.refreshTokens = refreshTokens;
		this.passwordEncoder = passwordEncoder;
		this.jwtService = jwtService;
		this.limitadorDeHashes = limitadorDeHashes;
		this.hashSenuelo = passwordEncoder.encode(TokensRefresh.generar());
	}

	// registrar y login ya no son @Transactional: el hash Argon2id tarda y reserva 19 MiB, y dentro
	// de una transacción tendría una conexión de la base retenida todo ese tiempo (y, con el pool
	// de 10, eso era lo único que limitaba cuántos hashes corrían a la vez). Cada operación de base
	// de datos (existsByEmail, saveAndFlush, findByEmail, save) ya se ejecuta en su propia
	// transacción, y aquí no hace falta que sean atómicas entre sí: el índice único decide los empates.
	public Usuario registrar(String email, String password) {
		String emailNormalizado = normalizar(email);
		if (usuarios.existsByEmail(emailNormalizado)) {
			throw new EmailYaRegistradoException();
		}
		String hash = limitadorDeHashes.conPermiso(() -> passwordEncoder.encode(password));
		try {
			return usuarios.saveAndFlush(new Usuario(emailNormalizado, hash));
		}
		catch (DataIntegrityViolationException e) {
			// Dos registros simultáneos con el mismo email: gana el índice único de la base.
			throw new EmailYaRegistradoException();
		}
	}

	public TokensRespuesta login(String email, String password) {
		Usuario usuario = usuarios.findByEmail(normalizar(email)).orElse(null);
		String hash = usuario != null ? usuario.getHashPassword() : hashSenuelo;
		boolean coincide = limitadorDeHashes.conPermiso(() -> passwordEncoder.matches(password, hash));
		if (usuario == null || !coincide) {
			throw new CredencialesInvalidasException();
		}
		return emitirTokens(usuario, null, Instant.now());
	}

	// noRollbackFor: al detectar la reutilización de un token se revocan todos los del usuario
	// y después se lanza la excepción; sin esto la transacción se desharía y la revocación
	// se perdería justo en el caso en que más hace falta.
	@Transactional(noRollbackFor = RefreshTokenInvalidoException.class)
	public TokensRespuesta refrescar(String refreshTokenEnClaro) {
		Instant ahora = Instant.now();
		RefreshToken actual = refreshTokens.buscarParaRotar(TokensRefresh.hash(refreshTokenEnClaro))
				.orElseThrow(RefreshTokenInvalidoException::new);

		if (actual.estaRevocado()) {
			// Llega un token que ya se rotó: alguien lo copió. No sabemos quién tiene la cadena
			// buena, así que se cierra la sesión de ese usuario en todos sitios.
			refreshTokens.revocarTodosDe(actual.getUsuarioId(), ahora);
			throw new RefreshTokenInvalidoException();
		}
		if (actual.haCaducado(ahora)) {
			throw new RefreshTokenInvalidoException();
		}

		Usuario usuario = usuarios.findById(actual.getUsuarioId())
				.orElseThrow(RefreshTokenInvalidoException::new);
		return emitirTokens(usuario, actual, ahora);
	}

	private TokensRespuesta emitirTokens(Usuario usuario, RefreshToken anterior, Instant ahora) {
		String refreshEnClaro = TokensRefresh.generar();
		RefreshToken nuevo = refreshTokens.save(
				new RefreshToken(usuario.getId(), TokensRefresh.hash(refreshEnClaro), ahora.plus(DURACION_REFRESH)));
		if (anterior != null) {
			anterior.rotar(ahora, nuevo.getId());
		}
		return new TokensRespuesta(jwtService.crearAccessToken(usuario), refreshEnClaro, "Bearer",
				jwtService.segundosDeVidaDelAcceso());
	}

	private static String normalizar(String email) {
		return email.trim().toLowerCase(Locale.ROOT);
	}

}
