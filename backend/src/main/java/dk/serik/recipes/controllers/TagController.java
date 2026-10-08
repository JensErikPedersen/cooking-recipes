package dk.serik.recipes.controllers;

import dk.serik.recipes.dto.TagDTO;
import dk.serik.recipes.exceptions.ApplicationErrorCodes;
import dk.serik.recipes.exceptions.ServiceException;
import dk.serik.recipes.service.TagService;
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
 * Tags label recipes, shaped like {@link CategoryController}.
 * <p>
 * {@code TagService.findTagByName} is not exposed: the name filter is being standardised across
 * resources as {@code ?name=} backed by a partial match, and Tag has no partial-match query yet.
 * See {@code docs/future_enhancements.md}.
 */
@RestController
@RequestMapping(TagController.PATH)
@AllArgsConstructor
public class TagController {

    // Also the start of each Location: relative, so it never names the backend's address behind the proxy.
    static final String PATH = "/api/v1/tags";

    private final TagService tagService;

    @GetMapping
    public List<TagDTO> findAll() {
        return tagService.findAll();
    }

    @GetMapping("/{id}")
    public TagDTO findById(@PathVariable @UUID String id) {
        return tagService.findById(id)
                .orElseThrow(() -> notFound(id));
    }

    @PostMapping
    public ResponseEntity<TagDTO> create(@Valid @RequestBody TagDTO tagDTO) {
        TagDTO saved = tagService.save(tagDTO);
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
    public TagDTO update(@PathVariable @UUID String id, @Valid @RequestBody TagDTO tagDTO) {
        tagDTO.setId(id);
        return tagService.update(tagDTO);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable @UUID String id) {
        if (!tagService.delete(id)) {
            throw notFound(id);
        }
        return ResponseEntity.noContent().build();
    }

    private ServiceException notFound(String id) {
        return ServiceException.builder()
                .message(String.format("Tag with id %s could not be found", id))
                .code(ApplicationErrorCodes.TAG_NOT_FOUND.getCode())
                .httpStatus(HttpStatus.NOT_FOUND)
                .build();
    }
}
