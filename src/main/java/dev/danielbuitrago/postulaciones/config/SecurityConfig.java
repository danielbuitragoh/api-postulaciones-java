package dev.danielbuitrago.postulaciones.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import dev.danielbuitrago.postulaciones.security.EntradaNoAutenticada;

@Configuration
public class SecurityConfig {

	@Bean
	SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http
				.cors(Customizer.withDefaults())
				.csrf(AbstractHttpConfigurer::disable)
				.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/", "/salud", "/salud/**").permitAll()
						.requestMatchers("/auth/**").permitAll()
						.requestMatchers("/docs", "/docs/**", "/swagger-ui/**", "/v3/api-docs", "/v3/api-docs/**")
								.permitAll()
						.anyRequest().authenticated())
				.oauth2ResourceServer(oauth2 -> oauth2
						.jwt(Customizer.withDefaults())
						.authenticationEntryPoint(new EntradaNoAutenticada()))
				.exceptionHandling(errores -> errores.authenticationEntryPoint(new EntradaNoAutenticada()))
				.httpBasic(AbstractHttpConfigurer::disable)
				.formLogin(AbstractHttpConfigurer::disable);
		return http.build();
	}

	// Argon2id con los parámetros mínimos recomendados por OWASP:
	// 19 MiB de memoria (19456 KiB), 2 iteraciones, 1 hilo.
	@Bean
	PasswordEncoder passwordEncoder() {
		return new Argon2PasswordEncoder(16, 32, 1, 19456, 2);
	}

}
