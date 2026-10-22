package dev.danielbuitrago.postulaciones.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import dev.danielbuitrago.postulaciones.dto.LoginPeticion;
import dev.danielbuitrago.postulaciones.dto.RefreshPeticion;
import dev.danielbuitrago.postulaciones.dto.RegistroPeticion;
import dev.danielbuitrago.postulaciones.dto.TokensRespuesta;
import dev.danielbuitrago.postulaciones.dto.UsuarioRespuesta;
import dev.danielbuitrago.postulaciones.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/auth")
@Tag(name = "Autenticación", description = "Cuenta y sesión. Estas rutas no necesitan token.")
@SecurityRequirements
public class AuthController {

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	@PostMapping("/registro")
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Crear una cuenta",
			description = "La contraseña (8 a 128 caracteres) se guarda con Argon2id. El email no distingue mayúsculas.")
	@ApiResponse(responseCode = "201", description = "Cuenta creada")
	@ApiResponse(responseCode = "400", description = "Email o contraseña no válidos")
	@ApiResponse(responseCode = "409", description = "Ya existe una cuenta con ese email")
	public UsuarioRespuesta registro(@Valid @RequestBody RegistroPeticion peticion) {
		return UsuarioRespuesta.de(authService.registrar(peticion.email(), peticion.password()));
	}

	@PostMapping("/login")
	@Operation(summary = "Iniciar sesión",
			description = """
					Devuelve un token de acceso (JWT, 15 minutos) y un refresh token (7 días). \
					Un usuario inexistente y una contraseña errónea dan exactamente la misma respuesta. \
					Límite estricto: 5 intentos por minuto y por IP.""")
	@ApiResponse(responseCode = "200", description = "Sesión iniciada")
	@ApiResponse(responseCode = "401", description = "Credenciales inválidas")
	@ApiResponse(responseCode = "429", description = "Demasiados intentos; mira la cabecera Retry-After")
	public TokensRespuesta login(@Valid @RequestBody LoginPeticion peticion) {
		return authService.login(peticion.email(), peticion.password());
	}

	@PostMapping("/logout")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Cerrar sesión",
			description = """
					Revoca el refresh token indicado. Responde 204 aunque el token no exista o ya esté revocado. \
					Solo cierra esa sesión; el token de acceso ya emitido sigue valiendo hasta que caduque (15 minutos).""")
	@ApiResponse(responseCode = "204", description = "Sesión cerrada")
	public void logout(@Valid @RequestBody RefreshPeticion peticion) {
		authService.cerrarSesion(peticion.refreshToken());
	}

	@PostMapping("/refresh")
	@Operation(summary = "Renovar la sesión",
			description = """
					Cambia un refresh token por un par nuevo; el anterior deja de valer (rotación). \
					Si se presenta un refresh que ya se usó, se revocan todos los de ese usuario.""")
	@ApiResponse(responseCode = "200", description = "Par de tokens nuevo")
	@ApiResponse(responseCode = "401", description = "Refresh token desconocido, caducado o ya usado")
	public TokensRespuesta refresh(@Valid @RequestBody RefreshPeticion peticion) {
		return authService.refrescar(peticion.refreshToken());
	}

}
