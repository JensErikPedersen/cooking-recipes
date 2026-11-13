package dk.serik.recipes.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

/**
 * The multipart form of {@code POST /api/v1/pictures}: the file, a name and an alt text. Bound from
 * the form's parts by name, so a missing one is a 400 on its field like any other DTO's.
 */
public record PictureUploadDTO(
        @NotNull(message = "{dk.serik.models.picture.file.notnull.message}")
        MultipartFile file,
        @NotBlank(message = "{dk.serik.models.picture.name.notblank.message}")
        @Size(max = 255, message = "{dk.serik.models.picture.name.size.message}")
        String name,
        @NotBlank(message = "{dk.serik.models.picture.alttext.notblank.message}")
        @Size(max = 255, message = "{dk.serik.models.picture.alttext.size.message}")
        String altText) {
}
