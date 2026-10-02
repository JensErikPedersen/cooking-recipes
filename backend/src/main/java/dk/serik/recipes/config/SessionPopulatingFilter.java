package dk.serik.recipes.config;

import dk.serik.recipes.bean.Session;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Copies the signed-in username into the request-scoped {@link Session} bean.
 * <p>
 * {@code BaseEntityListener} and the services read {@code session.getUserName()} into the NOT NULL
 * {@code created_by} / {@code updated_by} columns, and before this filter nothing ever set it - so
 * every write failed against a real database.
 * <p>
 * Created by {@code SecurityConfig} rather than declared a {@code @Component}: Spring Boot would
 * register a component filter a second time outside the security chain, and {@code @WebMvcTest}
 * slices would pick it up and need a {@code Session}.
 */
@AllArgsConstructor
class SessionPopulatingFilter extends OncePerRequestFilter {

	/**
	 * For requests without a signed-in user - after the security rules only login and health,
	 * neither of which writes a row. It keeps a null out of a NOT NULL column regardless.
	 */
	static final String ANONYMOUS = "anonymous";

	private final Session session;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		// An anonymous visitor carries an AnonymousAuthenticationToken whose name is "anonymousUser".
		boolean signedIn = authentication != null && !(authentication instanceof AnonymousAuthenticationToken);
		session.setUserName(signedIn ? authentication.getName() : ANONYMOUS);
		filterChain.doFilter(request, response);
	}
}
