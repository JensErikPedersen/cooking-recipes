package dk.serik.recipes.mapper;

import dk.serik.recipes.dto.TagDTO;
import dk.serik.recipes.model.Tag;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

@Slf4j
public class TagMapper {
    public static TagDTO from(Tag entity) {
        if(Objects.isNull(entity)) {
            return null;
        }
        TagDTO dto = TagDTO.builder()
                .id(entity.getId().toString())
                .name(entity.getName())
                .createdBy(entity.getCreatedBy())
                .created(entity.getCreated())
                .updatedBy(entity.getUpdatedBy())
                .updated(entity.getUpdated())
                .build();
        log.debug("Mapped DTO: {}", dto);
        return dto;
    }
}
