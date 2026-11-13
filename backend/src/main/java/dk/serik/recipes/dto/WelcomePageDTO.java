package dk.serik.recipes.dto;

/**
 * The Welcome page's picture, or {@code null} for none. On a write only the picture's {@code id}
 * is read.
 */
public record WelcomePageDTO(PictureDTO picture) {
}
