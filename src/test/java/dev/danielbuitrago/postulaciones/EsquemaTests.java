package dev.danielbuitrago.postulaciones;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class EsquemaTests extends PruebaIntegracion {

	@Autowired
	JdbcTemplate jdbc;

	@Test
	void lasMigracionesCreanLasTablasYLaVista() {
		List<String> tablas = jdbc.queryForList("""
				SELECT table_name FROM information_schema.tables
				WHERE table_schema = 'public' AND table_name <> 'flyway_schema_history'
				""", String.class);

		assertThat(tablas).containsExactlyInAnyOrder(
				"usuarios", "empresas", "postulaciones", "eventos", "refresh_tokens", "estado_actual");
	}

	@Test
	void elEstadoActualEsElEventoMasReciente() {
		long postulacionId = crearPostulacion();
		jdbc.update("INSERT INTO eventos (postulacion_id, tipo, ocurrido_en) VALUES (?, 'postulada', '2026-01-10T10:00:00Z')", postulacionId);
		jdbc.update("INSERT INTO eventos (postulacion_id, tipo, ocurrido_en) VALUES (?, 'respuesta', '2026-01-15T10:00:00Z')", postulacionId);
		jdbc.update("INSERT INTO eventos (postulacion_id, tipo, ocurrido_en) VALUES (?, 'entrevista', '2026-01-20T10:00:00Z')", postulacionId);

		String estado = jdbc.queryForObject(
				"SELECT estado FROM estado_actual WHERE postulacion_id = ?", String.class, postulacionId);

		assertThat(estado).isEqualTo("entrevista");
	}

	@Test
	void unTipoDeEventoDesconocidoSeRechaza() {
		long postulacionId = crearPostulacion();

		assertThatThrownBy(() -> jdbc.update(
				"INSERT INTO eventos (postulacion_id, tipo) VALUES (?, 'inventado')", postulacionId))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	private long crearPostulacion() {
		Long usuarioId = jdbc.queryForObject(
				"INSERT INTO usuarios (email, hash_password) VALUES ('prueba@example.com', 'hash') RETURNING id", Long.class);
		Long empresaId = jdbc.queryForObject(
				"INSERT INTO empresas (nombre) VALUES ('Empresa de prueba') RETURNING id", Long.class);
		return jdbc.queryForObject(
				"INSERT INTO postulaciones (usuario_id, empresa_id, puesto) VALUES (?, ?, 'Desarrollador Java') RETURNING id",
				Long.class, usuarioId, empresaId);
	}

}
