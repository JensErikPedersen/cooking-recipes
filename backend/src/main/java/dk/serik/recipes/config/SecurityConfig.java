package dk.serik.recipes.config;

import dk.serik.recipes.bean.Session;
import dk.serik.recipes.exceptions.ApplicationErrorCodes;
import dk.serik.recipes.exceptions.ExceptionEnvelope;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;

/**
 * Session-cookie authentication with CSRF protection.
 * <p>
 * The browser only ever talks to the Next.js origin, which proxies {@code /api} here, so every
 * request is same-origin and there is no CORS configuration.
 */
@Configuration
public class SecurityConfig {

	/** Local and static on purpose - never a {@code @Primary JsonMapper} bean, see backend/CLAUDE.md. */
	private static final JsonMapper MAPPER = JsonMapper.builder().build();

	/** Delegating, so a stored hash names its algorithm ({@code {bcrypt}...}) and can be migrated later. */
	@Bean
	public PasswordEncoder passwordEncoder() {
		return PasswordEncoderFactories.createDelegatingPasswordEncoder();
	}

	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) {
		return configuration.getAuthenticationManager();
	}

	/** Shared with {@code AuthServiceImpl}, which saves the context into the session after login. */
	@Bean
	public SecurityContextRepository securityContextRepository() {
		return new HttpSessionSecurityContextRepository();
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http, SecurityContextRepository securityContextRepository,
												   Session session) {
		http
			// CSRF stays on. This API authenticates with a cookie the browser attaches by itself,
			// which is exactly what CSRF protection exists for. spa(): the token travels in a
			// readable XSRF-TOKEN cookie and comes back in the X-XSRF-TOKEN header. Its request handler
			// reads the token on every request, which is what writes the cookie - so, unlike older
			// versions, no extra filter is needed. AuthenticationIT.shouldIssueCsrfCookieToAnonymousCaller
			// fails if that ever changes.
			.csrf(csrf -> csrf.spa())
			.securityContext(context -> context.securityContextRepository(securityContextRepository))
			.authorizeHttpRequests(authorize -> authorize
					.requestMatchers("/api/v1/auth/login", "/actuator/health").permitAll()
					.anyRequest().authenticated())
			// JSON in this application's envelope, not a redirect to a login page.
			.exceptionHandling(exceptions -> exceptions
					.authenticationEntryPoint((request, response, ex) -> writeEnvelope(response,
							HttpStatus.UNAUTHORIZED, ApplicationErrorCodes.AUTHENTICATION_REQUIRED, "Authentication required"))
					.accessDeniedHandler((request, response, ex) -> writeEnvelope(response,
							HttpStatus.FORBIDDEN, ApplicationErrorCodes.ACCESS_DENIED, "Access denied")))
			.logout(logout -> logout
					.logoutUrl("/api/v1/auth/logout")
					.logoutSuccessHandler((request, response, authentication) ->
							response.setStatus(HttpStatus.NO_CONTENT.value())))
			// After authorization, so the principal is known by the time it is copied.
			.addFilterAfter(new SessionPopulatingFilter(session), AuthorizationFilter.class)
			// Login is AuthController, speaking JSON.
			.httpBasic(httpBasic -> httpBasic.disable())
			.formLogin(formLogin -> formLogin.disable());
		return http.build();
	}

	private static void writeEnvelope(HttpServletResponse response, HttpStatus status, ApplicationErrorCodes code,
									  String message) throws IOException {
		response.setStatus(status.value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding("UTF-8");
		response.getWriter().write(MAPPER.writeValueAsString(ExceptionEnvelope.builder()
				.errorCode(code.getCode())
				.message(message)
				.build()));
	}
}
