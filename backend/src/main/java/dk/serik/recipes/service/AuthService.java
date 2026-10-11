package dk.serik.recipes.service;

import dk.serik.recipes.dto.AuthenticatedUserDTO;
import dk.serik.recipes.dto.ChangePasswordRequestDTO;
import dk.serik.recipes.dto.LoginRequestDTO;
import dk.serik.recipes.dto.RegisterRequestDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface AuthService {

	/**
	 * Authenticates and stores the result in the HTTP session.
	 *
	 * @throws dk.serik.recipes.exceptions.ServiceException 401 on bad credentials or a disabled
	 *         account, with one message for all cases
	 */
	AuthenticatedUserDTO login(LoginRequestDTO loginRequest, HttpServletRequest request, HttpServletResponse response);

	/**
	 * Creates an account with {@code ROLE_USER} and signs it in, exactly as {@link #login} does.
	 *
	 * @throws dk.serik.recipes.exceptions.ServiceException 400 on a password over 72 bytes or a
	 *         confirmation that differs, on their fields; 409 on a username taken in any case
	 */
	AuthenticatedUserDTO register(RegisterRequestDTO registerRequest, HttpServletRequest request, HttpServletResponse response);

	/**
	 * Changes the signed-in caller's password, to registration's rules, and gives the session a new id.
	 * Other sessions of the same account stay signed in.
	 *
	 * @throws dk.serik.recipes.exceptions.ServiceException 400 on a wrong current password, a new one
	 *         over 72 bytes or a confirmation that differs, every problem on its field
	 */
	void changePassword(ChangePasswordRequestDTO changeRequest, HttpServletRequest request);

	/** The signed-in caller. Only reachable authenticated: the security rules answer 401 first. */
	AuthenticatedUserDTO currentUser();
}
