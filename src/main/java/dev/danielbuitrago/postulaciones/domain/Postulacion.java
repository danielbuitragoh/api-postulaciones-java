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
@Table(name = "postulaciones")
public class Postulacion {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "usuario_id")
	private Long usuarioId;

	@Column(name = "empresa_id")
	private Long empresaId;

	private String puesto;

	private String fuente;

	@Column(name = "url_oferta")
	private String urlOferta;

	@Column(name = "salario_min")
	private Integer salarioMin;

	@Column(name = "salario_max")
	private Integer salarioMax;

	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(length = 3)
	private String moneda;

	@Column(name = "postulado_en")
	private Instant postuladoEn;

	@Column(name = "creado_en")
	private Instant creadoEn;

	protected Postulacion() {
		// Constructor sin argumentos que exige JPA.
	}

	public Postulacion(Long usuarioId, Long empresaId, String puesto, String fuente, String urlOferta,
			Integer salarioMin, Integer salarioMax, String moneda, Instant postuladoEn) {
		this.usuarioId = usuarioId;
		this.empresaId = empresaId;
		this.puesto = puesto;
		this.fuente = fuente;
		this.urlOferta = urlOferta;
		this.salarioMin = salarioMin;
		this.salarioMax = salarioMax;
		this.moneda = moneda;
		this.postuladoEn = postuladoEn;
		this.creadoEn = Instant.now();
	}

	public void actualizar(String puesto, String fuente, String urlOferta, Integer salarioMin, Integer salarioMax,
			String moneda) {
		this.puesto = puesto;
		this.fuente = fuente;
		this.urlOferta = urlOferta;
		this.salarioMin = salarioMin;
		this.salarioMax = salarioMax;
		this.moneda = moneda;
	}

	public Long getId() {
		return id;
	}

	public String getPuesto() {
		return puesto;
	}

	public String getFuente() {
		return fuente;
	}

	public String getUrlOferta() {
		return urlOferta;
	}

	public Integer getSalarioMin() {
		return salarioMin;
	}

	public Integer getSalarioMax() {
		return salarioMax;
	}

	public String getMoneda() {
		return moneda;
	}

	public Instant getPostuladoEn() {
		return postuladoEn;
	}

}
