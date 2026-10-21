package dev.danielbuitrago.postulaciones.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import dev.danielbuitrago.postulaciones.domain.Empresa;
import dev.danielbuitrago.postulaciones.dto.EmpresaRespuesta;

// Igual que en PostulacionRepository: TODA consulta lleva "usuarioId" en el WHERE.
public interface EmpresaRepository extends JpaRepository<Empresa, Long> {

	Optional<Empresa> findByIdAndUsuarioId(Long id, Long usuarioId);

	Optional<Empresa> findByUsuarioIdAndNombreIgnoreCase(Long usuarioId, String nombre);

	@Query("""
			select new dev.danielbuitrago.postulaciones.dto.EmpresaRespuesta(
			    e.id, e.nombre, e.sitioWeb, e.sector, e.creadoEn, count(p))
			from Empresa e
			left join Postulacion p on p.empresaId = e.id
			where e.usuarioId = :usuarioId
			group by e.id, e.nombre, e.sitioWeb, e.sector, e.creadoEn
			order by lower(e.nombre)
			""")
	List<EmpresaRespuesta> listar(@Param("usuarioId") Long usuarioId);

	// Si la empresa tiene postulaciones, la clave foránea de la base impide el borrado.
	@Modifying
	@Query("delete from Empresa e where e.id = :id and e.usuarioId = :usuarioId")
	int borrar(@Param("id") Long id, @Param("usuarioId") Long usuarioId);

}
