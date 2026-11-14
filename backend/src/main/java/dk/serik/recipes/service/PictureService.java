package dk.serik.recipes.service;

import dk.serik.recipes.dto.PictureDTO;
import dk.serik.recipes.dto.PictureUploadDTO;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public interface PictureService {
    /** The picture with where it is used. */
    Optional<PictureDTO> findById(String id);

    List<PictureDTO> findAll();

    PictureDTO create(PictureUploadDTO upload);

    /** Its name and alt text only: a picture's file never changes (docs/PLAN_V8.md decision 3). */
    PictureDTO update(PictureDTO dto);

    /** The file of one size of a picture: a 404 for an unknown picture or size. */
    Path file(String id, String size);

    /** A 409 while a recipe, an ingredient or the Welcome page uses it. */
    boolean delete(String id);
}
