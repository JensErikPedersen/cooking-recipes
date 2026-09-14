package dk.serik.recipes.service;

import dk.serik.recipes.dto.CategoryDTO;

import java.util.List;
import java.util.Optional;

public interface CategoryService {

	List<CategoryDTO> findAll();

	Optional<CategoryDTO> findById(String id);

	Optional<CategoryDTO> findByName(String string);
	
	CategoryDTO save(CategoryDTO categoryDto);

	boolean delete(String id);

	CategoryDTO update(CategoryDTO dto);

}
