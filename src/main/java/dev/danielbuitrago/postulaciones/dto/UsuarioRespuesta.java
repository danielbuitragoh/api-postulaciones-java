package dev.danielbuitrago.postulaciones.dto;

import dev.danielbuitrago.postulaciones.domain.Usuario;

public record UsuarioRespuesta(Long id, String email) {

	public static UsuarioRespuesta de(Usuario usuario) {
		return new UsuarioRespuesta(usuario.getId(), usuario.getEmail());
	}

}
