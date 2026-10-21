package dev.danielbuitrago.postulaciones.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import dev.danielbuitrago.postulaciones.domain.Evento;

public interface EventoRepository extends JpaRepository<Evento, Long> {

	// El filtro de usuario va aquí también: el recurso anidado /postulaciones/{id}/eventos
	// no hereda la protección de la postulación, hay que repetirla en su propia consulta.
	@Query("""
			select ev from Evento ev
			join Postulacion p on p.id = ev.postulacionId
			where ev.postulacionId = :postulacionId and p.usuarioId = :usuarioId
			order by ev.ocurridoEn asc, ev.id asc
			""")
	List<Evento> listarDe(@Param("postulacionId") Long postulacionId, @Param("usuarioId") Long usuarioId);

}
