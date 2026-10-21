package dev.danielbuitrago.postulaciones.repository;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import dev.danielbuitrago.postulaciones.domain.RefreshToken;
import jakarta.persistence.LockModeType;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

	// Bloqueo de fila: si llegan dos refresh con el mismo token a la vez, el segundo espera
	// al primero y ya lo encuentra rotado, en lugar de que los dos pasen por válido.
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select t from RefreshToken t where t.hashToken = :hash")
	Optional<RefreshToken> buscarParaRotar(@Param("hash") String hash);

	@Modifying
	@Query("update RefreshToken t set t.revocadoEn = :ahora where t.usuarioId = :usuarioId and t.revocadoEn is null")
	int revocarTodosDe(@Param("usuarioId") Long usuarioId, @Param("ahora") Instant ahora);

}
