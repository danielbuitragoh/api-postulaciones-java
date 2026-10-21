package dev.danielbuitrago.postulaciones.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import dev.danielbuitrago.postulaciones.domain.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

	Optional<Usuario> findByEmail(String email);

	boolean existsByEmail(String email);

}
