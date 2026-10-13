package dk.serik.recipes;

import dk.serik.recipes.dto.CategoryDTO;
import dk.serik.recipes.dto.LoginRequestDTO;
import dk.serik.recipes.dto.RegisterRequestDTO;
import dk.serik.recipes.exceptions.ApplicationErrorCodes;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Registration through the real security chain and database, as {@code AuthenticationIT} does sign
 * in: the CSRF token comes from the {@code XSRF-TOKEN} cookie of an earlier response, and nothing
 * stubs {@code Session}. Every test registers a username of its own.
 * <p>
 * The properties are {@code AuthenticationIT}'s, so the two share one cached context. Not for the
 * admin account: the other {@code *IT} classes use MockMvc's {@code csrf()}, which replaces the
 * CSRF token repository inside the context's filter with a test one that sets no cookie, for every
 * later test in that context. Run there, every test here found no {@code XSRF-TOKEN} cookie.
 */
@SpringBootTest(classes = RecipesApplication.class, properties = {
		"app.admin.username=" + AuthenticationIT.USERNAME,
		"app.admin.password=" + AuthenticationIT.PASSWORD
})
@AutoConfigureMockMvc
@AutoConfigureTestDatabase
class RegistrationIT {

	private static final String REGISTER = "/api/v1/auth/register";
	private static final String LOGIN = "/api/v1/auth/login";
	private static final String LOGOUT = "/api/v1/auth/logout";
	private static final String ME = "/api/v1/auth/me";
	private static final String XSRF_HEADER = "X-XSRF-TOKEN";
	private static final String PASSWORD = "long enough";
	private static final String TOO_LONG = "Password is too long: at most 72 characters, fewer with letters such as æ, ø and å";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JsonMapper jsonMapper;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	@DisplayName("Given a new username, When registering, Then 201, signed in as the new user with its address and ROLE_USER only")
	void shouldRegisterAndSignIn() throws Exception {
		String username = uniqueUsername();
		String email = uniqueEmail();
		MvcResult result = register(username, email, PASSWORD, PASSWORD)
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.username").value(username))
				.andExpect(jsonPath("$.email").value(email))
				.andExpect(jsonPath("$.roles", contains("ROLE_USER")))
				.andReturn();

		mockMvc.perform(get(ME).session((MockHttpSession) result.getRequest().getSession(false)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.username").value(username))
				.andExpect(jsonPath("$.email").value(email));
	}

	@Test
	@DisplayName("Given a session from before registering, When registering, Then the session gets a new id")
	void shouldChangeTheSessionIdOnRegister() throws Exception {
		MvcResult anonymous = mockMvc.perform(get(ME)).andReturn();
		MockHttpSession session = (MockHttpSession) anonymous.getRequest().getSession(false);
		Cookie xsrf = anonymous.getResponse().getCookie("XSRF-TOKEN");
		String idBefore = session.getId();

		mockMvc.perform(post(REGISTER).session(session).cookie(xsrf).header(XSRF_HEADER, xsrf.getValue())
						.contentType(MediaType.APPLICATION_JSON).content(body(uniqueUsername(), PASSWORD, PASSWORD)))
				.andExpect(status().isCreated());

		assertThat(session.getId()).isNotEqualTo(idBefore);
	}

	@Test
	@DisplayName("Given a registration, When the row is read, Then the address, a bcrypt hash, ROLE_USER, enabled, and created by the new user")
	void shouldStoreTheAccount() throws Exception {
		String username = uniqueUsername();
		register(username, username + "@example.com", PASSWORD, PASSWORD).andExpect(status().isCreated());

		Map<String, Object> row = jdbcTemplate.queryForMap(
				"SELECT email, password, roles, enabled, created_by FROM app_user WHERE username = ?", username);
		assertThat(row.get("email")).isEqualTo(username + "@example.com");
		assertThat((String) row.get("password")).startsWith("{bcrypt}").doesNotContain(PASSWORD);
		assertThat(row.get("roles")).isEqualTo("ROLE_USER");
		assertThat(row.get("enabled")).isEqualTo(true);
		// The request is anonymous: through JPA, BaseEntityListener would have written "anonymous".
		assertThat(row.get("created_by")).isEqualTo(username);
	}

	@Test
	@DisplayName("Given a registered user, When they POST a category, Then it is created by them, read back fresh")
	void shouldStampWritesWithTheNewUser() throws Exception {
		String username = uniqueUsername();
		Cookie xsrf = xsrfCookie();
		MvcResult registered = mockMvc.perform(post(REGISTER).cookie(xsrf).header(XSRF_HEADER, xsrf.getValue())
						.contentType(MediaType.APPLICATION_JSON).content(body(username, PASSWORD, PASSWORD)))
				.andExpect(status().isCreated())
				.andReturn();
		MockHttpSession session = (MockHttpSession) registered.getRequest().getSession(false);

		String location = mockMvc.perform(post("/api/v1/categories").session(session).cookie(xsrf).header(XSRF_HEADER, xsrf.getValue())
						.contentType(MediaType.APPLICATION_JSON)
						.content(jsonMapper.writeValueAsString(CategoryDTO.builder().name("Category of " + username).build())))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getHeader("Location");

		mockMvc.perform(get(location).session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.createdBy").value(username));
	}

	@Test
	@DisplayName("Given a registered user who signed out, When they sign in again, Then 200")
	void shouldSignInAgainAfterSigningOut() throws Exception {
		String username = uniqueUsername();
		Cookie xsrf = xsrfCookie();
		MvcResult registered = mockMvc.perform(post(REGISTER).cookie(xsrf).header(XSRF_HEADER, xsrf.getValue())
						.contentType(MediaType.APPLICATION_JSON).content(body(username, PASSWORD, PASSWORD)))
				.andExpect(status().isCreated())
				.andReturn();
		mockMvc.perform(post(LOGOUT).session((MockHttpSession) registered.getRequest().getSession(false))
						.cookie(xsrf).header(XSRF_HEADER, xsrf.getValue()))
				.andExpect(status().isNoContent());

		Cookie fresh = xsrfCookie();
		mockMvc.perform(post(LOGIN).cookie(fresh).header(XSRF_HEADER, fresh.getValue())
						.contentType(MediaType.APPLICATION_JSON)
						.content(jsonMapper.writeValueAsString(LoginRequestDTO.builder().username(username).password(PASSWORD).build())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.username").value(username));
	}

	@Test
	@DisplayName("Given a taken username, When registering it again, Then 409 on the field username")
	void shouldRejectTakenUsername() throws Exception {
		String username = uniqueUsername();
		register(username, PASSWORD, PASSWORD).andExpect(status().isCreated());

		assertTaken(username);
	}

	@Test
	@DisplayName("Given a taken username, When registering it in other case, Then the same 409 - not left to the collation")
	void shouldRejectTakenUsernameInAnotherCase() throws Exception {
		// H2, unlike MySQL's collation, tells the two apart: only the service's own check can refuse this.
		String username = uniqueUsername();
		register(username, PASSWORD, PASSWORD).andExpect(status().isCreated());

		assertTaken(username.toUpperCase());
	}

	@Test
	@DisplayName("Given a taken email address, When another username registers with it, Then 409 on the field email and no account")
	void shouldRejectTakenEmail() throws Exception {
		String email = uniqueEmail();
		register(uniqueUsername(), email, PASSWORD, PASSWORD).andExpect(status().isCreated());

		assertEmailTaken(email);
	}

	@Test
	@DisplayName("Given a taken email address, When registering it in other case, Then the same 409 - not left to the collation")
	void shouldRejectTakenEmailInAnotherCase() throws Exception {
		String email = uniqueEmail();
		register(uniqueUsername(), email, PASSWORD, PASSWORD).andExpect(status().isCreated());

		assertEmailTaken(email.toUpperCase());
	}

	@ParameterizedTest(name = "\"{0}\" -> {1}")
	@CsvSource(delimiter = '|', value = {
			"''                    | Email address is required",
			"not-an-address        | Email address is not valid",
			"two@at@example.com    | Email address is not valid",
	})
	@DisplayName("Given an email address breaking a rule, When registering, Then 400 on the field email")
	void shouldRejectInvalidEmail(String email, String message) throws Exception {
		assertRejected(uniqueUsername(), email, PASSWORD, PASSWORD, "email", message);
	}

	@Test
	@DisplayName("Given no email address, When registering, Then 400 on the field email")
	void shouldRequireEmail() throws Exception {
		assertRejected(uniqueUsername(), null, PASSWORD, PASSWORD, "email", "Email address is required");
	}

	@Test
	@DisplayName("Given an email address of 254 characters, When registering, Then 201 - the limit is inclusive")
	void shouldAcceptEmailOf254Characters() throws Exception {
		register(uniqueUsername(), emailOfLength(254), PASSWORD, PASSWORD).andExpect(status().isCreated());
	}

	@Test
	@DisplayName("Given a valid email address of 255 characters, When registering, Then 400 on the field email")
	void shouldRejectEmailOver254Characters() throws Exception {
		assertRejected(uniqueUsername(), emailOfLength(255), PASSWORD, PASSWORD, "email",
				"Email address must be at most 254 characters");
	}

	@ParameterizedTest(name = "\"{0}\" -> {1}")
	@CsvSource(delimiter = '|', value = {
			"ab                    | Username must be 3 to 64 characters",
			"has space             | Username may contain only letters, digits, '.', '_' and '-'",
			"semi;colon            | Username may contain only letters, digits, '.', '_' and '-'",
	})
	@DisplayName("Given a username breaking a rule, When registering, Then 400 on the field username")
	void shouldRejectInvalidUsername(String username, String message) throws Exception {
		assertRejected(username, PASSWORD, PASSWORD, "username", message);
	}

	@Test
	@DisplayName("Given a 65 character username, When registering, Then 400 on the field username")
	void shouldRejectTooLongUsername() throws Exception {
		assertRejected("u".repeat(65), PASSWORD, PASSWORD, "username", "Username must be 3 to 64 characters");
	}

	@Test
	@DisplayName("Given a username with Danish letters, When registering, Then 201 - letters are not only a-z")
	void shouldAcceptDanishLetters() throws Exception {
		register("Søren-" + UUID.randomUUID().toString().substring(0, 8), PASSWORD, PASSWORD)
				.andExpect(status().isCreated());
	}

	@Test
	@DisplayName("Given a 7 character password, When registering, Then 400 on the field password")
	void shouldRejectShortPassword() throws Exception {
		assertRejected(uniqueUsername(), "7 chars", "7 chars", "password", "Password must be at least 8 characters");
	}

	@Test
	@DisplayName("Given a password of exactly 72 bytes, When registering, Then 201 - the limit is inclusive")
	void shouldAcceptPasswordOf72Bytes() throws Exception {
		String password = "p".repeat(72);
		register(uniqueUsername(), password, password).andExpect(status().isCreated());
	}

	@Test
	@DisplayName("Given a password over 72 bytes, When registering, Then 400 on the field password, not the encoder's 500")
	void shouldRejectPasswordOver72Bytes() throws Exception {
		String password = "p".repeat(73);
		assertRejected(uniqueUsername(), password, password, "password", TOO_LONG);
	}

	@Test
	@DisplayName("Given 37 Danish letters - 74 bytes - as password, When registering, Then 400: bytes count, not characters")
	void shouldCountBytesNotCharacters() throws Exception {
		String password = "æ".repeat(37);
		assertRejected(uniqueUsername(), password, password, "password", TOO_LONG);
	}

	@Test
	@DisplayName("Given a confirmation that differs, When registering, Then 400 on the field confirmPassword and no account")
	void shouldRejectMismatchedConfirmation() throws Exception {
		String username = uniqueUsername();
		assertRejected(username, PASSWORD, PASSWORD + "!", "confirmPassword", "The passwords do not match");

		assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM app_user WHERE username = ?", Integer.class, username))
				.isZero();
	}

	@Test
	@DisplayName("Given no CSRF token, When registering, Then 403")
	void shouldRejectRegisterWithoutCsrfToken() throws Exception {
		mockMvc.perform(post(REGISTER).contentType(MediaType.APPLICATION_JSON).content(body(uniqueUsername(), PASSWORD, PASSWORD)))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.errorCode").value(ApplicationErrorCodes.ACCESS_DENIED.getCode()));
	}

	private void assertEmailTaken(String email) throws Exception {
		String username = uniqueUsername();
		register(username, email, PASSWORD, PASSWORD)
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.errorCode").value(ApplicationErrorCodes.EMAIL_TAKEN.getCode()))
				.andExpect(jsonPath("$.validationExceptions[0].objectName").value("email"))
				.andExpect(jsonPath("$.validationExceptions[0].message").value("The email address '" + email + "' is taken"));
		assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM app_user WHERE username = ?", Integer.class, username))
				.isZero();
	}

	private void assertTaken(String username) throws Exception {
		register(username, PASSWORD, PASSWORD)
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.errorCode").value(ApplicationErrorCodes.USERNAME_TAKEN.getCode()))
				.andExpect(jsonPath("$.validationExceptions[0].objectName").value("username"))
				.andExpect(jsonPath("$.validationExceptions[0].message").value("The username '" + username + "' is taken"));
	}

	private void assertRejected(String username, String password, String confirm, String field, String message) throws Exception {
		assertRejected(username, uniqueEmail(), password, confirm, field, message);
	}

	private void assertRejected(String username, String email, String password, String confirm, String field, String message) throws Exception {
		register(username, email, password, confirm)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errorCode").value(ApplicationErrorCodes.VALIDATION_EXCEPTION.getCode()))
				.andExpect(jsonPath("$.validationExceptions[0].objectName").value(field))
				.andExpect(jsonPath("$.validationExceptions[0].message").value(message));
	}

	private ResultActions register(String username, String password, String confirm) throws Exception {
		return register(username, uniqueEmail(), password, confirm);
	}

	private ResultActions register(String username, String email, String password, String confirm) throws Exception {
		Cookie xsrf = xsrfCookie();
		return mockMvc.perform(post(REGISTER).cookie(xsrf).header(XSRF_HEADER, xsrf.getValue())
				.contentType(MediaType.APPLICATION_JSON).content(body(username, email, password, confirm)));
	}

	private Cookie xsrfCookie() throws Exception {
		return mockMvc.perform(get(ME)).andReturn().getResponse().getCookie("XSRF-TOKEN");
	}

	private String body(String username, String password, String confirm) {
		return body(username, uniqueEmail(), password, confirm);
	}

	private String body(String username, String email, String password, String confirm) {
		return jsonMapper.writeValueAsString(RegisterRequestDTO.builder()
				.username(username).email(email).password(password).confirmPassword(confirm).build());
	}

	/**
	 * A unique address valid in all but length: a local part of 64 characters, the most allowed, and
	 * domain labels of at most 63 - so a longer one breaks only the 254 character limit.
	 */
	private static String emailOfLength(int length) {
		String local = uniqueUsername();
		local += "x".repeat(64 - local.length());
		return local + "@" + "a".repeat(63) + "." + "b".repeat(63) + "." + "c".repeat(length - 64 - 1 - 64 - 64 - 3) + ".dk";
	}

	private static String uniqueUsername() {
		return "user-" + UUID.randomUUID().toString().substring(0, 8);
	}

	/** An address of its own, not made from the username: a test of a bad username keeps a good address. */
	private static String uniqueEmail() {
		return uniqueUsername() + "@example.com";
	}
}
