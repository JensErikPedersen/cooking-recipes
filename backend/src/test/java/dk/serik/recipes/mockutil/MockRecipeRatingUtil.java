package dk.serik.recipes.mockutil;

import dk.serik.recipes.dto.RecipeRatingDTO;
import dk.serik.recipes.model.RecipeRating;
import dk.serik.recipes.testutil.OffsetDateTimeProvider;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Set;
import java.util.UUID;

public class MockRecipeRatingUtil {

    public static RecipeRating mockRecipeRating5() {
        RecipeRating mock = RecipeRating.builder()
                .description("Excellent!")
                .build();
        ReflectionTestUtils.setField(mock, "id", UUID.fromString("dc887f8c-e8a6-47fd-a007-0225fdd434fc"));
        ReflectionTestUtils.setField(mock, "createdBy", "Jens");
        ReflectionTestUtils.setField(mock, "created", OffsetDateTimeProvider.provideIsoFullFormat("2022-11-05T19:47:29"));
        MockRatingUtil.mockRating5().addRecipeRating(mock);
        return mock;
    }

    public static RecipeRating mockRecipeRating4() {
        RecipeRating mock = RecipeRating.builder()
                .description("This is good!")
                .build();
        ReflectionTestUtils.setField(mock, "id", UUID.fromString("fd051a23-4d96-4113-afcb-7e9b5b35754a"));
        ReflectionTestUtils.setField(mock, "updated", OffsetDateTimeProvider.provideIsoFullFormat("2023-01-25T14:25:15"));
        ReflectionTestUtils.setField(mock, "created", OffsetDateTimeProvider.provideIsoFullFormat("2022-11-05T19:47:29"));
        ReflectionTestUtils.setField(mock, "updatedBy", "Jens");
        ReflectionTestUtils.setField(mock, "createdBy", "Majken");
        MockRatingUtil.mockRating4().addRecipeRating(mock);
        return mock;
    }

    public static RecipeRating mockRecipeRating3_1() {
        RecipeRating mock = RecipeRating.builder()
                .description("Ok!")
                .build();
        ReflectionTestUtils.setField(mock, "id", UUID.fromString("8f3a8601-10b0-4f52-90e3-3962388049fe"));
        ReflectionTestUtils.setField(mock, "updated", OffsetDateTimeProvider.provideIsoFullFormat("2023-01-25T14:25:15"));
        ReflectionTestUtils.setField(mock, "created", OffsetDateTimeProvider.provideIsoFullFormat("2022-11-05T19:47:29"));
        return mock;
    }

    public static RecipeRating mockRecipeRating3_2() {
        RecipeRating mock = RecipeRating.builder()
                .description("Mediocre")
                .build();
        ReflectionTestUtils.setField(mock, "id", UUID.fromString("0afa2442-e8ab-485c-b451-28843aa0927b"));
        ReflectionTestUtils.setField(mock, "updated", OffsetDateTimeProvider.provideIsoFullFormat("2023-01-25T14:25:15"));
        ReflectionTestUtils.setField(mock, "created", OffsetDateTimeProvider.provideIsoFullFormat("2022-11-05T19:47:29"));
        return mock;
    }

    public static Set<RecipeRating> recipeRatings() {
        return Set.of(mockRecipeRating4(), mockRecipeRating5(), mockRecipeRating3_2(), mockRecipeRating3_1());
    }

    public static RecipeRatingDTO mockRecipeRatingDTO4() {
        RecipeRatingDTO mock = RecipeRatingDTO.builder()
                .description("This is good!")
                .recipeId("932b9ecc-ae01-47d3-996a-0e83c27f39b7")
                .build();
        ReflectionTestUtils.setField(mock, "id", "fd051a23-4d96-4113-afcb-7e9b5b35754a");
        ReflectionTestUtils.setField(mock, "updated", OffsetDateTimeProvider.provideIsoFullFormat("2023-01-25T14:25:15"));
        ReflectionTestUtils.setField(mock, "created", OffsetDateTimeProvider.provideIsoFullFormat("2022-11-05T19:47:29"));
        return mock;
    }

    public static RecipeRatingDTO mockRecipeRatingDTO5() {
        RecipeRatingDTO mock = RecipeRatingDTO.builder()
                .description("Excellent!")
                .recipeId("932b9ecc-ae01-47d3-996a-0e83c27f39b7")
                .build();
        ReflectionTestUtils.setField(mock, "id", "dc887f8c-e8a6-47fd-a007-0225fdd434fc");
        ReflectionTestUtils.setField(mock, "updated", OffsetDateTimeProvider.provideIsoFullFormat("2023-01-25T14:25:15"));
        ReflectionTestUtils.setField(mock, "created", OffsetDateTimeProvider.provideIsoFullFormat("2022-11-05T19:47:29"));
        return mock;
    }
}
