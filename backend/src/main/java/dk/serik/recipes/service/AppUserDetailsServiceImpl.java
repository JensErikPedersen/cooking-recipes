package dk.serik.recipes.service;

import dk.serik.recipes.repository.AppUserJpaRepository;
import lombok.AllArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Bridges {@code AppUser} to Spring Security, which is its only caller - hence no interface of its
 * own beyond {@link UserDetailsService}.
 */
@Service
@AllArgsConstructor
public class AppUserDetailsServiceImpl implements UserDetailsService {

	private final AppUserJpaRepository appUserJpaRepository;

	@Override
	@Transactional(readOnly = true)
	public UserDetails loadUserByUsername(String username) {
		// Spring Security turns this into the same BadCredentialsException as a wrong password, so
		// the caller cannot tell whether the account exists.
		return appUserJpaRepository.findByUsername(username)
				.map(user -> User.withUsername(user.getUsername())
						.password(user.getPassword())
						.disabled(!user.isEnabled())
						.authorities(user.roleList().toArray(String[]::new))
						.build())
				.orElseThrow(() -> new UsernameNotFoundException("Bad credentials"));
	}
}
