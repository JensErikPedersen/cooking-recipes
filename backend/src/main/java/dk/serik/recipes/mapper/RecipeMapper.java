package dk.serik.recipes.mapper;

import dk.serik.recipes.dto.RecipeDTO;
import dk.serik.recipes.dto.RecipeIngredientDTO;
import dk.serik.recipes.dto.RecipeRatingDTO;
import dk.serik.recipes.dto.TagDTO;
import dk.serik.recipes.model.Recipe;
import lombok.extern.slf4j.Slf4j;

import java.text.Collator;
import java.util.Comparator;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
public class RecipeMapper {

    private static final Locale DANISH = Locale.of("da");

    public static RecipeDTO from(Recipe entity) {
        if(Objects.isNull(entity)) {
            return null;
        }
        RecipeDTO dto = RecipeDTO.builder()
                .description(entity.getDescription())
                .instructions(entity.getInstructions())
                .name(entity.getName())
                .id(entity.getId().toString())
                .categoryId(String.valueOf(entity.getCategory().getId()))
                .categoryName(entity.getCategory().getName())
                .created(entity.getCreated())
                .createdBy(entity.getCreatedBy())
                .updated(entity.getUpdated())
                .updatedBy(entity.getUpdatedBy())
                .build();
        if(Objects.nonNull(entity.getRecipeIngredients())) {
            // The entity holds its lines in a set; sorted, a GET answers in the same order every time.
            dto.setRecipeIngredients(entity.getRecipeIngredients().stream()
                    .map(ri -> {
                        RecipeIngredientDTO riDto = RecipeIngredientMapper.from(ri);
                        log.debug("Mapped RecipeIngredientDTO: {}", riDto);
                        return riDto;
                    })
                    .sorted(Comparator.comparing(RecipeIngredientDTO::getIngredientName, Collator.getInstance(DANISH)))
                    .toList());
        }

        if(Objects.nonNull(entity.getRecipeRatings())) {
            dto.setRecipeRatings(entity.getRecipeRatings().stream()
                    .map(rr -> {
                        RecipeRatingDTO rrDto = RecipeRatingMapper.from(rr);
                        log.debug("Mapped RecipeRatingDTO: {}", rrDto);
                        return rrDto;
                    }).collect(Collectors.toSet()));
        }

        if (Objects.nonNull(entity.getTags())) {
            dto.setTags(entity.getTags().stream()
                    .map(t -> {
                        TagDTO tDto = TagMapper.from(t);
                        log.debug("Mapped TagDTO: {}", tDto);
                        return tDto;
                    }).collect(Collectors.toSet()));
        }

        log.debug("Mapped RecipeDTO: {}", dto);
        return dto;
    }

}
