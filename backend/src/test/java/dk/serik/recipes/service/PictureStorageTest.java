package dk.serik.recipes.service;

import dk.serik.recipes.exceptions.ApplicationErrorCodes;
import dk.serik.recipes.exceptions.ServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static dk.serik.recipes.testutil.TestPictures.gif;
import static dk.serik.recipes.testutil.TestPictures.jpeg;
import static dk.serik.recipes.testutil.TestPictures.jpegTurnedByExif;
import static dk.serik.recipes.testutil.TestPictures.transparentPng;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PictureStorageTest {

    @TempDir
    Path directory;

    private PictureStorage storage;

    private final UUID id = UUID.randomUUID();

    @BeforeEach
    void setUp() throws IOException {
        storage = new PictureStorage(directory);
    }

    @Test
    @DisplayName("Given a landscape JPEG of 2000 x 1000, When written, Then three JPEGs 400, 800 and 1600 wide, keeping 2:1")
    void shouldWriteThreeSizesOfALandscape() throws IOException {
        storage.write(id, jpeg(2000, 1000));

        assertSize(PictureSize.SMALL, 400, 200);
        assertSize(PictureSize.MEDIUM, 800, 400);
        assertSize(PictureSize.LARGE, 1600, 800);
        assertThat(Files.list(directory).map(path -> path.getFileName().toString()))
                .containsExactlyInAnyOrder(id + "-small.jpg", id + "-medium.jpg", id + "-large.jpg");
    }

    @Test
    @DisplayName("Given a portrait JPEG of 1000 x 2000, When written, Then large is 1000 wide - never enlarged")
    void shouldWriteAPortraitWithoutEnlarging() throws IOException {
        storage.write(id, jpeg(1000, 2000));

        assertSize(PictureSize.SMALL, 400, 800);
        assertSize(PictureSize.MEDIUM, 800, 1600);
        assertSize(PictureSize.LARGE, 1000, 2000);
    }

    @Test
    @DisplayName("Given a picture 300 px wide, When written, Then all three sizes are 300 wide")
    void shouldNotEnlargeASmallPicture() throws IOException {
        storage.write(id, jpeg(300, 200));

        for (PictureSize size : PictureSize.values()) {
            assertSize(size, 300, 200);
        }
    }

    @Test
    @DisplayName("Given a landscape photo with EXIF orientation 6, When written, Then it is upright: portrait, red on top")
    void shouldTurnByExifOrientation() throws IOException {
        storage.write(id, jpegTurnedByExif(1200, 600));

        BufferedImage large = read(PictureSize.LARGE);
        assertThat(large.getWidth()).isEqualTo(600);
        assertThat(large.getHeight()).isEqualTo(1200);
        assertThat(isReddish(large.getRGB(300, 100))).as("top is red").isTrue();
        assertThat(isReddish(large.getRGB(300, 1100))).as("bottom is blue").isFalse();
    }

    @Test
    @DisplayName("Given a transparent PNG, When written, Then a JPEG whose pixels are cream, not black")
    void shouldFlattenTransparencyOntoCream() throws IOException {
        storage.write(id, transparentPng(500, 500));

        Color pixel = new Color(read(PictureSize.SMALL).getRGB(200, 200));
        assertThat(pixel.getRed()).isBetween(0xF5 - 4, 0xF5 + 4);
        assertThat(pixel.getGreen()).isBetween(0xEF - 4, 0xEF + 4);
        assertThat(pixel.getBlue()).isBetween(0xE3 - 4, 0xE3 + 4);
    }

    @Test
    @DisplayName("Given a file that is not a picture, When written, Then 400 on the file field, and nothing on disk")
    void shouldRefuseWhatIsNotAPicture() throws IOException {
        assertRefused("not a picture".getBytes(StandardCharsets.UTF_8));
        assertThat(Files.list(directory)).isEmpty();
    }

    @Test
    @DisplayName("Given a GIF, which ImageIO reads, When written, Then refused: JPEG and PNG only")
    void shouldRefuseAGif() throws IOException {
        assertRefused(gif(100, 100));
        assertThat(Files.list(directory)).isEmpty();
    }

    @Test
    @DisplayName("Given a JPEG cut short after its header, When written, Then refused")
    void shouldRefuseABrokenJpeg() {
        byte[] whole = jpeg(400, 300);
        byte[] cut = java.util.Arrays.copyOf(whole, 20);

        assertRefused(cut);
    }

    @Test
    @DisplayName("Given a written picture, When deleted, Then its three files are gone; deleting again is fine")
    void shouldDeleteAllThree() throws IOException {
        storage.write(id, jpeg(800, 600));

        storage.delete(id);
        storage.delete(id);

        assertThat(Files.list(directory)).isEmpty();
    }

    private void assertRefused(byte[] bytes) {
        assertThatThrownBy(() -> storage.write(id, bytes))
                .isInstanceOfSatisfying(ServiceException.class, e -> {
                    assertThat(e.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(e.getCode()).isEqualTo(ApplicationErrorCodes.PICTURE_UNREADABLE.getCode());
                    assertThat(e.getFieldErrors()).singleElement()
                            .satisfies(error -> assertThat(error.getObjectName()).isEqualTo("file"));
                });
    }

    private void assertSize(PictureSize size, int width, int height) throws IOException {
        BufferedImage image = read(size);
        assertThat(image.getWidth()).as(size + " width").isEqualTo(width);
        assertThat(image.getHeight()).as(size + " height").isEqualTo(height);
    }

    private BufferedImage read(PictureSize size) throws IOException {
        Path file = storage.file(id, size);
        assertThat(Files.readAllBytes(file)).startsWith((byte) 0xFF, (byte) 0xD8);
        return ImageIO.read(file.toFile());
    }

    private static boolean isReddish(int rgb) {
        Color color = new Color(rgb);
        return color.getRed() > 150 && color.getBlue() < 100;
    }
}
