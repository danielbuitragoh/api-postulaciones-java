package dev.danielbuitrago.postulaciones.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "usuarios")
public class Usuario {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String email;

	@Column(name = "hash_password")
	private String hashPassword;

	private String rol;

	@Column(name = "creado_en")
	private Instant creadoEn;

	protected Usuario() {
		// Constructor sin argumentos que exige JPA.
	}

	public Usuario(String email, String hashPassword) {
		this.email = email;
		this.hashPassword = hashPassword;
		this.rol = "USUARIO";
		this.creadoEn = Instant.now();
	}

	public Long getId() {
		return id;
	}

	public String getEmail() {
		return email;
	}

	public String getHashPassword() {
		return hashPassword;
	}

	public String getRol() {
		return rol;
	}

	public Instant getCreadoEn() {
		return creadoEn;
	}

}
