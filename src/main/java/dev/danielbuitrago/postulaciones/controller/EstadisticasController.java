package dev.danielbuitrago.postulaciones.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.danielbuitrago.postulaciones.dto.EstadisticasRespuesta;
import dev.danielbuitrago.postulaciones.service.EstadisticasService;

@RestController
@RequestMapping("/estadisticas")
public class EstadisticasController {

	private final EstadisticasService servicio;

	public EstadisticasController(EstadisticasService servicio) {
		this.servicio = servicio;
	}

	@GetMapping
	public EstadisticasRespuesta estadisticas(@AuthenticationPrincipal Jwt jwt) {
		return servicio.de(Long.valueOf(jwt.getSubject()));
	}

}
