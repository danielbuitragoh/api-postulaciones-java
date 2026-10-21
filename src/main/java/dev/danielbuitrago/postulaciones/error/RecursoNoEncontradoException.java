package dev.danielbuitrago.postulaciones.error;

// Sirve igual para "no existe" y "es de otro usuario": el cliente no debe poder distinguirlos.
public class RecursoNoEncontradoException extends RuntimeException {

	public RecursoNoEncontradoException() {
		super("Recurso no encontrado");
	}

}
