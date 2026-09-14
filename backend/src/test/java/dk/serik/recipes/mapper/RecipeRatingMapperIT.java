package dk.serik.recipes.mapper;


import dk.serik.recipes.dto.RecipeRatingDTO;
import dk.serik.recipes.model.RecipeRating;
import dk.serik.recipes.repository.RecipeRatingJpaRepository;
import dk.serik.recipes.testutil.OffsetDateTimeProvider;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.jdbc.Sql;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Slf4j
public class RecipeRatingMapperIT {

    @Autowired
    private RecipeRatingJpaRepository recipeRatingJpaRepository;
    @Test
    @Sql({"/db/test-data/insert_recipes.sql", "/db/test-data/insert_recipe_ratings.sql"})
    @DisplayName("Given valid entity, When mapped by mapper, Then DTO is Ok")
    public void passMapperFromValidEntityToDto() {
        //Given
        Optional<RecipeRating> recipeRating = recipeRatingJpaRepository.findById(UUID.fromString("72d00adc-99c7-4c51-b4e9-29c9e769a231"));
        assertThat(recipeRating.isPresent()).isTrue();

        // When
        RecipeRatingDTO dto = RecipeRatingMapper.from(recipeRating.get());

        // Then
        log.info("DTO: {}", dto);
        assertThat(dto).isEqualTo(expectedDto());
    }



    private RecipeRatingDTO expectedDto() {
        RecipeRatingDTO dto = RecipeRatingDTO.builder()
                .id("72d00adc-99c7-4c51-b4e9-29c9e769a231")
                .ratingId("26f09c94-79ec-439c-9776-d826efad187e")
                .recipeId("06309a26-9ef8-43d2-82a0-f88e0be094e0")
                .description("Fantastisk")
                .rating(5)
                .createdBy("Jens")
                .created(OffsetDateTimeProvider.provideIsoFullFormat("2023-04-18T19:22:15"))
                .updated(OffsetDateTimeProvider.provideIsoFullFormat("2023-05-22T22:06:24"))
                .build();
        return dto;
    }

}
