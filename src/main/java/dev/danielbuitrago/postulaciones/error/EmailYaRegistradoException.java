package dev.danielbuitrago.postulaciones.error;

public class EmailYaRegistradoException extends RuntimeException {

	public EmailYaRegistradoException() {
		super("Ya existe una cuenta con ese email");
	}

}
