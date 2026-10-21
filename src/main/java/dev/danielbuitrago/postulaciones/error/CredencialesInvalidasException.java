package dev.danielbuitrago.postulaciones.error;

// Se usa tanto para "el usuario no existe" como para "contraseña incorrecta":
// el cliente no debe poder distinguir un caso del otro.
public class CredencialesInvalidasException extends RuntimeException {

	public CredencialesInvalidasException() {
		super("Credenciales inválidas");
	}

}
