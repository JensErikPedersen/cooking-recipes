package dk.serik.recipes.service;

import dk.serik.recipes.bean.Session;
import dk.serik.recipes.dto.UpdateUserRequestDTO;
import dk.serik.recipes.dto.UserDTO;
import dk.serik.recipes.exceptions.ApplicationErrorCodes;
import dk.serik.recipes.exceptions.ServiceException;
import dk.serik.recipes.mapper.UserMapper;
import dk.serik.recipes.model.AppUser;
import dk.serik.recipes.repository.AppUserJpaRepository;
import dk.serik.recipes.repository.AppUserWriter;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@AllArgsConstructor
@Slf4j
@Transactional(
		isolation = Isolation.READ_COMMITTED,
		propagation = Propagation.REQUIRED,
		readOnly = false,
		timeout = 5)
public class UserServiceImpl implements UserService {

	private final AppUserJpaRepository repository;

	private final AppUserWriter appUserWriter;

	private final SessionRegistry sessionRegistry;

	private final Session session;

	@Override
	@Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED, readOnly = true, timeout = 5)
	public List<UserDTO> findAll() {
		return repository.findAll(Sort.by("username")).stream()
				.map(UserMapper::from)
				.toList();
	}

	@Override
	public void update(String id, UpdateUserRequestDTO request) {
		AppUser user = repository.findById(ServiceArguments.toUuid(id, ApplicationErrorCodes.USER_ID_IS_NULL, "User"))
				.orElseThrow(() -> ServiceException.builder()
						.message(String.format("User with id %s could not be found", id))
						.code(ApplicationErrorCodes.USER_NOT_FOUND.getCode())
						.httpStatus(HttpStatus.NOT_FOUND)
						.build());
		// So there is always an enabled admin: whoever does this is one.
		if (user.getUsername().equals(session.getUserName())) {
			throw ServiceException.badRequest(ApplicationErrorCodes.OWN_ACCOUNT, "You cannot change your own account");
		}
		if (user.isEnabled() == request.getEnabled() && user.isAdmin() == request.getAdmin()) {
			return;
		}

		String roles = request.getAdmin() ? AppUser.ROLE_USER + "," + AppUser.ROLE_ADMIN : AppUser.ROLE_USER;
		appUserWriter.updateAccess(user.getId(), request.getEnabled(), roles, session.getUserName());
		// A session carries the roles it signed in with, and a disabled account must not keep one:
		// either way the account signs in again, if it can.
		sessionRegistry.getAllSessions(user.getUsername(), false).forEach(SessionInformation::expireNow);
		log.info("'{}' set '{}' to enabled={}, roles={}", session.getUserName(), user.getUsername(), request.getEnabled(), roles);
	}
}
