package dk.serik.recipes.service;

import dk.serik.recipes.exceptions.ApplicationErrorCodes;
import dk.serik.recipes.exceptions.ServiceException;

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
}
