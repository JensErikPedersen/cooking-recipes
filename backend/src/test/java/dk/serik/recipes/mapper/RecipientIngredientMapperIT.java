package dk.serik.recipes.mapper;

import dk.serik.recipes.dto.RecipeIngredientDTO;
import dk.serik.recipes.model.RecipeIngredient;
import dk.serik.recipes.repository.RecipeIngredientJpaRepository;
import dk.serik.recipes.testutil.OffsetDateTimeProvider;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.jdbc.Sql;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@DataJpaTest
@Slf4j
public class RecipientIngredientMapperIT {

    @Autowired
    private RecipeIngredientJpaRepository recipeIngredientJpaRepository;

    @Test
    @Sql({"/db/test-data/insert_recipes.sql", "/db/test-data/insert_recipe_ingredients.sql"})
    @DisplayName("Given valid entity, When mapped by mapper, Then DTO is Ok")
    public void passMapperFromValidEntityToDto() {
        // Given
        Optional<RecipeIngredient> recIngrOp = recipeIngredientJpaRepository.findByRecipeIdAndIngredientId(UUID.fromString("ce07075c-38b4-4b52-831c-5a9ce105e4af"), UUID.fromString("713ff039-25f2-471f-a1a4-ab8fc9efc8b0")); // Hvedebrød med Rugmel -> Gær
        assertThat(recIngrOp.isPresent()).isTrue();
        log.info("Gær: {}", recIngrOp.get());

        // When
        RecipeIngredientDTO dto = RecipeIngredientMapper.from(recIngrOp.get());
        log.info("DTO: {}", dto);

        // Then
        assertThat(dto.getIngredientId()).isEqualTo(expectedDto().getIngredientId());
        assertThat(dto.getIngredientName()).isEqualTo(expectedDto().getIngredientName());
        assertThat(dto.getUnitId()).isEqualTo(expectedDto().getUnitId());
        assertThat(dto.getUnitLabel()).isEqualTo(expectedDto().getUnitLabel());
        assertThat(dto.getRecipeId()).isEqualTo(expectedDto().getRecipeId());
        assertThat(dto.getAmount()).isEqualTo(expectedDto().getAmount().setScale(2, RoundingMode.CEILING));
        assertThat(dto.getCreated()).isCloseTo(expectedDto().getCreated(), within(0, ChronoUnit.SECONDS));
        assertThat(dto.getCreatedBy()).isEqualTo(expectedDto().getCreatedBy());
    }

    private RecipeIngredientDTO expectedDto() {
        RecipeIngredientDTO dto = RecipeIngredientDTO.builder()
                .ingredientId("713ff039-25f2-471f-a1a4-ab8fc9efc8b0")
                .ingredientName("Gær")
                .amount(new BigDecimal(5.00))
                .unitLabel("gr")
                .unitId("c5173731-3a7e-498c-84b1-b2d3abe68cef")
                .recipeId("ce07075c-38b4-4b52-831c-5a9ce105e4af")
                .createdBy("Jens")
                .created(OffsetDateTimeProvider.provideIsoFullFormat("2022-10-18T17:34:02"))
                .build();
        return dto;

    }




}
