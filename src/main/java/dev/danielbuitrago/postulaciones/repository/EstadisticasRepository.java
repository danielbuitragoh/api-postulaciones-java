package dev.danielbuitrago.postulaciones.repository;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import dev.danielbuitrago.postulaciones.dto.EstadisticasRespuesta;
import dev.danielbuitrago.postulaciones.dto.EstadisticasRespuesta.Embudo;
import dev.danielbuitrago.postulaciones.dto.EstadisticasRespuesta.TiempoDeRespuesta;

// Las agregaciones se escriben en SQL a mano: con JPQL serían más difíciles de leer.
// Igual que en el resto de repositorios, el usuario va en el WHERE: todo parte de "mias".
@Repository
public class EstadisticasRepository {

	// Qué cuenta como "respuesta de la empresa": un evento de tipo respuesta, entrevista, oferta
	// o rechazo. "seguimiento" y "cerrada" los pone el propio usuario y no son una respuesta.
	private static final String SQL = """
			WITH mias AS (
			    SELECT id, postulado_en
			    FROM postulaciones
			    WHERE usuario_id = :usuarioId
			),
			-- Primera respuesta de cada postulación (el MIN), no la última.
			primera_respuesta AS (
			    SELECT m.id, m.postulado_en, MIN(e.ocurrido_en) AS ocurrida_en
			    FROM mias m
			    JOIN eventos e ON e.postulacion_id = m.id
			    WHERE e.tipo IN ('respuesta', 'entrevista', 'oferta', 'rechazo')
			    GROUP BY m.id, m.postulado_en
			),
			-- Hitos del embudo: ¿ocurrió alguna vez? (bool_or), sin mirar el estado final.
			hitos AS (
			    SELECT e.postulacion_id,
			           bool_or(e.tipo = 'entrevista') AS hubo_entrevista,
			           bool_or(e.tipo = 'oferta')     AS hubo_oferta
			    FROM mias m
			    JOIN eventos e ON e.postulacion_id = m.id
			    GROUP BY e.postulacion_id
			)
			SELECT
			    (SELECT count(*) FROM mias)                                       AS postuladas,
			    (SELECT count(*) FROM primera_respuesta)                          AS respondieron,
			    (SELECT count(*) FROM hitos WHERE hubo_entrevista)                AS entrevista,
			    (SELECT count(*) FROM hitos WHERE hubo_oferta)                    AS oferta,
			    -- La media usa solo las que respondieron; las demás no entran como 0 días.
			    (SELECT avg(GREATEST(0, EXTRACT(EPOCH FROM (ocurrida_en - postulado_en))) / 86400.0)
			     FROM primera_respuesta)                                          AS media_dias
			""";

	private final JdbcClient jdbc;

	public EstadisticasRepository(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	public EstadisticasRespuesta calcular(Long usuarioId) {
		return jdbc.sql(SQL).param("usuarioId", usuarioId).query((rs, fila) -> {
			long postuladas = rs.getLong("postuladas");
			long respondieron = rs.getLong("respondieron");
			double media = rs.getDouble("media_dias");
			Double mediaDias = rs.wasNull() ? null : Math.round(media * 10) / 10.0;
			return new EstadisticasRespuesta(
					new Embudo(postuladas, respondieron, rs.getLong("entrevista"), rs.getLong("oferta")),
					new TiempoDeRespuesta(respondieron, postuladas - respondieron, mediaDias));
		}).single();
	}

}
