package dev.danielbuitrago.postulaciones.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import dev.danielbuitrago.postulaciones.domain.TipoEvento;
import dev.danielbuitrago.postulaciones.dto.EventoPeticion;
import dev.danielbuitrago.postulaciones.dto.EventoRespuesta;
import dev.danielbuitrago.postulaciones.dto.PaginaRespuesta;
import dev.danielbuitrago.postulaciones.dto.PostulacionCambios;
import dev.danielbuitrago.postulaciones.dto.PostulacionDetalle;
import dev.danielbuitrago.postulaciones.dto.PostulacionPeticion;
import dev.danielbuitrago.postulaciones.dto.PostulacionRespuesta;
import dev.danielbuitrago.postulaciones.service.PostulacionService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/postulaciones")
public class PostulacionController {

	private final PostulacionService servicio;

	public PostulacionController(PostulacionService servicio) {
		this.servicio = servicio;
	}

	@GetMapping
	public PaginaRespuesta<PostulacionRespuesta> listar(@AuthenticationPrincipal Jwt jwt,
			@RequestParam(required = false) TipoEvento estado,
			@RequestParam(required = false) String empresa,
			@RequestParam(defaultValue = "0") int pagina,
			@RequestParam(defaultValue = "reciente") String orden) {
		return servicio.listar(usuarioId(jwt), estado, empresa, pagina, orden);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public PostulacionDetalle crear(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody PostulacionPeticion peticion) {
		return servicio.crear(usuarioId(jwt), peticion);
	}

	@GetMapping("/{id}")
	public PostulacionDetalle obtener(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
		return servicio.obtener(usuarioId(jwt), id);
	}

	@PatchMapping("/{id}")
	public PostulacionDetalle actualizar(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
			@Valid @RequestBody PostulacionCambios cambios) {
		return servicio.actualizar(usuarioId(jwt), id, cambios);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void borrar(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
		servicio.borrar(usuarioId(jwt), id);
	}

	@PostMapping("/{id}/eventos")
	@ResponseStatus(HttpStatus.CREATED)
	public EventoRespuesta registrarEvento(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
			@Valid @RequestBody EventoPeticion peticion) {
		return servicio.registrarEvento(usuarioId(jwt), id, peticion);
	}

	// El usuario sale siempre del token (claim "sub"), nunca de la petición.
	private static Long usuarioId(Jwt jwt) {
		return Long.valueOf(jwt.getSubject());
	}

}
