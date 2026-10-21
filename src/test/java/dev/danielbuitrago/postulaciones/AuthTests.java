package dev.danielbuitrago.postulaciones;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.jayway.jsonpath.JsonPath;
import com.nimbusds.jose.jwk.source.ImmutableSecret;

import dev.danielbuitrago.postulaciones.config.JwtConfig;
import dev.danielbuitrago.postulaciones.security.TokensRefresh;

// Estas pruebas no son @Transactional a propósito: la revocación en cadena tiene que
// confirmarse de verdad en la base aunque la petición acabe en 401, y una transacción
// de prueba lo taparía. Cada prueba usa un email nuevo, así que no se pisan entre sí.
class AuthTests extends PruebaIntegracion {

	private static final String PASSWORD = "una-clave-larga-123";

	@Autowired
	MockMvc mockMvc;

	@Autowired
	JdbcTemplate jdbc;

	@Autowired
	JwtDecoder jwtDecoder;

	@Autowired
	JwtEncoder jwtEncoder;

	// ---------- registro y login ----------

	@Test
	void registroYLoginCorrectos() throws Exception {
		String email = emailNuevo();

		mockMvc.perform(registro(email, PASSWORD))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.email").value(email))
				.andExpect(jsonPath("$.id").isNumber());

		MvcResult login = mockMvc.perform(login(email, PASSWORD))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.tokenType").value("Bearer"))
				.andExpect(jsonPath("$.expiresIn").value(900))
				.andReturn();

		Jwt acceso = jwtDecoder.decode(campo(login, "accessToken"));
		assertThat(acceso.getSubject()).isEqualTo(String.valueOf(idDe(email)));
		assertThat(acceso.<String>getClaim("rol")).isEqualTo("USUARIO");
		assertThat(acceso.getExpiresAt()).isAfter(Instant.now().plusSeconds(14 * 60));
		assertThat(acceso.getExpiresAt()).isBefore(Instant.now().plusSeconds(16 * 60));
	}

	@Test
	void registrarDosVecesElMismoEmailDa409() throws Exception {
		String email = emailNuevo();
		mockMvc.perform(registro(email, PASSWORD)).andExpect(status().isCreated());

		mockMvc.perform(registro(email.toUpperCase(), PASSWORD))
				.andExpect(status().isConflict());
	}

