package dk.serik.recipes.service;

import dk.serik.recipes.dto.PictureDTO;
import dk.serik.recipes.exceptions.ApplicationErrorCodes;
import dk.serik.recipes.exceptions.ServiceException;
import dk.serik.recipes.model.Picture;
import dk.serik.recipes.repository.PictureJpaRepository;
import org.springframework.http.HttpStatus;

import java.util.Objects;
import java.util.UUID;

/**
 * Argument handling shared by the service layer.
 * <p>
 * Ids reach the services as Strings from outside the application, so a null or malformed value
 * is a client error. Parsing them here keeps {@code UUID.fromString} from throwing a raw
 * {@link IllegalArgumentException}, which would bypass {@link ServiceException} entirely and
 * surface as a 500 from the generic exception handler.
 */
final class ServiceArguments {

    private ServiceArguments() {
    }

    static UUID toUuid(String id, ApplicationErrorCodes code, String label) {
        if (Objects.isNull(id)) {
            throw ServiceException.badRequest(code, label + " Id is null");
        }
        try {
            return UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            throw ServiceException.badRequest(code, String.format("%s Id '%s' is not a valid UUID", label, id));
        }
    }

    /**
     * The picture a recipe, an ingredient or the Welcome page refers to, by its id: {@code null} for
     * none. An unknown one is a 400 on the field {@code picture}, not a 404 - the request is wrong,
     * not its address.
     */
    static Picture toPicture(PictureDTO reference, PictureJpaRepository repository) {
        if (Objects.isNull(reference)) {
            return null;
        }
        return repository.findById(toUuid(reference.getId(), ApplicationErrorCodes.PICTURE_ID_IS_NULL, "Picture"))
                .orElseThrow(() -> ServiceException.builder()
                        .message("This picture does not exist")
                        .code(ApplicationErrorCodes.PICTURE_NOT_FOUND.getCode())
                        .httpStatus(HttpStatus.BAD_REQUEST)
                        .field("picture")
                        .build());
    }
}
