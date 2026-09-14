package dk.serik.recipes.config;

import dk.serik.recipes.bean.Session;
import dk.serik.recipes.repository.CategoryJpaRepository;
import dk.serik.recipes.repository.IngredientJpaRepository;
import dk.serik.recipes.service.CategoryService;
import dk.serik.recipes.service.CategoryServiceImpl;
import dk.serik.recipes.service.IngredientService;
import dk.serik.recipes.service.IngredientServiceImpl;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class TestingConfiguration {

    @Bean
    public CategoryService categoryService(CategoryJpaRepository categoryJpaRepository, Session session) {
        return new CategoryServiceImpl(categoryJpaRepository, session);
    }

    @Bean
    public IngredientService ingredientService(IngredientJpaRepository ingredientJpaRepository, Session session) {
        return new IngredientServiceImpl(ingredientJpaRepository, session);
    }
}
