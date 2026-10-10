package dk.serik.recipes.controllers;

import dk.serik.recipes.dto.RecipeDTO;
import dk.serik.recipes.exceptions.ApplicationErrorCodes;
import dk.serik.recipes.exceptions.ServiceException;
import dk.serik.recipes.service.RecipeService;
import dk.serik.recipes.validator.UUID;
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
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * Recipes. A recipe's ingredient lines are written with the recipe itself, in its POST or PUT;
 * there is no sub-resource for them.
 * <p>
 * Ratings are read-only in this version: {@code RecipeMapper} populates {@code recipeRatings} on
 * the way out, but writes carrying ratings are rejected by the service with 400. There are
 * deliberately no {@code /ratings} routes - see {@code docs/future_enhancements.md}.
 */
@RestController
@RequestMapping(RecipeController.PATH)
@AllArgsConstructor
public class RecipeController {

    // Also the start of each Location: relative, so it never names the backend's address behind the proxy.
    static final String PATH = "/api/v1/recipes";

    private final RecipeService recipeService;

    @GetMapping
    public List<RecipeDTO> findAll() {
        return recipeService.findAll();
    }

    @GetMapping("/{id}")
    public RecipeDTO findById(@PathVariable @UUID String id) {
        return recipeService.findById(id)
                .orElseThrow(() -> notFound(id));
    }

    @PostMapping
    public ResponseEntity<RecipeDTO> create(@Valid @RequestBody RecipeDTO recipeDTO) {
        RecipeDTO saved = recipeService.save(recipeDTO);
        URI location = UriComponentsBuilder.fromPath(PATH)
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
    public RecipeDTO update(@PathVariable @UUID String id, @Valid @RequestBody RecipeDTO recipeDTO) {
        recipeDTO.setId(id);
        return recipeService.update(recipeDTO);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable @UUID String id) {
        if (!recipeService.delete(id)) {
            throw notFound(id);
        }
        return ResponseEntity.noContent().build();
    }

    private ServiceException notFound(String id) {
        return ServiceException.builder()
                .message(String.format("Recipe with id %s could not be found", id))
                .code(ApplicationErrorCodes.RECIPE_NOT_FOUND.getCode())
                .httpStatus(HttpStatus.NOT_FOUND)
                .build();
    }
}
