package dev.danielbuitrago.postulaciones.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.danielbuitrago.postulaciones.dto.EstadisticasRespuesta;
import dev.danielbuitrago.postulaciones.service.EstadisticasService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/estadisticas")
@Tag(name = "Estadísticas")
public class EstadisticasController {

	private final EstadisticasService servicio;

	public EstadisticasController(EstadisticasService servicio) {
		this.servicio = servicio;
	}

	@GetMapping
	@Operation(summary = "Estadísticas de mi búsqueda",
			description = """
					**Tiempo de respuesta:** media de días desde que te postulas hasta la PRIMERA respuesta \
					de la empresa (un evento `respuesta`, `entrevista`, `oferta` o `rechazo`), no la última. \
					Las postulaciones que nunca contestaron no cuentan como 0 días: salen aparte en `sinRespuesta`. \
					`seguimiento` y `cerrada` los pones tú y no cuentan como respuesta.

					**Embudo:** cuántas postulaciones pasaron alguna vez por cada paso, \
					sin mirar en qué estado están ahora.""")
	@ApiResponse(responseCode = "200", description = "Estadísticas del usuario autenticado")
	public EstadisticasRespuesta estadisticas(@AuthenticationPrincipal Jwt jwt) {
		return servicio.de(Long.valueOf(jwt.getSubject()));
	}

}
