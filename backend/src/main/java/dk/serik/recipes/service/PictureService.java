package dk.serik.recipes.service;

import dk.serik.recipes.dto.PictureDTO;
import dk.serik.recipes.dto.PictureUploadDTO;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public interface PictureService {
    Optional<PictureDTO> findById(String id);

    List<PictureDTO> findAll();

    PictureDTO create(PictureUploadDTO upload);

    /** The file of one size of a picture: a 404 for an unknown picture or size. */
    Path file(String id, String size);

    boolean delete(String id);
}
