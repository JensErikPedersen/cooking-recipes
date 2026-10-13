package dk.serik.recipes;

import dk.serik.recipes.dto.ChangePasswordRequestDTO;
import dk.serik.recipes.dto.LoginRequestDTO;
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
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Changing one's own password, through the real security chain and database. Each test registers an
 * account of its own and changes that one's password, never the admin's.
 * <p>
 * The properties are {@code AuthenticationIT}'s, so the three share one cached context - see
 * {@code RegistrationIT} for why the real {@code XSRF-TOKEN} cookie needs that.
 * <p>
 * MockMvc runs no servlet container, so the session registry never hears of a session's new id or of
 * its end here. What depends on those - a second change from the other session, after the first
 * changed this one's id - is covered by e2e's {@code password.spec.ts}, against the real stack.
 */
@SpringBootTest(classes = RecipesApplication.class, properties = {
		"app.admin.username=" + AuthenticationIT.USERNAME,
		"app.admin.password=" + AuthenticationIT.PASSWORD
})
@AutoConfigureMockMvc
@AutoConfigureTestDatabase
class PasswordChangeIT {

	private static final String PASSWORD_PATH = "/api/v1/auth/password";
	private static final String REGISTER = "/api/v1/auth/register";
	private static final String LOGIN = "/api/v1/auth/login";
	private static final String ME = "/api/v1/auth/me";
	private static final String XSRF_HEADER = "X-XSRF-TOKEN";
	private static final String OLD = "the old password";
	private static final String NEW = "the new password";
	private static final String TOO_LONG = "Password is too long: at most 72 characters, fewer with letters such as æ, ø and å";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JsonMapper jsonMapper;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	/** A newly registered account, signed in. */
	private record Account(String username, MockHttpSession session, Cookie xsrf) {
	}

	@Test
	@DisplayName("Given a signed-in user, When changing the password, Then 204, the new one signs in and the old one does not")
	void shouldChangePassword() throws Exception {
		Account account = registerAccount();

		change(account, OLD, NEW, NEW).andExpect(status().isNoContent());

		login(account.username(), OLD).andExpect(status().isUnauthorized());
		login(account.username(), NEW).andExpect(status().isOk());
	}

