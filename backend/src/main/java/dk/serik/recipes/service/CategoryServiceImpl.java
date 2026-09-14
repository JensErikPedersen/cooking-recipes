package dk.serik.recipes.service;

import dk.serik.recipes.bean.Session;
import dk.serik.recipes.dto.CategoryDTO;
import dk.serik.recipes.exceptions.ApplicationErrorCodes;
import dk.serik.recipes.exceptions.ServiceException;
import dk.serik.recipes.mapper.CategoryMapper;
import dk.serik.recipes.model.Category;
import dk.serik.recipes.repository.CategoryJpaRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@Transactional(
		  isolation = Isolation.READ_COMMITTED,
		  propagation = Propagation.REQUIRED, 
		  readOnly = false, 
		  timeout = 5)
@Slf4j
public class CategoryServiceImpl implements CategoryService {
	
	private CategoryJpaRepository categoryJpaRepository;

	private Session session;

	@Override
	@Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED, readOnly = true, timeout = 5)
	public List<CategoryDTO> findAll() {
		return categoryJpaRepository.findAll().stream()
				.map(CategoryMapper::from)
				.collect(Collectors.toList());
	}

	@Override
	@Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED, readOnly = true, timeout = 5)
	public Optional<CategoryDTO> findById(String id) {
		Optional<Category> categoryEntity = categoryJpaRepository.findById(ServiceArguments.toUuid(id, ApplicationErrorCodes.CATEGORY_ID_IS_NULL, "Category"));
		if(categoryEntity.isPresent()) {
			return Optional.ofNullable(CategoryMapper.from(categoryEntity.get()));
		}
		
		return Optional.empty();
	}

	@Override
	@Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED, readOnly = true, timeout = 5)
	public Optional<CategoryDTO> findByName(String name) {
		Optional<Category> categoryEntity =  categoryJpaRepository.findByName(name);
		
		if(categoryEntity.isPresent()) {
			return Optional.ofNullable(CategoryMapper.from(categoryEntity.get()));
		}
		
		return Optional.empty();
	}

	@Override
	public CategoryDTO save(CategoryDTO categoryDto) {
		if(Objects.isNull(categoryDto)) {
			throw ServiceException.badRequest(ApplicationErrorCodes.CATEGORY_DTO_IS_NULL, "CategoryDTO is null");
		}
		Category entity = new Category();
		entity.setDescription(categoryDto.getDescription());
		entity.setName(categoryDto.getName());
		entity.setCreatedBy(session.getUserName());
		Category managedCategory = categoryJpaRepository.save(entity);
		return CategoryMapper.from(managedCategory);
	}

	@Override
	public boolean delete(String id) {
		Optional<Category> toBeDeleted = categoryJpaRepository.findById(ServiceArguments.toUuid(id, ApplicationErrorCodes.CATEGORY_ID_IS_NULL, "Category"));
		if(toBeDeleted.isPresent()) {
			categoryJpaRepository.delete(toBeDeleted.get());
			return true;
		}
		return false;
	}

	@Override
	public CategoryDTO update(CategoryDTO dto) {
		if(Objects.isNull(dto) || Objects.isNull(dto.getId())) {
			throw ServiceException.badRequest(ApplicationErrorCodes.CATEGORY_ID_IS_NULL, "Category Id is required for an update");
		}
		Optional<Category> optional = categoryJpaRepository.findById(ServiceArguments.toUuid(dto.getId(), ApplicationErrorCodes.CATEGORY_ID_IS_NULL, "Category"));
		if(optional.isPresent()) {
			Category managedCategory = optional.get();
			managedCategory.setName(dto.getName());
			managedCategory.setDescription(dto.getDescription());
			Category savedCategory = categoryJpaRepository.save(managedCategory);
			return CategoryMapper.from(savedCategory);
		}
		log.info(String.format("Category with id %s could not be found", dto.getId()));
		throw ServiceException.builder()
				.message(String.format("Category with id %s could not be found", dto.getId()))
				.httpStatus(HttpStatus.NOT_FOUND)
				.code(ApplicationErrorCodes.CATEGORY_NOT_FOUND.getCode())
				.build();
	}
}
