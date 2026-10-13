package dk.serik.recipes;

import com.jayway.jsonpath.JsonPath;
import dk.serik.recipes.dto.LoginRequestDTO;
import dk.serik.recipes.dto.RegisterRequestDTO;
import dk.serik.recipes.dto.UpdateUserRequestDTO;
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

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * An admin managing accounts, through the real security chain and database. The admin is
 * {@code AuthenticationIT}'s bootstrapped account; every account changed here is registered by the
 * test that changes it.
 * <p>
 * The properties are {@code AuthenticationIT}'s, so these share its cached context - see
 * {@code RegistrationIT} for why the real {@code XSRF-TOKEN} cookie needs that.
 */
@SpringBootTest(classes = RecipesApplication.class, properties = {
		"app.admin.username=" + AuthenticationIT.USERNAME,
		"app.admin.password=" + AuthenticationIT.PASSWORD
})
@AutoConfigureMockMvc
@AutoConfigureTestDatabase
class UsersIT {

	private static final String USERS = "/api/v1/users";
	private static final String REGISTER = "/api/v1/auth/register";
	private static final String LOGIN = "/api/v1/auth/login";
	private static final String ME = "/api/v1/auth/me";
	private static final String XSRF_HEADER = "X-XSRF-TOKEN";
	private static final String PASSWORD = "a user password";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JsonMapper jsonMapper;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	/** A signed-in session plus the CSRF cookie a browser would hold alongside it. */
	private record SignedIn(String username, MockHttpSession session, Cookie xsrf) {
	}

