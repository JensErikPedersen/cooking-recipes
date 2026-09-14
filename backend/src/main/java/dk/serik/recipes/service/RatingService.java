package dk.serik.recipes.service;

import dk.serik.recipes.dto.RatingDTO;

import java.util.List;
import java.util.Optional;

public interface RatingService {

    Optional<RatingDTO> findById(String id);

    List<RatingDTO> findAll();

    RatingDTO save(RatingDTO dto);

    boolean delete(String id);

    RatingDTO update(RatingDTO dto);

}
