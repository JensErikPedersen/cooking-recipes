package dk.serik.recipes.service;

import dk.serik.recipes.dto.AuthenticatedUserDTO;
import dk.serik.recipes.dto.LoginRequestDTO;
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

	/** The signed-in caller. Only reachable authenticated: the security rules answer 401 first. */
	AuthenticatedUserDTO currentUser();
}
