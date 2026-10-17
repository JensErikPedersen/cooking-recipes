package dk.serik.recipes.controllers;

import dk.serik.recipes.dto.AuthenticatedUserDTO;
import dk.serik.recipes.dto.ChangeEmailRequestDTO;
import dk.serik.recipes.dto.ChangePasswordRequestDTO;
import dk.serik.recipes.dto.LoginRequestDTO;
import dk.serik.recipes.dto.PasswordResetConfirmDTO;
import dk.serik.recipes.dto.PasswordResetRequestDTO;
import dk.serik.recipes.dto.RegisterRequestDTO;
import dk.serik.recipes.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Sign in, register a new account - which signs it in - change one's own password or email address,
 * reset a forgotten password by email, and report who is signed in.
 * <p>
 * There is no logout method: Spring Security's logout filter handles
 * {@code POST /api/v1/auth/logout}, invalidates the session and answers 204 - see
 * {@code SecurityConfig}. A method here would never be reached.
 */
@RestController
@RequestMapping("/api/v1/auth")
@AllArgsConstructor
public class AuthController {

	private final AuthService authService;

	@PostMapping("/login")
	public AuthenticatedUserDTO login(@Valid @RequestBody LoginRequestDTO loginRequest,
									  HttpServletRequest request, HttpServletResponse response) {
		return authService.login(loginRequest, request, response);
	}

	@PostMapping("/register")
	@ResponseStatus(HttpStatus.CREATED)
	public AuthenticatedUserDTO register(@Valid @RequestBody RegisterRequestDTO registerRequest,
										 HttpServletRequest request, HttpServletResponse response) {
		return authService.register(registerRequest, request, response);
	}

	@PutMapping("/password")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void changePassword(@Valid @RequestBody ChangePasswordRequestDTO changeRequest, HttpServletRequest request) {
		authService.changePassword(changeRequest, request);
	}

	@PutMapping("/email")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void changeEmail(@Valid @RequestBody ChangeEmailRequestDTO changeRequest) {
		authService.changeEmail(changeRequest);
	}

	/** The same 202, with no body, whether or not the address belongs to an enabled account. */
	@PostMapping("/password-reset")
	@ResponseStatus(HttpStatus.ACCEPTED)
	public void requestPasswordReset(@Valid @RequestBody PasswordResetRequestDTO resetRequest) {
		authService.requestPasswordReset(resetRequest);
	}

	@PostMapping("/password-reset/confirm")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void confirmPasswordReset(@Valid @RequestBody PasswordResetConfirmDTO confirmation) {
		authService.confirmPasswordReset(confirmation);
	}

	@GetMapping("/me")
	public AuthenticatedUserDTO me() {
		return authService.currentUser();
	}
}
