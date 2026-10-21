package dev.danielbuitrago.postulaciones.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import dev.danielbuitrago.postulaciones.domain.Empresa;

public interface EmpresaRepository extends JpaRepository<Empresa, Long> {

	Optional<Empresa> findFirstByNombreIgnoreCase(String nombre);

}
