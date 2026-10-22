package dev.danielbuitrago.postulaciones;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import dev.danielbuitrago.postulaciones.error.ServicioSaturadoException;
import dev.danielbuitrago.postulaciones.security.LimitadorDeHashes;

// Pruebas unitarias del semáforo: no necesitan Spring ni base de datos.
class LimitadorDeHashesTests {

	@Test
	void nuncaHayMasTrabajosSimultaneosQuePermisosYTodosTerminan() throws Exception {
		LimitadorDeHashes limitador = new LimitadorDeHashes(3, 10_000);
		AtomicInteger enMarcha = new AtomicInteger();
		AtomicInteger maximo = new AtomicInteger();

		ExecutorService hilos = Executors.newFixedThreadPool(12);
		List<Future<Integer>> resultados = new ArrayList<>();
		for (int i = 0; i < 12; i++) {
			resultados.add(hilos.submit(() -> limitador.conPermiso(() -> {
				int ahora = enMarcha.incrementAndGet();
				maximo.accumulateAndGet(ahora, Math::max);
				dormir(60);
				enMarcha.decrementAndGet();
				return 1;
			})));
		}
		for (Future<Integer> resultado : resultados) {
			assertThat(resultado.get()).isEqualTo(1);
		}
		hilos.shutdown();

		assertThat(maximo.get()).isLessThanOrEqualTo(3);
	}

	@Test
	void sinHuecoNiEsperaSeRechazaYElPermisoSeDevuelveAlTerminar() {
		LimitadorDeHashes limitador = new LimitadorDeHashes(1, 0);

		// Dentro del primer trabajo el único permiso está ocupado: el segundo se rechaza.
		String resultado = limitador.conPermiso(() -> {
			assertThatThrownBy(() -> limitador.conPermiso(() -> "no deberia ejecutarse"))
					.isInstanceOf(ServicioSaturadoException.class);
			return "primero";
		});

		assertThat(resultado).isEqualTo("primero");
		assertThat(limitador.conPermiso(() -> "de nuevo")).isEqualTo("de nuevo");
	}

	@Test
	void siElTrabajoFallaElPermisoSeDevuelveIgualmente() {
		LimitadorDeHashes limitador = new LimitadorDeHashes(1, 0);

		assertThatThrownBy(() -> limitador.conPermiso(() -> {
			throw new IllegalStateException("fallo de prueba");
		})).isInstanceOf(IllegalStateException.class);

		assertThat(limitador.conPermiso(() -> "sigue funcionando")).isEqualTo("sigue funcionando");
	}

	private static void dormir(long milisegundos) {
		try {
			Thread.sleep(milisegundos);
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

}
