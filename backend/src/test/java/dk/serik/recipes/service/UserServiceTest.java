package dk.serik.recipes.service;

import dk.serik.recipes.bean.Session;
import dk.serik.recipes.dto.UpdateUserRequestDTO;
import dk.serik.recipes.dto.UserDTO;
import dk.serik.recipes.exceptions.ApplicationErrorCodes;
import dk.serik.recipes.exceptions.ServiceException;
import dk.serik.recipes.model.AppUser;
import dk.serik.recipes.repository.AppUserJpaRepository;
import dk.serik.recipes.repository.AppUserWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

	private static final String ADMIN = "the-admin";

	@Mock
	private AppUserJpaRepository repository;

	@Mock
	private AppUserWriter appUserWriter;

	@Mock
	private SessionRegistry sessionRegistry;

	@Mock
	private Session session;

	@InjectMocks
	private UserServiceImpl service;

	@BeforeEach
	void setUp() {
		lenient().when(session.getUserName()).thenReturn(ADMIN);
	}

	@Test
	@DisplayName("Given accounts, When findAll, Then each mapped, sorted by username, with its roles as a list")
	void shouldFindAllSortedByUsername() {
		given(repository.findAll(Sort.by("username"))).willReturn(List.of(user("anna", true, "ROLE_USER,ROLE_ADMIN")));

		List<UserDTO> users = service.findAll();

		assertThat(users).singleElement().satisfies(user -> {
			assertThat(user.getUsername()).isEqualTo("anna");
			assertThat(user.isEnabled()).isTrue();
			assertThat(user.getRoles()).containsExactly("ROLE_USER", "ROLE_ADMIN");
		});
	}

	@Test
	@DisplayName("Given a user, When disabled, Then written as a disabled user by the admin, and its sessions ended")
	void shouldDisableAndEndSessions() {
		AppUser user = user("bo", true, "ROLE_USER");
		given(repository.findById(user.getId())).willReturn(Optional.of(user));
		SessionInformation first = new SessionInformation("bo", "s1", new Date());
		SessionInformation second = new SessionInformation("bo", "s2", new Date());
		given(sessionRegistry.getAllSessions("bo", false)).willReturn(List.of(first, second));

		service.update(user.getId().toString(), request(false, false));

		then(appUserWriter).should().updateAccess(user.getId(), false, "ROLE_USER", ADMIN);
		assertThat(first.isExpired()).isTrue();
		assertThat(second.isExpired()).isTrue();
	}

	@Test
	@DisplayName("Given a user, When made an admin, Then written with both roles")
	void shouldGrantAdmin() {
		AppUser user = user("bo", true, "ROLE_USER");
		given(repository.findById(user.getId())).willReturn(Optional.of(user));

		service.update(user.getId().toString(), request(true, true));

		then(appUserWriter).should().updateAccess(user.getId(), true, "ROLE_USER,ROLE_ADMIN", ADMIN);
		then(sessionRegistry).should().getAllSessions("bo", false);
	}

	@Test
	@DisplayName("Given an admin, When the role is withdrawn, Then written as a user")
	void shouldWithdrawAdmin() {
		AppUser user = user("bo", true, "ROLE_USER,ROLE_ADMIN");
		given(repository.findById(user.getId())).willReturn(Optional.of(user));

		service.update(user.getId().toString(), request(true, false));

		then(appUserWriter).should().updateAccess(user.getId(), true, "ROLE_USER", ADMIN);
	}

	@Test
	@DisplayName("Given a request equal to the account, When update, Then nothing written and no session ended")
	void shouldDoNothingWhenNothingChanges() {
		AppUser user = user("bo", true, "ROLE_USER,ROLE_ADMIN");
		given(repository.findById(user.getId())).willReturn(Optional.of(user));

		service.update(user.getId().toString(), request(true, true));

		then(appUserWriter).should(never()).updateAccess(any(), anyBoolean(), anyString(), anyString());
		then(sessionRegistry).should(never()).getAllSessions(any(), anyBoolean());
	}

	@Test
	@DisplayName("Given the admin's own account, When update, Then 400 OWN_ACCOUNT and nothing written")
	void shouldRefuseOwnAccount() {
		AppUser self = user(ADMIN, true, "ROLE_USER,ROLE_ADMIN");
		given(repository.findById(self.getId())).willReturn(Optional.of(self));

		assertThatThrownBy(() -> service.update(self.getId().toString(), request(false, false)))
				.isInstanceOfSatisfying(ServiceException.class, ex -> {
					assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
					assertThat(ex.getCode()).isEqualTo(ApplicationErrorCodes.OWN_ACCOUNT.getCode());
				});
		then(appUserWriter).should(never()).updateAccess(any(), anyBoolean(), anyString(), anyString());
	}

	@Test
	@DisplayName("Given an unknown id, When update, Then 404 USER_NOT_FOUND")
	void shouldAnswerNotFound() {
		UUID id = UUID.randomUUID();
		given(repository.findById(id)).willReturn(Optional.empty());

		assertThatThrownBy(() -> service.update(id.toString(), request(false, false)))
				.isInstanceOfSatisfying(ServiceException.class, ex -> {
					assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
					assertThat(ex.getCode()).isEqualTo(ApplicationErrorCodes.USER_NOT_FOUND.getCode());
				});
	}

	@Test
	@DisplayName("Given a malformed id, When update, Then 400 USER_ID_IS_NULL, before any lookup")
	void shouldRejectMalformedId() {
		assertThatThrownBy(() -> service.update("not-a-uuid", request(false, false)))
				.isInstanceOfSatisfying(ServiceException.class, ex -> {
					assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
					assertThat(ex.getCode()).isEqualTo(ApplicationErrorCodes.USER_ID_IS_NULL.getCode());
				});
		then(repository).should(never()).findById(any());
	}

	private static UpdateUserRequestDTO request(boolean enabled, boolean admin) {
		return UpdateUserRequestDTO.builder().enabled(enabled).admin(admin).build();
	}

	/** AppUser is read-only to the application, so its fields are set as JPA would set them. */
	private static AppUser user(String username, boolean enabled, String roles) {
		AppUser user = new AppUser();
		ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
		ReflectionTestUtils.setField(user, "username", username);
		ReflectionTestUtils.setField(user, "enabled", enabled);
		ReflectionTestUtils.setField(user, "roles", roles);
		return user;
	}
}
