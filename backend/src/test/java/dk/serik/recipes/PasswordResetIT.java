package dk.serik.recipes;

import dk.serik.recipes.dto.LoginRequestDTO;
import dk.serik.recipes.dto.PasswordResetConfirmDTO;
import dk.serik.recipes.dto.PasswordResetRequestDTO;
import dk.serik.recipes.dto.RegisterRequestDTO;
import dk.serik.recipes.exceptions.ApplicationErrorCodes;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.json.JsonMapper;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A forgotten password reset by email, through the real security chain and database, with
 * {@link RecordingMailSender} in place of a mail server: the token is read from the mail, as a user
 * would follow its link. Each test registers accounts of its own, never touching the admin's.
 * <p>
 * The properties are {@code AuthenticationIT}'s, so these share one cached context - see
 * {@code RegistrationIT} for why the real {@code XSRF-TOKEN} cookie needs that.
 */
@SpringBootTest(classes = RecipesApplication.class, properties = {
		"app.admin.username=" + AuthenticationIT.USERNAME,
		"app.admin.password=" + AuthenticationIT.PASSWORD
})
@AutoConfigureMockMvc
@AutoConfigureTestDatabase
class PasswordResetIT {

	private static final String RESET = "/api/v1/auth/password-reset";
	private static final String CONFIRM = "/api/v1/auth/password-reset/confirm";
	private static final String REGISTER = "/api/v1/auth/register";
	private static final String LOGIN = "/api/v1/auth/login";
	private static final String ME = "/api/v1/auth/me";
	private static final String XSRF_HEADER = "X-XSRF-TOKEN";
	private static final String OLD = "the old password";
	private static final String NEW = "the new password";
	private static final String INVALID = "This reset link is invalid or has expired";
	private static final Pattern LINK = Pattern.compile("http://localhost:3000/reset-password\\?token=([A-Za-z0-9_-]+)");

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JsonMapper jsonMapper;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private RecordingMailSender mailSender;

	/** A newly registered account, still signed in. */
	private record Account(String username, String email, MockHttpSession session) {
	}

	@Test
	@DisplayName("Given an enabled account's address, When a reset is asked for, Then 202 and one mail to it, with a link and how long it works")
	void shouldMailTheLink() throws Exception {
		Account account = registerAccount();

		askForReset(account.email()).andExpect(status().isAccepted());

		List<SimpleMailMessage> mails = mailSender.sentTo(account.email());
		assertThat(mails).singleElement().satisfies(mail -> {
			assertThat(mail.getSubject()).isEqualTo("Reset your password");
			assertThat(mail.getFrom()).isEqualTo("Cooking Recipes <no-reply@cooking-recipes.local>");
			assertThat(mail.getText())
					.contains("Hello " + account.username() + ",")
					.containsPattern(LINK)
					.contains("The link works once, and for 30 minutes.");
		});
	}

	@Test
	@DisplayName("Given the address in another case, When a reset is asked for, Then the mail goes to the address as stored")
	void shouldFindTheAddressInAnyCase() throws Exception {
		Account account = registerAccount();

		askForReset(account.email().toUpperCase()).andExpect(status().isAccepted());

		assertThat(mailSender.sentTo(account.email())).hasSize(1);
	}

	@Test
	@DisplayName("Given an unknown address and a disabled account's, When a reset is asked for, Then no mail - and every answer the same as an enabled account's")
	void shouldAnswerAlikeWhetherOrNotAMailIsSent() throws Exception {
		Account enabled = registerAccount();
		Account disabled = registerAccount();
		jdbcTemplate.update("UPDATE app_user SET enabled = false WHERE username = ?", disabled.username());
		String unknown = "nobody-" + UUID.randomUUID() + "@example.com";

		MockHttpServletResponse toEnabled = askForReset(enabled.email()).andReturn().getResponse();
		MockHttpServletResponse toDisabled = askForReset(disabled.email()).andReturn().getResponse();
		MockHttpServletResponse toUnknown = askForReset(unknown).andReturn().getResponse();

		for (MockHttpServletResponse response : List.of(toEnabled, toDisabled, toUnknown)) {
			assertThat(response.getStatus()).isEqualTo(202);
			assertThat(response.getContentAsString()).isEmpty();
		}
		assertThat(mailSender.sentTo(enabled.email())).hasSize(1);
		assertThat(mailSender.sentTo(disabled.email())).isEmpty();
		assertThat(mailSender.sentTo(unknown)).isEmpty();
	}

