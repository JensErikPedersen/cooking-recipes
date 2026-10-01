package dk.serik.recipes.service;

import dk.serik.recipes.dto.AuthenticatedUserDTO;
import dk.serik.recipes.dto.LoginRequestDTO;
import dk.serik.recipes.exceptions.ApplicationErrorCodes;
import dk.serik.recipes.exceptions.ServiceException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

	private final AuthenticationManager authenticationManager;

	private final SecurityContextRepository securityContextRepository;

	@Override
	public AuthenticatedUserDTO login(LoginRequestDTO loginRequest, HttpServletRequest request, HttpServletResponse response) {
		Authentication authentication;
		try {
			authentication = authenticationManager.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(
					loginRequest.getUsername(), loginRequest.getPassword()));
		} catch (AuthenticationException ex) {
			// One message for unknown user, wrong password and disabled account alike - telling them
			// apart would let anyone probe which usernames exist. The log names neither.
			log.info("Login rejected");
			throw ServiceException.builder()
					.message("Invalid username or password")
					.code(ApplicationErrorCodes.BAD_CREDENTIALS.getCode())
					.httpStatus(HttpStatus.UNAUTHORIZED)
					.build();
		}

		// Spring Security 6+ no longer saves the context implicitly. Without the explicit save the
		// login answers 200 and the very next request is anonymous again.
		SecurityContext context = SecurityContextHolder.createEmptyContext();
		context.setAuthentication(authentication);
		SecurityContextHolder.setContext(context);
		securityContextRepository.saveContext(context, request, response);

		return toDto(authentication);
	}

	@Override
	public AuthenticatedUserDTO currentUser() {
		return toDto(SecurityContextHolder.getContext().getAuthentication());
	}

	private AuthenticatedUserDTO toDto(Authentication authentication) {
		// ROLE_* only. Spring Security 7 also grants factor authorities such as FACTOR_PASSWORD,
		// which describe how the caller signed in and are not application roles.
		return AuthenticatedUserDTO.builder()
				.username(authentication.getName())
				.roles(authentication.getAuthorities().stream()
						.map(GrantedAuthority::getAuthority)
						.filter(authority -> authority.startsWith("ROLE_"))
						.sorted()
						.toList())
				.build();
	}
}
