package dk.serik.recipes;

import dk.serik.recipes.dto.ChangeEmailRequestDTO;
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
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Changing one's own email address, through the real security chain and database. Each test
 * registers an account of its own and changes that one's address, never the admin's.
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
class EmailChangeIT {

	private static final String EMAIL_PATH = "/api/v1/auth/email";
	private static final String REGISTER = "/api/v1/auth/register";
	private static final String LOGIN = "/api/v1/auth/login";
	private static final String ME = "/api/v1/auth/me";
	private static final String XSRF_HEADER = "X-XSRF-TOKEN";
	private static final String PASSWORD = "long enough";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JsonMapper jsonMapper;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	/** A newly registered account, signed in, with the address it registered. */
	private record Account(String username, String email, MockHttpSession session, Cookie xsrf) {
	}

	@Test
	@DisplayName("Given a signed-in user, When changing the address, Then 204, and auth/me reports the new one")
	void shouldChangeEmail() throws Exception {
		Account account = registerAccount();
		String email = uniqueEmail();

		change(account, PASSWORD, email).andExpect(status().isNoContent());

		mockMvc.perform(get(ME).session(account.session()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value(email));
	}

	@Test
	@DisplayName("Given a change, When the row is read, Then the new address, and updated by the user")
	void shouldStoreTheNewAddress() throws Exception {
		Account account = registerAccount();
		String email = uniqueEmail();

		change(account, PASSWORD, email).andExpect(status().isNoContent());

		Map<String, Object> row = jdbcTemplate.queryForMap(
				"SELECT email, updated_by FROM app_user WHERE username = ?", account.username());
		assertThat(row.get("email")).isEqualTo(email);
		assertThat(row.get("updated_by")).isEqualTo(account.username());
	}

	@Test
	@DisplayName("Given a change, When signing in, Then the login answer carries the new address too")
	void shouldReportTheAddressAtLogin() throws Exception {
		Account account = registerAccount();
		String email = uniqueEmail();
		change(account, PASSWORD, email).andExpect(status().isNoContent());

		login(account.username(), PASSWORD)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value(email));
	}

	@Test
	@DisplayName("Given a second session of the account, When the address is changed, Then both stay signed in - an address signs no one in")
	void shouldKeepEverySession() throws Exception {
		Account account = registerAccount();
		MockHttpSession other = (MockHttpSession) login(account.username(), PASSWORD).andExpect(status().isOk())
				.andReturn().getRequest().getSession(false);
		String idBefore = account.session().getId();

		change(account, PASSWORD, uniqueEmail()).andExpect(status().isNoContent());

		assertThat(account.session().getId()).isEqualTo(idBefore);
		mockMvc.perform(get(ME).session(account.session())).andExpect(status().isOk());
		mockMvc.perform(get(ME).session(other)).andExpect(status().isOk());
	}

	@Test
	@DisplayName("Given the account's own address in another case, When changing to it, Then 204 - it is not taken by itself")
	void shouldAcceptOwnAddressInAnotherCase() throws Exception {
		Account account = registerAccount();
		String upper = account.email().toUpperCase();

		change(account, PASSWORD, upper).andExpect(status().isNoContent());

		assertThat(storedEmail(account.username())).isEqualTo(upper);
	}

	@Test
	@DisplayName("Given another account's address, in another case, When changing to it, Then 409 on the field email and the address unchanged")
	void shouldRejectAnotherAccountsAddress() throws Exception {
		Account account = registerAccount();
		String taken = registerAccount().email().toUpperCase();

		change(account, PASSWORD, taken)
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.errorCode").value(ApplicationErrorCodes.EMAIL_TAKEN.getCode()))
				.andExpect(jsonPath("$.validationExceptions[0].objectName").value("email"))
				.andExpect(jsonPath("$.validationExceptions[0].message").value("The email address '" + taken + "' is taken"));

		assertThat(storedEmail(account.username())).isEqualTo(account.email());
	}

	@Test
	@DisplayName("Given a wrong current password, When changing, Then 400 on the field currentPassword - not a 401, which would end the page")
	void shouldRejectWrongCurrentPassword() throws Exception {
		assertRejected(PASSWORD + "!", uniqueEmail(), "currentPassword", "The current password is wrong");
	}

	@Test
	@DisplayName("Given a wrong current password and a taken address, When changing, Then only the 400 - whether an address is taken is not told")
	void shouldNotTellATakenAddressWithoutThePassword() throws Exception {
		assertRejected(PASSWORD + "!", registerAccount().email(), "currentPassword", "The current password is wrong");
	}

