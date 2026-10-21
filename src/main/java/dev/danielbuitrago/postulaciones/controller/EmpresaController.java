package dev.danielbuitrago.postulaciones.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import dev.danielbuitrago.postulaciones.dto.EmpresaPeticion;
import dev.danielbuitrago.postulaciones.dto.EmpresaRespuesta;
import dev.danielbuitrago.postulaciones.service.EmpresaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/empresas")
@Tag(name = "Empresas",
		description = "Cada usuario tiene las suyas: dos personas pueden seguir a la misma empresa sin compartir nada.")
public class EmpresaController {

	private final EmpresaService servicio;

	public EmpresaController(EmpresaService servicio) {
		this.servicio = servicio;
	}

	@GetMapping
	@Operation(summary = "Listar mis empresas", description = "Ordenadas por nombre, con cuántas postulaciones tienes en cada una.")
	@ApiResponse(responseCode = "200", description = "Mis empresas")
	public List<EmpresaRespuesta> listar(@AuthenticationPrincipal Jwt jwt) {
		return servicio.listar(usuarioId(jwt));
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Crear una empresa",
			description = "El nombre es único entre tus empresas, sin distinguir mayúsculas.")
	@ApiResponse(responseCode = "201", description = "Empresa creada")
	@ApiResponse(responseCode = "400", description = "Datos no válidos")
	@ApiResponse(responseCode = "409", description = "Ya tienes una empresa con ese nombre")
	public EmpresaRespuesta crear(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody EmpresaPeticion peticion) {
		return servicio.crear(usuarioId(jwt), peticion);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Borrar una empresa",
			description = "No se puede borrar una empresa con postulaciones: se perdería su historial.")
	@ApiResponse(responseCode = "204", description = "Borrada")
	@ApiResponse(responseCode = "404", description = "No existe o no es tuya")
	@ApiResponse(responseCode = "409", description = "Tiene postulaciones")
	public void borrar(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
		servicio.borrar(usuarioId(jwt), id);
	}

	// El usuario sale siempre del token (claim "sub"), nunca de la petición.
	private static Long usuarioId(Jwt jwt) {
		return Long.valueOf(jwt.getSubject());
	}

}
