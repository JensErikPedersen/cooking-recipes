package dk.serik.recipes.service;

import dk.serik.recipes.bean.Session;
import dk.serik.recipes.dto.IngredientDTO;
import dk.serik.recipes.exceptions.ApplicationErrorCodes;
import dk.serik.recipes.exceptions.ServiceException;
import dk.serik.recipes.mapper.IngredientMapper;
import dk.serik.recipes.model.Ingredient;
import dk.serik.recipes.repository.IngredientJpaRepository;
import dk.serik.recipes.repository.RecipeIngredientJpaRepository;
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
public class IngredientServiceImpl implements IngredientService {

    private IngredientJpaRepository repository;
    private RecipeIngredientJpaRepository recipeIngredientRepository;
    private Session session;
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED, readOnly = true, timeout = 5)
    public List<IngredientDTO> findAll() {
        return repository.findAll().stream()
                .map(IngredientMapper::from)
                .collect(Collectors.toList());
    }
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED, readOnly = true, timeout = 5)
    public Optional<IngredientDTO> findById(String id) {
        Optional<Ingredient> ingredient = repository.findById(ServiceArguments.toUuid(id, ApplicationErrorCodes.INGREDIENT_ID_IS_NULL, "Ingredient"));
        if(ingredient.isPresent()) {
            return Optional.of(IngredientMapper.from(ingredient.get()));
        }
        return Optional.empty();
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED, readOnly = true, timeout = 5)
    public Optional<IngredientDTO> findByName(String name) {
        Optional<Ingredient> ingredient =
                repository.findByName(name);

        if(ingredient.isPresent()) {
            return Optional.of(IngredientMapper.from(ingredient.get()));
        }

        return Optional.empty();
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED, readOnly = true, timeout = 5)
    public List<IngredientDTO> findAllByNameContains(String name) {
        return repository.findAllByNameContains(name).stream()
                .map(IngredientMapper::from)
                .collect(Collectors.toList());
    }

    @Override
    public IngredientDTO save(IngredientDTO dto) {
        if(Objects.nonNull(dto)) {
            rejectDuplicateName(dto.getName(), null);
            Ingredient entity = new Ingredient();
            entity.setDescription(dto.getDescription());
            entity.setName(dto.getName());
            entity.setCreatedBy(session.getUserName());
            Ingredient managedIngredient = repository.save(entity);
            return IngredientMapper.from(managedIngredient);
        }
        throw ServiceException.builder()
                .code(ApplicationErrorCodes.INGREDIENT_DTO_IS_NULL.getCode())
                .httpStatus(HttpStatus.BAD_REQUEST)
                .message("Invalid argument exception: IngredientDTO is null")
                .build();
    }

    @Override
    public boolean delete(String id) {
        Optional<Ingredient> optional = repository.findById(ServiceArguments.toUuid(id, ApplicationErrorCodes.INGREDIENT_ID_IS_NULL, "Ingredient"));
        if(optional.isPresent()) {
            rejectDeleteInUse(optional.get());
            repository.delete(optional.get());
            return true;
        }
        return false;
    }

    // As in CategoryServiceImpl: the unique index on name and the recipe_ingredient foreign key
    // refuse these anyway, at commit and without a reason; checking first gives one.
    private void rejectDuplicateName(String name, UUID ownId) {
        repository.findByName(name)
                .filter(existing -> !existing.getId().equals(ownId))
                .ifPresent(existing -> {
                    throw ServiceException.builder()
                            .message(String.format("An ingredient named '%s' already exists", name))
                            .code(ApplicationErrorCodes.INGREDIENT_ALREADY_EXISTS.getCode())
                            .httpStatus(HttpStatus.CONFLICT)
                            .field("name")
                            .build();
                });
    }

    private void rejectDeleteInUse(Ingredient ingredient) {
        long recipes = recipeIngredientRepository.countByIngredientId(ingredient.getId());
        if (recipes > 0) {
            throw ServiceException.builder()
                    .message(String.format("Ingredient '%s' is used by %d %s and cannot be deleted",
                            ingredient.getName(), recipes, recipes == 1 ? "recipe" : "recipes"))
                    .code(ApplicationErrorCodes.INGREDIENT_IN_USE.getCode())
                    .httpStatus(HttpStatus.CONFLICT)
                    .build();
        }
    }

    @Override
    public IngredientDTO update(IngredientDTO dto) {
        if(Objects.isNull(dto) || Objects.isNull(dto.getId())) {
            throw ServiceException.badRequest(ApplicationErrorCodes.INGREDIENT_ID_IS_NULL, "Ingredient Id is required for an update");
        }
        Optional<Ingredient> optionalIngredient = repository.findById(ServiceArguments.toUuid(dto.getId(), ApplicationErrorCodes.INGREDIENT_ID_IS_NULL, "Ingredient"));
        if(optionalIngredient.isPresent()) {
            Ingredient managedIngredient = optionalIngredient.get();
            rejectDuplicateName(dto.getName(), managedIngredient.getId());
            managedIngredient.setDescription(dto.getDescription());
            managedIngredient.setName(dto.getName());
            Ingredient savedIngredient = repository.save(managedIngredient);
            return IngredientMapper.from(savedIngredient);
        } else {
            throw ServiceException.builder()
                    .message("Could not update Ingredient with id " + dto.getId() + " since it was not found")
                    .code(ApplicationErrorCodes.INGREDIENT_NOT_FOUND.getCode())
                    .httpStatus(HttpStatus.NOT_FOUND)
                    .build();
        }
    }
}
