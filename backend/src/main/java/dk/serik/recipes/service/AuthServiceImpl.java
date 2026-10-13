package dk.serik.recipes.service;

import dk.serik.recipes.dto.AuthenticatedUserDTO;
import dk.serik.recipes.dto.ChangePasswordRequestDTO;
import dk.serik.recipes.dto.LoginRequestDTO;
import dk.serik.recipes.dto.RegisterRequestDTO;
import dk.serik.recipes.exceptions.ApplicationErrorCodes;
import dk.serik.recipes.exceptions.ServiceException;
import dk.serik.recipes.exceptions.ValidationExceptionEnvelope;
import dk.serik.recipes.model.AppUser;
import dk.serik.recipes.repository.AppUserJpaRepository;
import dk.serik.recipes.repository.AppUserWriter;
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
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
@AllArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

	private final AuthenticationManager authenticationManager;

	private final SecurityContextRepository securityContextRepository;

	private final AppUserJpaRepository appUserJpaRepository;

	private final AppUserWriter appUserWriter;

	private final SessionRegistry sessionRegistry;

	/** bcrypt reads no further; Spring Security refuses to encode a longer password, with an exception. */
	private static final int MAX_PASSWORD_BYTES = 72;

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

		// Session fixation: a session may exist from before sign-in - an anonymous 401 creates one -
		// and its id, which someone could have planted, must not become the signed-in one. Spring's
		// own form login does the same; a controller login has to do it itself.
		if (request.getSession(false) != null) {
			request.changeSessionId();
		}

		// Spring Security 6+ no longer saves the context implicitly. Without the explicit save the
		// login answers 200 and the very next request is anonymous again.
		SecurityContext context = SecurityContextHolder.createEmptyContext();
		context.setAuthentication(authentication);
		SecurityContextHolder.setContext(context);
		securityContextRepository.saveContext(context, request, response);
		// By username, so that every session of an account can be found to end it. Spring registers a
		// session only at a login it performs itself.
		sessionRegistry.registerNewSession(request.getSession().getId(), authentication.getName());

		return toDto(authentication);
	}

	@Override
	public AuthenticatedUserDTO register(RegisterRequestDTO registerRequest, HttpServletRequest request, HttpServletResponse response) {
		List<ValidationExceptionEnvelope> problems = new ArrayList<>();
		checkNewPassword(registerRequest.getPassword(), "password", registerRequest.getConfirmPassword(), problems);
		throwIfAny(problems);

		String username = registerRequest.getUsername();
		// Telling a caller that a username or an address is taken tells them it exists. With open
		// sign-up that is unavoidable; login still answers a wrong password and an unknown user alike.
		if (appUserJpaRepository.existsByUsernameIgnoreCase(username)) {
			throw ServiceException.builder()
					.message(String.format("The username '%s' is taken", username))
					.code(ApplicationErrorCodes.USERNAME_TAKEN.getCode())
					.httpStatus(HttpStatus.CONFLICT)
					.field("username")
					.build();
		}
		String email = registerRequest.getEmail();
		if (appUserJpaRepository.existsByEmailIgnoreCase(email)) {
			throw ServiceException.builder()
					.message(String.format("The email address '%s' is taken", email))
					.code(ApplicationErrorCodes.EMAIL_TAKEN.getCode())
					.httpStatus(HttpStatus.CONFLICT)
					.field("email")
					.build();
		}

		// A new user creates their own account, so it is recorded as created by them.
		appUserWriter.insert(username, email, registerRequest.getPassword(), AppUser.ROLE_USER, username);
		log.info("Registered account '{}'", username);
		return login(LoginRequestDTO.builder().username(username).password(registerRequest.getPassword()).build(),
				request, response);
	}

	@Override
	public void changePassword(ChangePasswordRequestDTO changeRequest, HttpServletRequest request) {
		String username = SecurityContextHolder.getContext().getAuthentication().getName();
		List<ValidationExceptionEnvelope> problems = new ArrayList<>();
		// A 400 on the field, not login's 401: to the frontend a 401 means the session has ended.
		try {
			authenticationManager.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(
					username, changeRequest.getCurrentPassword()));
		} catch (AuthenticationException ex) {
			problems.add(new ValidationExceptionEnvelope("currentPassword", "The current password is wrong"));
		}
		checkNewPassword(changeRequest.getNewPassword(), "newPassword", changeRequest.getConfirmPassword(), problems);
		throwIfAny(problems);

		appUserWriter.updatePassword(username, changeRequest.getNewPassword(), username);
		// Whoever else holds a session of the account - someone who had the old password, perhaps - is
		// signed out at their next request. This session stays, under a new id as at login.
		String current = request.getSession().getId();
		sessionRegistry.getAllSessions(username, false).stream()
				.filter(session -> !session.getSessionId().equals(current))
				.forEach(SessionInformation::expireNow);
		request.changeSessionId();
		log.info("Changed the password of '{}'", username);
	}

	@Override
	public AuthenticatedUserDTO currentUser() {
		return toDto(SecurityContextHolder.getContext().getAuthentication());
	}

	/** The rules for a new password that Bean Validation cannot check, for registration and change alike. */
	private static void checkNewPassword(String password, String field, String confirmation, List<ValidationExceptionEnvelope> problems) {
		if (password.getBytes(StandardCharsets.UTF_8).length > MAX_PASSWORD_BYTES) {
			problems.add(new ValidationExceptionEnvelope(field,
					"Password is too long: at most 72 characters, fewer with letters such as æ, ø and å"));
		}
		if (!password.equals(confirmation)) {
			problems.add(new ValidationExceptionEnvelope("confirmPassword", "The passwords do not match"));
		}
	}

	private static void throwIfAny(List<ValidationExceptionEnvelope> problems) {
		if (!problems.isEmpty()) {
			throw ServiceException.builder()
					.message("Validation Exception")
					.code(ApplicationErrorCodes.VALIDATION_EXCEPTION.getCode())
					.httpStatus(HttpStatus.BAD_REQUEST)
					.fieldErrors(problems)
					.build();
		}
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
