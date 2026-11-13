package dk.serik.recipes.testutil;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * Pictures for the tests, made here rather than committed: the left half red, the right half blue,
 * so a test can tell which way a picture was turned.
 */
public final class TestPictures {

    private TestPictures() {
    }

    public static byte[] jpeg(int width, int height) {
        return encode(halves(width, height, BufferedImage.TYPE_INT_RGB), "jpg");
    }

    public static byte[] gif(int width, int height) {
        return encode(halves(width, height, BufferedImage.TYPE_INT_RGB), "gif");
    }

    /** A PNG whose every pixel is fully transparent. */
    public static byte[] transparentPng(int width, int height) {
        return encode(new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB), "png");
    }

    /**
     * A JPEG as a phone writes one held upright: the pixels as the sensor saw them, landscape, and
     * an EXIF orientation of 6 - turn 90 degrees clockwise to show it. Shown right, it is portrait,
     * the red half on top.
     */
    public static byte[] jpegTurnedByExif(int storedWidth, int storedHeight) {
        byte[] jpeg = jpeg(storedWidth, storedHeight);
        byte[] tiff = {
                'M', 'M', 0, 42, 0, 0, 0, 8,                // big-endian TIFF header, first IFD at 8
                0, 1,                                       // one entry
                0x01, 0x12, 0, 3, 0, 0, 0, 1, 0, 6, 0, 0,   // Orientation, SHORT, count 1, value 6
                0, 0, 0, 0};                                // no next IFD
        byte[] exif = {'E', 'x', 'i', 'f', 0, 0};
        int length = 2 + exif.length + tiff.length;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(jpeg, 0, 2);                              // SOI
        out.write(0xFF);
        out.write(0xE1);                                    // APP1
        out.write(length >> 8);
        out.write(length & 0xFF);
        out.write(exif, 0, exif.length);
        out.write(tiff, 0, tiff.length);
        out.write(jpeg, 2, jpeg.length - 2);
        return out.toByteArray();
    }

    public static BufferedImage decode(byte[] bytes) {
        try {
            return ImageIO.read(new java.io.ByteArrayInputStream(bytes));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static BufferedImage halves(int width, int height, int type) {
        BufferedImage image = new BufferedImage(width, height, type);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.RED);
        graphics.fillRect(0, 0, width / 2, height);
        graphics.setColor(Color.BLUE);
        graphics.fillRect(width / 2, 0, width - width / 2, height);
        graphics.dispose();
        return image;
    }

    private static byte[] encode(BufferedImage image, String format) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image, format, out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
