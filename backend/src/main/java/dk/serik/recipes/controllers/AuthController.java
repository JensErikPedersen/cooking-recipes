package dk.serik.recipes.controllers;

import dk.serik.recipes.dto.AuthenticatedUserDTO;
import dk.serik.recipes.dto.LoginRequestDTO;
import dk.serik.recipes.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Sign in, and report who is signed in.
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

	@GetMapping("/me")
	public AuthenticatedUserDTO me() {
		return authService.currentUser();
	}
}
