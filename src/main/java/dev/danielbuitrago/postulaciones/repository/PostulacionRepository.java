package dev.danielbuitrago.postulaciones.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import dev.danielbuitrago.postulaciones.domain.Postulacion;
import dev.danielbuitrago.postulaciones.dto.PostulacionRespuesta;

// Regla de autorización de este repositorio: TODA consulta lleva "usuarioId" en el WHERE.
// Un recurso ajeno simplemente no aparece en el resultado, y el servicio responde 404.
// Nunca se trae el dato primero para comparar el dueño después en un if.
public interface PostulacionRepository extends JpaRepository<Postulacion, Long> {

	String RESUMEN = """
			select new dev.danielbuitrago.postulaciones.dto.PostulacionRespuesta(
			    p.id, e.nombre, p.puesto, p.fuente, p.urlOferta, p.salarioMin, p.salarioMax, p.moneda,
			    p.postuladoEn, ea.estado, ea.desde)
			from Postulacion p
			join Empresa e on e.id = p.empresaId
			left join EstadoActual ea on ea.postulacionId = p.id
			""";

	String FILTROS = """
			where p.usuarioId = :usuarioId
			  and (cast(:estado as string) is null or ea.estado = :estado)
			  and (cast(:empresa as string) is null
			       or lower(e.nombre) like lower(concat('%', cast(:empresa as string), '%')))
			""";

	@Query(value = RESUMEN + FILTROS, countQuery = """
			select count(p)
			from Postulacion p
			join Empresa e on e.id = p.empresaId
			left join EstadoActual ea on ea.postulacionId = p.id
			""" + FILTROS)
	Page<PostulacionRespuesta> listar(@Param("usuarioId") Long usuarioId, @Param("estado") String estado,
			@Param("empresa") String empresa, Pageable pageable);

	@Query(RESUMEN + "where p.id = :id and p.usuarioId = :usuarioId")
	Optional<PostulacionRespuesta> buscarResumen(@Param("id") Long id, @Param("usuarioId") Long usuarioId);

	Optional<Postulacion> findByIdAndUsuarioId(Long id, Long usuarioId);

	// Los eventos se borran solos por el ON DELETE CASCADE de la base.
	@Modifying
	@Query("delete from Postulacion p where p.id = :id and p.usuarioId = :usuarioId")
	int borrar(@Param("id") Long id, @Param("usuarioId") Long usuarioId);

}