	@Test
	@DisplayName("Given a signed-in user, When changing the password, Then the session stays signed in, under a new id")
	void shouldKeepTheSessionUnderANewId() throws Exception {
		Account account = registerAccount();
		String idBefore = account.session().getId();

		change(account, OLD, NEW, NEW).andExpect(status().isNoContent());

		assertThat(account.session().getId()).isNotEqualTo(idBefore);
		mockMvc.perform(get(ME).session(account.session()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.username").value(account.username()));
	}

	@Test
	@DisplayName("Given a second session of the account, When the password is changed in the first, Then the second is signed out with a 401")
	void shouldEndTheAccountsOtherSessions() throws Exception {
		Account account = registerAccount();
		MockHttpSession other = signIn(account.username(), OLD);

		change(account, OLD, NEW, NEW).andExpect(status().isNoContent());

		mockMvc.perform(get(ME).session(other))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.errorCode").value(ApplicationErrorCodes.AUTHENTICATION_REQUIRED.getCode()))
				// As at logout, so the frontend's proxy.ts sees a visitor without a session.
				.andExpect(cookie().maxAge("JSESSIONID", 0));
		assertThat(other.isInvalid()).isTrue();
		mockMvc.perform(get(ME).session(account.session())).andExpect(status().isOk());
	}

	@Test
	@DisplayName("Given a browser still holding an ended session, When it signs in, Then the sign-in succeeds at the first attempt")
	void shouldSignInFromAnEndedSession() throws Exception {
		Account account = registerAccount();
		MockHttpSession ended = signIn(account.username(), OLD);
		change(account, OLD, NEW, NEW).andExpect(status().isNoContent());

		// The ended session's cookie is the first thing the browser sends again, with the login.
		Cookie xsrf = xsrfCookie();
		MockHttpSession again = (MockHttpSession) mockMvc.perform(post(LOGIN).session(ended)
						.cookie(xsrf).header(XSRF_HEADER, xsrf.getValue())
						.contentType(MediaType.APPLICATION_JSON)
						.content(jsonMapper.writeValueAsString(LoginRequestDTO.builder()
								.username(account.username()).password(NEW).build())))
				.andExpect(status().isOk())
				.andReturn().getRequest().getSession(false);

		assertThat(again).isNotSameAs(ended);
		mockMvc.perform(get(ME).session(again))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.username").value(account.username()));
	}

	@Test
	@DisplayName("Given another account signed in, When a password is changed, Then that account stays signed in")
	void shouldLeaveOtherAccountsSignedIn() throws Exception {
		Account account = registerAccount();
		Account bystander = registerAccount();

		change(account, OLD, NEW, NEW).andExpect(status().isNoContent());

		mockMvc.perform(get(ME).session(bystander.session()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.username").value(bystander.username()));
	}

	@Test
	@DisplayName("Given a rejected change, When the account's other session asks, Then it is still signed in")
	void shouldKeepOtherSessionsWhenRejected() throws Exception {
		Account account = registerAccount();
		MockHttpSession other = signIn(account.username(), OLD);

		change(account, OLD + "!", NEW, NEW).andExpect(status().isBadRequest());

		mockMvc.perform(get(ME).session(other)).andExpect(status().isOk());
	}

	@Test
	@DisplayName("Given a change, When the row is read, Then a new bcrypt hash, and updated by the user")
	void shouldStoreTheNewHash() throws Exception {
		Account account = registerAccount();
		String hashBefore = storedHash(account.username());

		change(account, OLD, NEW, NEW).andExpect(status().isNoContent());

		Map<String, Object> row = jdbcTemplate.queryForMap(
				"SELECT password, updated_by FROM app_user WHERE username = ?", account.username());
		assertThat((String) row.get("password")).startsWith("{bcrypt}").doesNotContain(NEW).isNotEqualTo(hashBefore);
		assertThat(row.get("updated_by")).isEqualTo(account.username());
	}

	@Test
	@DisplayName("Given a wrong current password, When changing, Then 400 on the field currentPassword - not a 401, which would end the page")
	void shouldRejectWrongCurrentPassword() throws Exception {
		assertRejected(OLD + "!", NEW, NEW, "currentPassword", "The current password is wrong");
	}

	@Test
	@DisplayName("Given a 7 character new password, When changing, Then 400 on the field newPassword")
	void shouldRejectShortNewPassword() throws Exception {
		assertRejected(OLD, "7 chars", "7 chars", "newPassword", "Password must be at least 8 characters");
	}

	@Test
	@DisplayName("Given 37 Danish letters - 74 bytes - as new password, When changing, Then 400 on the field newPassword")
	void shouldRejectNewPasswordOver72Bytes() throws Exception {
		String password = "æ".repeat(37);
		assertRejected(OLD, password, password, "newPassword", TOO_LONG);
	}

	@Test
	@DisplayName("Given a confirmation that differs, When changing, Then 400 on the field confirmPassword")
	void shouldRejectMismatchedConfirmation() throws Exception {
		assertRejected(OLD, NEW, NEW + "!", "confirmPassword", "The passwords do not match");
	}

	@Test
	@DisplayName("Given no current password, When changing, Then 400 on the field currentPassword")
	void shouldRequireCurrentPassword() throws Exception {
		assertRejected(null, NEW, NEW, "currentPassword", "Current password is required");
	}

	@Test
	@DisplayName("Given a wrong current password, an over-long new one and a mismatch, When changing, Then all three in one 400")
	void shouldReportEveryProblemAtOnce() throws Exception {
		Account account = registerAccount();
		String password = "æ".repeat(37);

		change(account, OLD + "!", password, NEW)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.validationExceptions", hasSize(3)))
				.andExpect(jsonPath("$.validationExceptions[0].objectName").value("currentPassword"))
				.andExpect(jsonPath("$.validationExceptions[1].objectName").value("newPassword"))
				.andExpect(jsonPath("$.validationExceptions[2].objectName").value("confirmPassword"));
	}

	@Test
	@DisplayName("Given no session, When changing a password, Then 401")
	void shouldRequireASession() throws Exception {
		Cookie xsrf = xsrfCookie();
		mockMvc.perform(put(PASSWORD_PATH).cookie(xsrf).header(XSRF_HEADER, xsrf.getValue())
						.contentType(MediaType.APPLICATION_JSON).content(body(OLD, NEW, NEW)))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.errorCode").value(ApplicationErrorCodes.AUTHENTICATION_REQUIRED.getCode()));
	}

	@Test
	@DisplayName("Given no CSRF token, When changing a password, Then 403 and the password unchanged")
	void shouldRejectWithoutCsrfToken() throws Exception {
		Account account = registerAccount();
		String hashBefore = storedHash(account.username());

		mockMvc.perform(put(PASSWORD_PATH).session(account.session())
						.contentType(MediaType.APPLICATION_JSON).content(body(OLD, NEW, NEW)))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.errorCode").value(ApplicationErrorCodes.ACCESS_DENIED.getCode()));

		assertThat(storedHash(account.username())).isEqualTo(hashBefore);
	}

	/** Rejected with the one problem given, and the stored password unchanged. */
	private void assertRejected(String current, String newPassword, String confirm, String field, String message) throws Exception {
		Account account = registerAccount();
		String hashBefore = storedHash(account.username());

		change(account, current, newPassword, confirm)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errorCode").value(ApplicationErrorCodes.VALIDATION_EXCEPTION.getCode()))
				.andExpect(jsonPath("$.validationExceptions", hasSize(1)))
				.andExpect(jsonPath("$.validationExceptions[0].objectName").value(field))
				.andExpect(jsonPath("$.validationExceptions[0].message").value(message));

		assertThat(storedHash(account.username())).isEqualTo(hashBefore);
	}

	private ResultActions change(Account account, String current, String newPassword, String confirm) throws Exception {
		return mockMvc.perform(put(PASSWORD_PATH).session(account.session())
				.cookie(account.xsrf()).header(XSRF_HEADER, account.xsrf().getValue())
				.contentType(MediaType.APPLICATION_JSON).content(body(current, newPassword, confirm)));
	}

	private Account registerAccount() throws Exception {
		String username = "user-" + UUID.randomUUID().toString().substring(0, 8);
		Cookie xsrf = xsrfCookie();
		MockHttpSession session = (MockHttpSession) mockMvc.perform(post(REGISTER).cookie(xsrf).header(XSRF_HEADER, xsrf.getValue())
						.contentType(MediaType.APPLICATION_JSON)
						.content(jsonMapper.writeValueAsString(RegisterRequestDTO.builder()
								.username(username).email(username + "@example.com").password(OLD).confirmPassword(OLD).build())))
				.andExpect(status().isCreated())
				.andReturn().getRequest().getSession(false);
		return new Account(username, session, xsrf);
	}

	private ResultActions login(String username, String password) throws Exception {
		Cookie xsrf = xsrfCookie();
		return mockMvc.perform(post(LOGIN).cookie(xsrf).header(XSRF_HEADER, xsrf.getValue())
				.contentType(MediaType.APPLICATION_JSON)
				.content(jsonMapper.writeValueAsString(LoginRequestDTO.builder().username(username).password(password).build())));
	}

	/** A second session of an existing account, signed in through the login endpoint. */
	private MockHttpSession signIn(String username, String password) throws Exception {
		return (MockHttpSession) login(username, password).andExpect(status().isOk())
				.andReturn().getRequest().getSession(false);
	}

	private String storedHash(String username) {
		return jdbcTemplate.queryForObject("SELECT password FROM app_user WHERE username = ?", String.class, username);
	}

	private Cookie xsrfCookie() throws Exception {
		return mockMvc.perform(get(ME)).andReturn().getResponse().getCookie("XSRF-TOKEN");
	}

	private String body(String current, String newPassword, String confirm) {
		return jsonMapper.writeValueAsString(ChangePasswordRequestDTO.builder()
				.currentPassword(current).newPassword(newPassword).confirmPassword(confirm).build());
	}
}
