package dk.serik.recipes.controllers;

import dk.serik.recipes.dto.IngredientDTO;
import dk.serik.recipes.exceptions.ApplicationErrorCodes;
import dk.serik.recipes.exceptions.ServiceException;
import dk.serik.recipes.service.IngredientService;
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
 * Ingredients are a flat lookup resource, shaped identically to {@link CategoryController}.
 * <p>
 * The controller stays deliberately thin: id parsing, existence and argument validation all
 * live in {@link IngredientService}, which reports failures as {@link ServiceException}
 * carrying an HTTP status.
 */
@RestController
@RequestMapping("/api/v1/ingredients")
@AllArgsConstructor
public class IngredientController {

    private final IngredientService ingredientService;

    @GetMapping
    public List<IngredientDTO> findAll() {
        return ingredientService.findAll();
    }

    @GetMapping("/{id}")
    public IngredientDTO findById(@PathVariable String id) {
        return ingredientService.findById(id)
                .orElseThrow(() -> notFound(id));
    }

    @PostMapping
    public ResponseEntity<IngredientDTO> create(@Valid @RequestBody IngredientDTO ingredientDTO) {
        IngredientDTO saved = ingredientService.save(ingredientDTO);
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
    public IngredientDTO update(@PathVariable String id, @Valid @RequestBody IngredientDTO ingredientDTO) {
        ingredientDTO.setId(id);
        return ingredientService.update(ingredientDTO);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        if (!ingredientService.delete(id)) {
            throw notFound(id);
        }
        return ResponseEntity.noContent().build();
    }

    private ServiceException notFound(String id) {
        return ServiceException.builder()
                .message(String.format("Ingredient with id %s could not be found", id))
                .code(ApplicationErrorCodes.INGREDIENT_NOT_FOUND.getCode())
                .httpStatus(HttpStatus.NOT_FOUND)
                .build();
    }
}
