package dk.serik.recipes;

import org.junit.jupiter.api.Test;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = RecipesApplication.class)
@AutoConfigureTestDatabase
class RecipesApplicationTests {

	@Test
	void contextLoads() {
	}

}
