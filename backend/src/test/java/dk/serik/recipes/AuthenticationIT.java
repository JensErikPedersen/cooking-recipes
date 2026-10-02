package dk.serik.recipes;

import dk.serik.recipes.dto.CategoryDTO;
import dk.serik.recipes.dto.LoginRequestDTO;
import dk.serik.recipes.exceptions.ApplicationErrorCodes;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The security filter chain against the real application context - the only place it runs, since
 * the controller slice tests switch filters off. Nothing here stubs {@code Session}.
 * <p>
 * The CSRF token is obtained the way a browser obtains it: from the {@code XSRF-TOKEN} cookie of an
 * earlier response, sent back as the {@code X-XSRF-TOKEN} header. MockMvc's {@code csrf()} helper
 * would write a token straight into the repository and hide a cookie that never reaches the client.
 * <p>
 * The account comes from {@code AdminBootstrap}, fed by the properties below, so there is no
 * password hash in the repository and the bootstrap is covered as a side effect.
 */
@SpringBootTest(classes = RecipesApplication.class, properties = {
		"app.admin.username=" + AuthenticationIT.USERNAME,
		"app.admin.password=" + AuthenticationIT.PASSWORD
})
@AutoConfigureMockMvc
@AutoConfigureTestDatabase
class AuthenticationIT {

	static final String USERNAME = "it-admin";
	static final String PASSWORD = "it-password-2026";

	private static final String CATEGORIES = "/api/v1/categories";
	private static final String LOGIN = "/api/v1/auth/login";
	private static final String LOGOUT = "/api/v1/auth/logout";
	private static final String ME = "/api/v1/auth/me";
	private static final String XSRF_HEADER = "X-XSRF-TOKEN";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JsonMapper jsonMapper;

	/** A signed-in session plus the CSRF cookie a browser would hold alongside it. */
	private record SignedIn(MockHttpSession session, Cookie xsrf) {
	}

	@Test
	@DisplayName("Given no session, When GET a protected resource, Then 401 in the error envelope")
	void shouldRejectAnonymousRequest() throws Exception {
		mockMvc.perform(get(CATEGORIES))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.errorCode").value(ApplicationErrorCodes.AUTHENTICATION_REQUIRED.getCode()));
	}

	@Test
	@DisplayName("Given no session, When GET a protected resource, Then the XSRF-TOKEN cookie is issued anyway")
	void shouldIssueCsrfCookieToAnonymousCaller() throws Exception {
		// Without this the login POST, itself CSRF-protected, could never succeed.
		assertThat(xsrfCookie()).as("XSRF-TOKEN cookie").isNotNull();
	}

	@Test
	@DisplayName("Given no session, When GET the health endpoint, Then 200 - Compose polls it unauthenticated")
	void shouldLeaveHealthOpen() throws Exception {
		mockMvc.perform(get("/actuator/health"))
				.andExpect(status().isOk());
	}

	@Test
	@DisplayName("Given no CSRF token, When POST login, Then 403")
	void shouldRejectLoginWithoutCsrfToken() throws Exception {
		mockMvc.perform(post(LOGIN).contentType(MediaType.APPLICATION_JSON).content(loginBody(USERNAME, PASSWORD)))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.errorCode").value(ApplicationErrorCodes.ACCESS_DENIED.getCode()));
	}

	@Test
	@DisplayName("Given a wrong password, When POST login, Then 401 without saying which half was wrong")
	void shouldRejectBadCredentials() throws Exception {
		assertBadCredentials(USERNAME, "not-the-password");
	}

	@Test
	@DisplayName("Given an unknown user, When POST login, Then the same 401 as a wrong password")
	void shouldNotRevealWhetherTheUserExists() throws Exception {
		assertBadCredentials("nobody-at-all", PASSWORD);
	}

