package dk.serik.recipes.controllers;

import dk.serik.recipes.dto.RecipeDTO;
import dk.serik.recipes.dto.RecipeIngredientDTO;
import dk.serik.recipes.exceptions.ApplicationErrorCodes;
import dk.serik.recipes.exceptions.ServiceException;
import dk.serik.recipes.service.RecipeService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * Recipes, plus their ingredients as a nested sub-resource.
 * <p>
 * Ratings are read-only in this version: {@code RecipeMapper} populates {@code recipeRatings} on
 * the way out, but writes carrying ratings are rejected by the service with 400. There are
 * deliberately no {@code /ratings} routes - see {@code docs/future_enhancements.md}.
 */
@RestController
@RequestMapping("/api/v1/recipes")
@AllArgsConstructor
public class RecipeController {

    private final RecipeService recipeService;

    @GetMapping
    public List<RecipeDTO> findAll() {
        return recipeService.findAll();
    }

    @GetMapping("/{id}")
    public RecipeDTO findById(@PathVariable String id) {
        return recipeService.findById(id)
                .orElseThrow(() -> notFound(id));
    }

    @PostMapping
    public ResponseEntity<RecipeDTO> create(@Valid @RequestBody RecipeDTO recipeDTO) {
        RecipeDTO saved = recipeService.save(recipeDTO);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(saved.getId())
                .toUri();
        return ResponseEntity.created(location).body(saved);
    }

    /**
     * The path id is authoritative; any id in the body is ignored, so a mismatched payload
     * cannot silently update a different row.
     */
    @PutMapping("/{id}")
    public RecipeDTO update(@PathVariable String id, @Valid @RequestBody RecipeDTO recipeDTO) {
        recipeDTO.setId(id);
        return recipeService.update(recipeDTO);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        if (!recipeService.delete(id)) {
            throw notFound(id);
        }
        return ResponseEntity.noContent().build();
    }

    // ---------------------------------------------------------------- ingredients sub-resource

    /**
     * Adds an ingredient to a recipe. The recipe comes from the path; which ingredient, in what
     * amount and unit, comes from the body. Responds with the updated recipe.
     */
    @PostMapping("/{id}/ingredients")
    public ResponseEntity<RecipeDTO> addIngredient(@PathVariable String id,
                                                   @Valid @RequestBody RecipeIngredientDTO recipeIngredientDTO) {
        RecipeDTO updated = recipeService.addRecipeIngredient(withPath(id, recipeIngredientDTO.getIngredientId(), recipeIngredientDTO));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{ingredientId}")
                .buildAndExpand(recipeIngredientDTO.getIngredientId())
                .toUri();
        return ResponseEntity.created(location).body(updated);
    }

    /**
     * Both ids come from the path, so the body carries only the amount and unit.
     */
    @PutMapping("/{id}/ingredients/{ingredientId}")
    public RecipeDTO updateIngredient(@PathVariable String id,
                                      @PathVariable String ingredientId,
                                      @Valid @RequestBody RecipeIngredientDTO recipeIngredientDTO) {
        return recipeService.updateRecipeIngredient(withPath(id, ingredientId, recipeIngredientDTO));
    }

    /**
     * Returns the updated recipe rather than 204, so the caller can see what is left.
     */
    @DeleteMapping("/{id}/ingredients/{ingredientId}")
    public RecipeDTO deleteIngredient(@PathVariable String id, @PathVariable String ingredientId) {
        return recipeService.deleteRecipeIngredient(RecipeIngredientDTO.builder()
                .recipeId(id)
                .ingredientId(ingredientId)
                .build());
    }

    /**
     * Rebuilds the DTO with the path ids authoritative. {@code RecipeIngredientDTO} is immutable,
     * so this is a copy rather than a mutation - and it makes explicit which fields the path owns.
     */
    private RecipeIngredientDTO withPath(String recipeId, String ingredientId, RecipeIngredientDTO body) {
        return RecipeIngredientDTO.builder()
                .recipeId(recipeId)
                .ingredientId(ingredientId)
                .amount(body.getAmount())
                .unitId(body.getUnitId())
                .build();
    }

    private ServiceException notFound(String id) {
        return ServiceException.builder()
                .message(String.format("Recipe with id %s could not be found", id))
                .code(ApplicationErrorCodes.RECIPE_NOT_FOUND.getCode())
                .httpStatus(HttpStatus.NOT_FOUND)
                .build();
    }
}
