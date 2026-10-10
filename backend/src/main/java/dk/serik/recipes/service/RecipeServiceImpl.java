package dk.serik.recipes.service;

import dk.serik.recipes.dto.RecipeDTO;
import dk.serik.recipes.dto.RecipeIngredientDTO;
import dk.serik.recipes.dto.RecipeRatingDTO;
import dk.serik.recipes.dto.TagDTO;
import dk.serik.recipes.exceptions.ApplicationErrorCodes;
import dk.serik.recipes.exceptions.ServiceException;
import dk.serik.recipes.exceptions.ValidationExceptionEnvelope;
import dk.serik.recipes.mapper.RecipeMapper;
import dk.serik.recipes.model.*;
import dk.serik.recipes.repository.*;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional(
        isolation = Isolation.READ_COMMITTED,
        propagation = Propagation.REQUIRED,
        readOnly = false,
        timeout = 5)
@AllArgsConstructor
public class RecipeServiceImpl implements RecipeService {

    // recipe_ingredient.amount is DECIMAL(6,2); a larger amount would fail in the database.
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("9999.99");

    private RecipeJpaRepository repository;

    private CategoryJpaRepository categoryJpaRepository;

    private RecipeIngredientJpaRepository recipeIngredientJpaRepository;

    private IngredientJpaRepository ingredientJpaRepository;

    private UnitJpaRepository unitJpaRepository;

