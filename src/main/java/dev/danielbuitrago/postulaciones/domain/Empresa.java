package dev.danielbuitrago.postulaciones.domain;

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

	private String nombre;

	@Column(name = "sitio_web")
	private String sitioWeb;

	private String sector;

	protected Empresa() {
		// Constructor sin argumentos que exige JPA.
	}

	public Empresa(String nombre, String sitioWeb, String sector) {
		this.nombre = nombre;
		this.sitioWeb = sitioWeb;
		this.sector = sector;
	}

	public Long getId() {
		return id;
	}

}
