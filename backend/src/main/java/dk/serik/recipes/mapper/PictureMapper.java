package dk.serik.recipes.mapper;

import dk.serik.recipes.dto.PictureDTO;
import dk.serik.recipes.model.Picture;

import java.util.Objects;

public class PictureMapper {
    public static PictureDTO from(Picture entity) {
        if (Objects.isNull(entity)) {
            return null;
        }
        return PictureDTO.builder()
                .id(entity.getId().toString())
                .name(entity.getName())
                .altText(entity.getAltText())
                .createdBy(entity.getCreatedBy())
                .created(entity.getCreated())
                .updatedBy(entity.getUpdatedBy())
                .updated(entity.getUpdated())
                .build();
    }
}
