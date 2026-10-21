-- Estado actual = tipo del evento más reciente de cada postulación.
-- El desempate por id evita resultados no deterministas si dos eventos comparten fecha.
CREATE VIEW estado_actual AS
SELECT DISTINCT ON (postulacion_id)
       postulacion_id,
       tipo        AS estado,
       ocurrido_en AS desde
FROM eventos
ORDER BY postulacion_id, ocurrido_en DESC, id DESC;