	@Test
	@DisplayName("Given an admin, When GET the users, Then every account, sorted by username, and no password anywhere")
	void shouldListAccountsForAdmin() throws Exception {
		SignedIn user = registerAccount();
		String byUsername = "$[?(@.username == '" + user.username() + "')]";

		String body = mockMvc.perform(get(USERS).session(admin().session()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[*].username", hasItem(AuthenticationIT.USERNAME)))
				.andExpect(jsonPath(byUsername + ".roles[*]", contains("ROLE_USER")))
				.andExpect(jsonPath(byUsername + ".enabled", contains(true)))
				.andExpect(jsonPath(byUsername + ".createdBy", contains(user.username())))
				.andExpect(content().string(not(containsString("password"))))
				.andExpect(content().string(not(containsString("{bcrypt}"))))
				.andReturn().getResponse().getContentAsString();

		List<String> usernames = JsonPath.read(body, "$[*].username");
		assertThat(usernames).isSorted();
	}

	@Test
	@DisplayName("Given a user, When GET or PUT the users, Then 403 and nothing changes")
	void shouldRefuseUser() throws Exception {
		SignedIn user = registerAccount();
		SignedIn other = registerAccount();

		mockMvc.perform(get(USERS).session(user.session()))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.errorCode").value(ApplicationErrorCodes.ACCESS_DENIED.getCode()));
		update(user, idOf(other.username()), false, true)
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.errorCode").value(ApplicationErrorCodes.ACCESS_DENIED.getCode()));

		assertThat(row(other.username())).containsEntry("enabled", true).containsEntry("roles", "ROLE_USER");
	}

	@Test
	@DisplayName("Given no session, When GET the users, Then 401")
	void shouldRequireASession() throws Exception {
		mockMvc.perform(get(USERS))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.errorCode").value(ApplicationErrorCodes.AUTHENTICATION_REQUIRED.getCode()));
	}

	@Test
	@DisplayName("Given an admin, When disabling an account, Then its session ends and it signs in no more - with a wrong password's 401")
	void shouldDisableAccount() throws Exception {
		SignedIn user = registerAccount();

		update(admin(), idOf(user.username()), false, false).andExpect(status().isNoContent());

		mockMvc.perform(get(ME).session(user.session())).andExpect(status().isUnauthorized());
		login(user.username(), PASSWORD)
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.errorCode").value(ApplicationErrorCodes.BAD_CREDENTIALS.getCode()))
				.andExpect(jsonPath("$.message").value("Invalid username or password"));
		assertThat(row(user.username())).containsEntry("enabled", false)
				.containsEntry("updated_by", AuthenticationIT.USERNAME);
	}

	@Test
	@DisplayName("Given a disabled account, When an admin enables it again, Then it signs in")
	void shouldEnableAccountAgain() throws Exception {
		SignedIn admin = admin();
		SignedIn user = registerAccount();
		String id = idOf(user.username());
		update(admin, id, false, false).andExpect(status().isNoContent());

		update(admin, id, true, false).andExpect(status().isNoContent());

		login(user.username(), PASSWORD).andExpect(status().isOk());
	}

	@Test
	@DisplayName("Given a user, When an admin makes it an admin, Then its session ends, and signed in again it is an admin")
	void shouldGrantAdmin() throws Exception {
		SignedIn user = registerAccount();

		update(admin(), idOf(user.username()), true, true).andExpect(status().isNoContent());

		// The old session carried ROLE_USER alone; it does not live on with it.
		mockMvc.perform(get(ME).session(user.session())).andExpect(status().isUnauthorized());
		MockHttpSession again = signIn(user.username());
		mockMvc.perform(get(ME).session(again))
				.andExpect(jsonPath("$.roles", contains("ROLE_ADMIN", "ROLE_USER")));
		mockMvc.perform(get(USERS).session(again)).andExpect(status().isOk());
		assertThat(row(user.username())).containsEntry("roles", "ROLE_USER,ROLE_ADMIN");
	}

	@Test
	@DisplayName("Given an admin, When another admin withdraws the role, Then signed in again it is a user and refused the users")
	void shouldWithdrawAdmin() throws Exception {
		SignedIn admin = admin();
		SignedIn user = registerAccount();
		String id = idOf(user.username());
		update(admin, id, true, true).andExpect(status().isNoContent());

		update(admin, id, true, false).andExpect(status().isNoContent());

		MockHttpSession again = signIn(user.username());
		mockMvc.perform(get(ME).session(again)).andExpect(jsonPath("$.roles", contains("ROLE_USER")));
		mockMvc.perform(get(USERS).session(again)).andExpect(status().isForbidden());
	}

	@Test
	@DisplayName("Given an admin, When changing their own account, Then 400 and nothing changes")
	void shouldRefuseOwnAccount() throws Exception {
		SignedIn admin = admin();
		Map<String, Object> before = row(AuthenticationIT.USERNAME);

		update(admin, idOf(AuthenticationIT.USERNAME), false, false)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errorCode").value(ApplicationErrorCodes.OWN_ACCOUNT.getCode()))
				.andExpect(jsonPath("$.message").value("You cannot change your own account"));

		assertThat(row(AuthenticationIT.USERNAME)).isEqualTo(before);
		mockMvc.perform(get(ME).session(admin.session())).andExpect(status().isOk());
	}

	@Test
	@DisplayName("Given a request that changes nothing, When PUT, Then 204, nothing written and the account's session lives on")
	void shouldKeepSessionWhenNothingChanges() throws Exception {
		SignedIn user = registerAccount();

		update(admin(), idOf(user.username()), true, false).andExpect(status().isNoContent());

		mockMvc.perform(get(ME).session(user.session())).andExpect(status().isOk());
		assertThat(row(user.username()).get("updated_by")).isNull();
	}

	@Test
	@DisplayName("Given an unknown id, When PUT, Then 404")
	void shouldAnswerNotFound() throws Exception {
		update(admin(), UUID.randomUUID().toString(), false, false)
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.errorCode").value(ApplicationErrorCodes.USER_NOT_FOUND.getCode()));
	}

	@Test
	@DisplayName("Given a body without admin, When PUT, Then 400 on the field admin and nothing changes")
	void shouldRequireBothFields() throws Exception {
		SignedIn admin = admin();
		SignedIn user = registerAccount();

		mockMvc.perform(put(USERS + "/" + idOf(user.username())).session(admin.session())
						.cookie(admin.xsrf()).header(XSRF_HEADER, admin.xsrf().getValue())
						.contentType(MediaType.APPLICATION_JSON).content("{\"enabled\": false}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.validationExceptions[0].objectName").value("admin"))
				.andExpect(jsonPath("$.validationExceptions[0].message").value("Admin is required"));

		assertThat(row(user.username())).containsEntry("enabled", true);
	}

	private ResultActions update(SignedIn by, String id, boolean enabled, boolean admin) throws Exception {
		return mockMvc.perform(put(USERS + "/" + id).session(by.session())
				.cookie(by.xsrf()).header(XSRF_HEADER, by.xsrf().getValue())
				.contentType(MediaType.APPLICATION_JSON)
				.content(jsonMapper.writeValueAsString(UpdateUserRequestDTO.builder().enabled(enabled).admin(admin).build())));
	}

	private SignedIn admin() throws Exception {
		Cookie xsrf = xsrfCookie();
		MockHttpSession session = (MockHttpSession) login(AuthenticationIT.USERNAME, AuthenticationIT.PASSWORD, xsrf)
				.andExpect(status().isOk())
				.andReturn().getRequest().getSession(false);
		return new SignedIn(AuthenticationIT.USERNAME, session, xsrf);
	}

	private SignedIn registerAccount() throws Exception {
		String username = "user-" + UUID.randomUUID().toString().substring(0, 8);
		Cookie xsrf = xsrfCookie();
		MockHttpSession session = (MockHttpSession) mockMvc.perform(post(REGISTER).cookie(xsrf).header(XSRF_HEADER, xsrf.getValue())
						.contentType(MediaType.APPLICATION_JSON)
						.content(jsonMapper.writeValueAsString(RegisterRequestDTO.builder()
								.username(username).email(username + "@example.com").password(PASSWORD).confirmPassword(PASSWORD).build())))
				.andExpect(status().isCreated())
				.andReturn().getRequest().getSession(false);
		return new SignedIn(username, session, xsrf);
	}

	private MockHttpSession signIn(String username) throws Exception {
		return (MockHttpSession) login(username, PASSWORD).andExpect(status().isOk())
				.andReturn().getRequest().getSession(false);
	}

	private ResultActions login(String username, String password) throws Exception {
		return login(username, password, xsrfCookie());
	}

	private ResultActions login(String username, String password, Cookie xsrf) throws Exception {
		return mockMvc.perform(post(LOGIN).cookie(xsrf).header(XSRF_HEADER, xsrf.getValue())
				.contentType(MediaType.APPLICATION_JSON)
				.content(jsonMapper.writeValueAsString(LoginRequestDTO.builder().username(username).password(password).build())));
	}

	private String idOf(String username) {
		return jdbcTemplate.queryForObject("SELECT id FROM app_user WHERE username = ?", String.class, username);
	}

	private Map<String, Object> row(String username) {
		return jdbcTemplate.queryForMap("SELECT enabled, roles, updated_by FROM app_user WHERE username = ?", username);
	}

	private Cookie xsrfCookie() throws Exception {
		return mockMvc.perform(get(ME)).andReturn().getResponse().getCookie("XSRF-TOKEN");
	}
}
