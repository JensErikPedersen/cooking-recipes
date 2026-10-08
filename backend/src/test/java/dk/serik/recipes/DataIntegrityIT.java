package dk.serik.recipes;

import dk.serik.recipes.exceptions.ApplicationExceptionHandler;
import dk.serik.recipes.repository.AppUserWriter;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.orm.jpa.vendor.HibernateJpaDialect;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The violations the database really raises, handed to the real handler. Every service checks for
 * a conflict before it writes, so over HTTP the handler is reached only by a race; here the
 * violation is provoked directly instead - native SQL through Hibernate, in a transaction that is
 * always rolled back - and translated the way the transaction manager translates it at commit.
 * <p>
 * Run against MySQL as well during V3 Part 3, with the same kinds: see {@code docs/PLAN_V3.md}.
 */
@SpringBootTest(classes = RecipesApplication.class)
@AutoConfigureTestDatabase
class DataIntegrityIT {

	private static final String DESSERT = "913a5159-3717-4b9d-a290-0158d31ea8aa";

	@PersistenceContext
	private EntityManager entityManager;

	@Autowired
	private TransactionTemplate transactionTemplate;

	@Autowired
	private ApplicationExceptionHandler handler;

	@Autowired
	private AppUserWriter appUserWriter;

	@Test
	@DisplayName("Given a name another category has, When inserted, Then the handler answers 409")
	void shouldAnswerConflictForUniqueViolation() {
		assertStatus(HttpStatus.CONFLICT, "INSERT INTO category (id, name, created, created_by) VALUES ('"
				+ UUID.randomUUID() + "', 'Dessert', CURRENT_TIMESTAMP, 'it')");
	}

	@Test
	@DisplayName("Given a recipe naming no existing category, When inserted, Then the handler answers 409")
	void shouldAnswerConflictForForeignKeyOnInsert() {
		assertStatus(HttpStatus.CONFLICT, "INSERT INTO recipe (id, name, created, created_by, category_id) VALUES ('"
				+ UUID.randomUUID() + "', 'it', CURRENT_TIMESTAMP, 'it', '" + UUID.randomUUID() + "')");
	}

	@Test
	@DisplayName("Given a category a recipe uses, When deleted, Then the handler answers 409")
	void shouldAnswerConflictForForeignKeyOnDelete() {
		assertStatus(HttpStatus.CONFLICT,
				"INSERT INTO recipe (id, name, created, created_by, category_id) VALUES ('"
						+ UUID.randomUUID() + "', 'it', CURRENT_TIMESTAMP, 'it', '" + DESSERT + "')",
				"DELETE FROM category WHERE id = '" + DESSERT + "'");
	}

	@Test
	@DisplayName("Given a NOT NULL column left empty, When inserted, Then the handler answers 500 - a server fault, not a conflict")
	void shouldAnswerServerErrorForNotNullViolation() {
		assertStatus(HttpStatus.INTERNAL_SERVER_ERROR, "INSERT INTO category (id, name, created, created_by) VALUES ('"
				+ UUID.randomUUID() + "', 'it-not-null', CURRENT_TIMESTAMP, NULL)");
	}

	@Test
	@DisplayName("Given a username taken through AppUserWriter's plain SQL, When inserted again, Then the handler answers 409")
	void shouldAnswerConflictForDuplicateKeyFromJdbc() {
		String username = "it-" + UUID.randomUUID().toString().substring(0, 8);
		appUserWriter.insert(username, "long enough", "ROLE_USER", username);

		assertThatThrownBy(() -> appUserWriter.insert(username, "long enough", "ROLE_USER", username))
				.isInstanceOfSatisfying(DataIntegrityViolationException.class, ex ->
						assertThat(handler.handleDataIntegrityViolation(ex).getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
	}

	private void assertStatus(HttpStatus expected, String... statements) {
		DataIntegrityViolationException violation = transactionTemplate.execute(status -> {
			status.setRollbackOnly();   // nothing is ever kept
			try {
				for (String sql : statements) {
					entityManager.createNativeQuery(sql).executeUpdate();
				}
				return null;
			} catch (RuntimeException ex) {
				return (DataIntegrityViolationException) new HibernateJpaDialect().translateExceptionIfPossible(ex);
			}
		});

		assertThat(violation).as("the statements were refused").isNotNull();
		assertThat(handler.handleDataIntegrityViolation(violation).getStatusCode()).isEqualTo(expected);
	}
}
