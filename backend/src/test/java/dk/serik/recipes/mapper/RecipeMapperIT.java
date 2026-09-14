package dk.serik.recipes.mapper;

import dk.serik.recipes.dto.RecipeDTO;
import dk.serik.recipes.dto.RecipeIngredientDTO;
import dk.serik.recipes.dto.RecipeRatingDTO;
import dk.serik.recipes.dto.TagDTO;
import dk.serik.recipes.model.Recipe;
import dk.serik.recipes.repository.RecipeJpaRepository;
import dk.serik.recipes.testutil.OffsetDateTimeProvider;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.jdbc.Sql;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Slf4j
public class RecipeMapperIT {

    @Autowired
    private RecipeJpaRepository recipeJpaRepository;

    @Test
    @Sql({"/db/test-data/insert_recipes.sql", "/db/test-data/insert_recipe_ingredients.sql", "/db/test-data/insert_recipe_tags.sql", "/db/test-data/insert_recipe_ratings.sql"})
    @DisplayName("Given Recipe entity, When mapped to DTO, Then ok")
    public void passMappingEntityToDto() {
        Optional<Recipe> recipe = recipeJpaRepository.findById(UUID.fromString("5d22c394-b5ce-48c3-8199-72ccc92c737c"));
        assertThat(recipe.isPresent()).isTrue();

        RecipeDTO dto = RecipeMapper.from(recipe.get());
        log.info("RecipeDTO: {}", dto);
        assertThat(dto).isEqualTo(mockRecipeDto());
    }

    private RecipeDTO mockRecipeDto() {
        RecipeDTO dto = RecipeDTO.builder()
                .name("Fuldkorns hvedebrød")
                .description("Et dejligt Fuldkorns brød som smager godt og mætter dejligt")
                .instructions("1. Hæld vand og gær i en røremaskine...")
                .id("5d22c394-b5ce-48c3-8199-72ccc92c737c")
                .categoryId("14d4c0b0-46ea-498d-a3a5-56060a3d7a7c")
                .categoryName("Brød")
                .recipeIngredients(mockRecipeIngredients())
                .recipeRatings(mockRecipeRatings())
                .tags(mockTags())
                .created(OffsetDateTimeProvider.provideIsoFullFormat("2022-10-18T17:34:02"))
                .createdBy("Jens")
                .updated(OffsetDateTimeProvider.provideIsoFullFormat("2022-10-18T17:34:03"))
                .updatedBy("Majken")
                .build();
        return dto;

    }