	@Test
	void datosInvalidosDan400ConElDetalleDeLosCampos() throws Exception {
		mockMvc.perform(registro("esto-no-es-un-email", "corta"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errores.email").exists())
				.andExpect(jsonPath("$.errores.password").exists());
	}

	@Test
	void laContrasenaSeGuardaConArgon2idYParametrosOwasp() throws Exception {
		String email = emailNuevo();
		mockMvc.perform(registro(email, PASSWORD)).andExpect(status().isCreated());

		String hash = jdbc.queryForObject("SELECT hash_password FROM usuarios WHERE email = ?", String.class, email);

		assertThat(hash).startsWith("$argon2id$").contains("m=19456,t=2,p=1").doesNotContain(PASSWORD);
	}

	@Test
	void passwordErroneaYUsuarioInexistenteDanLaMismaRespuesta() throws Exception {
		String email = emailNuevo();
		mockMvc.perform(registro(email, PASSWORD)).andExpect(status().isCreated());

		MvcResult passwordMala = mockMvc.perform(login(email, "otra-clave-distinta"))
				.andExpect(status().isUnauthorized()).andReturn();
		MvcResult sinUsuario = mockMvc.perform(login(emailNuevo(), PASSWORD))
				.andExpect(status().isUnauthorized()).andReturn();

		assertThat(sinUsuario.getResponse().getContentAsString())
				.isEqualTo(passwordMala.getResponse().getContentAsString());
	}

	// ---------- refresh token ----------

	@Test
	void refreshRotaElTokenYEnlazaConSuSustituto() throws Exception {
		MvcResult login = registrarYEntrar();
		String refresh1 = campo(login, "refreshToken");

		MvcResult rotado = mockMvc.perform(refresh(refresh1))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken").isString())
				.andReturn();
		String refresh2 = campo(rotado, "refreshToken");

		assertThat(refresh2).isNotEqualTo(refresh1);
		Long idDelNuevo = jdbc.queryForObject("SELECT id FROM refresh_tokens WHERE hash_token = ?", Long.class,
				TokensRefresh.hash(refresh2));
		Long reemplazadoPor = jdbc.queryForObject(
				"SELECT reemplazado_por FROM refresh_tokens WHERE hash_token = ?", Long.class, TokensRefresh.hash(refresh1));
		assertThat(reemplazadoPor).isEqualTo(idDelNuevo);
		assertThat(jdbc.queryForObject("SELECT revocado_en IS NOT NULL FROM refresh_tokens WHERE hash_token = ?",
				Boolean.class, TokensRefresh.hash(refresh1))).isTrue();
	}

	@Test
	void unRefreshReutilizadoRevocaLaCadenaDelUsuario() throws Exception {
		MvcResult login = registrarYEntrar();
		String refresh1 = campo(login, "refreshToken");
		String refresh2 = campo(mockMvc.perform(refresh(refresh1)).andExpect(status().isOk()).andReturn(), "refreshToken");

		// Alguien presenta de nuevo el primero, que ya se había rotado.
		mockMvc.perform(refresh(refresh1)).andExpect(status().isUnauthorized());

		// El segundo era legítimo, pero la cadena entera queda revocada.
		mockMvc.perform(refresh(refresh2)).andExpect(status().isUnauthorized());
		Long usuarioId = jdbc.queryForObject("SELECT usuario_id FROM refresh_tokens WHERE hash_token = ?", Long.class,
				TokensRefresh.hash(refresh2));
		Integer activos = jdbc.queryForObject(
				"SELECT count(*) FROM refresh_tokens WHERE usuario_id = ? AND revocado_en IS NULL", Integer.class, usuarioId);
		assertThat(activos).isZero();
	}

	@Test
	void unRefreshCaducadoSeRechaza() throws Exception {
		String refresh = campo(registrarYEntrar(), "refreshToken");
		jdbc.update("UPDATE refresh_tokens SET expira_en = now() - interval '1 minute' WHERE hash_token = ?",
				TokensRefresh.hash(refresh));

		mockMvc.perform(refresh(refresh)).andExpect(status().isUnauthorized());
	}

	@Test
	void unRefreshDesconocidoSeRechaza() throws Exception {
		mockMvc.perform(refresh(TokensRefresh.generar())).andExpect(status().isUnauthorized());
	}

	@Test
	void enLaBaseNuncaSeGuardaElRefreshTokenEnClaro() throws Exception {
		String refresh = campo(registrarYEntrar(), "refreshToken");

		Integer enClaro = jdbc.queryForObject("SELECT count(*) FROM refresh_tokens WHERE hash_token = ?",
				Integer.class, refresh);
		String guardado = jdbc.queryForObject("SELECT hash_token FROM refresh_tokens WHERE hash_token = ?",
				String.class, TokensRefresh.hash(refresh));

		assertThat(enClaro).isZero();
		assertThat(guardado).hasSize(64).isEqualTo(TokensRefresh.hash(refresh)).isNotEqualTo(refresh);
	}

	// ---------- JWT de acceso ----------

	@Test
	void unJwtConAlgNoneSeRechaza() throws Exception {
		String cabecera = base64("{\"alg\":\"none\",\"typ\":\"JWT\"}");
		String cuerpo = base64("{\"iss\":\"" + JwtConfig.EMISOR + "\",\"sub\":\"1\",\"exp\":"
				+ Instant.now().plusSeconds(600).getEpochSecond() + "}");
		String token = cabecera + "." + cuerpo + ".";

		assertThatThrownBy(() -> jwtDecoder.decode(token)).isInstanceOf(JwtException.class);
		mockMvc.perform(get("/postulaciones").header("Authorization", "Bearer " + token))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void unJwtCaducadoSeRechaza() throws Exception {
		Instant haceDiezMinutos = Instant.now().minusSeconds(600);
		String token = firmar(jwtEncoder, haceDiezMinutos.minusSeconds(900), haceDiezMinutos);

		assertThatThrownBy(() -> jwtDecoder.decode(token))
				.isInstanceOf(JwtException.class).hasMessageContaining("expired");
		mockMvc.perform(get("/postulaciones").header("Authorization", "Bearer " + token))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void unJwtFirmadoConOtraClaveSeRechaza() throws Exception {
		byte[] otraClave = "otra-clave-que-no-es-la-del-servidor-1234567890".getBytes(StandardCharsets.UTF_8);
		JwtEncoder ajeno = new NimbusJwtEncoder(new ImmutableSecret<>(new SecretKeySpec(otraClave, "HmacSHA256")));
		String token = firmar(ajeno, Instant.now(), Instant.now().plusSeconds(600));

		assertThatThrownBy(() -> jwtDecoder.decode(token)).isInstanceOf(JwtException.class);
		mockMvc.perform(get("/postulaciones").header("Authorization", "Bearer " + token))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void sinTokenUnaRutaProtegidaDa401() throws Exception {
		mockMvc.perform(get("/postulaciones")).andExpect(status().isUnauthorized());
	}

	@Test
	void unJwtValidoPasaElFiltroDeSeguridad() throws Exception {
		String acceso = campo(registrarYEntrar(), "accessToken");

		// Todavía no hay controlador de postulaciones (fase 3): basta con que no sea 401.
		int estado = mockMvc.perform(get("/postulaciones").header("Authorization", "Bearer " + acceso))
				.andReturn().getResponse().getStatus();

		assertThat(estado).isNotEqualTo(401);
	}

	// ---------- ayudas ----------

	private String firmar(JwtEncoder encoder, Instant emitidoEn, Instant caducaEn) {
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.issuer(JwtConfig.EMISOR).subject("1").issuedAt(emitidoEn).expiresAt(caducaEn).build();
		return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
				.getTokenValue();
	}

	private MvcResult registrarYEntrar() throws Exception {
		String email = emailNuevo();
		mockMvc.perform(registro(email, PASSWORD)).andExpect(status().isCreated());
		return mockMvc.perform(login(email, PASSWORD)).andExpect(status().isOk()).andReturn();
	}

	private Long idDe(String email) {
		return jdbc.queryForObject("SELECT id FROM usuarios WHERE email = ?", Long.class, email);
	}

	private static String campo(MvcResult resultado, String nombre) throws Exception {
		return JsonPath.read(resultado.getResponse().getContentAsString(), "$." + nombre);
	}

	private static String emailNuevo() {
		return "u-" + UUID.randomUUID() + "@example.com";
	}

	private static String base64(String texto) {
		return Base64.getUrlEncoder().withoutPadding().encodeToString(texto.getBytes(StandardCharsets.UTF_8));
	}

	private static MockHttpServletRequestBuilder registro(String email,
			String password) {
		return post("/auth/registro").contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password));
	}

	private static MockHttpServletRequestBuilder login(String email,
			String password) {
		return post("/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password));
	}

	private static MockHttpServletRequestBuilder refresh(String token) {
		return post("/auth/refresh").contentType(MediaType.APPLICATION_JSON)
				.content("{\"refreshToken\":\"%s\"}".formatted(token));
	}

}
