package dk.serik.recipes.service;

import java.util.Arrays;
import java.util.Optional;

/**
 * The three widths every picture is kept in (docs/PLAN_V8.md decision 4). The browser picks one by
 * the space it has; the name is the last part of the file's address.
 */
public enum PictureSize {
    SMALL(400),
    MEDIUM(800),
    LARGE(1600);

    private final int width;

    PictureSize(int width) {
        this.width = width;
    }

    public int width() {
        return width;
    }

    public String key() {
        return name().toLowerCase();
    }

    /** {@code small}, {@code medium} or {@code large}, as in the address; empty for anything else. */
    public static Optional<PictureSize> of(String key) {
        return Arrays.stream(values()).filter(size -> size.key().equals(key)).findFirst();
    }
}
