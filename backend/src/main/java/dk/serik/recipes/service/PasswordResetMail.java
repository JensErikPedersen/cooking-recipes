package dk.serik.recipes.service;

import dk.serik.recipes.model.AppUser;
import dk.serik.recipes.repository.PasswordResetTokens;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Component;

/**
 * The mail that carries a password reset link: plain text, to the account's address. A send that
 * fails throws, and the request that asked for it answers 500 (docs/PLAN_V6.md decision 3).
 */
@Component
public class PasswordResetMail {

	/** No mail is ever answered; in the stack, Mailpit takes any sender. */
	private static final String FROM = "Cooking Recipes <no-reply@cooking-recipes.local>";

	private final MailSender mailSender;

	private final String baseUrl;

	public PasswordResetMail(MailSender mailSender, @Value("${app.base-url}") String baseUrl) {
		this.mailSender = mailSender;
		this.baseUrl = baseUrl;
	}

	public void send(AppUser user, String token) {
		SimpleMailMessage message = new SimpleMailMessage();
		message.setFrom(FROM);
		message.setTo(user.getEmail());
		message.setSubject("Reset your password");
		message.setText("""
				Hello %s,

				Someone asked to reset the password of your Cooking Recipes account. If it was you, choose a new password here:

				%s/reset-password?token=%s

				The link works once, and for %d minutes. If you did not ask, ignore this mail: your password stays as it is.
				""".formatted(user.getUsername(), baseUrl, token, PasswordResetTokens.VALID_FOR.toMinutes()));
		mailSender.send(message);
	}
}
