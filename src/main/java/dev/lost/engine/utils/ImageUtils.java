package dev.lost.engine.utils;

import org.jetbrains.annotations.Range;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class ImageUtils {
    public static BufferedImage resizeImage(BufferedImage image, int width, int height) {
        BufferedImage resizedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = resizedImage.createGraphics();
        g2d.drawImage(image, 0, 0, width, height, null);
        g2d.dispose();
        return resizedImage;
    }

    public static boolean isImageEmpty(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if ((image.getRGB(x, y) >> 24) != 0) return false;
            }
        }
        return true;
    }

    public static byte[] createWhitePixelImageForHandOrLegModels(@Range(from = 0, to = 223) int pixel) throws IOException {
        BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);

        int x, y;
        int currentPixel = 0;

        for (int row = 0; row < 16; row++) {
            for (int col = 0; col < 16; col++) {
                boolean inForbiddenZone = row <= 3 && col <= 3 || row <= 3 && col >= 12;

                if (!inForbiddenZone) {
                    if (currentPixel == pixel) {
                        x = col;
                        y = row;
                        image.setRGB(x, y, 0xFFFFFFFF);

                        return getImageBytes(image);
                    }
                    currentPixel++;
                }
            }
        }

        // Should never happen
        throw new IllegalArgumentException("Invalid pixel index: " + pixel);
    }

    public static int[] extractPixelColorsFromHandOrLegModel(BufferedImage image) {
        if (image.getWidth() != 16 || image.getHeight() != 16) {
            throw new IllegalArgumentException("Image must be 16x16 pixels");
        }

        int[] colors = new int[224]; // Maximum 224 valid pixels
        int currentPixel = 0;

        for (int row = 0; row < 16; row++) {
            for (int col = 0; col < 16; col++) {
                boolean inForbiddenZone = row <= 3 && col <= 3 || row <= 3 && col >= 12;

                if (!inForbiddenZone) {
                    colors[currentPixel] = image.getRGB(col, row);
                    currentPixel++;
                }
            }
        }

        return colors;
    }

    public static byte[] createWhitePixelImageForTorsoModels(@Range(from = 0,to = 351) int pixel) throws IOException {
        BufferedImage image = new BufferedImage(24, 16, BufferedImage.TYPE_INT_ARGB);

        int x, y;
        int currentPixel = 0;

        for (int row = 0; row < 16; row++) {
            for (int col = 0; col < 24; col++) {
                boolean inForbiddenZone = row <= 3 && col <= 3 || row <= 3 && col >= 20;

                if (!inForbiddenZone) {
                    if (currentPixel == pixel) {
                        x = col;
                        y = row;
                        image.setRGB(x, y, 0xFFFFFFFF);

                        return getImageBytes(image);
                    }
                    currentPixel++;
                }
            }
        }

        // Should never happen
        throw new IllegalArgumentException("Invalid pixel index: " + pixel);
    }

    public static int[] extractPixelColorsFromTorsoModel(BufferedImage image) {
        if (image.getWidth() != 24 || image.getHeight() != 16) {
            throw new IllegalArgumentException("Image must be 24x16 pixels");
        }

        int[] colors = new int[352]; // Maximum 352 valid pixels
        int currentPixel = 0;

        for (int row = 0; row < 16; row++) {
            for (int col = 0; col < 24; col++) {
                boolean inForbiddenZone = row <= 3 && col <= 3 || row <= 3 && col >= 20;

                if (!inForbiddenZone) {
                    colors[currentPixel] = image.getRGB(col, row);
                    currentPixel++;
                }
            }
        }

        return colors;
    }

    public static byte[] createWhitePixelImageForHeadModels(@Range(from = 0,to = 383) int pixel) throws IOException {
        BufferedImage image = new BufferedImage(32, 16, BufferedImage.TYPE_INT_ARGB);

        int x, y;
        int currentPixel = 0;

        for (int row = 0; row < 16; row++) {
            for (int col = 0; col < 32; col++) {
                boolean inForbiddenZone = row <= 7 && col <= 7 || row <= 7 && col >= 24;

                if (!inForbiddenZone) {
                    if (currentPixel == pixel) {
                        x = col;
                        y = row;
                        image.setRGB(x, y, 0xFFFFFFFF);

                        return getImageBytes(image);
                    }
                    currentPixel++;
                }
            }
        }

        // Should never happen
        throw new IllegalArgumentException("Invalid pixel index: " + pixel);
    }

    public static int[] extractPixelColorsFromHeadModel(BufferedImage image) {
        if (image.getWidth() != 32 || image.getHeight() != 16) {
            throw new IllegalArgumentException("Image must be 32x16 pixels");
        }

        int[] colors = new int[384]; // Maximum 384 valid pixels
        int currentPixel = 0;

        for (int row = 0; row < 16; row++) {
            for (int col = 0; col < 32; col++) {
                boolean inForbiddenZone = row <= 7 && col <= 7 || row <= 7 && col >= 24;

                if (!inForbiddenZone) {
                    colors[currentPixel] = image.getRGB(col, row);
                    currentPixel++;
                }
            }
        }

        return colors;
    }

    public static byte[] getImageBytes(BufferedImage image) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, "png", baos);
        } catch (IOException e) {
            return new byte[0];
        }
        return baos.toByteArray();
    }

    public static byte[] getImageBytesFromBase64(String base64String) {
        String base64Image = base64String.substring(base64String.indexOf(",") + 1);
        return java.util.Base64.getDecoder().decode(base64Image);
    }
}