	@Test
	@DisplayName("Given the token from the mail, When confirmed with a new password, Then 204, the new password signs in and the old one does not")
	void shouldResetThePassword() throws Exception {
		Account account = registerAccount();
		String token = mailedToken(account);

		confirm(token, NEW, NEW).andExpect(status().isNoContent());

		login(account.username(), OLD).andExpect(status().isUnauthorized());
		login(account.username(), NEW).andExpect(status().isOk());
	}

	@Test
	@DisplayName("Given a reset, When the row is read, Then a new bcrypt hash, updated by the account itself")
	void shouldStoreTheNewHash() throws Exception {
		Account account = registerAccount();
		String hashBefore = storedHash(account.username());

		confirm(mailedToken(account), NEW, NEW).andExpect(status().isNoContent());

		assertThat(storedHash(account.username())).startsWith("{bcrypt}").isNotEqualTo(hashBefore).doesNotContain(NEW);
		assertThat(jdbcTemplate.queryForObject("SELECT updated_by FROM app_user WHERE username = ?", String.class,
				account.username())).isEqualTo(account.username());
	}

	@Test
	@DisplayName("Given sessions of the account, When its password is reset, Then every one is signed out, and the confirm signs no one in")
	void shouldEndEverySessionAndSignNoOneIn() throws Exception {
		Account account = registerAccount();
		MockHttpSession other = (MockHttpSession) login(account.username(), OLD).andExpect(status().isOk())
				.andReturn().getRequest().getSession(false);
		String token = mailedToken(account);
		// The browser following the link: a session of its own, anonymous, from asking who it is.
		MvcResult anonymous = mockMvc.perform(get(ME)).andExpect(status().isUnauthorized()).andReturn();
		MockHttpSession confirming = (MockHttpSession) anonymous.getRequest().getSession(false);
		Cookie xsrf = anonymous.getResponse().getCookie("XSRF-TOKEN");

		mockMvc.perform(post(CONFIRM).session(confirming).cookie(xsrf).header(XSRF_HEADER, xsrf.getValue())
						.contentType(MediaType.APPLICATION_JSON).content(confirmBody(token, NEW, NEW)))
				.andExpect(status().isNoContent());

		mockMvc.perform(get(ME).session(account.session())).andExpect(status().isUnauthorized());
		mockMvc.perform(get(ME).session(other)).andExpect(status().isUnauthorized());
		mockMvc.perform(get(ME).session(confirming)).andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("Given a token used once, When used again, Then 400 on the field token, and the password from the first use stays")
	void shouldWorkOnlyOnce() throws Exception {
		Account account = registerAccount();
		String token = mailedToken(account);
		confirm(token, NEW, NEW).andExpect(status().isNoContent());

		assertInvalidToken(token);

		login(account.username(), NEW).andExpect(status().isOk());
	}

	@Test
	@DisplayName("Given a token past its 30 minutes, When confirmed, Then the same 400 on the field token, and the password unchanged")
	void shouldRefuseAnExpiredToken() throws Exception {
		Account account = registerAccount();
		String token = mailedToken(account);
		jdbcTemplate.update("UPDATE password_reset_token SET expires = ? WHERE app_user_id = (SELECT id FROM app_user WHERE username = ?)",
				Timestamp.from(Instant.now().minusSeconds(1)), account.username());

		assertInvalidToken(token);

		login(account.username(), OLD).andExpect(status().isOk());
	}

	@Test
	@DisplayName("Given a made-up token, When confirmed, Then the same 400 on the field token")
	void shouldRefuseAMadeUpToken() throws Exception {
		assertInvalidToken("made-up-" + UUID.randomUUID());
	}

	@Test
	@DisplayName("Given a second request, When the first mail's token is confirmed, Then the same 400 - and the second's works")
	void shouldReplaceTheEarlierToken() throws Exception {
		Account account = registerAccount();
		String first = mailedToken(account);
		String second = mailedToken(account);

		assertInvalidToken(first);

		confirm(second, NEW, NEW).andExpect(status().isNoContent());
	}

	@Test
	@DisplayName("Given a mailed token, When the table is read, Then one row for the account, holding the token's SHA-256 - not the token")
	void shouldStoreOnlyTheHash() throws Exception {
		Account account = registerAccount();
		String token = mailedToken(account);

		List<String> stored = jdbcTemplate.queryForList(
				"SELECT token_hash FROM password_reset_token WHERE app_user_id = (SELECT id FROM app_user WHERE username = ?)",
				String.class, account.username());
		assertThat(stored).singleElement().satisfies(hash -> assertThat(hash).matches("[0-9a-f]{64}").doesNotContain(token));
	}

	@Test
	@DisplayName("Given a new password breaking the rules, When confirmed, Then 400 on its fields - and the token still works")
	void shouldApplyThePasswordRulesWithoutSpendingTheToken() throws Exception {
		Account account = registerAccount();
		String token = mailedToken(account);

		confirm(token, "7 chars", "7 chars")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.validationExceptions[0].objectName").value("newPassword"))
				.andExpect(jsonPath("$.validationExceptions[0].message").value("Password must be at least 8 characters"));
		confirm(token, NEW, NEW + "!")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.validationExceptions", hasSize(1)))
				.andExpect(jsonPath("$.validationExceptions[0].objectName").value("confirmPassword"));
		String password = "æ".repeat(37);
		confirm(token, password, password)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.validationExceptions[0].objectName").value("newPassword"));

		confirm(token, NEW, NEW).andExpect(status().isNoContent());
	}

	@Test
	@DisplayName("Given a made-up token and a mismatch, When confirmed, Then both in one 400")
	void shouldReportEveryProblemAtOnce() throws Exception {
		confirm("made-up", NEW, NEW + "!")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.validationExceptions", hasSize(2)))
				.andExpect(jsonPath("$.validationExceptions[0].objectName").value("token"))
				.andExpect(jsonPath("$.validationExceptions[1].objectName").value("confirmPassword"));
	}

	@Test
	@DisplayName("Given a mail that cannot be sent, When a reset is asked for, Then 500 and no token kept")
	void shouldFailWhenTheMailCannotBeSent() throws Exception {
		Account account = registerAccount();
		mailSender.failNextSend(new MailSendException("Mail server down"));

		askForReset(account.email())
				.andExpect(status().isInternalServerError());

		assertThat(jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM password_reset_token WHERE app_user_id = (SELECT id FROM app_user WHERE username = ?)",
				Integer.class, account.username())).isZero();
	}