	@Test
	@DisplayName("Given no current password, When changing, Then 400 on the field currentPassword")
	void shouldRequireCurrentPassword() throws Exception {
		assertRejected(null, uniqueEmail(), "currentPassword", "Current password is required");
	}

	@ParameterizedTest(name = "\"{0}\" -> {1}")
	@CsvSource(delimiter = '|', value = {
			"''                    | Email address is required",
			"not-an-address        | Email address is not valid",
	})
	@DisplayName("Given an address breaking a rule, When changing, Then 400 on the field email")
	void shouldRejectInvalidEmail(String email, String message) throws Exception {
		assertRejected(PASSWORD, email, "email", message);
	}

	@Test
	@DisplayName("Given no address, When changing, Then 400 on the field email")
	void shouldRequireEmail() throws Exception {
		assertRejected(PASSWORD, null, "email", "Email address is required");
	}

	@Test
	@DisplayName("Given no session, When changing an address, Then 401")
	void shouldRequireASession() throws Exception {
		Cookie xsrf = xsrfCookie();
		mockMvc.perform(put(EMAIL_PATH).cookie(xsrf).header(XSRF_HEADER, xsrf.getValue())
						.contentType(MediaType.APPLICATION_JSON).content(body(PASSWORD, uniqueEmail())))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.errorCode").value(ApplicationErrorCodes.AUTHENTICATION_REQUIRED.getCode()));
	}

	@Test
	@DisplayName("Given no CSRF token, When changing an address, Then 403 and the address unchanged")
	void shouldRejectWithoutCsrfToken() throws Exception {
		Account account = registerAccount();

		mockMvc.perform(put(EMAIL_PATH).session(account.session())
						.contentType(MediaType.APPLICATION_JSON).content(body(PASSWORD, uniqueEmail())))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.errorCode").value(ApplicationErrorCodes.ACCESS_DENIED.getCode()));

		assertThat(storedEmail(account.username())).isEqualTo(account.email());
	}

	/** Rejected with the one problem given, and the stored address unchanged. */
	private void assertRejected(String current, String email, String field, String message) throws Exception {
		Account account = registerAccount();

		change(account, current, email)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errorCode").value(ApplicationErrorCodes.VALIDATION_EXCEPTION.getCode()))
				.andExpect(jsonPath("$.validationExceptions", hasSize(1)))
				.andExpect(jsonPath("$.validationExceptions[0].objectName").value(field))
				.andExpect(jsonPath("$.validationExceptions[0].message").value(message));

		assertThat(storedEmail(account.username())).isEqualTo(account.email());
	}

	private ResultActions change(Account account, String current, String email) throws Exception {
		return mockMvc.perform(put(EMAIL_PATH).session(account.session())
				.cookie(account.xsrf()).header(XSRF_HEADER, account.xsrf().getValue())
				.contentType(MediaType.APPLICATION_JSON).content(body(current, email)));
	}

	private Account registerAccount() throws Exception {
		String username = "user-" + UUID.randomUUID().toString().substring(0, 8);
		String email = username + "@example.com";
		Cookie xsrf = xsrfCookie();
		MockHttpSession session = (MockHttpSession) mockMvc.perform(post(REGISTER).cookie(xsrf).header(XSRF_HEADER, xsrf.getValue())
						.contentType(MediaType.APPLICATION_JSON)
						.content(jsonMapper.writeValueAsString(RegisterRequestDTO.builder()
								.username(username).email(email).password(PASSWORD).confirmPassword(PASSWORD).build())))
				.andExpect(status().isCreated())
				.andReturn().getRequest().getSession(false);
		return new Account(username, email, session, xsrf);
	}

	private ResultActions login(String username, String password) throws Exception {
		Cookie xsrf = xsrfCookie();
		return mockMvc.perform(post(LOGIN).cookie(xsrf).header(XSRF_HEADER, xsrf.getValue())
				.contentType(MediaType.APPLICATION_JSON)
				.content(jsonMapper.writeValueAsString(LoginRequestDTO.builder().username(username).password(password).build())));
	}

	private String storedEmail(String username) {
		return jdbcTemplate.queryForObject("SELECT email FROM app_user WHERE username = ?", String.class, username);
	}

	private Cookie xsrfCookie() throws Exception {
		return mockMvc.perform(get(ME)).andReturn().getResponse().getCookie("XSRF-TOKEN");
	}

	private String body(String current, String email) {
		return jsonMapper.writeValueAsString(ChangeEmailRequestDTO.builder().currentPassword(current).email(email).build());
	}

	private static String uniqueEmail() {
		return "new-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
	}
}
