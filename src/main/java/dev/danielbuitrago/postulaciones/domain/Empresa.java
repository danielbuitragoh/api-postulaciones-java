package dev.danielbuitrago.postulaciones.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "empresas")
public class Empresa {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "usuario_id")
	private Long usuarioId;

	private String nombre;

	@Column(name = "sitio_web")
	private String sitioWeb;

	private String sector;

	@Column(name = "creado_en")
	private Instant creadoEn;

	protected Empresa() {
		// Constructor sin argumentos que exige JPA.
	}

	public Empresa(Long usuarioId, String nombre, String sitioWeb, String sector) {
		this.usuarioId = usuarioId;
		this.nombre = nombre;
		this.sitioWeb = sitioWeb;
		this.sector = sector;
		this.creadoEn = Instant.now();
	}

	public Long getId() {
		return id;
	}

	public String getNombre() {
		return nombre;
	}

	public String getSitioWeb() {
		return sitioWeb;
	}

	public String getSector() {
		return sector;
	}

	public Instant getCreadoEn() {
		return creadoEn;
	}

}
