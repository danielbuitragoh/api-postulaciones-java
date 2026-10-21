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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/postulaciones")
@Tag(name = "Postulaciones",
		description = "Solo ves y tocas las tuyas: una postulación de otro usuario responde 404, igual que una que no existe.")
public class PostulacionController {

	private final PostulacionService servicio;

	public PostulacionController(PostulacionService servicio) {
		this.servicio = servicio;
	}

	@GetMapping
	@Operation(summary = "Listar mis postulaciones",
			description = "20 por página. El estado es el del evento más reciente de cada postulación.")
	@ApiResponse(responseCode = "200", description = "Página de postulaciones")
	@ApiResponse(responseCode = "400", description = "Estado, orden o página no válidos")
	public PaginaRespuesta<PostulacionRespuesta> listar(@AuthenticationPrincipal Jwt jwt,
			@Parameter(description = "Solo las que están ahora en este estado") @RequestParam(required = false) TipoEvento estado,
			@Parameter(description = "Parte del nombre de la empresa, sin distinguir mayúsculas", example = "acme")
			@RequestParam(required = false) String empresa,
			@Parameter(description = "Número de página, empezando en 0") @RequestParam(defaultValue = "0") int pagina,
			@Parameter(description = "reciente (por defecto), antigua o empresa", example = "reciente")
			@RequestParam(defaultValue = "reciente") String orden) {
		return servicio.listar(usuarioId(jwt), estado, empresa, pagina, orden);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Crear una postulación",
			description = """
					Indica la empresa con `empresaId` (una de las tuyas) o con `empresa` (por nombre: se reutiliza \
					la tuya con ese nombre o se crea), pero no con ambos. \
					Se crea también el evento `postulada` con la fecha de la postulación.""")
	@ApiResponse(responseCode = "201", description = "Postulación creada")
	@ApiResponse(responseCode = "400", description = "Datos no válidos")
	@ApiResponse(responseCode = "404", description = "El empresaId no existe o no es tuyo")
	public PostulacionDetalle crear(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody PostulacionPeticion peticion) {
		return servicio.crear(usuarioId(jwt), peticion);
	}

	@GetMapping("/{id}")
	@Operation(summary = "Ver una postulación con su historial de eventos")
	@ApiResponse(responseCode = "200", description = "Postulación con sus eventos en orden cronológico")
	@ApiResponse(responseCode = "404", description = "No existe o no es tuya")
	public PostulacionDetalle obtener(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
		return servicio.obtener(usuarioId(jwt), id);
	}

	@PatchMapping("/{id}")
	@Operation(summary = "Modificar campos de una postulación",
			description = "Solo cambian los campos que envíes. La empresa y el dueño no se pueden cambiar.")
	@ApiResponse(responseCode = "200", description = "Postulación actualizada")
	@ApiResponse(responseCode = "400", description = "Datos no válidos")
	@ApiResponse(responseCode = "404", description = "No existe o no es tuya")
	public PostulacionDetalle actualizar(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
			@Valid @RequestBody PostulacionCambios cambios) {
		return servicio.actualizar(usuarioId(jwt), id, cambios);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Borrar una postulación y todos sus eventos")
	@ApiResponse(responseCode = "204", description = "Borrada")
	@ApiResponse(responseCode = "404", description = "No existe o no es tuya")
	public void borrar(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
		servicio.borrar(usuarioId(jwt), id);
	}

	@PostMapping("/{id}/eventos")
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Registrar un evento en una postulación",
			description = """
					El estado actual pasa a ser el del evento con fecha más reciente. \
					Si no envías `ocurridoEn`, se usa el momento actual.""")
	@ApiResponse(responseCode = "201", description = "Evento registrado")
	@ApiResponse(responseCode = "400", description = "Tipo de evento no válido")
	@ApiResponse(responseCode = "404", description = "La postulación no existe o no es tuya")
	public EventoRespuesta registrarEvento(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
			@Valid @RequestBody EventoPeticion peticion) {
		return servicio.registrarEvento(usuarioId(jwt), id, peticion);
	}

	// El usuario sale siempre del token (claim "sub"), nunca de la petición.
	private static Long usuarioId(Jwt jwt) {
		return Long.valueOf(jwt.getSubject());
	}

}
