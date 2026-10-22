package dev.danielbuitrago.postulaciones.error;

// Hay demasiados hashes de contraseña en marcha a la vez: se responde 503 con Retry-After en lugar
// de empezar otro y arriesgar quedarse sin memoria.
public class ServicioSaturadoException extends RuntimeException {

	public ServicioSaturadoException() {
		super("El servicio está ocupado. Vuelve a intentarlo en unos segundos");
	}

}
