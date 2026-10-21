package dev.danielbuitrago.postulaciones.error;

import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

// Todas las respuestas de error son ProblemDetail (RFC 9457). Nada de trazas ni de
// mensajes internos hacia fuera: lo inesperado se registra en el log y se responde genérico.
@RestControllerAdvice
public class GestorDeErrores extends ResponseEntityExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GestorDeErrores.class);

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		Map<String, String> errores = new LinkedHashMap<>();
		for (FieldError error : ex.getBindingResult().getFieldErrors()) {
			errores.putIfAbsent(error.getField(), error.getDefaultMessage());
		}
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
				"La petición no cumple la validación");
		problema.setTitle("Datos no válidos");
		problema.setProperty("errores", errores);
		return ResponseEntity.badRequest().body(problema);
	}

	// Los mensajes por defecto de Spring incluyen nombres de clases y de paquetes; se sustituyen.
	@Override
	protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException ex, HttpHeaders headers,
			HttpStatusCode status, WebRequest request) {
		return ResponseEntity.badRequest().body(problema(HttpStatus.BAD_REQUEST, "Datos no válidos",
				"Un parámetro de la petición tiene un valor no válido"));
	}

	@Override
	protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		return ResponseEntity.badRequest().body(problema(HttpStatus.BAD_REQUEST, "Datos no válidos",
				"El cuerpo de la petición no es un JSON válido o contiene valores no permitidos"));
	}

	@ExceptionHandler(RecursoNoEncontradoException.class)
	ProblemDetail noEncontrado(RecursoNoEncontradoException ex) {
		return problema(HttpStatus.NOT_FOUND, "No encontrado", ex.getMessage());
	}

	@ExceptionHandler(PeticionInvalidaException.class)
	ProblemDetail peticionInvalida(PeticionInvalidaException ex) {
		return problema(HttpStatus.BAD_REQUEST, "Datos no válidos", ex.getMessage());
	}

	@ExceptionHandler(CredencialesInvalidasException.class)
	ProblemDetail credencialesInvalidas(CredencialesInvalidasException ex) {
		return problema(HttpStatus.UNAUTHORIZED, "No autorizado", ex.getMessage());
	}

	@ExceptionHandler(RefreshTokenInvalidoException.class)
	ProblemDetail refreshInvalido(RefreshTokenInvalidoException ex) {
		return problema(HttpStatus.UNAUTHORIZED, "No autorizado", ex.getMessage());
	}

	@ExceptionHandler(ConflictoException.class)
	ProblemDetail conflicto(ConflictoException ex) {
		return problema(HttpStatus.CONFLICT, "Conflicto", ex.getMessage());
	}

	@ExceptionHandler(EmailYaRegistradoException.class)
	ProblemDetail emailYaRegistrado(EmailYaRegistradoException ex) {
		return problema(HttpStatus.CONFLICT, "Conflicto", ex.getMessage());
	}

	@ExceptionHandler(Exception.class)
	ProblemDetail inesperado(Exception ex) {
		log.error("Error inesperado", ex);
		return problema(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno", "Ha ocurrido un error inesperado");
	}

	private static ProblemDetail problema(HttpStatus estado, String titulo, String detalle) {
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(estado, detalle);
		problema.setTitle(titulo);
		return problema;
	}

}
