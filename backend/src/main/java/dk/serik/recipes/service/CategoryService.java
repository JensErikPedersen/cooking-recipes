package dk.serik.recipes.service;

import dk.serik.recipes.dto.CategoryDTO;

import java.util.List;
import java.util.Optional;

public interface CategoryService {

	List<CategoryDTO> findAll();

	Optional<CategoryDTO> findById(String id);

	List<CategoryDTO> findAllByNameContains(String name);

	CategoryDTO save(CategoryDTO categoryDto);

	boolean delete(String id);

	CategoryDTO update(CategoryDTO dto);

}
