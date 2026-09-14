package dk.serik.recipes.mockutil;

import dk.serik.recipes.dto.RecipeIngredientDTO;
import dk.serik.recipes.model.RecipeIngredient;
import dk.serik.recipes.testutil.OffsetDateTimeProvider;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

public class MockRecipeIngredientUtil {

    public static RecipeIngredient mockRecipeIngredientHvedemel() {
        return RecipeIngredient.builder()
                .ingredient(MockIngredientUtil.mockHvedemel())
                .unit(MockUnitUtil.mockGram())
                .amount(new BigDecimal(500))
                .build();
    }

    public static RecipeIngredient mockSavedRecipeIngredientHvedemel() {
        RecipeIngredient entity = RecipeIngredient.builder()
                .ingredient(MockIngredientUtil.mockHvedemel())
                .unit(MockUnitUtil.mockGram())
                .amount(new BigDecimal(500))
                .build();
        ReflectionTestUtils.setField(entity, "created", OffsetDateTimeProvider.provideIsoFullFormat("2023-11-05T19:47:29"));
        ReflectionTestUtils.setField(entity, "createdBy", "Jens");

        return entity;
    }

    public static RecipeIngredient mockSavedRecipeIngredientRugmel() {
        RecipeIngredient entity = RecipeIngredient.builder()
                .ingredient(MockIngredientUtil.mockRugmel())
                .unit(MockUnitUtil.mockGram())
                .amount(new BigDecimal(200))
                .build();
        ReflectionTestUtils.setField(entity, "created", OffsetDateTimeProvider.provideIsoFullFormat("2024-11-05T19:47:29"));
        ReflectionTestUtils.setField(entity, "createdBy", "Jens");

        return entity;
    }

    public static RecipeIngredient mockRecipeIngredientSalt() {
        return RecipeIngredient.builder()
                .ingredient(MockIngredientUtil.mockHavsalt())
                .unit(MockUnitUtil.mockGram())
                .amount(new BigDecimal(15))
                .build();

    }

    public static RecipeIngredient mockSavedRecipeIngredientSalt() {
        RecipeIngredient entity = RecipeIngredient.builder()
                .ingredient(MockIngredientUtil.mockHavsalt())
                .unit(MockUnitUtil.mockGram())
                .amount(new BigDecimal(15))
                .build();
        ReflectionTestUtils.setField(entity, "created", OffsetDateTimeProvider.provideIsoFullFormat("2024-12-05T19:47:29"));
        ReflectionTestUtils.setField(entity, "createdBy", "Jens");

        return entity;
    }

    public static RecipeIngredient mockRecipeIngredientSurdej() {
        return RecipeIngredient.builder()
                .ingredient(MockIngredientUtil.mockSurdej())
                .unit(MockUnitUtil.mockDl())
                .amount(new BigDecimal(1))
                .build();

    }

    public static RecipeIngredient mockSavedRecipeIngredientSurdej() {
        RecipeIngredient entity = RecipeIngredient.builder()
                .ingredient(MockIngredientUtil.mockSurdej())
                .unit(MockUnitUtil.mockDl())
                .amount(new BigDecimal(1))
                .build();
        ReflectionTestUtils.setField(entity, "created", OffsetDateTimeProvider.provideIsoFullFormat("2024-12-16T19:47:29"));
        ReflectionTestUtils.setField(entity, "createdBy", "Jens");

        return entity;
    }

    public static RecipeIngredientDTO mockRecipeIngredientDTOHvedemel() {
        return RecipeIngredientDTO.builder()
                .recipeId("932b9ecc-ae01-47d3-996a-0e83c27f39b7")
                .ingredientId("5f01d434-5a68-4359-9f2e-0a6793dce48d")
                .ingredientName("Hvedemel")
                .unitId("f7823293-7874-4459-9fb7-6b420a0627fa")
                .unitLabel("g")
                .amount(new BigDecimal(500))
                .build();
    }

    public static RecipeIngredientDTO mockRecipeIngredientDTOSalt() {
        return RecipeIngredientDTO.builder()
                .recipeId("932b9ecc-ae01-47d3-996a-0e83c27f39b7")
                .ingredientId("01a50907-8141-4dd1-acdf-c4384669c2b2")
                .ingredientName("Havsalt")
                .unitId("f7823293-7874-4459-9fb7-6b420a0627fa")
                .unitLabel("g")
                .amount(new BigDecimal(15))
                .build();
    }

    public static RecipeIngredientDTO mockRecipeIngredientDTOSurdej() {
        return RecipeIngredientDTO.builder()
                .recipeId("932b9ecc-ae01-47d3-996a-0e83c27f39b7")
                .ingredientId("1d150b3f-1a7c-4c08-8264-2910af5b9d25")
                .ingredientName("Surdej")
                .unitId("046d0928-0806-480b-ad8b-c6845e99643b")
                .unitLabel("dl")
                .amount(new BigDecimal(1))
                .build();
    }
}
