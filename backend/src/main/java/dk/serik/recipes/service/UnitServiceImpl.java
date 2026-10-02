package dk.serik.recipes.service;

import dk.serik.recipes.bean.Session;
import dk.serik.recipes.dto.UnitDTO;
import dk.serik.recipes.exceptions.ApplicationErrorCodes;
import dk.serik.recipes.exceptions.ServiceException;
import dk.serik.recipes.mapper.UnitMapper;
import dk.serik.recipes.model.Unit;
import dk.serik.recipes.repository.RecipeIngredientJpaRepository;
import dk.serik.recipes.repository.UnitJpaRepository;
import lombok.AllArgsConstructor;
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
public class UnitServiceImpl implements UnitService {

    private UnitJpaRepository repository;

    private RecipeIngredientJpaRepository recipeIngredientRepository;

    private Session session;
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED, readOnly = true, timeout = 5)
    public Optional<UnitDTO> findById(String id) {
        Optional<Unit> unit = repository.findById(ServiceArguments.toUuid(id, ApplicationErrorCodes.UNIT_ID_IS_NULL, "Unit"));
        if(unit.isPresent()) {
            return Optional.of(UnitMapper.from(unit.get()));
        }
        return Optional.empty();
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED, readOnly = true, timeout = 5)
    public List<UnitDTO> findAll() {
        return repository.findAll().stream()
                .map(UnitMapper::from)
                .collect(Collectors.toList());
    }

    @Override
    public UnitDTO save(UnitDTO dto) {
        if(Objects.nonNull(dto)) {
            rejectDuplicateName(dto.getName(), null);
            Unit toBeSaved = new Unit();
            toBeSaved.setLabel(dto.getLabel());
            toBeSaved.setName(dto.getName());
            toBeSaved.setCreatedBy(session.getUserName());
            Unit saved = repository.save(toBeSaved);
            return UnitMapper.from(saved);
        }
        throw ServiceException.builder()
                .message("Unit is null")
                .code(ApplicationErrorCodes.UNIT_DTO_IS_NULL.getCode())
                .httpStatus(HttpStatus.BAD_REQUEST)
                .build();
    }


    @Override
    public UnitDTO update(UnitDTO dto) {
        if(Objects.isNull(dto) || Objects.isNull(dto.getId())) {
            throw ServiceException.badRequest(ApplicationErrorCodes.UNIT_ID_IS_NULL, "Unit Id is required for an update");
        }
        Optional<Unit> optional = repository.findById(ServiceArguments.toUuid(dto.getId(), ApplicationErrorCodes.UNIT_ID_IS_NULL, "Unit"));
        if(optional.isPresent()) {
            Unit managedUnit = optional.get();
            rejectDuplicateName(dto.getName(), managedUnit.getId());
            managedUnit.setName(dto.getName());
            managedUnit.setLabel(dto.getLabel());
            Unit savedUnit = repository.save(managedUnit);
            return UnitMapper.from(savedUnit);
        } else {
            throw ServiceException.builder()
                    .message("Could not update Unit with id " + dto.getId() + " since it was not found")
                    .code(ApplicationErrorCodes.UNIT_NOT_FOUND.getCode())
                    .httpStatus(HttpStatus.NOT_FOUND)
                    .build();
        }

    }

    @Override
    public boolean delete(String id) {
        Optional<Unit> optional = repository.findById(ServiceArguments.toUuid(id, ApplicationErrorCodes.UNIT_ID_IS_NULL, "Unit"));
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
                            .message(String.format("A unit named '%s' already exists", name))
                            .code(ApplicationErrorCodes.UNIT_ALREADY_EXISTS.getCode())
                            .httpStatus(HttpStatus.CONFLICT)
                            .field("name")
                            .build();
                });
    }

    private void rejectDeleteInUse(Unit unit) {
        long recipes = recipeIngredientRepository.countRecipesByUnitId(unit.getId());
        if (recipes > 0) {
            throw ServiceException.builder()
                    .message(String.format("Unit '%s' is used by %d %s and cannot be deleted",
                            unit.getName(), recipes, recipes == 1 ? "recipe" : "recipes"))
                    .code(ApplicationErrorCodes.UNIT_IN_USE.getCode())
                    .httpStatus(HttpStatus.CONFLICT)
                    .build();
        }
    }

}
