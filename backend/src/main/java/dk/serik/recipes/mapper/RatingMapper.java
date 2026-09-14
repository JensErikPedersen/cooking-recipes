package dk.serik.recipes.mapper;

import dk.serik.recipes.dto.RatingDTO;
import dk.serik.recipes.model.Rating;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;


@Slf4j
public class RatingMapper {
    public static RatingDTO from(Rating entity) {
        if(Objects.isNull(entity)) {
            return null;
        }

        RatingDTO dto = RatingDTO.builder()
                .id(entity.getId().toString())
                .rating(entity.getRating())
                .description(entity.getDescription())
                .createdBy(entity.getCreatedBy())
                .created(entity.getCreated())
                .updatedBy(entity.getUpdatedBy())
                .updated(entity.getUpdated())
                .build();
        log.debug("Mapped DTO: {}", dto);
        return dto;
    }
}
