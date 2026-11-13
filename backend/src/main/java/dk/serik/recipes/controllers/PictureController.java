package dk.serik.recipes.controllers;

import dk.serik.recipes.dto.PictureDTO;
import dk.serik.recipes.dto.PictureUploadDTO;
import dk.serik.recipes.exceptions.ApplicationErrorCodes;
import dk.serik.recipes.exceptions.ServiceException;
import dk.serik.recipes.service.PictureService;
import dk.serik.recipes.validator.UUID;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Duration;
import java.util.List;

/**
 * The picture library (docs/PLAN_V8.md). A picture is uploaded as a multipart form and kept in three
 * sizes, each served as a JPEG of its own.
 */
@RestController
@RequestMapping(PictureController.PATH)
@AllArgsConstructor
public class PictureController {

    // Also the start of each Location: relative, so it never names the backend's address behind the proxy.
    static final String PATH = "/api/v1/pictures";

    // A picture's file never changes (decision 3), so the browser keeps each size for a year and
    // never asks again. Private: it is served only to a signed-in account.
    private static final CacheControl FOREVER = CacheControl.maxAge(Duration.ofDays(365)).cachePrivate().immutable();

    private final PictureService pictureService;

    @GetMapping
    public List<PictureDTO> findAll() {
        return pictureService.findAll();
    }

    @GetMapping("/{id}")
    public PictureDTO findById(@PathVariable @UUID String id) {
        return pictureService.findById(id).orElseThrow(() -> notFound(id));
    }

    @GetMapping("/{id}/{size}")
    public ResponseEntity<Resource> file(@PathVariable @UUID String id, @PathVariable String size) {
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .cacheControl(FOREVER)
                .body(new FileSystemResource(pictureService.file(id, size)));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PictureDTO> create(@Valid @ModelAttribute PictureUploadDTO upload) {
        PictureDTO saved = pictureService.create(upload);
        URI location = UriComponentsBuilder.fromPath(PATH)
                .path("/{id}")
                .buildAndExpand(saved.getId())
                .toUri();
        return ResponseEntity.created(location).body(saved);
    }

    /** Name and alt text; anything else in the body is ignored. The path id is authoritative. */
    @PutMapping("/{id}")
    public PictureDTO update(@PathVariable @UUID String id, @Valid @RequestBody PictureDTO pictureDTO) {
        pictureDTO.setId(id);
        return pictureService.update(pictureDTO);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable @UUID String id) {
        if (!pictureService.delete(id)) {
            throw notFound(id);
        }
        return ResponseEntity.noContent().build();
    }

    private ServiceException notFound(String id) {
        return ServiceException.builder()
                .message(String.format("Picture with id %s could not be found", id))
                .code(ApplicationErrorCodes.PICTURE_NOT_FOUND.getCode())
                .httpStatus(HttpStatus.NOT_FOUND)
                .build();
    }
}
