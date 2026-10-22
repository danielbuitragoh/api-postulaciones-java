package dev.danielbuitrago.postulaciones.domain;

import java.time.Instant;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "usuario_id")
	private Long usuarioId;

	// SHA-256 del token en hexadecimal; el token en claro no se guarda nunca.
	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(name = "hash_token", length = 64)
	private String hashToken;

	@Column(name = "expira_en")
	private Instant expiraEn;

	@Column(name = "revocado_en")
	private Instant revocadoEn;

	@Column(name = "reemplazado_por")
	private Long reemplazadoPor;

	protected RefreshToken() {
		// Constructor sin argumentos que exige JPA.
	}

	public RefreshToken(Long usuarioId, String hashToken, Instant expiraEn) {
		this.usuarioId = usuarioId;
		this.hashToken = hashToken;
		this.expiraEn = expiraEn;
	}

	public Long getId() {
		return id;
	}

	public Long getUsuarioId() {
		return usuarioId;
	}

	public Instant getExpiraEn() {
		return expiraEn;
	}

	public boolean estaRevocado() {
		return revocadoEn != null;
	}

	public boolean haCaducado(Instant ahora) {
		return !expiraEn.isAfter(ahora);
	}

	// Cierre de sesión: el token deja de valer sin que lo sustituya otro.
	public void revocar(Instant ahora) {
		this.revocadoEn = ahora;
	}

	public void rotar(Instant ahora, Long idDelSustituto) {
		this.revocadoEn = ahora;
		this.reemplazadoPor = idDelSustituto;
	}

}
