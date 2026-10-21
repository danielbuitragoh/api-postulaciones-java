package dev.danielbuitrago.postulaciones.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "eventos")
public class Evento {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "postulacion_id")
	private Long postulacionId;

	@Enumerated(EnumType.STRING)
	private TipoEvento tipo;

	private String nota;

	@Column(name = "ocurrido_en")
	private Instant ocurridoEn;

	protected Evento() {
		// Constructor sin argumentos que exige JPA.
	}

	public Evento(Long postulacionId, TipoEvento tipo, String nota, Instant ocurridoEn) {
		this.postulacionId = postulacionId;
		this.tipo = tipo;
		this.nota = nota;
		this.ocurridoEn = ocurridoEn;
	}

	public Long getId() {
		return id;
	}

	public TipoEvento getTipo() {
		return tipo;
	}

	public String getNota() {
		return nota;
	}

	public Instant getOcurridoEn() {
		return ocurridoEn;
	}

}
