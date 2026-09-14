package dk.serik.recipes.aspect;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import dk.serik.recipes.RecipesApplication;
import dk.serik.recipes.repository.CategoryJpaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.InvalidDataAccessApiUsageException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Proves {@link JpaLoggingAspect} actually fires.
 * <p>
 * This has to be an integration test. The open question is not whether the advice body works -
 * that is four lines - but whether the pointcut matches at all, given Spring Data repositories are
 * themselves proxies. A unit test calling {@code logRepositoryErrors} directly with a mock join
 * point would pass whether or not the aspect was ever wired to anything.
 * <p>
 * {@code @DataJpaTest} would not do either: it is a slice and does not scan {@code @Component},
 * so the aspect would not be registered.
 */
@SpringBootTest(classes = RecipesApplication.class)
@AutoConfigureTestDatabase
class JpaLoggingAspectIT {

    @Autowired
    private CategoryJpaRepository categoryJpaRepository;

    private Logger aspectLogger;
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void attachAppender() {
        aspectLogger = (Logger) LoggerFactory.getLogger(JpaLoggingAspect.class);
        appender = new ListAppender<>();
        appender.start();
        aspectLogger.addAppender(appender);
    }

    @AfterEach
    void detachAppender() {
        aspectLogger.detachAppender(appender);
    }

    @Test
    @DisplayName("Given a repository call that throws, When it propagates, Then the aspect logs it at ERROR and rethrows unchanged")
    void shouldLogRepositoryExceptionAndRethrow() {
        // Given / When - a null id makes Spring Data throw from inside the repository method,
        // which is deterministic and needs no seeded data
        assertThatThrownBy(() -> categoryJpaRepository.findById(null))
                .as("the aspect must rethrow unchanged, not swallow or wrap")
                .isInstanceOf(InvalidDataAccessApiUsageException.class);

        // Then
        assertThat(appender.list)
                .as("JpaLoggingAspect logged nothing - the pointcut does not match repository calls")
                .isNotEmpty();
        assertThat(appender.list.get(0).getLevel()).isEqualTo(Level.ERROR);
    }
}
