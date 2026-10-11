package dk.serik.recipes.controllers;

import dk.serik.recipes.dto.UnitDTO;
import dk.serik.recipes.exceptions.ApplicationErrorCodes;
import dk.serik.recipes.exceptions.ServiceException;
import dk.serik.recipes.service.UnitService;
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
 * Units of measure - a small, fixed lookup resource, shaped like {@link CategoryController}.
 * <p>
 * Unlike the other four there is deliberately no {@code ?name=} filter here: units are a fixed
 * handful of measures, so {@code UnitService} exposes no name query and the collection endpoint
 * takes no parameters.
 */
@RestController
@RequestMapping(UnitController.PATH)
@AllArgsConstructor
public class UnitController {

    // Also the start of each Location: relative, so it never names the backend's address behind the proxy.
    static final String PATH = "/api/v1/units";

    private final UnitService unitService;

    @GetMapping
    public List<UnitDTO> findAll() {
        return unitService.findAll();
    }

    @GetMapping("/{id}")
    public UnitDTO findById(@PathVariable @UUID String id) {
        return unitService.findById(id)
                .orElseThrow(() -> notFound(id));
    }

    @PostMapping
    public ResponseEntity<UnitDTO> create(@Valid @RequestBody UnitDTO unitDTO) {
        UnitDTO saved = unitService.save(unitDTO);
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
    public UnitDTO update(@PathVariable @UUID String id, @Valid @RequestBody UnitDTO unitDTO) {
        unitDTO.setId(id);
        return unitService.update(unitDTO);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable @UUID String id) {
        if (!unitService.delete(id)) {
            throw notFound(id);
        }
        return ResponseEntity.noContent().build();
    }

    private ServiceException notFound(String id) {
        return ServiceException.builder()
                .message(String.format("Unit with id %s could not be found", id))
                .code(ApplicationErrorCodes.UNIT_NOT_FOUND.getCode())
                .httpStatus(HttpStatus.NOT_FOUND)
                .build();
    }
}
