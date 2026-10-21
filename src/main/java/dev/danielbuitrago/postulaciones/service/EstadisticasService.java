package dev.danielbuitrago.postulaciones.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.danielbuitrago.postulaciones.dto.EstadisticasRespuesta;
import dev.danielbuitrago.postulaciones.repository.EstadisticasRepository;

@Service
public class EstadisticasService {

	private final EstadisticasRepository estadisticas;

	public EstadisticasService(EstadisticasRepository estadisticas) {
		this.estadisticas = estadisticas;
	}

	@Transactional(readOnly = true)
	public EstadisticasRespuesta de(Long usuarioId) {
		return estadisticas.calcular(usuarioId);
	}

}
