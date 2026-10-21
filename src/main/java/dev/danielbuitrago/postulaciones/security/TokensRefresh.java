package dev.danielbuitrago.postulaciones.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

// El refresh token es una cadena aleatoria opaca (no un JWT). Al cliente se le entrega
// en claro una sola vez; en la base solo se guarda su hash SHA-256.
public final class TokensRefresh {

	private static final SecureRandom ALEATORIO = new SecureRandom();

	private TokensRefresh() {
	}

	public static String generar() {
		byte[] bytes = new byte[32];
		ALEATORIO.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	public static String hash(String tokenEnClaro) {
		try {
			MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
			return HexFormat.of().formatHex(sha256.digest(tokenEnClaro.getBytes(StandardCharsets.UTF_8)));
		}
		catch (NoSuchAlgorithmException e) {
			// SHA-256 está garantizado en toda JVM.
			throw new IllegalStateException(e);
		}
	}

}
