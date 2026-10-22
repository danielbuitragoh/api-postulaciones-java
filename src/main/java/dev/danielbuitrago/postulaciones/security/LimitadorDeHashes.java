package dev.danielbuitrago.postulaciones.security;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import dev.danielbuitrago.postulaciones.error.ServicioSaturadoException;

// Limita cuántos hashes de contraseña (Argon2id) se calculan a la vez. Cada uno reserva 19 MiB, y
// midiendo la imagen Docker con 512 MB (como el plan gratuito de Render), 8 registros simultáneos
// bastaban y 12 mataban el contenedor por falta de memoria. Con pocos permisos, el resto espera un
// momento y, si no hay hueco, se responde 503: mejor rechazar una petición que tirar la API.
@Component
public class LimitadorDeHashes {

	private final Semaphore permisos;
	private final long esperaMs;

	public LimitadorDeHashes(@Value("${app.hash.concurrentes:3}") int concurrentes,
			@Value("${app.hash.espera-ms:3000}") long esperaMs) {
		this.permisos = new Semaphore(concurrentes);
		this.esperaMs = esperaMs;
	}

	public <T> T conPermiso(Supplier<T> trabajo) {
		boolean obtenido;
		try {
			obtenido = permisos.tryAcquire(esperaMs, TimeUnit.MILLISECONDS);
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new ServicioSaturadoException();
		}
		if (!obtenido) {
			throw new ServicioSaturadoException();
		}
		try {
			return trabajo.get();
		}
		finally {
			permisos.release();
		}
	}

}
