package dev.danielbuitrago.postulaciones.security;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// Limita las peticiones por IP con Bucket4j. Hay dos cubos por IP: uno estricto para el login
// (frena la fuerza bruta de contraseñas) y otro más holgado para el resto de la API.
// Corre antes que Spring Security para que un atacante gaste su cupo aunque ni siquiera
// tenga un token válido. Los contadores viven en memoria: al reiniciar la API se vacían.
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RateLimitFilter extends OncePerRequestFilter {

	// La salud (la usa la plataforma de despliegue para comprobar que la API vive) y la
	// documentación (Swagger UI carga muchos archivos) no se limitan.
	private static final List<String> SIN_LIMITE = List.of("/salud", "/docs", "/swagger-ui", "/v3/api-docs");

	private static final String CUERPO_429 = """
			{"type":"about:blank","title":"Demasiadas peticiones","status":429,\
			"detail":"Has superado el límite de peticiones. Vuelve a intentarlo más tarde."}""";

	// Una IP inactiva durante 10 minutos se olvida; el tope evita que se llene la memoria.
	private final Cache<String, Bucket> cubos = Caffeine.newBuilder()
			.expireAfterAccess(Duration.ofMinutes(10))
			.maximumSize(10_000)
			.build();

	private final int loginPorMinuto;
	private final int generalPorMinuto;
	private final String cabeceraIpReal;

	public RateLimitFilter(@Value("${app.rate-limit.login-por-minuto:5}") int loginPorMinuto,
			@Value("${app.rate-limit.general-por-minuto:100}") int generalPorMinuto,
			@Value("${app.rate-limit.cabecera-ip-real:}") String cabeceraIpReal) {
		this.loginPorMinuto = loginPorMinuto;
		this.generalPorMinuto = generalPorMinuto;
		this.cabeceraIpReal = cabeceraIpReal.trim();
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		String ruta = request.getRequestURI();
		if (SIN_LIMITE.stream().anyMatch(ruta::startsWith)) {
			chain.doFilter(request, response);
			return;
		}

		boolean esLogin = ruta.equals("/auth/login");
		int limite = esLogin ? loginPorMinuto : generalPorMinuto;
		String clave = (esLogin ? "login:" : "general:") + ipDelCliente(request);
		Bucket cubo = cubos.get(clave, k -> nuevoCubo(limite));

		ConsumptionProbe sonda = cubo.tryConsumeAndReturnRemaining(1);
		if (sonda.isConsumed()) {
			chain.doFilter(request, response);
			return;
		}

		long segundos = Math.max(1, (long) Math.ceil(sonda.getNanosToWaitForRefill() / 1_000_000_000.0));
		response.setStatus(429);
		response.setHeader("Retry-After", String.valueOf(segundos));
		response.setContentType("application/problem+json");
		response.setCharacterEncoding("UTF-8");
		response.getWriter().write(CUERPO_429);
	}

	// Por defecto, la IP de la conexión. Detrás de proxies esa IP es la del proxy y, además, puede
	// cambiar en cada petición, con lo que cada una caería en un cubo distinto y el límite no
	// serviría. Si se configura una cabecera (en Render, "CF-Connecting-IP"), se usa su valor.
	// Esa cabecera solo es de fiar si el proxy de delante la sobrescribe en cada petición, y eso
	// hay que comprobarlo con pruebas contra el despliegue real (ver README).
	private String ipDelCliente(HttpServletRequest request) {
		if (!cabeceraIpReal.isEmpty()) {
			String valor = request.getHeader(cabeceraIpReal);
			// El tope de longitud evita que una cabecera enorme llene la memoria con claves largas.
			if (valor != null && !valor.isBlank() && valor.length() <= 64) {
				return valor.trim();
			}
		}
		return request.getRemoteAddr();
	}

	private static Bucket nuevoCubo(int porMinuto) {
		return Bucket.builder()
				.addLimit(Bandwidth.builder().capacity(porMinuto).refillGreedy(porMinuto, Duration.ofMinutes(1)).build())
				.build();
	}

}