	@Test
	@DisplayName("Given the bootstrapped account, When POST login, Then 200 with the username and only ROLE_ roles")
	void shouldLogIn() throws Exception {
		Cookie xsrf = xsrfCookie();
		mockMvc.perform(post(LOGIN).cookie(xsrf).header(XSRF_HEADER, xsrf.getValue())
						.contentType(MediaType.APPLICATION_JSON).content(loginBody(USERNAME, PASSWORD)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.username").value(USERNAME))
				// Spring Security 7 also grants FACTOR_PASSWORD; it must not reach the frontend.
				.andExpect(jsonPath("$.roles", contains("ROLE_ADMIN", "ROLE_USER")));
	}

	@Test
	@DisplayName("Given a session from before sign-in, When POST login, Then the session gets a new id")
	void shouldChangeTheSessionIdOnLogin() throws Exception {
		// Session fixation: an anonymous 401 already creates a session, and an id someone planted
		// before sign-in must not be the one that ends up signed in.
		MvcResult anonymous = mockMvc.perform(get(CATEGORIES)).andReturn();
		MockHttpSession session = (MockHttpSession) anonymous.getRequest().getSession(false);
		Cookie xsrf = anonymous.getResponse().getCookie("XSRF-TOKEN");
		String idBeforeLogin = session.getId();

		mockMvc.perform(post(LOGIN).session(session).cookie(xsrf).header(XSRF_HEADER, xsrf.getValue())
						.contentType(MediaType.APPLICATION_JSON).content(loginBody(USERNAME, PASSWORD)))
				.andExpect(status().isOk());

		assertThat(session.getId()).isNotEqualTo(idBeforeLogin);
	}

	@Test
	@DisplayName("Given a signed-in session, When GET a protected resource, Then 200 - the session survives")
	void shouldKeepTheSessionAcrossRequests() throws Exception {
		mockMvc.perform(get(CATEGORIES).session(signIn().session()))
				.andExpect(status().isOk());
	}

	@Test
	@DisplayName("Given a signed-in session, When GET auth/me, Then the signed-in user")
	void shouldReportCurrentUser() throws Exception {
		mockMvc.perform(get(ME).session(signIn().session()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.username").value(USERNAME));
	}

	@Test
	@DisplayName("Given a signed-in session but no CSRF token, When POST, Then 403")
	void shouldRejectWriteWithoutCsrfToken() throws Exception {
		mockMvc.perform(post(CATEGORIES).session(signIn().session())
						.contentType(MediaType.APPLICATION_JSON).content(categoryBody("Forret")))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.errorCode").value(ApplicationErrorCodes.ACCESS_DENIED.getCode()));
	}

	@Test
	@DisplayName("Given a signed-in session, When POST a category, Then created_by is the signed-in user, read back fresh")
	void shouldStampCreatedByWithTheSignedInUser() throws Exception {
		// The reason sign-in had to come first: created_by is NOT NULL, and until
		// SessionPopulatingFilter nothing ever filled the Session bean it is read from.
		SignedIn signedIn = signIn();
		String location = mockMvc.perform(post(CATEGORIES).session(signedIn.session())
						.cookie(signedIn.xsrf()).header(XSRF_HEADER, signedIn.xsrf().getValue())
						.contentType(MediaType.APPLICATION_JSON).content(categoryBody("Suppe")))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getHeader("Location");

		// A separate GET, so the assertion is on the stored row rather than on the save response.
		mockMvc.perform(get(location).session(signedIn.session()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Suppe"))
				.andExpect(jsonPath("$.createdBy").value(USERNAME));
	}

	@Test
	@DisplayName("Given a signed-in session, When POST logout, Then 204, the session cookie is deleted and the session no longer works")
	void shouldLogOut() throws Exception {
		SignedIn signedIn = signIn();
		mockMvc.perform(post(LOGOUT).session(signedIn.session())
						.cookie(signedIn.xsrf()).header(XSRF_HEADER, signedIn.xsrf().getValue()))
				.andExpect(status().isNoContent())
				.andExpect(cookie().maxAge("JSESSIONID", 0));

		mockMvc.perform(get(ME).session(signedIn.session()))
				.andExpect(status().isUnauthorized());
	}

	private void assertBadCredentials(String username, String password) throws Exception {
		Cookie xsrf = xsrfCookie();
		mockMvc.perform(post(LOGIN).cookie(xsrf).header(XSRF_HEADER, xsrf.getValue())
						.contentType(MediaType.APPLICATION_JSON).content(loginBody(username, password)))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.errorCode").value(ApplicationErrorCodes.BAD_CREDENTIALS.getCode()))
				.andExpect(jsonPath("$.message").value("Invalid username or password"));
	}

	private Cookie xsrfCookie() throws Exception {
		return mockMvc.perform(get(ME)).andReturn().getResponse().getCookie("XSRF-TOKEN");
	}

	private SignedIn signIn() throws Exception {
		Cookie xsrf = xsrfCookie();
		MvcResult result = mockMvc.perform(post(LOGIN).cookie(xsrf).header(XSRF_HEADER, xsrf.getValue())
						.contentType(MediaType.APPLICATION_JSON).content(loginBody(USERNAME, PASSWORD)))
				.andExpect(status().isOk())
				.andReturn();
		return new SignedIn((MockHttpSession) result.getRequest().getSession(false), xsrf);
	}

	private String loginBody(String username, String password) {
		return jsonMapper.writeValueAsString(LoginRequestDTO.builder().username(username).password(password).build());
	}

	private String categoryBody(String name) {
		return jsonMapper.writeValueAsString(CategoryDTO.builder().name(name).build());
	}
}
