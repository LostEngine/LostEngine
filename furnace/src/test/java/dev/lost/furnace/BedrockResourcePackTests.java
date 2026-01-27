package dev.lost.furnace;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.lost.furnace.files.manifest.Manifest;
import dev.lost.furnace.files.texture.Texture;
import dev.lost.furnace.files.unknown.UnknownFile;
import dev.lost.furnace.resourcepack.BedrockResourcePack;
import dev.lost.furnace.resourcepackbuilder.ResourcePackBuilder;
import org.junit.jupiter.api.*;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class BedrockResourcePackTests {

    @BeforeEach
    void logTestStart(TestInfo testInfo) {
        System.out.println("----------------------------------------");
        System.out.println("Executing test: " + testInfo.getDisplayName());
        System.out.println("----------------------------------------");
    }

    @AfterAll
    static void logTestComplete() {
        System.out.println("----------------------------------------");
        System.out.println("All tests completed.");
        System.out.println("----------------------------------------");
    }

    @Order(1)
    @Test
    void testNoCompressionPackBuild() throws Exception {
        System.out.println("Creating resource pack with NO_COMPRESSION...");
        BedrockResourcePack pack = BedrockResourcePack.resourcePack();
        pack.manifest("Test Pack", "Test Description");

        JsonObject jo = new JsonObject();
        jo.addProperty("hello", "world");
        pack.jsonFile("texts/example.json", jo);

        byte[] png = pngBytes(4, 4);
        pack.texture(Texture.bytes("textures/example.png", png));

        UnknownFile txt = UnknownFile.utf8("extras/readme.txt", "Hello Furnace!");
        pack.unknownFile(txt);

        File tmpPack = Files.createTempFile("pack", ".mcpack").toFile();
        try {
            System.out.println("Building resource pack to: " + tmpPack.getAbsolutePath());
            pack.build(tmpPack, ResourcePackBuilder.BuildOptions.NO_COMPRESSION);

            System.out.println("Verifying built resource pack files...");
            try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(tmpPack.toPath()))) {
                Set<String> names = new HashSet<>();
                ZipEntry ze;
                while ((ze = zis.getNextEntry()) != null) {
                    names.add(ze.getName());
                }
                assertTrue(names.contains("manifest.json"));
                assertTrue(names.contains("texts/example.json"));
                assertTrue(names.contains("textures/example.png"));
                assertTrue(names.contains("extras/readme.txt"));
            }

            System.out.println("Verifying built resource pack file contents...");
            byte[] builtPng = readZipEntry(tmpPack, "textures/example.png");
            assertNotNull(builtPng);
            assertArrayEquals(png, builtPng, "PNG bytes should be stored unchanged with NO_COMPRESSION");

            byte[] builtTxtBytes = readZipEntry(tmpPack, "extras/readme.txt");
            assertNotNull(builtTxtBytes);
            String builtTxt = new String(builtTxtBytes);
            assertEquals("Hello Furnace!", builtTxt);
            System.out.println("Everything looks good!");
        } finally {
            //noinspection ResultOfMethodCallIgnored
            tmpPack.delete();
        }
    }

    @Order(2)
    @Test
    void testResourcePackCompression() throws Exception {
        System.out.println("Creating resource pack...");
        BedrockResourcePack pack = BedrockResourcePack.resourcePack();
        pack.manifest(Manifest.manifest("Test Pack", "Test Description"));
        pack.jsonFile("texts/example.json", JsonParser.parseString("{\"test\": true}"));
        pack.unknownFile(UnknownFile.utf8("texts/test.txt", "Just some text " + UUID.randomUUID()));
        pack.texture(Texture.bytes("test.png", pngBytes(512, 512)));

        File tmpPackMaxCompressed = Files.createTempFile("pack-max-compressed", ".mcpack").toFile();
        File tmpPackCompressed = Files.createTempFile("pack-compressed", ".mcpack").toFile();
        File tmpPackUncompressed = Files.createTempFile("pack-uncompressed", ".mcpack").toFile();
        try {
            System.out.println("Building resource pack with MAX_COMPRESSION option...");
            pack.build(tmpPackMaxCompressed, ResourcePackBuilder.BuildOptions.MAX_COMPRESSION);
            System.out.println("Building resource pack with COMPRESSED option...");
            pack.build(tmpPackCompressed, ResourcePackBuilder.BuildOptions.COMPRESSED);
            System.out.println("Building resource pack with NO_COMPRESSION option...");
            pack.build(tmpPackUncompressed, ResourcePackBuilder.BuildOptions.NO_COMPRESSION);
            long sizeMaxCompressed = Files.size(tmpPackMaxCompressed.toPath());
            long sizeCompressed = Files.size(tmpPackCompressed.toPath());
            long sizeUncompressed = Files.size(tmpPackUncompressed.toPath());
            System.out.println("Max compressed size: " + sizeMaxCompressed);
            System.out.println("Compressed pack size: " + sizeCompressed);
            System.out.println("Uncompressed pack size: " + sizeUncompressed);
            assertTrue(sizeMaxCompressed < sizeCompressed, "Max compressed pack should be smaller than compressed pack");
            assertTrue(sizeCompressed < sizeUncompressed, "Compressed pack should be smaller than uncompressed pack");
        } finally {
            //noinspection ResultOfMethodCallIgnored
            tmpPackCompressed.delete();
        }
    }

    private static byte[] readZipEntry(File zipFile, String entryName) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipFile.toPath()))) {
            ZipEntry ze;
            while ((ze = zis.getNextEntry()) != null) {
                if (entryName.equals(ze.getName())) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    zis.transferTo(baos);
                    return baos.toByteArray();
                }
            }
        }
        fail("Entry not found: " + entryName);
        return null;
    }

    private static byte[] pngBytes(int width, int height) throws IOException {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        // 16 random colors + 1 transparent
        Color[] colors = new Color[17];
        for (int i = 0; i < 16; i++) {
            colors[i] = new Color(
                    (int) (Math.random() * 256),
                    (int) (Math.random() * 256),
                    (int) (Math.random() * 256),
                    255
            );
        }
        colors[16] = new Color(0, 0, 0, 0);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Color color = colors[(int) (Math.random() * colors.length)];
                img.setRGB(x, y, color.getRGB());
            }
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", baos);
        return baos.toByteArray();
    }

}