    private Set<RecipeIngredientDTO> mockRecipeIngredients() {
        Set<RecipeIngredientDTO> mocks = new HashSet<>();
        RecipeIngredientDTO dto = RecipeIngredientDTO.builder()
                .recipeId("5d22c394-b5ce-48c3-8199-72ccc92c737c")
                .ingredientId("549ab6e6-f2d8-4ab3-8ba8-6bc7af82f2fb")
                .ingredientName("Hvedemel")
                .amount(new BigDecimal(550.00))
                .unitId("c5173731-3a7e-498c-84b1-b2d3abe68cef")
                .unitLabel("gr")
                .created(OffsetDateTimeProvider.provideIsoFullFormat("2022-10-18T17:34:02"))
                .createdBy("Jens")
                .build();
        mocks.add(dto);

        dto = RecipeIngredientDTO.builder()
                .recipeId("5d22c394-b5ce-48c3-8199-72ccc92c737c")
                .ingredientId("e00aec55-2eb7-4eeb-a594-0bbb948f09c1")
                .ingredientName("Vand")
                .amount(new BigDecimal(5.00))
                .unitId("b9cef3df-4bb5-49ab-8bde-5848d1363bce")
                .unitLabel("dl")
                .created(OffsetDateTimeProvider.provideIsoFullFormat("2022-10-15T21:08:02"))
                .createdBy("Majken")
                .build();
        mocks.add(dto);

        dto = RecipeIngredientDTO.builder()
                .recipeId("5d22c394-b5ce-48c3-8199-72ccc92c737c")
                .ingredientId("4a8819f6-2f7b-48ac-a5c1-20beb2e7e0b4")
                .ingredientName("Fuldkorns hvedemel")
                .amount(new BigDecimal(170.00))
                .unitId("c5173731-3a7e-498c-84b1-b2d3abe68cef")
                .unitLabel("gr")
                .created(OffsetDateTimeProvider.provideIsoFullFormat("2022-10-15T21:08:02"))
                .createdBy("Majken")
                .build();
        mocks.add(dto);

        dto = RecipeIngredientDTO.builder()
                .recipeId("5d22c394-b5ce-48c3-8199-72ccc92c737c")
                .ingredientId("381e5cd5-0a5d-48d2-b69c-71516254937e")
                .ingredientName("Surdej")
                .amount(new BigDecimal(1.00))
                .unitId("b9cef3df-4bb5-49ab-8bde-5848d1363bce")
                .unitLabel("dl")
                .created(OffsetDateTimeProvider.provideIsoFullFormat("2022-10-18T17:34:02"))
                .createdBy("Jens")
                .build();
        mocks.add(dto);

        dto = RecipeIngredientDTO.builder()
                .recipeId("5d22c394-b5ce-48c3-8199-72ccc92c737c")
                .ingredientId("713ff039-25f2-471f-a1a4-ab8fc9efc8b0")
                .ingredientName("Gær")
                .amount(new BigDecimal(10.00))
                .unitId("c5173731-3a7e-498c-84b1-b2d3abe68cef")
                .unitLabel("gr")
                .created(OffsetDateTimeProvider.provideIsoFullFormat("2022-10-18T17:34:02"))
                .createdBy("Jens")
                .build();
        mocks.add(dto);

        dto = RecipeIngredientDTO.builder()
                .recipeId("5d22c394-b5ce-48c3-8199-72ccc92c737c")
                .ingredientId("e0aa2252-c5f1-4c87-b42c-9dd10486f366")
                .ingredientName("Salt")
                .amount(new BigDecimal(16.00))
                .unitId("c5173731-3a7e-498c-84b1-b2d3abe68cef")
                .unitLabel("gr")
                .created(OffsetDateTimeProvider.provideIsoFullFormat("2022-10-15T21:08:02"))
                .createdBy("Majken")
                .build();
        mocks.add(dto);

        return mocks;
    }

    private Set<RecipeRatingDTO> mockRecipeRatings() {
        Set<RecipeRatingDTO> mockRecipeRatings = new HashSet<>();

        RecipeRatingDTO dto = RecipeRatingDTO.builder()
                .recipeId("5d22c394-b5ce-48c3-8199-72ccc92c737c")
                .ratingId("66946e5d-d798-474a-8111-4989bd4c04cd")
                .rating(1)
                .description("Ikke lige mig")
                .id("c20fd30b-56b9-4848-8bd4-17bd3fe21df6")
                .created(OffsetDateTimeProvider.provideIsoFullFormat("2023-02-27T15:07:50"))
                .createdBy("Gerda")
                .build();
        mockRecipeRatings.add(dto);

        dto = RecipeRatingDTO.builder()
                .recipeId("5d22c394-b5ce-48c3-8199-72ccc92c737c")
                .ratingId("2ea87b61-3ffa-433f-bc29-3fdfc43168e2")
                .rating(3)
                .description("Udemærket")
                .id("bb8ea663-c008-413b-b4ae-d15606d2e15d")
                .created(OffsetDateTimeProvider.provideIsoFullFormat("2023-01-15T21:24:39"))
                .createdBy("Jens")
                .build();
        mockRecipeRatings.add(dto);

        dto = RecipeRatingDTO.builder()
                .recipeId("5d22c394-b5ce-48c3-8199-72ccc92c737c")
                .ratingId("2ea87b61-3ffa-433f-bc29-3fdfc43168e2")
                .rating(3)
                .description("Udemærket")
                .id("a676d57a-32f9-4d3c-aa05-02b624eb7301")
                .created(OffsetDateTimeProvider.provideIsoFullFormat("2022-11-14T17:40:03"))
                .createdBy("Jens")
                .build();
        mockRecipeRatings.add(dto);

        return mockRecipeRatings;

    }

    private Set<TagDTO> mockTags() {
        Set<TagDTO> mockTags = new HashSet<>();
        TagDTO dto = TagDTO.builder()
                .name("Godt til kaffen")
                .created(OffsetDateTimeProvider.provideIsoFullFormat("2022-11-14T17:40:03"))
                .createdBy("Jens")
                .build();
        mockTags.add(dto);

        return mockTags;
    }
}
