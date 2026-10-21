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
import jakarta.validation.Valid;

@RestController
@RequestMapping("/auth")
public class AuthController {

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	@PostMapping("/registro")
	@ResponseStatus(HttpStatus.CREATED)
	public UsuarioRespuesta registro(@Valid @RequestBody RegistroPeticion peticion) {
		return UsuarioRespuesta.de(authService.registrar(peticion.email(), peticion.password()));
	}

	@PostMapping("/login")
	public TokensRespuesta login(@Valid @RequestBody LoginPeticion peticion) {
		return authService.login(peticion.email(), peticion.password());
	}

	@PostMapping("/refresh")
	public TokensRespuesta refresh(@Valid @RequestBody RefreshPeticion peticion) {
		return authService.refrescar(peticion.refreshToken());
	}

}
