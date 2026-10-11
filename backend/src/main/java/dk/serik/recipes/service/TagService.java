package dk.serik.recipes.service;

import dk.serik.recipes.dto.TagDTO;

import java.util.List;
import java.util.Optional;


public interface TagService {
    Optional<TagDTO> findById(String id);

    List<TagDTO> findAll();

    List<TagDTO> findAllByNameContains(String name);

    TagDTO save(TagDTO dto);

    boolean delete(String id);

    TagDTO update(TagDTO dto);
}
