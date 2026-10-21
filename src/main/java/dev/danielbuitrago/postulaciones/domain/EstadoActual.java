package dev.danielbuitrago.postulaciones.domain;

import java.time.Instant;

import org.hibernate.annotations.Immutable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// Lectura de la vista estado_actual (V4): el estado no se guarda, se deduce del último evento.
@Entity
@Immutable
@Table(name = "estado_actual")
public class EstadoActual {

	@Id
	@Column(name = "postulacion_id")
	private Long postulacionId;

	private String estado;

	private Instant desde;

	protected EstadoActual() {
		// Constructor sin argumentos que exige JPA.
	}

}
