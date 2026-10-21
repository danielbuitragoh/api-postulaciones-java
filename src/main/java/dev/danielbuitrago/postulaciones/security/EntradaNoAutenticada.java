package dev.danielbuitrago.postulaciones.security;

import java.io.IOException;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// Respuesta 401 de los filtros de seguridad. Estos errores ocurren antes de llegar a los
// controladores, así que el @RestControllerAdvice no los ve: se escribe aquí el mismo
// formato ProblemDetail (RFC 9457) que usa el resto de la API. El motivo exacto (falta el
// token, está caducado, la firma no cuadra) no se cuenta: no le sirve a un cliente legítimo
// y sí a quien está probando tokens.
public class EntradaNoAutenticada implements AuthenticationEntryPoint {

	private static final String CUERPO = """
			{"type":"about:blank","title":"No autorizado","status":401,\
			"detail":"Falta el token de acceso o no es válido"}""";

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException excepcion) throws IOException {
		response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		response.setHeader("WWW-Authenticate", "Bearer");
		response.setContentType("application/problem+json");
		response.setCharacterEncoding("UTF-8");
		response.getWriter().write(CUERPO);
	}

}
