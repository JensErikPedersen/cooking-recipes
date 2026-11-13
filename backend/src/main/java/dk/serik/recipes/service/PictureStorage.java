package dk.serik.recipes.service;

import dk.serik.recipes.exceptions.ApplicationErrorCodes;
import dk.serik.recipes.exceptions.ServiceException;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * A picture's files, in {@code app.pictures.dir}: {@code {id}-small.jpg}, {@code -medium.jpg} and
 * {@code -large.jpg} (docs/PLAN_V8.md decisions 1 and 4). Made once, on upload, and never changed.
 * The original is not kept.
 */
@Component
public class PictureStorage {

    // The page's cream, docs/PLAN_V7.md decision 1: what a transparent PNG shows through.
    private static final Color CREAM = new Color(0xF5EFE3);

    private static final float QUALITY = 0.85f;

    private final Path directory;

    public PictureStorage(@Value("${app.pictures.dir}") Path directory) throws IOException {
        this.directory = Files.createDirectories(directory);
    }

    /**
     * Writes the three sizes of a JPEG or PNG, turned upright by its EXIF orientation and never
     * enlarged. Anything else - by its content, not its name - is a 400 on the field {@code file}.
     */
    public void write(UUID id, byte[] bytes) {
        BufferedImage picture = flatten(read(bytes));
        try {
            for (PictureSize size : PictureSize.values()) {
                Thumbnails.of(picture)
                        .width(Math.min(size.width(), picture.getWidth()))
                        .outputFormat("jpg")
                        .outputQuality(QUALITY)
                        .toFile(file(id, size).toFile());
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public Path file(UUID id, PictureSize size) {
        return directory.resolve(id + "-" + size.key() + ".jpg");
    }

    /** Deletes all three; one already gone is fine. */
    public void delete(UUID id) {
        try {
            for (PictureSize size : PictureSize.values()) {
                Files.deleteIfExists(file(id, size));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    // Thumbnailator reads the EXIF orientation from the file and applies it; scale(1) keeps the size.
    // The first bytes decide the format: ImageIO would also read GIF and BMP.
    private static BufferedImage read(byte[] bytes) {
        if (!isJpeg(bytes) && !isPng(bytes)) {
            throw unreadable();
        }
        try {
            return Thumbnails.of(new ByteArrayInputStream(bytes)).scale(1).asBufferedImage();
        } catch (IOException e) {
            throw unreadable();
        }
    }

    // JPEG has no transparency: drawn onto cream, a transparent pixel is cream, not black.
    private static BufferedImage flatten(BufferedImage picture) {
        BufferedImage rgb = new BufferedImage(picture.getWidth(), picture.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = rgb.createGraphics();
        graphics.setColor(CREAM);
        graphics.fillRect(0, 0, rgb.getWidth(), rgb.getHeight());
        graphics.drawImage(picture, 0, 0, null);
        graphics.dispose();
        return rgb;
    }

    private static boolean isJpeg(byte[] b) {
        return b.length > 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF;
    }

    private static boolean isPng(byte[] b) {
        return b.length > 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G';
    }

    private static ServiceException unreadable() {
        return ServiceException.builder()
                .message("The file is not a JPEG or PNG picture")
                .code(ApplicationErrorCodes.PICTURE_UNREADABLE.getCode())
                .httpStatus(HttpStatus.BAD_REQUEST)
                .field("file")
                .build();
    }
}
