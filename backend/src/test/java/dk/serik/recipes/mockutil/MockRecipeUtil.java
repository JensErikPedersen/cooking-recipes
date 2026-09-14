package dk.serik.recipes.mockutil;

import dk.serik.recipes.dto.RecipeDTO;
import dk.serik.recipes.model.Recipe;
import dk.serik.recipes.testutil.OffsetDateTimeProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Set;
import java.util.UUID;

@Slf4j
public class MockRecipeUtil {

    public static RecipeDTO mockSavedRecipeWheatBreadWithIngredientsAndRatingDTO() {
        return RecipeDTO.builder()
                .id("932b9ecc-ae01-47d3-996a-0e83c27f39b7")
                .description("Lækkert brød til morgenmaden")
                .name("Hvedebrød")
                .instructions("Hæld vand i en skål...")
                .categoryName("Brød")
                .categoryId("913a5159-3717-4b9d-a290-0158d31ea8ab")
                .recipeRatings(Set.of(MockRecipeRatingUtil.mockRecipeRatingDTO4(), MockRecipeRatingUtil.mockRecipeRatingDTO5()))
                .recipeIngredients(Set.of(MockRecipeIngredientUtil.mockRecipeIngredientDTOHvedemel(), MockRecipeIngredientUtil.mockRecipeIngredientDTOSalt(), MockRecipeIngredientUtil.mockRecipeIngredientDTOSurdej()))
                .created(OffsetDateTimeProvider.provideIsoFullFormat("2022-11-05T19:47:29"))
                .updated(OffsetDateTimeProvider.provideIsoFullFormat("2023-01-25T14:25:15"))
                .createdBy("Jens")
                .updatedBy("Majken")
                .build();
    }

    public static RecipeDTO mockRecipeWheatBreadWithIngredientsDTOToBeSaved() {
        return RecipeDTO.builder()
                .description("Lækkert brød til morgenmaden")
                .name("Hvedebrød")
                .instructions("Hæld vand i en skål...")
                .categoryName("Brød")
                .categoryId("913a5159-3717-4b9d-a290-0158d31ea8ab")
                .recipeIngredients(Set.of(MockRecipeIngredientUtil.mockRecipeIngredientDTOHvedemel(), MockRecipeIngredientUtil.mockRecipeIngredientDTOSalt(), MockRecipeIngredientUtil.mockRecipeIngredientDTOSurdej()))
                .build();
    }

    public static Recipe mockSavedRecipeWheatBread() {
        Recipe mock = Recipe.builder()
                .name("Hvedebrød")
                .description("Lækkert brød til morgenmaden")
                .instructions("Hæld vand i en skål...")
                .category(MockCategoryUtil.mockBread())
                .build();
        ReflectionTestUtils.setField(mock, "id", UUID.fromString("932b9ecc-ae01-47d3-996a-0e83c27f39b7"));
        ReflectionTestUtils.setField(mock, "updated", OffsetDateTimeProvider.provideIsoFullFormat("2023-01-25T14:25:15"));
        ReflectionTestUtils.setField(mock, "updatedBy", "Majken");
        ReflectionTestUtils.setField(mock, "created", OffsetDateTimeProvider.provideIsoFullFormat("2022-11-05T19:47:29"));
        ReflectionTestUtils.setField(mock, "createdBy", "Jens");
        mock.addRecipeIngredient(MockRecipeIngredientUtil.mockSavedRecipeIngredientHvedemel());
        mock.addRecipeIngredient(MockRecipeIngredientUtil.mockSavedRecipeIngredientSalt());
        mock.addRecipeIngredient(MockRecipeIngredientUtil.mockSavedRecipeIngredientSurdej());

        log.info("Saved Recipe: {}", mock);
        return mock;
    }

    public static Recipe mockSavedRecipeRyeBread() {
        Recipe mock = Recipe.builder()
                .name("Rugbrød")
                .description("Brød til frokosten")
                .instructions("Hæld vand i en skål...")
                .category(MockCategoryUtil.mockBread())
                .build();
        ReflectionTestUtils.setField(mock, "id", UUID.fromString("67218e0e-0d91-4a1e-af0f-ddeb8ceaca32"));
        ReflectionTestUtils.setField(mock, "updated", OffsetDateTimeProvider.provideIsoFullFormat("2025-01-25T14:25:15"));
        ReflectionTestUtils.setField(mock, "updatedBy", "Majken");
        ReflectionTestUtils.setField(mock, "created", OffsetDateTimeProvider.provideIsoFullFormat("2024-11-05T19:47:29"));
        ReflectionTestUtils.setField(mock, "createdBy", "Majken");
        mock.addRecipeIngredient(MockRecipeIngredientUtil.mockSavedRecipeIngredientRugmel());
        mock.addRecipeIngredient(MockRecipeIngredientUtil.mockSavedRecipeIngredientHvedemel());
        mock.addRecipeIngredient(MockRecipeIngredientUtil.mockSavedRecipeIngredientSalt());
        mock.addRecipeIngredient(MockRecipeIngredientUtil.mockSavedRecipeIngredientSurdej());

        return mock;
    }

    public static Recipe mockSavedRecipeWheatBreadWithRatings() {
        Recipe mock = Recipe.builder()
                .name("Hvedebrød")
                .description("Lækkert brød til morgenmaden")
                .instructions("Hæld vand i en skål...")
                .category(MockCategoryUtil.mockBread())
                .build();
        ReflectionTestUtils.setField(mock, "id", UUID.fromString("932b9ecc-ae01-47d3-996a-0e83c27f39b7"));
        ReflectionTestUtils.setField(mock, "updated", OffsetDateTimeProvider.provideIsoFullFormat("2023-01-25T14:25:15"));
        ReflectionTestUtils.setField(mock, "updatedBy", "Majken");
        ReflectionTestUtils.setField(mock, "created", OffsetDateTimeProvider.provideIsoFullFormat("2022-11-05T19:47:29"));
        ReflectionTestUtils.setField(mock, "createdBy", "Jens");
        mock.addRecipeRating(MockRecipeRatingUtil.mockRecipeRating4());
        mock.addRecipeRating(MockRecipeRatingUtil.mockRecipeRating5());
        mock.addRecipeIngredient(MockRecipeIngredientUtil.mockSavedRecipeIngredientHvedemel());
        mock.addRecipeIngredient(MockRecipeIngredientUtil.mockSavedRecipeIngredientSalt());
        mock.addRecipeIngredient(MockRecipeIngredientUtil.mockSavedRecipeIngredientSurdej());

        log.info("Saved Recipe: {}", mock);
        return mock;
    }




    public static Recipe mockRecipeWheatBreadToBeSaved() {
        Recipe mock = Recipe.builder()
                .name("Hvedebrød")
                .description("Lækkert brød til morgenmaden")
                .instructions("Hæld vand i en skål...")
                .category(MockCategoryUtil.mockBread())
                .build();
        mock.addRecipeIngredient(MockRecipeIngredientUtil.mockRecipeIngredientHvedemel());
        mock.addRecipeIngredient(MockRecipeIngredientUtil.mockRecipeIngredientSalt());
        mock.addRecipeIngredient(MockRecipeIngredientUtil.mockRecipeIngredientSurdej());

        return mock;
    }
}