    private TagJpaRepository tagJpaRepository;

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED, readOnly = true, timeout = 5)
    public List<RecipeDTO> findAllByCategoryName(String categoryName) {
        return repository.findAllByCategoryName(categoryName).stream()
                .map(RecipeMapper::from)
                .toList();

    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED, readOnly = true, timeout = 5)
    public List<RecipeDTO> findAllByNameContains(String name) {
        return repository.findAllByNameContains(name).stream()
                .map(RecipeMapper::from)
                .toList();
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED, readOnly = true, timeout = 5)
    public List<RecipeDTO> findAll() {
        return repository.findAll().stream()
                .map(RecipeMapper::from)
                .toList();
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED, readOnly = true, timeout = 5)
    public Optional<RecipeDTO> findById(String id) {
        Optional<Recipe> recipe = repository.findById(ServiceArguments.toUuid(id, ApplicationErrorCodes.RECIPE_ID_IS_NULL, "Recipe"));
        if(recipe.isPresent()) {
            return Optional.ofNullable(RecipeMapper.from(recipe.get()));
        }
        return Optional.empty();
    }

    @Override
    public RecipeDTO save(RecipeDTO recipeDTO) {
        if(Objects.isNull(recipeDTO)) {
            throw ServiceException.badRequest(ApplicationErrorCodes.RECIPE_DTO_IS_NULL, "RecipeDTO is null");
        }
        rejectRecipeRatings(recipeDTO);
        Recipe recipe = getRecipe(recipeDTO);
        rejectDuplicateName(recipeDTO.getName(), recipe.getId());
        handleCategory(recipeDTO, recipe);
        handleTags(recipeDTO, recipe);
        handleRecipeIngredients(recipeDTO, recipe);
        log.info("Saving recipe: {}", recipe);
        Recipe savedRecipe = repository.save(recipe);
        return RecipeMapper.from(savedRecipe);
    }

    @Override
    public boolean delete(String id) {
        Optional<Recipe> recipe = repository.findById(ServiceArguments.toUuid(id, ApplicationErrorCodes.RECIPE_ID_IS_NULL, "Recipe"));
        if(recipe.isPresent()) {
            repository.delete(recipe.get());
            return true;
        }
        return false;
    }

    @Override
    public RecipeDTO update(RecipeDTO dto) {
        if(Objects.isNull(dto) || Objects.isNull(dto.getId())) {
            throw ServiceException.badRequest(ApplicationErrorCodes.RECIPE_ID_IS_NULL, "Recipe Id is required for an update");
        }
        rejectRecipeRatings(dto);
        Recipe managedRecipe = findRecipeOrThrow(dto.getId());
        rejectDuplicateName(dto.getName(), managedRecipe.getId());
        managedRecipe.setName(dto.getName());
        managedRecipe.setDescription(dto.getDescription());
        managedRecipe.setInstructions(dto.getInstructions());
        handleCategory(dto, managedRecipe);
        handleTags(dto, managedRecipe);
        handleRecipeIngredients(dto, managedRecipe);

        return RecipeMapper.from(repository.save(managedRecipe));
    }

    @Override
    public RecipeDTO addRecipeRating(RecipeRatingDTO dto) {
        // Ratings are deliberately out of scope for this version. Fail loudly rather than
        // returning null, so a caller cannot mistake this for a successful no-op.
        throw new UnsupportedOperationException("Recipe ratings are not supported in this version");
    }

    /**
     * Ratings are out of scope for this version, but {@code RecipeDTO} still carries the field
     * because it is populated on the way out. Accepting them on a write and silently discarding
     * them would lose data, so say so instead. Delete this guard when ratings are implemented.
     */
    private void rejectRecipeRatings(RecipeDTO recipeDTO) {
        if(Objects.nonNull(recipeDTO.getRecipeRatings()) && !recipeDTO.getRecipeRatings().isEmpty()) {
            throw ServiceException.badRequest(ApplicationErrorCodes.RECIPE_RATING_NOT_SUPPORTED,
                    "Recipe ratings cannot be set through this endpoint in this version");
        }
    }

    private Recipe findRecipeOrThrow(String recipeId) {
        return repository.findById(ServiceArguments.toUuid(recipeId, ApplicationErrorCodes.RECIPE_ID_IS_NULL, "Recipe"))
                .orElseThrow(() -> ServiceException.builder()
                        .message("Cannot find recipe with id: " + recipeId)
                        .code(ApplicationErrorCodes.RECIPE_NOT_FOUND.getCode())
                        .httpStatus(HttpStatus.NOT_FOUND)
                        .build());
    }

    private Recipe getRecipe(RecipeDTO recipeDTO) {
        Recipe recipe;
        if(Objects.isNull(recipeDTO.getId())) {
            // new Recipe
            recipe = Recipe.builder()
                    .instructions(recipeDTO.getInstructions())
                    .description(recipeDTO.getDescription())
                    .name(recipeDTO.getName())
                    .build();
        } else {
            Optional<Recipe> recipeOptional = repository.findById(ServiceArguments.toUuid(recipeDTO.getId(), ApplicationErrorCodes.RECIPE_ID_IS_NULL, "Recipe"));
            if(recipeOptional.isPresent()) {
                recipe = recipeOptional.get();
            } else {
                throw ServiceException.builder()
                        .message("Cannot find recipe with id: " + recipeDTO.getId())
                        .code(ApplicationErrorCodes.RECIPE_NOT_FOUND.getCode())
                        .httpStatus(HttpStatus.NOT_FOUND)
                        .build();
            }
        }
        return recipe;
    }

    /**
     * A recipe must reference an existing category. Categories are created through
     * {@link CategoryService} only - saving a recipe is not a back door for creating one,
     * which would turn a mistyped category name into a silent duplicate.
     */
    private void handleCategory(RecipeDTO recipeDTO, Recipe recipe) {
        if(Objects.isNull(recipeDTO.getCategory()) || Objects.isNull(recipeDTO.getCategory().getId())) {
            // Reported on the field, so the form shows it under the category dropdown.
            throw ServiceException.builder()
                    .message("A recipe requires a category")
                    .code(ApplicationErrorCodes.CATEGORY_IS_REQUIRED.getCode())
                    .httpStatus(HttpStatus.BAD_REQUEST)
                    .field("category")
                    .build();
        }
        Category category = categoryJpaRepository
                .findById(ServiceArguments.toUuid(recipeDTO.getCategory().getId(), ApplicationErrorCodes.CATEGORY_ID_IS_NULL, "Category"))
                .orElseThrow(() -> ServiceException.builder()
                        .message("Cannot find category with id: " + recipeDTO.getCategory().getId())
                        .code(ApplicationErrorCodes.CATEGORY_NOT_FOUND.getCode())
                        .httpStatus(HttpStatus.NOT_FOUND)
                        .build());
        recipe.setCategory(category);
    }

    // As in CategoryServiceImpl: the unique index on name refuses a duplicate anyway, at commit
    // and without saying which field; checking first lets the error name it.
    private void rejectDuplicateName(String name, UUID ownId) {
        repository.findByName(name)
                .filter(existing -> !existing.getId().equals(ownId))
                .ifPresent(existing -> {
                    throw ServiceException.builder()
                            .message(String.format("A recipe named '%s' already exists", name))
                            .code(ApplicationErrorCodes.RECIPE_ALREADY_EXISTS.getCode())
                            .httpStatus(HttpStatus.CONFLICT)
                            .field("name")
                            .build();
                });
    }

    /**
     * Sets the recipe's tags to exactly those listed, each an existing tag found by id. Like the
     * category, a recipe never creates a tag. Absent tags leave the recipe's tags as they are.
     */
    private void handleTags(RecipeDTO recipeDTO, Recipe recipe) {
        if(Objects.isNull(recipeDTO.getTags())) {
            return;
        }
        Set<Tag> tags = new HashSet<>();
        for(TagDTO tagDTO : recipeDTO.getTags()) {
            String tagId = Objects.isNull(tagDTO) ? null : tagDTO.getId();
            tags.add(tagJpaRepository
                    .findById(ServiceArguments.toUuid(tagId, ApplicationErrorCodes.TAG_ID_IS_NULL, "Tag"))
                    .orElseThrow(() -> ServiceException.builder()
                            .message("Cannot find tag with id: " + tagId)
                            .code(ApplicationErrorCodes.TAG_NOT_FOUND.getCode())
                            .httpStatus(HttpStatus.NOT_FOUND)
                            .build()));
        }
        recipe.setTags(tags);
    }

    /**
     * Resolves the nested ingredient payload onto the recipe.
     * <p>
     * Every line is checked before any is attached, and all problems are reported together in one
     * 400 rather than one-at-a-time, because a caller fixing a bulk payload needs the whole list.
     * Each is reported on its line and input - {@code recipeIngredients[2].amount} - for the recipe
     * form to show there, so one line can carry a problem on each of its inputs.
     * Nothing is skipped silently: this method used to log {@code "RecipeIngredientDTO is not
     * valid"} and drop the entry, so a create carrying ingredients returned 201 describing a recipe
     * that had none.
     * <p>
     * The nested {@code recipeId} is ignored. On create the recipe has no id yet, so requiring one
     * dropped every ingredient; on update the recipe being edited is authoritative. This is the same
     * rule the controllers apply to path ids.
     * <p>
     * The payload is the whole list. A line already on the recipe takes the payload's amount and
     * unit, and a line left out is removed - before Part 7a an existing line was kept unchanged and
     * an omitted one stayed, so an edit could neither change nor remove an ingredient. Absent lines
     * leave the recipe's lines as they are.
     */
    private void handleRecipeIngredients(RecipeDTO recipeDTO, Recipe recipe) {
        if(Objects.isNull(recipeDTO.getRecipeIngredients())) {
            // a recipe without ingredients is valid input, not an error
            return;
        }

        List<ValidationExceptionEnvelope> problems = new ArrayList<>();
        List<RecipeIngredient> resolved = new ArrayList<>();
        // The recipe keeps its lines in a set keyed by ingredient, which would silently drop the
        // second of two lines for the same ingredient. This holds the line each one is first on.
        Map<UUID, Integer> firstLine = new HashMap<>();

        List<RecipeIngredientDTO> lines = recipeDTO.getRecipeIngredients();
        for(int index = 0; index < lines.size(); index++) {
            String field = "recipeIngredients[" + index + "].";
            // a null in the list is a line with nothing chosen
            RecipeIngredientDTO dto = Objects.requireNonNullElseGet(lines.get(index), () -> RecipeIngredientDTO.builder().build());

            // Checked in the order the form shows them: ingredient, amount, unit.
            UUID ingredientId = null;
            Optional<Ingredient> ingredient = Optional.empty();
            if(Objects.isNull(dto.getIngredientId())) {
                problems.add(new ValidationExceptionEnvelope(field + "ingredientId", "Choose an ingredient."));
            } else {
                ingredientId = ServiceArguments.toUuid(dto.getIngredientId(), ApplicationErrorCodes.INGREDIENT_ID_IS_NULL, "Ingredient");
                ingredient = ingredientJpaRepository.findById(ingredientId);
                if(ingredient.isEmpty()) {
                    problems.add(new ValidationExceptionEnvelope(field + "ingredientId", "This ingredient does not exist."));
                } else if(firstLine.containsKey(ingredientId)) {
                    problems.add(new ValidationExceptionEnvelope(field + "ingredientId",
                            ingredient.get().getName() + " is already on line " + (firstLine.get(ingredientId) + 1) + "."));
                } else {
                    firstLine.put(ingredientId, index);
                }
            }

            if(Objects.isNull(dto.getAmount()) || dto.getAmount().signum() <= 0) {
                problems.add(new ValidationExceptionEnvelope(field + "amount", "Needs an amount above zero."));
            } else if(dto.getAmount().compareTo(MAX_AMOUNT) > 0) {
                problems.add(new ValidationExceptionEnvelope(field + "amount", "At most " + MAX_AMOUNT.toPlainString() + "."));
            }

            Optional<Unit> unit = Optional.empty();
            if(Objects.isNull(dto.getUnitId())) {
                problems.add(new ValidationExceptionEnvelope(field + "unitId", "Choose a unit."));
            } else {
                unit = unitJpaRepository.findById(ServiceArguments.toUuid(dto.getUnitId(), ApplicationErrorCodes.UNIT_ID_IS_NULL, "Unit"));
                if(unit.isEmpty()) {
                    problems.add(new ValidationExceptionEnvelope(field + "unitId", "This unit does not exist."));
                }
            }

            if(!problems.isEmpty()) {
                // nothing will be written, so there is no line to build
                continue;
            }

            // an existing line can only be found once the recipe itself has been persisted
            Optional<RecipeIngredient> existing = Objects.isNull(recipe.getId())
                    ? Optional.empty()
                    : recipeIngredientJpaRepository.findByRecipeIdAndIngredientId(recipe.getId(), ingredientId);
            Ingredient chosen = ingredient.get();
            RecipeIngredient line = existing.orElseGet(() -> RecipeIngredient.builder()
                    .recipe(recipe)
                    .ingredient(chosen)
                    .build());
            line.setUnit(unit.get());
            line.setAmount(dto.getAmount());
            resolved.add(line);
        }

        if(!problems.isEmpty()) {
            throw ServiceException.builder()
                    .message("One or more ingredient lines are invalid")
                    .code(ApplicationErrorCodes.RECIPE_INGREDIENTS_INVALID.getCode())
                    .httpStatus(HttpStatus.BAD_REQUEST)
                    .fieldErrors(problems)
                    .build();
        }

        // The association has no orphanRemoval, so a dropped line's row is deleted explicitly.
        if(Objects.nonNull(recipe.getRecipeIngredients())) {
            List<RecipeIngredient> dropped = recipe.getRecipeIngredients().stream()
                    .filter(line -> !resolved.contains(line))
                    .toList();
            dropped.forEach(line -> {
                recipe.getRecipeIngredients().remove(line);
                recipeIngredientJpaRepository.delete(line);
            });
        }
        resolved.forEach(recipe::addRecipeIngredient);
    }
}