	@Test
	@DisplayName("Given an account with a token, When the account row is deleted, Then its token goes with it")
	void shouldDeleteTheTokenWithTheAccount() throws Exception {
		Account account = registerAccount();
		mailedToken(account);
		String id = jdbcTemplate.queryForObject("SELECT id FROM app_user WHERE username = ?", String.class, account.username());

		jdbcTemplate.update("DELETE FROM app_user WHERE id = ?", id);

		assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM password_reset_token WHERE app_user_id = ?",
				Integer.class, id)).isZero();
	}

	@Test
	@DisplayName("Given a malformed address, When a reset is asked for, Then 400 on the field email")
	void shouldRejectAMalformedAddress() throws Exception {
		askForReset("not-an-address")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.validationExceptions[0].objectName").value("email"))
				.andExpect(jsonPath("$.validationExceptions[0].message").value("Email address is not valid"));
	}

	@Test
	@DisplayName("Given no CSRF token, When either endpoint is called, Then 403 and no mail")
	void shouldRequireTheCsrfToken() throws Exception {
		Account account = registerAccount();

		mockMvc.perform(post(RESET).contentType(MediaType.APPLICATION_JSON)
						.content(jsonMapper.writeValueAsString(PasswordResetRequestDTO.builder().email(account.email()).build())))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.errorCode").value(ApplicationErrorCodes.ACCESS_DENIED.getCode()));
		mockMvc.perform(post(CONFIRM).contentType(MediaType.APPLICATION_JSON).content(confirmBody("any", NEW, NEW)))
				.andExpect(status().isForbidden());

		assertThat(mailSender.sentTo(account.email())).isEmpty();
	}

	private void assertInvalidToken(String token) throws Exception {
		confirm(token, NEW, NEW)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errorCode").value(ApplicationErrorCodes.VALIDATION_EXCEPTION.getCode()))
				.andExpect(jsonPath("$.validationExceptions", hasSize(1)))
				.andExpect(jsonPath("$.validationExceptions[0].objectName").value("token"))
				.andExpect(jsonPath("$.validationExceptions[0].message").value(INVALID));
	}

	/** Asks for a reset and reads the token from the link in the newest mail to the account. */
	private String mailedToken(Account account) throws Exception {
		askForReset(account.email()).andExpect(status().isAccepted());
		List<SimpleMailMessage> mails = mailSender.sentTo(account.email());
		Matcher link = LINK.matcher(mails.getLast().getText());
		assertThat(link.find()).isTrue();
		return link.group(1);
	}

	private ResultActions askForReset(String email) throws Exception {
		Cookie xsrf = xsrfCookie();
		return mockMvc.perform(post(RESET).cookie(xsrf).header(XSRF_HEADER, xsrf.getValue())
				.contentType(MediaType.APPLICATION_JSON)
				.content(jsonMapper.writeValueAsString(PasswordResetRequestDTO.builder().email(email).build())));
	}

	private ResultActions confirm(String token, String newPassword, String confirmPassword) throws Exception {
		Cookie xsrf = xsrfCookie();
		return mockMvc.perform(post(CONFIRM).cookie(xsrf).header(XSRF_HEADER, xsrf.getValue())
				.contentType(MediaType.APPLICATION_JSON).content(confirmBody(token, newPassword, confirmPassword)));
	}

	private String confirmBody(String token, String newPassword, String confirmPassword) {
		return jsonMapper.writeValueAsString(PasswordResetConfirmDTO.builder()
				.token(token).newPassword(newPassword).confirmPassword(confirmPassword).build());
	}

	private Account registerAccount() throws Exception {
		String username = "user-" + UUID.randomUUID().toString().substring(0, 8);
		String email = username + "@example.com";
		Cookie xsrf = xsrfCookie();
		MockHttpSession session = (MockHttpSession) mockMvc.perform(post(REGISTER).cookie(xsrf).header(XSRF_HEADER, xsrf.getValue())
						.contentType(MediaType.APPLICATION_JSON)
						.content(jsonMapper.writeValueAsString(RegisterRequestDTO.builder()
								.username(username).email(email).password(OLD).confirmPassword(OLD).build())))
				.andExpect(status().isCreated())
				.andReturn().getRequest().getSession(false);
		return new Account(username, email, session);
	}

	private ResultActions login(String username, String password) throws Exception {
		Cookie xsrf = xsrfCookie();
		return mockMvc.perform(post(LOGIN).cookie(xsrf).header(XSRF_HEADER, xsrf.getValue())
				.contentType(MediaType.APPLICATION_JSON)
				.content(jsonMapper.writeValueAsString(LoginRequestDTO.builder().username(username).password(password).build())));
	}

	private String storedHash(String username) {
		return jdbcTemplate.queryForObject("SELECT password FROM app_user WHERE username = ?", String.class, username);
	}

	private Cookie xsrfCookie() throws Exception {
		return mockMvc.perform(get(ME)).andReturn().getResponse().getCookie("XSRF-TOKEN");
	}
}
