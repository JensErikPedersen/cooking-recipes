package dk.serik.recipes.service;

import dk.serik.recipes.dto.IngredientDTO;

import java.util.List;
import java.util.Optional;

public interface IngredientService {

    List<IngredientDTO> findAll();

    Optional<IngredientDTO> findById(String string);

    Optional<IngredientDTO> findByName(String string);

    List<IngredientDTO> findAllByNameContains(String name);

    IngredientDTO save(IngredientDTO dto);

    boolean delete(String id);

    IngredientDTO update(IngredientDTO dto);

}
