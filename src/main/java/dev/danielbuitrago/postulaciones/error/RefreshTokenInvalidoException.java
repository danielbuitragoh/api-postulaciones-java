package dev.danielbuitrago.postulaciones.error;

public class RefreshTokenInvalidoException extends RuntimeException {

	public RefreshTokenInvalidoException() {
		super("Refresh token no válido");
	}

}
