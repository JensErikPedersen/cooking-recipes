package dk.serik.recipes.service;

import dk.serik.recipes.bean.Session;
import dk.serik.recipes.dto.PictureDTO;
import dk.serik.recipes.dto.PictureUploadDTO;
import dk.serik.recipes.exceptions.ApplicationErrorCodes;
import dk.serik.recipes.exceptions.ServiceException;
import dk.serik.recipes.mapper.PictureMapper;
import dk.serik.recipes.model.Picture;
import dk.serik.recipes.repository.PictureJpaRepository;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@AllArgsConstructor
@Transactional(
        isolation = Isolation.READ_COMMITTED,
        propagation = Propagation.REQUIRED,
        readOnly = false,
        timeout = 5)
public class PictureServiceImpl implements PictureService {

    private PictureJpaRepository repository;

    private PictureStorage storage;

    private Session session;

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED, readOnly = true, timeout = 5)
    public Optional<PictureDTO> findById(String id) {
        return repository.findById(toUuid(id)).map(PictureMapper::from);
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED, readOnly = true, timeout = 5)
    public List<PictureDTO> findAll() {
        return repository.findAll().stream().map(PictureMapper::from).toList();
    }

    /**
     * The row first, flushed, then the files under its id: a file that is not a picture throws, and
     * the row is rolled back with it. Flushed before resizing, which can take seconds for a large
     * photo: the transaction's timeout is checked at each SQL statement, and an insert left for the
     * commit would come after the resizing.
     */
    @Override
    public PictureDTO create(PictureUploadDTO upload) {
        Picture picture = new Picture();
        picture.setName(upload.name());
        picture.setAltText(upload.altText());
        picture.setCreatedBy(session.getUserName());
        Picture saved = repository.saveAndFlush(picture);
        storage.write(saved.getId(), bytes(upload));
        return PictureMapper.from(saved);
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED, readOnly = true, timeout = 5)
    public Path file(String id, String size) {
        UUID uuid = toUuid(id);
        PictureSize pictureSize = PictureSize.of(size)
                .orElseThrow(() -> notFound(String.format("Picture size '%s' does not exist; use small, medium or large", size)));
        if (!repository.existsById(uuid)) {
            throw notFound(String.format("Picture with id %s could not be found", id));
        }
        return storage.file(uuid, pictureSize);
    }

    @Override
    public boolean delete(String id) {
        Optional<Picture> picture = repository.findById(toUuid(id));
        picture.ifPresent(found -> {
            repository.delete(found);
            storage.delete(found.getId());
        });
        return picture.isPresent();
    }

    private static UUID toUuid(String id) {
        return ServiceArguments.toUuid(id, ApplicationErrorCodes.PICTURE_ID_IS_NULL, "Picture");
    }

    private static byte[] bytes(PictureUploadDTO upload) {
        try {
            return upload.file().getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static ServiceException notFound(String message) {
        return ServiceException.builder()
                .message(message)
                .code(ApplicationErrorCodes.PICTURE_NOT_FOUND.getCode())
                .httpStatus(HttpStatus.NOT_FOUND)
                .build();
    }
}
