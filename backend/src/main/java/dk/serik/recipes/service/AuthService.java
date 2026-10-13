package dk.serik.recipes.service;

import dk.serik.recipes.dto.AuthenticatedUserDTO;
import dk.serik.recipes.dto.ChangeEmailRequestDTO;
import dk.serik.recipes.dto.ChangePasswordRequestDTO;
import dk.serik.recipes.dto.LoginRequestDTO;
import dk.serik.recipes.dto.PasswordResetConfirmDTO;
import dk.serik.recipes.dto.PasswordResetRequestDTO;
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
	 *         confirmation that differs, on their fields; 409 on a username or an email address
	 *         taken in any case
	 */
	AuthenticatedUserDTO register(RegisterRequestDTO registerRequest, HttpServletRequest request, HttpServletResponse response);

	/**
	 * Changes the signed-in caller's password, to registration's rules, and gives the session a new id.
	 * Every other session of the same account is ended.
	 *
	 * @throws dk.serik.recipes.exceptions.ServiceException 400 on a wrong current password, a new one
	 *         over 72 bytes or a confirmation that differs, every problem on its field
	 */
	void changePassword(ChangePasswordRequestDTO changeRequest, HttpServletRequest request);

	/**
	 * Changes the signed-in caller's email address, to registration's rules. The session and the
	 * account's other sessions stay as they are: the address does not sign anyone in.
	 *
	 * @throws dk.serik.recipes.exceptions.ServiceException 400 on a wrong current password, on its
	 *         field; 409 on an address another account has, in any case
	 */
	void changeEmail(ChangeEmailRequestDTO changeRequest);

	/**
	 * Mails a reset link to the enabled account with this address, in any case, replacing any earlier
	 * token of it. Anything else - an unknown address, a disabled account - is no mail, and the same
	 * answer, so the caller cannot tell an address that has an account.
	 *
	 * @throws org.springframework.mail.MailException when the mail cannot be sent: a 500, and no token kept
	 */
	void requestPasswordReset(PasswordResetRequestDTO resetRequest);

	/**
	 * Sets a new password, to registration's rules, with a token from a reset mail, and spends the
	 * token. Every session of the account is ended, and the caller is not signed in: signing in with
	 * the new password proves it.
	 *
	 * @throws dk.serik.recipes.exceptions.ServiceException 400 on an unknown, expired or spent token -
	 *         one message for all three, on {@code token} - and on the password's rules, every problem
	 *         on its field
	 */
	void confirmPasswordReset(PasswordResetConfirmDTO confirmation);

	/** The signed-in caller. Only reachable authenticated: the security rules answer 401 first. */
	AuthenticatedUserDTO currentUser();
}
